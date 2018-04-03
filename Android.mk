LOCAL_PATH:= $(call my-dir)/app/src/main/

include $(CLEAR_VARS)
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := risenumber-mobile:libs/nineoldandroids-2.4.0.jar
include $(BUILD_MULTI_PREBUILT)

include $(CLEAR_VARS)

LOCAL_PROGUARD_ENABLED := disabled

LOCAL_MODULE_TAGS := optional

LOCAL_STATIC_JAVA_LIBRARIES := \
        risenumber-mobile\
        myuisdk \
        android-support-v4 \
        android-support-v13 \
        android-support-v7-appcompat \
        android-support-design \
        glide \

#LOCAL_JAVA_LIBRARIES := mediatek-framework
#LOCAL_JAVA_LIBRARIES += mediatek-common
#LOCAL_JAVA_LIBRARIES += com.mediatek.settings.ext


LOCAL_SRC_FILES := \
        $(call all-java-files-under)

LOCAL_RESOURCE_DIR := \
        $(LOCAL_PATH)res \
        vendor/ragentek/myui_sdk/res \
        frameworks/support/v7/appcompat/res \
        frameworks/support/design/res

LOCAL_AAPT_FLAGS := \
        --auto-add-overlay \
        --extra-packages com.ragentek.myuisdk \
        --extra-packages android.support.v7.appcompat \
        --extra-packages android.support.design \

        
LOCAL_JAVA_LIBRARIES += telephony-common \
                        org.apache.http.legacy \

                        
LOCAL_PACKAGE_NAME := MobileManager




LOCAL_CERTIFICATE := platform

LOCAL_PROGUARD_FLAG_FILES := ../../../../../../../frameworks/support/design/proguard-rules.pro

include $(BUILD_PACKAGE)

# Include plug-in's makefile to automated generate .mpinfo
include vendor/mediatek/proprietary/frameworks/opt/plugin/mplugin.mk

include $(CLEAR_VARS)


include $(BUILD_MULTI_PREBUILT)