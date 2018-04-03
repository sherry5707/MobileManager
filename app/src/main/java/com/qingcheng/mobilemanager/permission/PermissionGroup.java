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

package com.qingcheng.mobilemanager.permission;

import android.graphics.drawable.Drawable;

public final class PermissionGroup implements Comparable<PermissionGroup> {
    private final String mName;
    private final String mDeclaringPackage;
    private final CharSequence mLabel;
    private final Drawable mIcon;
    private boolean mIsCategory;
    private int subPermissionCount=0;
    private PermissionGroup mRealGroup;

    public PermissionGroup getmRealGroup() {
        return mRealGroup;
    }

    public void setmRealGroup(PermissionGroup mRealGroup) {
        this.mRealGroup = mRealGroup;
    }

    public String getmGroupName() {
        if(mRealGroup!=null) {
            return mRealGroup.getName();
        }else {
            return null;
        }
    }

    public boolean ismIsCategory() {
        return mIsCategory;
    }

    public void setmIsCategory(boolean mIsCategory) {
        this.mIsCategory = mIsCategory;
    }

    public int getSubPermissionCount() {
        return subPermissionCount;
    }

    public void setSubPermissionCount(int subPermissionCount) {
        this.subPermissionCount = subPermissionCount;
    }

    PermissionGroup(String name, String declaringPackage,
                    CharSequence label, Drawable icon, boolean isCategory) {
        mDeclaringPackage = declaringPackage;
        mName = name;
        mLabel = label;
        mIcon = icon;
        mIsCategory=isCategory;
    }

    PermissionGroup(String name, String declaringPackage,
                    CharSequence label, Drawable icon, PermissionGroup realGroup) {
        mDeclaringPackage = declaringPackage;
        mName = name;
        mLabel = label;
        mIcon = icon;
        mIsCategory=false;
        mRealGroup=realGroup;
    }

    PermissionGroup(String name, String declaringPackage,
                    CharSequence label, Drawable icon) {
        mDeclaringPackage = declaringPackage;
        mName = name;
        mLabel = label;
        mIcon = icon;
        mIsCategory=true;
    }

    public String getName() {
        return mName;
    }

    public String getDeclaringPackage() {
        return mDeclaringPackage;
    }

    public CharSequence getLabel() {
        return mLabel;
    }

    public Drawable getIcon() {
        return mIcon;
    }

    @Override
    public int compareTo(PermissionGroup another) {
        return mLabel.toString().compareTo(another.mLabel.toString());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null) {
            return false;
        }

        if (getClass() != obj.getClass()) {
            return false;
        }

        PermissionGroup other = (PermissionGroup) obj;

        if (mName == null) {
            if (other.mName != null) {
                return false;
            }
        } else if (!mName.equals(other.mName)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return mName != null ? mName.hashCode() : 0;
    }
}
