package com.greenskinmonster.a51nb.async;

import android.text.TextUtils;

import com.greenskinmonster.a51nb.bean.HiSettingsHelper;
import com.greenskinmonster.a51nb.bean.UserBean;
import com.greenskinmonster.a51nb.okhttp.OkHttpHelper;
import com.greenskinmonster.a51nb.okhttp.ParamsMap;
import com.greenskinmonster.a51nb.parser.HiParser;
import com.greenskinmonster.a51nb.utils.Constants;
import com.greenskinmonster.a51nb.utils.HiUtils;
import com.greenskinmonster.a51nb.utils.Utils;

import org.greenrobot.eventbus.EventBus;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;

public class LoginHelper {
    private String mErrorMsg = "";

    public LoginHelper() {
    }

    public int login() {
        return login(false);
    }

    public int login(boolean manual) {
        int status = Constants.STATUS_FAIL_ABORT;

        if (HiSettingsHelper.getInstance().isLoginInfoValid())
            status = doLogin();
        else
            mErrorMsg = "登录信息不完整";

        if (status == Constants.STATUS_SUCCESS) {
            LoginEvent event = new LoginEvent();
            event.mManual = manual;
            EventBus.getDefault().post(event);
        }

        return status;
    }

    private int doLogin_old() {
        mErrorMsg = "登录失败: 未知错误";
        try {
            String rstStr = OkHttpHelper.getInstance().get(HiUtils.LoginSubmit);

            Document doc = Jsoup.parse(rstStr);
            Element loginForm = doc.select("form[name=login]").first();

            String submitUrl = HiUtils.BaseUrl + loginForm.attr("action");
            String formhash = doc.select("input[name=formhash]").first().val();
            String referer = doc.select("input[name=referer]").first().val();

            ParamsMap params = new ParamsMap();
            params.put("formhash", formhash);
            params.put("referer", referer);
            params.put("loginfield", "username");
            params.put("username", HiSettingsHelper.getInstance().getUsername());
            params.put("password", processedPassword());
            params.put("questionid", HiSettingsHelper.getInstance().getSecQuestion());
            params.put("answer", HiSettingsHelper.getInstance().getSecAnswer());
            params.put("cookietime", "2592000");
            params.put("quickforward", "yes");

            String rspAfter = OkHttpHelper.getInstance().post(submitUrl, params);

            Document docAfter = Jsoup.parse(rspAfter);
            Element spaceLink = docAfter.select("div#um strong > a").first();
            if (spaceLink != null) {
                String username = spaceLink.text();

                String uid = Utils.getMiddleString(spaceLink.attr("href"), "space-uid-", ".");
                if (!HiUtils.isValidId(uid))
                    uid = Utils.getMiddleString(spaceLink.attr("href"), "&uid=", "&");

                if (HiUtils.isValidId(uid)) {
                    HiSettingsHelper.getInstance().setUid(uid);
                    HiSettingsHelper.getInstance().setUsername(username);

                    return Constants.STATUS_SUCCESS;
                }
            } else {
                mErrorMsg = HiParser.parseErrorMessage(docAfter);
                if (!TextUtils.isEmpty(mErrorMsg)) {
                    logout();
                    return Constants.STATUS_FAIL_ABORT;
                }
            }
        } catch (Exception e) {
            mErrorMsg = "登录失败: " + OkHttpHelper.getErrorMessage(e);
            return Constants.STATUS_FAIL;
        }

        return Constants.STATUS_FAIL;
    }

