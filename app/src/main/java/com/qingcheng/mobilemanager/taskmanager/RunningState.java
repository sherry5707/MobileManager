package com.qingcheng.mobilemanager.taskmanager;

import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageItemInfo;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.Debug;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.util.Log;
import android.util.SparseArray;

import com.qingcheng.mobilemanager.utils.LogUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;



/**
 * Created by cunhuan.liu on 17/2/8.
 */
public class RunningState {

    //instance
    static Object sGlobalLock = new Object();
    static RunningState sInstance;


    private final Context mApplicationContext;
    private final ActivityManager mAm;
    private final PackageManager mPm;
    // activity status
    private boolean mResumed;

    int mSequence = 0;

    private Locale mLastLocale;


    private final HandlerThread mBackgroundThread;
    final BackgroundHandler mBackgroundHandler;
    // ----- following protected by mLock -----

    // Lock for protecting the state that will be shared between the
    // background update thread and the UI thread.
    public final Object mLock = new Object();

    ArrayList<BaseItem> mItems = new ArrayList<BaseItem>();
    ArrayList<MergedItem> mMergedItems = new ArrayList<MergedItem>();
    ArrayList<MergedItem> mBackgroundItems = new ArrayList<MergedItem>();

    int mNumBackgroundProcesses;
    long mBackgroundProcessMemory;
    int mNumForegroundProcesses;
    long mForegroundProcessMemory;
    int mNumServiceProcesses;
    long mServiceProcessMemory;

    boolean mHaveData;
    boolean mWatchingBackgroundItems;



    //handle
    static final int MSG_UPDATE_CONTENTS = 1;
    static final int MSG_REFRESH_UI = 2;
    static final int MSG_UPDATE_TIME = 3;
    static final long TIME_UPDATE_DELAY = 1000;
    static final long CONTENTS_UPDATE_DELAY = 2000;

    static final int MAX_SERVICES = 100;


    OnRefreshUiListener mRefreshUiListener;

    public static RunningState getInstance(Context baseContext) {
        LogUtil.i();
        synchronized (sGlobalLock) {
            if (sInstance == null) {
                sInstance = new RunningState(baseContext);
            }
            return sInstance;
        }
    }

    private RunningState(Context context) {
        mApplicationContext = context.getApplicationContext();
        ///////////////////////////////
        mAm = (ActivityManager) mApplicationContext.getSystemService(Context.ACTIVITY_SERVICE);
        mPm = mApplicationContext.getPackageManager();
        mResumed = false;
        /////////
        mBackgroundThread = new HandlerThread("RunningState:Background");
        mBackgroundThread.start();
        mBackgroundHandler = new BackgroundHandler(mBackgroundThread.getLooper());
    }


