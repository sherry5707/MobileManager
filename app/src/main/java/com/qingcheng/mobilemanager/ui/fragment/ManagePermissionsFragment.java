/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.qingcheng.mobilemanager.ui.fragment;

import android.app.AppOpsManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.util.ArraySet;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import com.qingcheng.mobilemanager.R;
import com.qingcheng.mobilemanager.permission.PermissionApps;
import com.qingcheng.mobilemanager.permission.PermissionGroup;
import com.qingcheng.mobilemanager.permission.PermissionGroups;
import com.qingcheng.mobilemanager.utils.LogUtil;
import com.qingcheng.mobilemanager.utils.PermissionUtil;

import java.util.ArrayList;
import java.util.List;

/// M: CTA requirement - permission control

public final class ManagePermissionsFragment extends PermissionsFrameFragment
        implements PermissionGroups.PermissionsGroupsChangeCallback
        , OnPreferenceClickListener {

    private static final String TAG = "ManagePermissionsFragment";
    private static final String OS_PKG = "android";

    private ArraySet<String> mLauncherPkgs;

    private PermissionGroups mPermissions;

    private AppOpsManager mAppOpsManager;

    public static ManagePermissionsFragment newInstance() {
        return new ManagePermissionsFragment();
    }


    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setLoading(true /* loading */, false /* animate */);

        mLauncherPkgs = PermissionUtil.getLauncherPackages(getContext());
        mPermissions = new PermissionGroups(getActivity(), getLoaderManager(), this);
        mAppOpsManager = (AppOpsManager) getContext().getSystemService(Context.APP_OPS_SERVICE);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bindPermissionUi(getActivity(), getView());
    }

    private static void bindPermissionUi(Context context, View rootView) {
        if (context == null || rootView == null) {
            return;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mPermissions.refresh();
        updatePermissionsUi();
    }

    @Override
    public void onPermissionGroupsChanged() {
        updatePermissionsUi();
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        String key = preference.getKey();

        PermissionGroup group = mPermissions.getGroup(key);
        if (group == null) {
            return false;
        }

        Intent intent = new Intent(Intent.ACTION_MANAGE_PERMISSION_APPS)
                .putExtra(Intent.EXTRA_PERMISSION_NAME, key);
        if(group.getmGroupName()!=null) {
            intent.putExtra("REAL_GROUP_NAME", group.getmGroupName());
        }
        try {
            getActivity().startActivity(intent);
        } catch (ActivityNotFoundException e) {
            LogUtil.d("No app to handle " + intent);
        }

        return true;
    }


    private void updatePermissionsUi() {
        final Context context = getActivity();
        if (context == null) {
            return;
        }
        List<PermissionGroup> groups = mPermissions.getGroups();
        PreferenceScreen screen = getPreferenceScreen();
        if (screen == null) {
            screen = getPreferenceManager().createPreferenceScreen(getActivity());
            setPreferenceScreen(screen);
        }
        // Use this to speed up getting the info for all of the PermissionApps below.
        // Create a new one for each refresh to make sure it has fresh data.
        PermissionApps.PmCache cache = new PermissionApps.PmCache(getContext().getPackageManager());

        /// M: CTA requirement - permission control @{
        boolean isSystemPermission = true;
        //  CtaUtils.isPlatformPermissionGroup(group.getDeclaringPackage(), group.getName());
        /// @}

        for (PermissionGroup group : groups) {
            Preference preference = findPreference(group.getName());
            if (preference == null) {
                if(group.getName().equals("com.android.email.permission.READ_ATTACHMENT")){
                    continue;
                }
                //悬浮窗权限（另加）
                if ("APPS_WITH_OVERLAY".equals(group.getName())) {
                    preference = new Preference(context);
                    preference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference preference) {
                            Intent intent=new Intent("com.android.settings.action.SETTINGS.simple.overlaylist");
                            //Intent intent = new Intent("android.settings.action.MANAGE_OVERLAY_PERMISSION");
                            try {
                                getActivity().startActivity(intent);
                            } catch (ActivityNotFoundException e) {
                                LogUtil.d("No app to handle " + intent);
                            }
                            return true;
                        }
                    });
                    preference.setKey(group.getName());
//                    preference.setIcon(PermissionUtil.applyTint(context, group.getIcon(),
//                            android.R.attr.colorControlNormal));
                    preference.setTitle(group.getLabel());
                    // Set blank summary so that no resizing/jumping happens when the summary is loaded.
                    preference.setPersistent(false);

                    PreferenceCategory pc = new PreferenceCategory(context);
                    pc.setTitle(getString(R.string.additional_permissions));
                    pc.setKey(group.getName());
                    screen.addPreference(pc);
                    screen.addPreference(preference);

                } else {
                    Log.i(TAG, "group.mIsCategory:" + group.ismIsCategory());
                    if(!group.ismIsCategory()){
                        preference = new Preference(context);
                        preference.setOnPreferenceClickListener(this);
                        preference.setKey(group.getName());
//                        preference.setIcon(PermissionUtil.applyTint(context, group.getIcon(),
//                                android.R.attr.colorControlNormal));
                        preference.setTitle(group.getLabel());
                        // Set blank summary so that no resizing/jumping happens when the summary is loaded.
                        preference.setSummary(" ");
                        preference.setPersistent(false);
                        //加载summary中应用的个数
                        final PreferenceScreen finalScreen = screen;
                        final Preference finalPreference = preference;
                        final boolean finalIsSystemPermission =isSystemPermission;
                        new PermissionApps(getContext(), group, new PermissionApps.Callback() {
                            @Override
                            public void onPermissionsLoaded(PermissionApps permissionApps, PermissionGroup group) {
                                if (getActivity() == null) {
                                    return;
                                }
                                int granted = permissionApps.getGrantedCount(mLauncherPkgs);
                                int total = permissionApps.getTotalCount(mLauncherPkgs);
                                if (total !=0) {
                                    finalPreference.setSummary(getString(R.string.app_permissions_group_summary,
                                            granted, total));
                                    if (finalPreference != null && finalIsSystemPermission) {
                                        //添加category
                                        Preference preference = findPreference(group.getmGroupName());{
                                            if(preference==null){
                                                preference = new PreferenceCategory(context);
                                                preference.setTitle(group.getmRealGroup().getLabel());
                                                preference.setKey(group.getmGroupName());
                                                if (preference != null) {
                                                    finalScreen.addPreference(preference);
                                                }
                                            }
                                        }
                                        finalScreen.addPreference(finalPreference);
                                    }
                                    return;
                                }
                            }
                        }, cache).refresh(false);
                    }
                }
            }

            //加载summary中应用的个数
            final PreferenceScreen finalScreen = screen;
            if (!"APPS_WITH_OVERLAY".equals(group.getName())) {
                new PermissionApps(getContext(), group, new PermissionApps.Callback() {
                    @Override
                    public void onPermissionsLoaded(PermissionApps permissionApps, PermissionGroup group) {
                        if (getActivity() == null) {
                            return;
                        }
                        int granted = permissionApps.getGrantedCount(mLauncherPkgs);
                        int total = permissionApps.getTotalCount(mLauncherPkgs);
                        Preference preference = findPreference(group.getName());
                        if (total == 0 && !"APPS_WITH_OVERLAY".equals(group.getName())) {
                            if (finalScreen != null && preference != null) {
                                finalScreen.removePreference(preference);
                            } else {
                                Log.i("MPF", "finalScreen==null");
                            }
                            return;
                        }
                        if (total > 0 || granted > 0) {
                            if (group.ismIsCategory() && group.getSubPermissionCount() == 0&& preference != null) {
                                if(preference!=null) {
                                    finalScreen.removePreference(preference);
                                    preference = new Preference(getActivity());
                                    preference.setOnPreferenceClickListener(ManagePermissionsFragment.this);
                                    preference.setKey(group.getName());
//                                    preference.setIcon(PermissionUtil.applyTint(getActivity(), group.getIcon(),
//                                            android.R.attr.colorControlNormal));
                                    preference.setTitle(group.getLabel());
                                    // Set blank summary so that no resizing/jumping happens when the summary is loaded.
                                    preference.setSummary(" ");
                                    preference.setPersistent(false);
                                    finalScreen.addPreference(preference);
                                    preference.setSummary(getString(R.string.app_permissions_group_summary,
                                            granted, total));
                                }
                            }
                        }
                        if (!"APPS_WITH_OVERLAY".equals(group.getName()) && !group.ismIsCategory()&&preference!=null) {
                            preference.setSummary(getString(R.string.app_permissions_group_summary,
                                    granted, total));
                        }
                    }
                }, cache).refresh(false);
            }

            if (screen.getPreferenceCount() != 0) {
                setLoading(false /* loading */, true /* animate */);
            }
        }
    }
}
