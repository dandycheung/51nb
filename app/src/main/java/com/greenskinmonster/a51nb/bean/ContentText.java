package com.greenskinmonster.a51nb.bean;

import com.greenskinmonster.a51nb.utils.Utils;

public class ContentText extends ContentAbs {
    private StringBuilder mSb;

    public ContentText(String text) {
        mSb = new StringBuilder();
        mSb.append(text);
    }

    public void append(String txt) {
        mSb.append(txt);
    }

    @Override
    public String getContent() {
        return mSb.toString();
    }

    @Override
    public String getCopyText() {
        return Utils.fromHtmlAndStrip(mSb.toString()).toString();
    }
}