    // ----- BACKGROUND MONITORING THREAD -----
    private class BackgroundHandler extends Handler {
        public BackgroundHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            LogUtil.i();
            switch (msg.what) {
                case MSG_UPDATE_CONTENTS:
                    Message cmd = mHandler.obtainMessage(MSG_REFRESH_UI);
                    cmd.arg1 = update(mApplicationContext, mAm) ? 1 : 0;
                    mHandler.sendMessage(cmd);

                    // scan repeat  2s //////////////////////
                    removeMessages(MSG_UPDATE_CONTENTS);
                    msg = obtainMessage(MSG_UPDATE_CONTENTS);
                    sendMessageDelayed(msg, CONTENTS_UPDATE_DELAY);
                    break;
                default:
                    break;
            }
        }
    }


    //////////////////////refresh ui update data
        final Handler mHandler = new Handler() {
            int mNextUpdate = OnRefreshUiListener.REFRESH_TIME;

            @Override
            public void handleMessage(Message msg) {LogUtil.i();
                LogUtil.i("msg="+msg.what);
                switch (msg.what) {
                    case MSG_REFRESH_UI:
                        mNextUpdate = msg.arg1 != 0 ? OnRefreshUiListener.REFRESH_STRUCTURE
                                : OnRefreshUiListener.REFRESH_DATA;
                        break;
                    case MSG_UPDATE_TIME:
                        synchronized (mLock) {
                            if (!mResumed) {
                                LogUtil.d("No resumed ,return in handler");
                                return;
                            }
                        }
                        removeMessages(MSG_UPDATE_TIME);
                        Message m = obtainMessage(MSG_UPDATE_TIME);
                        sendMessageDelayed(m, TIME_UPDATE_DELAY);
                        LogUtil.d(" Handler MSG_UPDATE_TIME ");
                        if (mRefreshUiListener != null) {
                            LogUtil.d("Call Listener in Handler ");
                            mRefreshUiListener.onRefreshUi(mNextUpdate);
                            mNextUpdate = OnRefreshUiListener.REFRESH_TIME;
                        }
                        break;
                    default:
                        break;
                }
            }
        };


    // ----- DATA STRUCTURES -----
    public void pause() {
        synchronized (mLock) {
            mResumed = false;
            mRefreshUiListener = null;
            mHandler.removeMessages(MSG_UPDATE_TIME);
        }
    }
    public void resume(OnRefreshUiListener listener) {
        LogUtil.i();
        synchronized (mLock) {
            mResumed = true;
            mRefreshUiListener = listener;
            if (!mBackgroundHandler.hasMessages(MSG_UPDATE_CONTENTS)) {
                LogUtil.d("resume send update content");
                mBackgroundHandler.sendEmptyMessage(MSG_UPDATE_CONTENTS);
            }
            mHandler.sendEmptyMessage(MSG_UPDATE_TIME);
        }
    }

    public interface OnRefreshUiListener {
        int REFRESH_TIME = 0;
        int REFRESH_DATA = 1;
        int REFRESH_STRUCTURE = 2;

        void onRefreshUi(int what);
    }
    public static class BaseItem {
        final boolean mIsProcess;

        public PackageItemInfo mPackageInfo;
        String mLabel;
        int mCurSeq;

        public long mActiveSince;
        public long mSize;
        public String mSizeStr;
        public String mCurSizeStr;
        boolean mNeedDivider;
        public boolean mBackground;
        public CharSequence mDisplayLabel;

        public BaseItem(boolean isProcess) {
            mIsProcess = isProcess;
        }
    }
    public static class ServiceItem extends BaseItem {
        ActivityManager.RunningServiceInfo mRunningService;
        ServiceInfo mServiceInfo;
        boolean mShownAsStarted;

        MergedItem mMergedItem;

        public ServiceItem() {
            super(false);
        }
    }

    public static class ProcessItem extends BaseItem {

        final HashMap<ComponentName, ServiceItem> mServices = new HashMap<ComponentName, ServiceItem>();
        final SparseArray<ProcessItem> mDependentProcesses = new SparseArray<ProcessItem>();

        final int mUid;
        public final String mProcessName;
        int mPid;

        ProcessItem mClient;
        int mLastNumDependentProcesses;

        int mRunningSeq;
        ActivityManager.RunningAppProcessInfo mRunningProcessInfo;

        MergedItem mMergedItem;

        // Purely for sorting.
        boolean mIsSystem;
        static boolean mIsStarted;
        long mActiveSince;

        public ProcessItem(Context context, int uid, String processName) {
            super(true);
            mUid = uid;
            mProcessName = processName;
        }
        boolean updateService(Context context,ActivityManager.RunningServiceInfo service) {
            final PackageManager pm = context.getPackageManager();
            boolean changed = false;
            ServiceItem si = mServices.get(service.service);
            if (si == null) {
                changed = true;
                si = new ServiceItem();
                si.mRunningService = service;
                try {
                    si.mServiceInfo = pm.getServiceInfo(service.service, 0);
                } catch (PackageManager.NameNotFoundException e) {
                    LogUtil.d("catch exception, process name not found");
                }
                si.mDisplayLabel = makeLabel(pm, si.mRunningService.service
                        .getClassName(), si.mServiceInfo);
                mLabel = mDisplayLabel != null ? mDisplayLabel.toString()
                        : null;
                si.mPackageInfo = si.mServiceInfo.applicationInfo;
                mServices.put(service.service, si);
            }

            si.mCurSeq = mCurSeq;
            si.mRunningService = service;
            long activeSince = service.restarting == 0 ? service.activeSince
                    : -1;
            if (si.mActiveSince != activeSince) {
                si.mActiveSince = activeSince;
                changed = true;
            }
            if (service.clientPackage != null && service.clientLabel != 0) {
                if (si.mShownAsStarted) {
                    si.mShownAsStarted = false;
                    changed = true;
                }
            } else {
                if (!si.mShownAsStarted) {
                    si.mShownAsStarted = true;
                    changed = true;
                }
            }

            return changed;
        }
        public void ensureLabel(PackageManager pm) {
            if (mLabel != null) {
                return;
            }
            try {
                ApplicationInfo ai = pm.getApplicationInfo(mProcessName, 0);
                if (ai.uid == mUid) {
                    mDisplayLabel = ai.loadLabel(pm);
                    mLabel = mDisplayLabel.toString();
                    mPackageInfo = ai;
                    return;
                }
            } catch (PackageManager.NameNotFoundException e) {
               LogUtil.d("catch exception, process name not found");
            }

            // If we couldn't get information about the overall
            // process, try to find something about the uid.
            String[] pkgs = pm.getPackagesForUid(mUid);

            //No such packages or had been removed.
            if (pkgs == null) {
                return;
            }
            // If there is one package with this uid, that is what we want.
            if (pkgs.length == 1) {
                try {
                    ApplicationInfo ai = pm.getApplicationInfo(pkgs[0], 0);
                    mDisplayLabel = ai.loadLabel(pm);
                    mLabel = mDisplayLabel.toString();
                    mPackageInfo = ai;
                    return;
                } catch (PackageManager.NameNotFoundException e) {
                   LogUtil.d("catch exception, process name not found");
                }
            }
            // If there are multiple, see if one gives us the official name
            // for this uid.
            for (String name : pkgs) {
                try {
                    PackageInfo pi = pm.getPackageInfo(name, 0);
                    if (pi.sharedUserLabel != 0) {
                        CharSequence nm = pm.getText(name, pi.sharedUserLabel,
                                pi.applicationInfo);
                        if (nm != null) {
                            mDisplayLabel = nm;
                            mLabel = nm.toString();
                            mPackageInfo = pi.applicationInfo;
                            return;
                        }
                    }
                } catch (PackageManager.NameNotFoundException e) {
                    LogUtil.d("catch exception, process name not found");
                }
            }
            // If still don't have anything to display, just use the
            // service info.
            if (mServices.size() > 0) {
                mPackageInfo = mServices.values().iterator().next().mServiceInfo.applicationInfo;
                mDisplayLabel = mPackageInfo.loadLabel(pm);
                mLabel = mDisplayLabel.toString();
                return;
            }
            // Finally... whatever, just pick the first package's name.
            try {
                ApplicationInfo ai = pm.getApplicationInfo(pkgs[0], 0);
                mDisplayLabel = ai.loadLabel(pm);
                mLabel = mDisplayLabel.toString();
                mPackageInfo = ai;
                return;
            } catch (PackageManager.NameNotFoundException e) {
               LogUtil.d("catch exception, process name not found");
            }
        }
        boolean updateSize(Context context, Debug.MemoryInfo mem, int curSeq) {
            mSize = ((long) mem.getTotalPrivateDirty() * 1024);
            if (mCurSeq == curSeq) {
                String sizeStr = Formatter.formatShortFileSize(context, mSize);
                if (!sizeStr.equals(mSizeStr)) {
                    mSizeStr = sizeStr;
                    // We update this on the second tick where we update just
                    // the text in the current items, so no need to say we
                    // changed here.
                    return false;
                }
            }
            return false;
        }
        boolean buildDependencyChain(Context context, PackageManager pm,
                                     int curSeq) {
            final int np = mDependentProcesses.size();
            boolean changed = false;
            for (int i = 0; i < np; i++) {
                ProcessItem proc = mDependentProcesses.valueAt(i);
                if (proc.mClient != this) {
                    changed = true;
                    proc.mClient = this;
                }
                proc.mCurSeq = curSeq;
                proc.ensureLabel(pm);
                changed |= proc.buildDependencyChain(context, pm, curSeq);
            }

            if (mLastNumDependentProcesses != mDependentProcesses.size()) {
                changed = true;
                mLastNumDependentProcesses = mDependentProcesses.size();
            }

            return changed;
        }
        void addDependentProcesses(ArrayList<BaseItem> dest,
                                   ArrayList<ProcessItem> destProc) {
            final int np = mDependentProcesses.size();
            for (int i = 0; i < np; i++) {
                ProcessItem proc = mDependentProcesses.valueAt(i);
                proc.addDependentProcesses(dest, destProc);
                dest.add(proc);
                if (proc.mPid > 0) {
                    destProc.add(proc);
                }
            }
        }
    }
    public static class MergedItem extends BaseItem {
        public ProcessItem mProcess;
        final ArrayList<ProcessItem> mOtherProcesses = new ArrayList<ProcessItem>();
        final ArrayList<ServiceItem> mServices = new ArrayList<ServiceItem>();

        private int mLastNumProcesses = -1;
        private int mLastNumServices = -1;

        MergedItem() {
            super(false);
        }
        boolean update(Context context, boolean background) {
            mPackageInfo = mProcess.mPackageInfo;
            if (TextUtils.isEmpty(mProcess.mDisplayLabel)) {
                mDisplayLabel = mProcess.mProcessName;
            } else {
                mDisplayLabel = mProcess.mDisplayLabel;
            }
            mLabel = mProcess.mLabel;
            mBackground = background;

            if (!mBackground) {
                int numProcesses = (mProcess.mPid > 0 ? 1 : 0)
                        + mOtherProcesses.size();
                int numServices = mServices.size();
                if (mLastNumProcesses != numProcesses
                        || mLastNumServices != numServices) {
                    mLastNumProcesses = numProcesses;
                    mLastNumServices = numServices;
                }
            }

            mActiveSince = -1;
            for (int i = 0; i < mServices.size(); i++) {
                ServiceItem si = mServices.get(i);
                if (si.mActiveSince >= 0 && mActiveSince < si.mActiveSince) {
                    mActiveSince = si.mActiveSince;
                }
            }

            return false;
        }
        boolean updateSize(Context context) {
            mSize = mProcess.mSize;
            for (int i = 0; i < mOtherProcesses.size(); i++) {
                mSize += mOtherProcesses.get(i).mSize;
            }
            String sizeStr = Formatter.formatShortFileSize(context, mSize);
            if (!sizeStr.equals(mSizeStr)) {
                mSizeStr = sizeStr;
                // We update this on the second tick where we update just
                // the text in the current items, so no need to say we
                // changed here.
                return false;
            }
            return false;
        }

    }
    static CharSequence makeLabel(PackageManager pm, String className,
                                  PackageItemInfo item) {
        if (item != null
                && (item.labelRes != 0 || item.nonLocalizedLabel != null)) {
            CharSequence label = item.loadLabel(pm);
            if (label != null) {
                return label;
            }
        }

        String label = className;
        int tail = label.lastIndexOf('.');
        if (tail >= 0) {
            label = label.substring(tail + 1, label.length());
        }
        return label;
    }
    private boolean isInterestingProcess( ActivityManager.RunningAppProcessInfo pi) {
        if ((pi.flags & ActivityManager.RunningAppProcessInfo.FLAG_CANT_SAVE_STATE) != 0) {
            return true;
        }
        if ((pi.flags & ActivityManager.RunningAppProcessInfo.FLAG_PERSISTENT) == 0
                && pi.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
                && pi.importanceReasonCode == ActivityManager.RunningAppProcessInfo.REASON_UNKNOWN) {
            return true;
        }
        return false;
    }

    static class ServiceProcessComparator implements Comparator<ProcessItem> {
        public int compare(ProcessItem object1, ProcessItem object2) {
            if (object1.mIsStarted != object2.mIsStarted) {
                // Non-started processes go last.
                return object1.mIsStarted ? -1 : 1;
            }
            if (object1.mIsSystem != object2.mIsSystem) {
                // System processes go below non-system.
                return object1.mIsSystem ? 1 : -1;
            }
            if (object1.mActiveSince != object2.mActiveSince) {
                // Remaining ones are sorted with the longest running
                // services last.
                return (object1.mActiveSince > object2.mActiveSince) ? -1 : 1;
            }
            return 0;
        }
    }
    //////////////////////////////////////////////////////////////
    // Processes that are hosting a service we are interested in, organized
    // by uid and name. Note that this mapping does not change even across
    // service restarts, and during a restart there will still be a process
    // entry.
    final SparseArray<HashMap<String, ProcessItem>> mServiceProcessesByName
            = new SparseArray<HashMap<String, ProcessItem>>();

    // Processes that are hosting a service we are interested in, organized
    // by their pid. These disappear and re-appear as services are restarted.
    final SparseArray<ProcessItem> mServiceProcessesByPid = new SparseArray<ProcessItem>();

    // Used to sort the interesting processes.
    final ServiceProcessComparator mServiceProcessComparator = new ServiceProcessComparator();

    // Additional interesting processes to be shown to the user, even if
    // there is no service running in them.
    final ArrayList<ProcessItem> mInterestingProcesses = new ArrayList<ProcessItem>();

    // All currently running processes, for finding dependencies etc.
    final SparseArray<ProcessItem> mRunningProcesses = new SparseArray<ProcessItem>();

    // The processes associated with services, in sorted order.
    final ArrayList<ProcessItem> mProcessItems = new ArrayList<ProcessItem>();

    // All processes, used for retrieving memory information.
    final ArrayList<ProcessItem> mAllProcessItems = new ArrayList<ProcessItem>();

    private boolean update(Context context, ActivityManager am) {
        LogUtil.i();
        mSequence++;
        boolean changed = false;

        changed |= updateServiceProcess(context, am);
        changed |= updateRunningProcess(context, am);
        changed |= removeDeadProcess(context, am);
        if (changed) {
            updateDisplayProcess(context);
        }
        changed |= updateMemoryInfo(context);

        return changed;
    }
    private boolean updateServiceProcess(Context context, ActivityManager am) {
        boolean changed = false;
        LogUtil.i();
        mServiceProcessesByName.clear();
        mServiceProcessesByPid.clear();
        mAllProcessItems.clear();

        List<ActivityManager.RunningServiceInfo> services = am.getRunningServices(MAX_SERVICES);
        LogUtil.d("getRunningServices  size"+services.size());
        Iterator<ActivityManager.RunningServiceInfo> iterator = services.iterator();
        while (iterator.hasNext()){

            ActivityManager.RunningServiceInfo si = iterator.next();
            // We are not interested in services that have not been started
            // and don't have a known client, because
            // there is nothing the user can do about them.
            if (!si.started && si.clientLabel == 0) {
                continue;
            }
            // We likewise don't care about services running in a
            // persistent process like the system or phone.
            if ((si.flags & ActivityManager.RunningServiceInfo.FLAG_PERSISTENT_PROCESS) != 0) {
                continue;
            }
            HashMap<String, ProcessItem> procs = mServiceProcessesByName.get(si.uid);
            if (procs == null) {
                procs = new HashMap<String, ProcessItem>();
                mServiceProcessesByName.put(si.uid, procs);

            }


            LogUtil.d("mServiceProcessesByName  "+mServiceProcessesByName.get(si.uid));


            ProcessItem proc = procs.get(si.process);
            if (proc == null) {
                changed = true;
                proc = new ProcessItem(context, si.uid, si.process);
                procs.put(si.process, proc);
            }
            if (proc.mCurSeq != mSequence) {
                int pid = si.restarting == 0 ? si.pid : 0;
                if (pid != proc.mPid) {
                    changed = true;
                    if (proc.mPid != 0) {
                        mServiceProcessesByPid.remove(proc.mPid);
                    }
                    if (pid != 0) {
                        mServiceProcessesByPid.put(pid, proc);
                    }
                    proc.mPid = pid;

                }
                proc.mDependentProcesses.clear();
                proc.mCurSeq = mSequence;

            }

            changed |= proc.updateService(context, si);

        }
        return changed;
    }

    private boolean updateRunningProcess(Context context, ActivityManager am) {
        boolean changed = false;
        final PackageManager pm = context.getPackageManager();

        LogUtil.i();
        mInterestingProcesses.clear();
        mRunningProcesses.clear();

        List<ActivityManager.RunningAppProcessInfo> processes = am.getRunningAppProcesses();

        LogUtil.d("RunningAppProcess  size"+processes.size());
        Iterator<ActivityManager.RunningAppProcessInfo> iterator = processes.iterator();
        while (iterator.hasNext()){
            ActivityManager.RunningAppProcessInfo pi =  iterator.next();

            ProcessItem proc = mServiceProcessesByPid.get(pi.pid);
            if (proc == null) {
                // This process is not one that is a direct container
                // of a service, so look for it in the secondary
                // running list.
                proc = mRunningProcesses.get(pi.pid);
                if (proc == null) {
                    changed = true;
                    proc = new ProcessItem(context, pi.uid, pi.processName);
                    proc.mPid = pi.pid;
                    mRunningProcesses.put(pi.pid, proc);
                }
                proc.mDependentProcesses.clear();
            }
            if (isInterestingProcess(pi)) {
                if (!mInterestingProcesses.contains(proc)) {
                    changed = true;
                    mInterestingProcesses.add(proc);
                }
                proc.mCurSeq = mSequence;
                proc.ensureLabel(pm);
            }

            proc.mRunningSeq = mSequence;
            proc.mRunningProcessInfo = pi;
        }
        // Build the chains from client processes to the process they are
        // dependent on; also remove any old running processes.
        int nrp = mRunningProcesses.size();
        for (int i = 0; i < nrp; i++) {
            ProcessItem proc = mRunningProcesses.valueAt(i);
            if (proc != null && proc.mRunningSeq == mSequence) {
                int clientPid = proc.mRunningProcessInfo.importanceReasonPid;
                if (clientPid != 0) {
                    ProcessItem client = mServiceProcessesByPid.get(clientPid);
                    if (client == null) {
                        client = mRunningProcesses.get(clientPid);
                    }
                    if (client != null) {
                        client.mDependentProcesses.put(proc.mPid, proc);
                    }
                } else {
                    // In this pass the process doesn't have a client.
                    // Clear to make sure that, if it later gets the same one,
                    // we will detect the change.
                    proc.mClient = null;
                }
            } else {
                changed = true;
                mRunningProcesses.remove(mRunningProcesses.keyAt(i));
            }
        }
        // Remove any old interesting processes.
        int nhp = mInterestingProcesses.size();
        for (int i = 0; i < nhp; i++) {
            ProcessItem proc = mInterestingProcesses.get(i);
            if (mRunningProcesses.get(proc.mPid) == null) {
                changed = true;
                mInterestingProcesses.remove(i);
                i--;
                nhp--;
            }
        }
        return changed;
    }
    private boolean removeDeadProcess(Context context, ActivityManager am) {
        LogUtil.i();
        // Follow the tree from all primary service processes to all
        // processes they are dependent on, marking these processes as
        // still being active and determining if anything has changed.
        boolean changed = false;
        final PackageManager pm = context.getPackageManager();
        final int nap = mServiceProcessesByPid.size();
        for (int i = 0; i < nap; i++) {
            ProcessItem proc = mServiceProcessesByPid.valueAt(i);
            if (proc.mCurSeq == mSequence) {
                changed |= proc.buildDependencyChain(context, pm, mSequence);
            }
        }
        // Look for services and their primary processes that no longer exist...
        for (int i = 0; i < mServiceProcessesByName.size(); i++) {
            HashMap<String, ProcessItem> procs = mServiceProcessesByName
                    .valueAt(i);
            Iterator<ProcessItem> pit = procs.values().iterator();
            while (pit.hasNext()) {
                ProcessItem pi = pit.next();
                if (pi.mCurSeq == mSequence) {
                    pi.ensureLabel(pm);
                    if (pi.mPid == 0) {
                        // Sanity: a non-process can't be dependent on
                        // anything.
                        pi.mDependentProcesses.clear();
                    }
                } else {
                    changed = true;
                    pit.remove();
                    if (procs.size() == 0) {
                        mServiceProcessesByName.remove(mServiceProcessesByName
                                .keyAt(i));
                    }
                    if (pi.mPid != 0) {
                        mServiceProcessesByPid.remove(pi.mPid);
                    }
                    continue;
                }
                Iterator<ServiceItem> sit = pi.mServices.values().iterator();
                while (sit.hasNext()) {
                    ServiceItem si = sit.next();
                    if (si.mCurSeq != mSequence) {
                        changed = true;
                        sit.remove();
                    }
                }
            }
        }
        return changed;

    }
    private void updateDisplayProcess(Context context) {LogUtil.i();
        ArrayList<ProcessItem> sortedProcesses = new ArrayList<ProcessItem>();

        for (int i = 0; i < mServiceProcessesByName.size(); i++) {
            for (ProcessItem pi : mServiceProcessesByName.valueAt(i).values()) {
                pi.mIsSystem = false;
                pi.mIsStarted = true;
                pi.mActiveSince = Long.MAX_VALUE;
                for (ServiceItem si : pi.mServices.values()) {
                    if (si.mServiceInfo != null && (si.mServiceInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
                        pi.mIsSystem = true;
                    }
                    if (si.mRunningService != null && si.mRunningService.clientLabel != 0) {
                        pi.mIsStarted = false;
                        if (pi.mActiveSince > si.mRunningService.activeSince) {
                            pi.mActiveSince = si.mRunningService.activeSince;
                        }
                    }
                }
                sortedProcesses.add(pi);
            }
        }

        Collections.sort(sortedProcesses, mServiceProcessComparator);

        ArrayList<BaseItem> newItems = new ArrayList<BaseItem>();
        ArrayList<MergedItem> newMergedItems = new ArrayList<MergedItem>();
        mProcessItems.clear();
        for (int i = 0; i < sortedProcesses.size(); i++) {
            ProcessItem pi = sortedProcesses.get(i);
            pi.mNeedDivider = false;

            int firstProc = mProcessItems.size();
            // First add processes we are dependent on.
            pi.addDependentProcesses(newItems, mProcessItems);
            // And add the process itself.
            newItems.add(pi);
            if (pi.mPid > 0) {
                mProcessItems.add(pi);
            }

            // Now add the services running in it.
            MergedItem mergedItem = null;
            boolean haveAllMerged = false;
            boolean needDivider = false;
            for (ServiceItem si : pi.mServices.values()) {
                si.mNeedDivider = needDivider;
                needDivider = true;
                newItems.add(si);
                if (si.mMergedItem != null) {
                    if (mergedItem != null && mergedItem != si.mMergedItem) {
                        haveAllMerged = false;
                    }
                    mergedItem = si.mMergedItem;
                } else {
                    haveAllMerged = false;
                }
            }

            if (!haveAllMerged || mergedItem == null
                    || mergedItem.mServices.size() != pi.mServices.size()) {
                // Whoops, we need to build a new MergedItem!
                mergedItem = new MergedItem();
                for (ServiceItem si : pi.mServices.values()) {
                    mergedItem.mServices.add(si);
                    si.mMergedItem = mergedItem;
                }
                mergedItem.mProcess = pi;
                mergedItem.mOtherProcesses.clear();
                for (int mpi = firstProc; mpi < (mProcessItems.size() - 1); mpi++) {
                    mergedItem.mOtherProcesses.add(mProcessItems.get(mpi));
                }
            }
            mergedItem.update(context, false);
            newMergedItems.add(mergedItem);
        }

        // Finally, interesting processes need to be shown and will
        // go at the top.
        int nhp = mInterestingProcesses.size();
        for (int i = 0; i < nhp; i++) {
            ProcessItem proc = mInterestingProcesses.get(i);
            if (proc.mClient == null && proc.mServices.size() <= 0) {
                if (proc.mMergedItem == null) {
                    proc.mMergedItem = new MergedItem();
                    proc.mMergedItem.mProcess = proc;
                }
                proc.mMergedItem.update(context, false);
                newMergedItems.add(0, proc.mMergedItem);
                mProcessItems.add(proc);
            }
        }

        synchronized (mLock) {
            mItems = newItems;
            mMergedItems = newMergedItems;
        }
    }
    private boolean updateMemoryInfo(Context context) {LogUtil.i();
        mAllProcessItems.clear();
        mAllProcessItems.addAll(mProcessItems);
        boolean changed = false;
        int numBackgroundProcesses = 0;
        int numForegroundProcesses = 0;
        int numServiceProcesses = 0;
        int nrp = mRunningProcesses.size();
        for (int i = 0; i < nrp; i++) {
            ProcessItem proc = mRunningProcesses.valueAt(i);
            if (proc.mCurSeq != mSequence) {
                // We didn't hit this process as a dependency on one
                // of our active ones, so add it up if needed.
                if (proc.mRunningProcessInfo.importance >= ActivityManager.RunningAppProcessInfo.IMPORTANCE_BACKGROUND) {
                    numBackgroundProcesses++;
                    mAllProcessItems.add(proc);
                } else if (proc.mRunningProcessInfo.importance <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE) {
                    numForegroundProcesses++;
                    mAllProcessItems.add(proc);
                } else {
                   LogUtil.d("Unknown non-service process: "
                            + proc.mProcessName + " #" + proc.mPid);
                }
            } else {
                numServiceProcesses++;
            }
        }


        long backgroundProcessMemory = 0;
        long foregroundProcessMemory = 0;
        long serviceProcessMemory = 0;


        ArrayList<MergedItem> newBackgroundItems = null;
        try {
            final int numProc = mAllProcessItems.size();
            int[] pids = new int[numProc];
            for (int i = 0; i < numProc; i++) {
                pids[i] = mAllProcessItems.get(i).mPid;
            }
            Debug.MemoryInfo[] mem = ActivityManagerNative.getDefault()
                    .getProcessMemoryInfo(pids);
            int bgIndex = 0;
            for (int i = 0; i < pids.length; i++) {
                ProcessItem proc = mAllProcessItems.get(i);
                changed |= proc.updateSize(context, mem[i], mSequence);
                if (proc.mCurSeq == mSequence) {
                    serviceProcessMemory += proc.mSize;
                } else if (proc.mRunningProcessInfo.importance >=
                        ActivityManager.RunningAppProcessInfo.IMPORTANCE_BACKGROUND) {
                    backgroundProcessMemory += proc.mSize;
                    MergedItem mergedItem;
                    if (newBackgroundItems != null) {
                        proc.mMergedItem = new MergedItem();
                        mergedItem = proc.mMergedItem;
                        proc.mMergedItem.mProcess = proc;
                        newBackgroundItems.add(mergedItem);
                    } else {
                        if (bgIndex >= mBackgroundItems.size()
                                || mBackgroundItems.get(bgIndex).mProcess != proc) {
                            newBackgroundItems = new ArrayList<MergedItem>(
                                    numBackgroundProcesses);
                            for (int bgi = 0; bgi < bgIndex; bgi++) {
                                newBackgroundItems.add(mBackgroundItems
                                        .get(bgi));
                            }
                            proc.mMergedItem = new MergedItem();
                            mergedItem = proc.mMergedItem;
                            proc.mMergedItem.mProcess = proc;
                            newBackgroundItems.add(mergedItem);
                        } else {
                            mergedItem = mBackgroundItems.get(bgIndex);
                        }
                    }
                    mergedItem.update(context, true);
                    mergedItem.updateSize(context);
                    bgIndex++;
                } else if (proc.mRunningProcessInfo.importance <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE) {
                    foregroundProcessMemory += proc.mSize;
                }
            }
        } catch (RemoteException e) {
           LogUtil.d("catch remote exception");
        }

        if (newBackgroundItems == null) {
            // One or more at the bottom may no longer exit.
            if (mBackgroundItems.size() > numBackgroundProcesses) {
                newBackgroundItems = new ArrayList<MergedItem>(
                        numBackgroundProcesses);
                for (int bgi = 0; bgi < numBackgroundProcesses; bgi++) {
                    newBackgroundItems.add(mBackgroundItems.get(bgi));
                }
            }
        }

        for (int i = 0; i < mMergedItems.size(); i++) {
            mMergedItems.get(i).updateSize(context);
        }

        synchronized (mLock) {
            mNumBackgroundProcesses = numBackgroundProcesses;
            mNumForegroundProcesses = numForegroundProcesses;
            mNumServiceProcesses = numServiceProcesses;
            mBackgroundProcessMemory = backgroundProcessMemory;
            mForegroundProcessMemory = foregroundProcessMemory;
            mServiceProcessMemory = serviceProcessMemory;
            if (newBackgroundItems != null) {
                mBackgroundItems = newBackgroundItems;
                if (mWatchingBackgroundItems) {
                    changed = true;
                }
            }
            if (!mHaveData) {
                mHaveData = true;
                mLock.notifyAll();
            }
        }
        return changed;

    }
    public boolean hasData() {
        synchronized (mLock) {
            return mHaveData;
        }
    }
    public ArrayList<MergedItem> getCurrentMergedItems() {
        synchronized (mLock) {LogUtil.i();
            return mMergedItems;
        }
    }

    public ArrayList<MergedItem> getCurrentBackgroundItems() {
        synchronized (mLock) {LogUtil.i();
            return mBackgroundItems;
        }
    }
 
}

