package com.greenskinmonster.a51nb.ui.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LevelListDrawable;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.AppCompatTextView;
import android.text.Html;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.greenskinmonster.a51nb.bean.DetailBean;
import com.greenskinmonster.a51nb.ui.BaseFragment;
import com.greenskinmonster.a51nb.ui.FragmentArgs;
import com.greenskinmonster.a51nb.ui.FragmentUtils;
import com.greenskinmonster.a51nb.ui.ThreadDetailFragment;
import com.greenskinmonster.a51nb.ui.textstyle.HiHtmlTagHandler;
import com.greenskinmonster.a51nb.utils.ColorHelper;
import com.greenskinmonster.a51nb.utils.HiUtils;
import com.greenskinmonster.a51nb.utils.HtmlCompat;
import com.greenskinmonster.a51nb.utils.Logger;
import com.greenskinmonster.a51nb.utils.UIUtils;
import com.greenskinmonster.a51nb.utils.Utils;
import com.vanniktech.emoji.EmojiHandler;

public class TextViewWithEmoticon extends AppCompatTextView {
    private Context mCtx;
    private BaseFragment mFragment;

    private static final long MIN_CLICK_INTERVAL = 600;

    private long mLastClickTime;

    public TextViewWithEmoticon(Context context) {
        super(context);
        mCtx = context;
        init();
    }

    public TextViewWithEmoticon(Context context, AttributeSet attrs) {
        super(context, attrs);
        mCtx = context;
        init();
    }

    private void init() {
        setTextColor(ColorHelper.getTextColorPrimary(mCtx));
        setLinkTextColor(ColorHelper.getColorAccent(mCtx));

        UIUtils.setLineSpacing(this);
    }

    public void setFragment(BaseFragment fragment) {
        mFragment = fragment;
    }

    public void setRichText(CharSequence text) {
        String t = text.toString().trim();
        SpannableStringBuilder b = (SpannableStringBuilder) HtmlCompat.fromHtml(t, imageGetter, new HiHtmlTagHandler());
        for (URLSpan s : b.getSpans(0, b.length(), URLSpan.class)) {
            String url = s.getURL();
            if (url.contains(HiUtils.ForumAttatchUrlPattern)) {
                URLSpan newSpan = getDownloadUrlSpan(url);
                b.setSpan(newSpan, b.getSpanStart(s), b.getSpanEnd(s), b.getSpanFlags(s));
                b.removeSpan(s);
            } else {
                FragmentArgs args = FragmentUtils.parseUrl(url);
                if (args != null) {
                    URLSpan newSpan = getFragmentArgsUrlSpan(url);
                    b.setSpan(newSpan, b.getSpanStart(s), b.getSpanEnd(s), b.getSpanFlags(s));
                    b.removeSpan(s);
                }
            }
        }
        setText(trimSpannable(b));
    }

    private Handler mHandler;

    private Html.ImageGetter imageGetter = new Html.ImageGetter() {
        public Drawable getDrawable(String src) {
            src = Utils.nullToText(src);

            Drawable drawable = TextViewWithEmoticon.this.getDrawable(src);
            // NOTE: 在 Android 10 上，以上调用会使动画 gif 返回一个 AnimatedImageDrawable，
            //   但是该对象不会正确处理 setBounds() 的意图，因此将之强制弃用。不做此特殊处理的话，
            //   表情（即动图首帧）会以本来大小显示，看上去非常小。此问题可能在 Android 9 上就有。
            if (drawable == null || !(drawable instanceof BitmapDrawable))
                drawable = TextViewWithEmoticon.this.getDrawable2(src);

            return drawable;
        }
    };

    @Nullable
    private Drawable getDrawable(String src) {
        int idx = src.indexOf(HiUtils.SmiliesPattern);
        if (idx == -1 || src.indexOf(".", idx) == -1)
            return null;

        int lastSlash = src.lastIndexOf("/");
        int lastDot = src.lastIndexOf(".");
        if (lastDot < lastSlash + 2)
            return null;

        String s = src.substring(lastSlash + 1, lastDot);
        int id = EmojiHandler.getDrawableResId(s);
        if (id == 0)
            return null;

        Drawable icon = ContextCompat.getDrawable(mCtx, id);
        if (icon != null) {
            int size = (int) (getLineHeight() * 1.2);
            icon.setBounds(0, 0, size, size);
        }

        return icon;
    }