    private int doLogin() { // doLoginWithReload
        mErrorMsg = "登录失败: 未知错误";

        try {
            String rstStr = OkHttpHelper.getInstance().get(HiUtils.LoginSubmit);
            Document doc = Jsoup.parse(rstStr);

            if (checkLoginSucceeded(doc))
                return Constants.STATUS_SUCCESS;

            // 之前没有登录，此处执行正常的登录请求
            Element loginForm = doc.select("form[name=login]").first();
            String submitUrl = HiUtils.BaseUrl + loginForm.attr("action");
            String formhash = doc.select("input[name=formhash]").first().val();
            String referer = doc.select("input[name=referer]").first().val();
            // submitUrl += "&inajax=1";

            ParamsMap params = new ParamsMap();
            params.put("formhash", formhash);
            params.put("referer", referer);
            params.put("loginfield", "username");
            params.put("username", HiSettingsHelper.getInstance().getUsername());
            params.put("password", processedPassword());
            params.put("questionid", HiSettingsHelper.getInstance().getSecQuestion());
            params.put("answer", HiSettingsHelper.getInstance().getSecAnswer());
            params.put("cookietime", "2592000");
            params.put("quickforward", "yes");

            rstStr = OkHttpHelper.getInstance().post(submitUrl, params);

            doc = Jsoup.parse(rstStr);
            if (checkLoginSucceeded(doc))
                return Constants.STATUS_SUCCESS;

            // TODO: check again? maybe "reload" page
            // rstStr = OkHttpHelper.getInstance().get(HiUtils.LoginSubmit);

            // TODO: change the error parser accordingly
            mErrorMsg = HiParser.parseErrorMessage(doc);
            if (!TextUtils.isEmpty(mErrorMsg)) {
                logout();
                return Constants.STATUS_FAIL_ABORT;
            }
        } catch (Exception e) {
            mErrorMsg = "登录失败: " + OkHttpHelper.getErrorMessage(e);
        }

        return Constants.STATUS_FAIL;
    }

    private boolean checkLoginSucceeded(Document doc) {
        Element userInfo = doc.select("a[href].userinfo").first();
        if (userInfo == null)
            return false;

        // 如果进入此处，应该是此前已经登录成功的情况
        String userId = null;

        String url = userInfo.attributes().get("href");
        url = url.trim();

        if (!url.equals("")) {
            String[] urlParts = url.split("\\?");
            if (urlParts.length > 1) {
                String[] paramsStr = URLDecoder.decode(urlParts[1]).split("&");
                HashMap<String, String> params = new HashMap<>();
                for (String param : paramsStr) {
                    String[] kv = param.split("=");
                    params.put(kv[0], kv[1]);
                }

                userId = params.get("uid");
            }
        }

        if (userId == null || !HiUtils.isValidId(userId))
            return false;

        String userName = userInfo.attributes().get("title");
        if (!userName.equals(HiSettingsHelper.getInstance().getUsername()))
            return false;

        HiSettingsHelper.getInstance().setUid(userId);
        HiSettingsHelper.getInstance().setUsername(userName);

        return true;
    }

    public static boolean checkLoggedin(Document doc) {
        Element usernameEl = doc.select("input#ls_username").first();
        return (usernameEl == null);
    }

    public static boolean isLoggedIn() {
        return HiUtils.isValidId(HiSettingsHelper.getInstance().getUid());
    }

    public static void logout() {
        HiSettingsHelper.getInstance().setUid("");
        HiSettingsHelper.getInstance().setUsername("");
        HiSettingsHelper.getInstance().setPassword("");
        HiSettingsHelper.getInstance().setSecQuestion("");
        HiSettingsHelper.getInstance().setSecAnswer("");

        OkHttpHelper.getInstance().clearCookies();
        FavoriteHelper.getInstance().clearAll();

        HiSettingsHelper.getInstance().setBlacklists(new ArrayList<UserBean>());
        EventBus.getDefault().postSticky(new LogoutEvent());
    }

    public String getErrorMsg() {
        return mErrorMsg;
    }

    private String processedPassword() {
        String pass = HiSettingsHelper.getInstance().getPassword();
        try {
            return Utils.md5(pass.replace("\\", "\\\\")
                    .replace("'", "\'")
                    .replace("\"", "\\\""));
        } catch (Exception e) {
            return pass;
        }
    }
}
