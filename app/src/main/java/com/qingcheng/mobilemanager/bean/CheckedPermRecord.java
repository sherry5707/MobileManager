package com.qingcheng.mobilemanager.bean;

public class CheckedPermRecord {
    /** The name to be updated. */
    public final String mPackageName;
    public final String mPermission;
    public final int mUid;
    /** Enabled or not. */
    /** It's ok to use the permission. */
    public static final int STATUS_GRANTED = 0;
    /** Forbidden to use the permission, be care for the error handling. */
    public static final int STATUS_DENIED = 1;
    /** Ask user to grant the permission or not. */
    public static final int STATUS_CHECK = 2;
    public static final int STATUS_FIRST_CHECK = 3;
    private int status = STATUS_FIRST_CHECK;

    public CheckedPermRecord(String _packageName, int _uid,String _permission) {
        mPackageName = _packageName;
        mPermission = _permission;
        mUid=_uid;
    }
    public CheckedPermRecord(String _packageName, int _uid,String _permission, int _status) {
        this(_packageName, _uid,_permission);
        status = _status;
    }

    public CheckedPermRecord(String _packageName, int _uid,String _permission, boolean enable) {
        this(_packageName, _uid,_permission);
        setEnable(enable);
    }


    public CheckedPermRecord(CheckedPermRecord data) {
        this(data.mPackageName, data.mUid,data.mPermission, data.status);
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public boolean isEnable() {
        return status == STATUS_GRANTED;
    }

    public void setEnable(boolean enable) {
        status = enable?STATUS_GRANTED:STATUS_DENIED;
    }

    @Override
    public String toString() {
        return "CheckedPermRecord {"
                + this.mPackageName + ", " + this.status + "}";
    }
}
