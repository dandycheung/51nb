package com.greenskinmonster.a51nb.ui.widget;

import android.view.View;

/**
 * Avoid double click on view
 * http://stackoverflow.com/questions/5608720/android-preventing-double-click-on-a-button
 */
public abstract class OnSingleClickListener implements View.OnClickListener {
    /**
     * 最短 click 事件的时间间隔
     */
    private static final long MIN_CLICK_INTERVAL = 600;
    /**
     * 上次 click 的时间
     */
    private long mLastClickTime;

    /**
     * click 响应函数
     *
     * @param v The view that was clicked.
     */
    public abstract void onSingleClick(View v);

    @Override
    public final void onClick(View v) {
        long currentClickTime = System.currentTimeMillis();
        long elapsedTime = currentClickTime - mLastClickTime;

        // 有可能 2 次连击，也有可能 3 连击，保证 mLastClickTime 记录的总是上次 click 的时间
        mLastClickTime = currentClickTime;

        if (elapsedTime <= MIN_CLICK_INTERVAL)
            return;

        onSingleClick(v);
    }
}
