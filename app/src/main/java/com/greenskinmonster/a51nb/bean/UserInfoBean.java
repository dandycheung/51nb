package com.greenskinmonster.a51nb.bean;

import com.greenskinmonster.a51nb.utils.HiUtils;

import java.util.Map;

public class UserInfoBean {
    private String mUserName;
    private String mUid;
    private String mFormHash;
    private Map<String, String> mInfos;
    private boolean mOnline;

    public UserInfoBean() {
    }

    public String getAvatarUrl() {
        return HiUtils.getAvatarUrlByUid(mUid);
    }

    public String getUid() {
        return mUid;
    }

    public void setUid(String mUid) {
        this.mUid = mUid;
    }

    public String getUserName() {
        return mUserName;
    }

    public void setUserName(String userName) {
        this.mUserName = userName;
    }

    public String getFormHash() {
        return mFormHash;
    }

    public void setFormHash(String formHash) {
        mFormHash = formHash;
    }

    public Map<String, String> getInfos() {
        return mInfos;
    }

    public void setInfos(Map<String, String> infos) {
        mInfos = infos;
    }

    public boolean isOnline() {
        return mOnline;
    }

    public void setOnline(boolean online) {
        this.mOnline = online;
    }
}
