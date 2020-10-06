package com.greenskinmonster.a51nb.parser;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.greenskinmonster.a51nb.utils.HiUtils;
import com.greenskinmonster.a51nb.utils.Utils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;

/**
 * Created by GreenSkinMonster on 2017-07-27.
 */

public class ParserUtil {
    public static String parseXmlMessage(String xml) {
        Document doc = Jsoup.parse(xml, "", Parser.xmlParser());
        return doc.text();
    }

    public static String parseXmlErrorMessage(String xml) {
        String html = ParserUtil.parseXmlMessage(xml);
        return Jsoup.parse(html).text();
    }

    @NonNull
    public static String getAbsoluteUrl(String url) {
        if (TextUtils.isEmpty(url))
            return "";

        if (url.contains("://"))
            return url;

        return HiUtils.BaseUrl + url;
    }

    public static int parseForumId(String url) {
        if (url.contains("fid="))
            return Utils.getMiddleInt(url, "fid=", "&");

        if (url.contains("forum-")) {
            String fidKeyword = Utils.getMiddleString(url, "forum-", "-");
            if (HiUtils.StaticKeywordMap.containsKey(fidKeyword))
                return HiUtils.StaticKeywordMap.get(fidKeyword);

            if (HiUtils.isValidId(fidKeyword))
                return Utils.parseInt(fidKeyword);
        }

        return 0;
    }

    public static String parseTid(String url) {
        String tid = Utils.getMiddleString(url, "tid=", "&");

        if (TextUtils.isEmpty(tid))
            tid = Utils.getMiddleString(url, "thread-", "-");

        return tid;
    }
}
