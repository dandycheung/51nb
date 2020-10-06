package com.greenskinmonster.a51nb.async;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;

import com.greenskinmonster.a51nb.bean.HiSettingsHelper;
import com.greenskinmonster.a51nb.okhttp.OkHttpHelper;
import com.greenskinmonster.a51nb.ui.HiApplication;
import com.greenskinmonster.a51nb.ui.widget.HiProgressDialog;
import com.greenskinmonster.a51nb.utils.Logger;
import com.greenskinmonster.a51nb.utils.UIUtils;
import com.greenskinmonster.a51nb.utils.Utils;

import java.util.Date;

import okhttp3.Request;

/**
 * check and download update file
 * Created by GreenSkinMonster on 2015-03-09.
 */
public class UpdateHelper {
    private Activity mCtx;
    private boolean mSilent;

    private HiProgressDialog pd;

    private final static String checkUrl = "https://forum.51nb.com/app/51nb.txt";
    private final static String downloadUrl = "https://forum.51nb.com/app/51nb-release-{version}.apk";

    public UpdateHelper(Activity ctx, boolean isSilent) {
        mCtx = ctx;
        mSilent = isSilent;
    }

    public void check() {
        HiSettingsHelper.getInstance().setAutoUpdateCheck(true);
        HiSettingsHelper.getInstance().setLastUpdateCheckTime(new Date());

        if (mSilent) {
            doCheck();
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                doCheck();
            }
        }).start();
    }

    private void doCheck() {
        if (TextUtils.isEmpty(checkUrl))
            return;

        if (!mSilent) {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    pd = HiProgressDialog.show(mCtx, "正在检查新版本，请稍候...");
                }
            });
        }

        OkHttpHelper.getInstance().asyncGet(checkUrl, new UpdateCheckCallback());
    }

    private class UpdateCheckCallback implements OkHttpHelper.ResultCallback {
        @Override
        public void onError(Request request, Exception e) {
            Logger.e(e);
            if (!HiApplication.isAppVisible())
                return;

            if (!mSilent)
                pd.dismissError("检查新版本时发生错误: " + OkHttpHelper.getErrorMessage(e));
        }

        @Override
        public void onResponse(final String response) {
            processUpdate(response);
        }
    }

    private void processUpdate(String response) {
        if (!HiApplication.isAppVisible())
            return;

        response = Utils.nullToText(response).replace("\r\n", "\n").trim();

        String version = HiApplication.getAppVersion();

        String newVersion = "";
        String updateNotes = "";

        int firstLineIndex = response.indexOf("\n");
        if (response.startsWith("v") && firstLineIndex > 0) {
            newVersion = response.substring(1, firstLineIndex).trim();
            updateNotes = response.substring(firstLineIndex + 1).trim();
        }

        boolean found = !TextUtils.isEmpty(newVersion)
                && !TextUtils.isEmpty(updateNotes)
                && newer(version, newVersion);

        if (!mSilent) {
            if (found)
                pd.dismiss();
            else
                pd.dismiss("没有发现新版本");
        }

        if (!found)
            return;

        final String url = downloadUrl.replace("{version}", newVersion);
        final String filename = (url.contains("/")) ? url.substring(url.lastIndexOf("/") + 1) : "";

        Dialog dialog = new AlertDialog.Builder(mCtx).setTitle("发现新版本: " + newVersion)
                .setMessage(updateNotes)
                .setPositiveButton("下载",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        try {
                                            Utils.download(mCtx, url, filename);
                                        } catch (Exception e) {
                                            Logger.e(e);
                                            UIUtils.toast("下载出现错误，请到客户端发布帖中手动下载。\n" + e.getMessage());
                                        }
                                    }
                                }).setNegativeButton("暂不", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                }).setNeutralButton("不再提醒", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        HiSettingsHelper.getInstance().setAutoUpdateCheck(false);
                    }
                }).create();

        if (!mCtx.isFinishing())
            dialog.show();
    }

    private static boolean newer(String version, String newVersion) {
        // version format #.#.##
        if (TextUtils.isEmpty(newVersion))
            return false;

        try {
            return Integer.parseInt(newVersion.replace(".", "")) > Integer.parseInt(version.replace(".", ""));
        } catch (Exception ignored) {
            return false;
        }
    }

    public static boolean updateApp() {
        String installedVersion = HiSettingsHelper.getInstance().getInstalledVersion();
        String currentVersion = HiApplication.getAppVersion();

        if (!currentVersion.equals(installedVersion))
            HiSettingsHelper.getInstance().setInstalledVersion(currentVersion);

        return newer(installedVersion, currentVersion);
    }
}
