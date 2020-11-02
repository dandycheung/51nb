package com.greenskinmonster.a51nb.ui.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.Rect;
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
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.Request;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.target.ViewTarget;
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

import java.util.HashSet;
import java.util.Set;

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
        SpannableStringBuilder b = (SpannableStringBuilder) HtmlCompat.fromHtml(t, imageGetterEx, new HiHtmlTagHandler());
        for (URLSpan s : b.getSpans(0, b.length(), URLSpan.class)) {
            String url = s.getURL();

            boolean attachment = url.contains(HiUtils.ForumAttatchUrlPattern);
            if (!attachment) {
                FragmentArgs args = FragmentUtils.parseUrl(url);
                if (args == null)
                    continue;
            }

            URLSpan newSpan = attachment ? getDownloadUrlSpan(url) : getFragmentArgsUrlSpan(url);
            b.setSpan(newSpan, b.getSpanStart(s), b.getSpanEnd(s), b.getSpanFlags(s));
            b.removeSpan(s);
        }

        setText(trimSpannable(b));
    }

    private Html.ImageGetter imageGetterEx = new GlideImageGetter();

    private Html.ImageGetter imageGetter = new Html.ImageGetter() {
        public Drawable getDrawable(String src) {
            src = Utils.nullToText(src);

            Drawable drawable = TextViewWithEmoticon.this.getDrawable(src);
            // NOTE: 在 Android 10 上，以上调用会使动画 gif 返回一个 AnimatedImageDrawable，
            //   但是该对象不会正确处理 setBounds() 的意图，因此将之强制弃用。不做此特殊处理的话，
            //   表情（即动图首帧）会以本来大小显示，看上去非常小。此问题可能在 Android 9 上就有。
            if (!(drawable instanceof BitmapDrawable))
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


    private Handler mHandler;

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
                if (args.getType() == FragmentArgs.TYPE_THREAD && mFragment instanceof ThreadDetailFragment) {
                    // redirect by goto floor in same fragment
                    ThreadDetailFragment detailFragment = (ThreadDetailFragment) mFragment;
                    if (!TextUtils.isEmpty(args.getTid()) && args.getTid().equals(detailFragment.getTid())) {
                        floor = args.getFloor();
                        if (floor == 0) {
                            String postId = args.getPostId();
                            if (TextUtils.isEmpty(postId))
                                floor = 1;
                            else {
                                // get floor if postId is cached
                                DetailBean detailBean = detailFragment.getCachedPost(postId);
                                if (detailBean != null)
                                    floor = detailBean.getFloor();
                            }
                        }
                    }
                }

                if (floor > 0 || floor == ThreadDetailFragment.LAST_FLOOR) // redirect in same thread
                    ((ThreadDetailFragment) mFragment).gotoFloor(floor);
                else if (args.getType() == FragmentArgs.TYPE_THREAD)
                    FragmentUtils.showThreadActivity(mFragment.getActivity(), args.isSkipEnterAnim(), args.getTid(), "", args.getPage(), args.getFloor(), args.getPostId(), -1);
                else
                    FragmentUtils.show(mFragment.getActivity(), args);
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
                    if (urls.length > 0)
                        fileName = b.toString().substring(b.getSpanStart(urls[0]), b.getSpanEnd(urls[0]));

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

    /*
     * New image getter implementation
     */

    public class GlideImageGetter implements Html.ImageGetter, Drawable.Callback {
        private final TextView mTextView;
        private final Set<ImageGetterViewTarget> mTargets;

        public void clear() {
            for (ImageGetterViewTarget target : mTargets)
                Glide.clear(target);
        }

        public GlideImageGetter() {
            mTextView = TextViewWithEmoticon.this;
            mTargets = new HashSet<>();
        }

        @Override
        public Drawable getDrawable(String url) {
            final UrlDrawableGlide urlDrawable = new UrlDrawableGlide();

            System.out.println("Downloading from: " + url);

            // NOTE: 此处之前使用 mCtx 传入 glide，会导致动画在加载过程中如果旋转屏幕的话产生崩溃，
            // 查看 Glide.with 函数前的说明后照猫画虎改为 mFragment 的上下文问题消失。未予深究。
            Context ctx = mFragment.getContext(); // mCtx;
            Glide.with(ctx)
                    .load(url)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(new ImageGetterViewTarget(mTextView, urlDrawable));

            return urlDrawable;
        }

        @Override
        public void invalidateDrawable(Drawable who) {
            mTextView.invalidate();
        }

        @Override
        public void scheduleDrawable(Drawable who, Runnable what, long when) {
        }

        @Override
        public void unscheduleDrawable(Drawable who, Runnable what) {
        }

        private class ImageGetterViewTarget extends ViewTarget<TextView, GlideDrawable> {
            private final UrlDrawableGlide mDrawable;

            private ImageGetterViewTarget(TextView view, UrlDrawableGlide drawable) {
                super(view);

                mTargets.add(this);
                mDrawable = drawable;
            }

            @Override
            public void onResourceReady(GlideDrawable resource, GlideAnimation<? super GlideDrawable> glideAnimation) {
                int width = resource.getIntrinsicWidth();
                int height = resource.getIntrinsicHeight();

                int size = (int) (getLineHeight() * 1.2);
                // if (width > size)
                    width = size;

                // if (height > size)
                    height = size;

                Rect rect = new Rect(0, 0, width, height);
                resource.setBounds(rect);

                mDrawable.setBounds(rect);
                mDrawable.setDrawable(resource);

                if (resource.isAnimated()) {
                    mDrawable.setCallback(GlideImageGetter.this);
                    resource.setLoopCount(GlideDrawable.LOOP_FOREVER);
                    resource.start();
                }

                getView().setText(getView().getText());
                getView().invalidate();
            }

            private Request request;

            @Override
            public Request getRequest() {
                return request;
            }

            @Override
            public void setRequest(Request request) {
                this.request = request;
            }
        }
    }

    // Nullable GlideDrawable wrapper/container, with default Drawable callback
    public class UrlDrawableGlide extends Drawable implements Drawable.Callback {
        private GlideDrawable mDrawable;

        @Override
        public void draw(Canvas canvas) {
            if (mDrawable != null)
                mDrawable.draw(canvas);
        }

        @Override
        public void setAlpha(int alpha) {
            if (mDrawable != null)
                mDrawable.setAlpha(alpha);
        }

        @Override
        public void setColorFilter(ColorFilter cf) {
            if (mDrawable != null)
                mDrawable.setColorFilter(cf);
        }

        @Override
        public int getOpacity() {
            if (mDrawable != null)
                return mDrawable.getOpacity();

            return PixelFormat.UNKNOWN;
        }

        public void setDrawable(GlideDrawable drawable) {
            if (mDrawable != null)
                mDrawable.setCallback(null);

            drawable.setCallback(this);
            mDrawable = drawable;
        }

        @Override
        public void invalidateDrawable(Drawable who) {
            if (getCallback() != null)
                getCallback().invalidateDrawable(who);
        }

        @Override
        public void scheduleDrawable(Drawable who, Runnable what, long when) {
            if (getCallback() != null)
                getCallback().scheduleDrawable(who, what, when);
        }

        @Override
        public void unscheduleDrawable(Drawable who, Runnable what) {
            if (getCallback() != null)
                getCallback().unscheduleDrawable(who, what);
        }
    }
}