    @NonNull
    private Drawable getDrawable2(String source) {
        final LevelListDrawable drawable = new LevelListDrawable();
        Glide.with(mCtx).load(source).asBitmap().into(new SimpleTarget<Bitmap>() {
            @Override
            public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
                BitmapDrawable d = new BitmapDrawable(mCtx.getResources(), resource);
                drawable.addLevel(1, 1, d);
                drawable.setLevel(1);

                int size = (int) (getLineHeight() * 1.2);
                drawable.setBounds(0, 0, size, size);

                if (mHandler != null)
                    return;

                mHandler = new Handler();
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        setText(getText());
                        invalidate();
                        mHandler = null;
                    }
                }, 30);
            }
        });

        return drawable;
    }

    private SpannableStringBuilder trimSpannable(SpannableStringBuilder spannable) {
        int trimStart = 0;
        int trimEnd = 0;

        String text = spannable.toString();

        while (text.length() > 0 && text.startsWith("\n")) {
            text = text.substring(1);
            trimStart ++;
        }

        while (text.length() > 0 && text.endsWith("\n")) {
            text = text.substring(0, text.length() - 1);
            trimEnd ++;
        }

        return spannable.delete(0, trimStart).delete(spannable.length() - trimEnd, spannable.length());
    }

    private URLSpan getFragmentArgsUrlSpan(final String url) {
        return new URLSpan(url) {
            public void onClick(View view) {
                if (mFragment == null)
                    return;

                FragmentArgs args = FragmentUtils.parseUrl(url);
                if (args == null)
                    return;

                int floor = 0;
                if (args.getType() == FragmentArgs.TYPE_THREAD
                        && mFragment instanceof ThreadDetailFragment) {
                    // redirect by goto floor in same fragment
                    ThreadDetailFragment detailFragment = (ThreadDetailFragment) mFragment;
                    if (!TextUtils.isEmpty(args.getTid()) && args.getTid().equals(detailFragment.getTid())) {
                        if (args.getFloor() != 0) {
                            floor = args.getFloor();
                        } else if (!TextUtils.isEmpty(args.getPostId())) {
                            // get floor if postId is cached
                            DetailBean detailBean = detailFragment.getCachedPost(args.getPostId());
                            if (detailBean != null)
                                floor = detailBean.getFloor();
                        } else {
                            floor = 1;
                        }
                    }
                }

                if (floor > 0 || floor == ThreadDetailFragment.LAST_FLOOR) {
                    // redirect in same thread
                    ((ThreadDetailFragment) mFragment).gotoFloor(floor);
                } else {
                    if (args.getType() == FragmentArgs.TYPE_THREAD) {
                        FragmentUtils.showThreadActivity(mFragment.getActivity(), args.isSkipEnterAnim(), args.getTid(), "", args.getPage(), args.getFloor(), args.getPostId(), -1);
                    } else {
                        FragmentUtils.show(mFragment.getActivity(), args);
                    }
                }
            }
        };
    }

    private URLSpan getDownloadUrlSpan(final String s_url) {
        return new URLSpan(s_url) {
            public void onClick(View view) {
                try {
                    String fileName = "";

                    // clean way to get fileName
                    SpannableStringBuilder b = new SpannableStringBuilder(((TextView) view).getText());
                    URLSpan[] urls = b.getSpans(0, b.length(), URLSpan.class);
                    if (urls.length > 0) {
                        fileName = b.toString().substring(b.getSpanStart(urls[0]), b.getSpanEnd(urls[0]));
                    }
                    if (TextUtils.isEmpty(fileName)) {
                        // failsafe dirty way, to get rid of ( xxx K ) file size string
                        fileName = ((TextView) view).getText().toString();
                        if (fileName.contains(" ("))
                            fileName = fileName.substring(0, fileName.lastIndexOf(" (")).trim();
                    }
                    UIUtils.toast("开始下载 " + fileName + " ...");
                    Utils.download(mCtx, getURL(), fileName);
                } catch (Exception e) {
                    Logger.e(e);
                    UIUtils.toast("下载出现错误，请使用浏览器下载。\n" + e.getMessage());
                }
            }
        };
    }

    /**
     * http://stackoverflow.com/a/17246463/2299887
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        if (action != MotionEvent.ACTION_UP && action != MotionEvent.ACTION_DOWN)
            return false;

        boolean ret = false;
        CharSequence text = getText();
        Spannable stext = Spannable.Factory.getInstance().newSpannable(text);

        int x = (int) event.getX();
        int y = (int) event.getY();

        x -= getTotalPaddingLeft();
        y -= getTotalPaddingTop();

        x += getScrollX();
        y += getScrollY();

        Layout layout = getLayout();
        int line = layout.getLineForVertical(y);
        int off = layout.getOffsetForHorizontal(line, x);

        ClickableSpan[] link = stext.getSpans(off, off, ClickableSpan.class);
        if (link.length == 0)
            return false;

        if (action == MotionEvent.ACTION_UP) {
            long currentClickTime = System.currentTimeMillis();
            long elapsedTime = currentClickTime - mLastClickTime;
            mLastClickTime = currentClickTime;

            if (elapsedTime > MIN_CLICK_INTERVAL) {
                try {
                    link[0].onClick(this);
                } catch (Exception e) {
                    UIUtils.toast("发生错误: " + e.getMessage());
                }
            }
        }

        return true;
    }
}
