package com.greenskinmonster.a51nb.ui;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.github.angads25.filepicker.view.FilePickerDialog;
import com.greenskinmonster.a51nb.R;
import com.greenskinmonster.a51nb.async.LoginEvent;
import com.greenskinmonster.a51nb.async.LoginHelper;
import com.greenskinmonster.a51nb.async.LogoutEvent;
import com.greenskinmonster.a51nb.async.NetworkReadyEvent;
import com.greenskinmonster.a51nb.async.TaskHelper;
import com.greenskinmonster.a51nb.bean.Forum;
import com.greenskinmonster.a51nb.bean.HiSettingsHelper;
import com.greenskinmonster.a51nb.glide.GlideHelper;
import com.greenskinmonster.a51nb.job.ForumChangedEvent;
import com.greenskinmonster.a51nb.job.SimpleListJob;
import com.greenskinmonster.a51nb.service.NotiHelper;
import com.greenskinmonster.a51nb.ui.settings.AllForumsFragment;
import com.greenskinmonster.a51nb.ui.widget.BadgeView;
import com.greenskinmonster.a51nb.ui.widget.FABHideOnScrollBehavior;
import com.greenskinmonster.a51nb.ui.widget.HiProgressDialog;
import com.greenskinmonster.a51nb.ui.widget.LoginDialog;
import com.greenskinmonster.a51nb.ui.widget.OnSingleClickListener;
import com.greenskinmonster.a51nb.utils.ColorHelper;
import com.greenskinmonster.a51nb.utils.Constants;
import com.greenskinmonster.a51nb.utils.DrawerHelper;
import com.greenskinmonster.a51nb.utils.HiUtils;
import com.greenskinmonster.a51nb.utils.MaterialDrawerColorManager;
import com.greenskinmonster.a51nb.utils.UIUtils;
import com.greenskinmonster.a51nb.utils.Utils;
import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.materialdrawer.AccountHeader;
import com.mikepenz.materialdrawer.AccountHeaderBuilder;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.holder.BadgeStyle;
import com.mikepenz.materialdrawer.holder.ImageHolder;
import com.mikepenz.materialdrawer.holder.StringHolder;
import com.mikepenz.materialdrawer.interfaces.OnCheckedChangeListener;
import com.mikepenz.materialdrawer.model.DividerDrawerItem;
import com.mikepenz.materialdrawer.model.ExpandableDrawerItem;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileDrawerItem;
import com.mikepenz.materialdrawer.model.SwitchDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IProfile;
import com.mikepenz.materialdrawer.util.AbstractDrawerImageLoader;
import com.mikepenz.materialdrawer.util.DrawerImageLoader;
import com.mikepenz.materialdrawer.view.BezelImageView;
import com.vanniktech.emoji.EmojiHandler;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class MainFrameActivity extends BaseActivity {
    public final static int PERMISSIONS_REQUEST_CODE_STORAGE = 200;
    private final static int DRAG_SENSITIVITY = Utils.dpToPx(HiApplication.getAppContext(), 32);

    private Drawer mDrawer;
    private AccountHeader mAccountHeader;

    private NetworkStateReceiver mNetworkReceiver;
    private LoginDialog mLoginDialog;

    private BadgeView mMessageBadge;
    private BadgeView mNoticeBadge;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main_frame);
        // hack, to avoid MainFrameActivity be created more than once
        if (HiApplication.getMainActivityCount() > 1) {
            finish();
            return;
        }

        mRootView = findViewById(R.id.main_activity_root_view);
        mMainFrameContainer = findViewById(R.id.main_frame_container);
        mAppBarLayout = (AppBarLayout) findViewById(R.id.appbar_layout);

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mDrawer != null) {
                    if (mDrawer.isDrawerOpen())
                        mDrawer.closeDrawer();
                    else
                        mDrawer.openDrawer();
                }
            }
        });

        mToolbar.setOnClickListener(new OnSingleClickListener() {
            @Override
            public void onSingleClick(View v) {
                // get top displaying fragment
                Fragment fg = getSupportFragmentManager().findFragmentById(R.id.main_frame_container);
                if (fg instanceof BaseFragment)
                    ((BaseFragment) fg).scrollToTop();
            }
        });

        GlideHelper.initDefaultFiles();
        EmojiHandler.init();

        EventBus.getDefault().register(this);

        setupDrawer();
        updateAppBarScrollFlag();

        mMainFab = (FloatingActionButton) findViewById(R.id.fab_main);
        mNotiificationFab = (FloatingActionButton) findViewById(R.id.fab_notification);

        if (UIUtils.isTablet(this)) {
            mMainFab.setSize(FloatingActionButton.SIZE_NORMAL);
            mNotiificationFab.setSize(FloatingActionButton.SIZE_NORMAL);
        }

        updateFabGravity();

        mNetworkReceiver = new NetworkStateReceiver();
        registerReceiver(mNetworkReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

        if (savedInstanceState == null) {
            int fid = HiSettingsHelper.getInstance().getLastForumId();
            FragmentArgs args = FragmentUtils.parse(getIntent());
            if (args != null && args.getType() == FragmentArgs.TYPE_FORUM)
                fid = args.getFid();

            FragmentUtils.showForum(getSupportFragmentManager(), fid);

            if (args != null && args.getType() != FragmentArgs.TYPE_FORUM) {
                args.setSkipEnterAnim(true);
                args.setFid(fid);

                if (args.getType() == FragmentArgs.TYPE_NEW_THREAD)
                    args.setParentId(mSessionId);

                FragmentUtils.show(this, args);
            }

            TaskHelper.runDailyTask(false);

            if (HiApplication.isUpdated()) {
                HiApplication.setUpdated(false);
                UIUtils.showReleaseNotesDialog(this);
            } else {
                if (HiSettingsHelper.getInstance().isAutoUpdateCheckable()) {
                    // new UpdateHelper(this, true).check();
                }
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        FragmentArgs args = FragmentUtils.parse(intent);
        if (args != null) {
            NotiHelper.holdFetchNotify();
            args.setSkipEnterAnim(true);
            FragmentUtils.show(this, args);
        }
    }

    static public void destroyBezelImageViewMask(BezelImageView biv) {
        try {
            Class cls = BezelImageView.class;
            Field mask = cls.getDeclaredField("mMaskDrawable");
            mask.setAccessible(true);
            mask.set(biv, null);
        } catch (Exception e) {
            ;
        }
    }

    private void setupDrawer() {
        DrawerImageLoader.init(new AbstractDrawerImageLoader() {
            @Override
            public void set(ImageView imageView, Uri uri, Drawable placeholder) {
                GlideHelper.loadAvatar(Glide.with(MainFrameActivity.this), imageView, uri.toString());
            }

            @Override
            public void cancel(ImageView imageView) {
            }

            @Override
            public Drawable placeholder(Context ctx) {
                return null;
            }
        });

        int themePrimaryColor = HiSettingsHelper.getInstance().getPrimaryColor();

        // 以下程序段试图根据亮色主题的主色调对 AccountHeader 的背景图自动进行调整
        Drawable hdrBg = getResources().getDrawable(R.drawable.header);
        if (HiSettingsHelper.getInstance().isUsingLightTheme()) {
            /*
            // 此过滤器是将图转变为灰度图
            ColorMatrixColorFilter filter = new ColorMatrixColorFilter(new float[]{
                    0.33F, 0.59F, 0.11F, 0, 0,
                    0.33F, 0.59F, 0.11F, 0, 0,
                    0.33F, 0.59F, 0.11F, 0, 0,
                    0, 0, 0, 1, 0,
            });
            // */

            // ColorFilter filter = new LightingColorFilter(themePrimaryColor, 1);

            // 最终选定此过滤器效果
            PorterDuffColorFilter filter = new PorterDuffColorFilter(themePrimaryColor, PorterDuff.Mode.OVERLAY);
            hdrBg.setColorFilter(filter);
        } else {
            hdrBg.setColorFilter(Color.TRANSPARENT, PorterDuff.Mode.SRC_ATOP);
        }

        // Create the AccountHeader
        mAccountHeader = new AccountHeaderBuilder()
                .withActivity(this)
                .withAccountHeader(R.layout.header_drawer)
                // .withHeaderBackground(R.drawable.header)
                .withHeaderBackground(hdrBg)
                // .withCompactStyle(true)
                .withDividerBelowHeader(false)
                .withSelectionListEnabled(false)
                .addProfiles(getProfileDrawerItem())
                .withOnAccountHeaderProfileImageListener(new ProfileImageListener())
                .build();

        // HACK: 以下代码对 Account Header 进行定制，以显示需要的组件，并设定适当的响应
        View mies = mAccountHeader.getView();

        final class MyShortcut {
            public int viewId;
            public GoogleMaterial.Icon icon;
            public int jobId;

            public MyShortcut(int viewId, GoogleMaterial.Icon icon, int jobId) {
                this.viewId = viewId;
                this.icon = icon;
                this.jobId = jobId;
            }
        }

        MyShortcut shortcuts[] = {
            new MyShortcut(R.id.material_drawer_account_header_small_first, GoogleMaterial.Icon.gmd_email, SimpleListJob.TYPE_SMS),
            new MyShortcut(R.id.material_drawer_account_header_small_second, GoogleMaterial.Icon.gmd_notifications, -1),
            new MyShortcut(R.id.material_drawer_account_header_small_third, null, 0),
            new MyShortcut(R.id.material_drawer_account_header_my_posts, GoogleMaterial.Icon.gmd_assignment_ind, SimpleListJob.TYPE_MYPOST),
            new MyShortcut(R.id.material_drawer_account_header_my_favorites, GoogleMaterial.Icon.gmd_favorite, SimpleListJob.TYPE_FAVORITES),
            new MyShortcut(R.id.material_drawer_account_header_my_histories, GoogleMaterial.Icon.gmd_history, SimpleListJob.TYPE_HISTORIES),
        };

        // 以下代码，设置顶行/底行通知图标；因为色彩的关系，图标要加载出来以后反色处理；
        // 且，由于 BezelImageView 的默认实现会将要显示的图强制裁剪为圆形，会干扰图标
        // 的正常显示，因而用反射方法将圆形遮罩强行去除。反色代码来自于：
        // https://stackoverflow.com/questions/17841787/invert-colors-of-drawable

        /**
         * Color matrix that flips the components (<code>-1.0f * c + 255 = 255 - c</code>)
         * and keeps the alpha intact.
         */
        final float[] NEGATIVE = {
            -1.0f,     0,     0,    0, 255, // red
            0,     -1.0f,     0,    0, 255, // green
            0,         0, -1.0f,    0, 255, // blue
            0,         0,     0, 1.0f,   0  // alpha
        };

        for (final MyShortcut shortcut : shortcuts) {
            BezelImageView imageView = (BezelImageView) mies.findViewById(shortcut.viewId);
            destroyBezelImageViewMask(imageView);
            imageView.setColorFilter(new ColorMatrixColorFilter(NEGATIVE));

            if (shortcut.icon != null) {
                imageView.setVisibility(View.VISIBLE);

                ImageHolder imageHolder = new ImageHolder(shortcut.icon);
                imageHolder.applyTo(imageView);
            }

            if (shortcut.jobId > 0) {
                imageView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        FragmentUtils.showSimpleListActivity(MainFrameActivity.this, false, shortcut.jobId);
                    }
                });
            } else {
                if (shortcut.jobId == -1)
                    assignNotificationViewActions(imageView, SimpleListJob.TYPE_THREAD_NOTIFY);
            }
        }

        //*
        mMessageBadge = BadgeView.Builder.create(this)
                .setTextColor(Color.WHITE)
                .setWidthAndHeight(16,16)
                .setBadgeBackground(Color.RED)
                .setTextSize(10)
                .setBadgeGravity(Gravity.RIGHT | Gravity.BOTTOM)
                .setBadgeCount(0)
                .setShape(BadgeView.SHAPE_CIRCLE)
                .setSpace(2,2)
                .bind(mies.findViewById(R.id.material_drawer_account_header_small_first));
        if (mMessageBadge != null) {
            mMessageBadge.bringToFront();
            mMessageBadge.setVisibility(View.INVISIBLE);
        }
        // */

        //*
        mNoticeBadge = BadgeView.Builder.create(this)
                .setTextColor(Color.WHITE)
                .setWidthAndHeight(16,16)
                .setBadgeBackground(Color.RED)
                .setTextSize(10)
                .setBadgeGravity(Gravity.RIGHT | Gravity.BOTTOM)
                .setBadgeCount(0)
                .setShape(BadgeView.SHAPE_CIRCLE)
                .setSpace(2,2)
                .bind(mies.findViewById(R.id.material_drawer_account_header_small_second));
        // */
        if (mNoticeBadge != null) {
            mNoticeBadge.bringToFront();
            mNoticeBadge.setVisibility(View.INVISIBLE);
        }

        ArrayList<IDrawerItem> drawerItems = new ArrayList<>();
        drawerItems.add(DrawerHelper.getPrimaryMenuItem(DrawerHelper.DrawerItem.SEARCH));
        drawerItems.add(DrawerHelper.getPrimaryMenuItem(DrawerHelper.DrawerItem.NEW_POSTS));

        drawerItems.add(new DividerDrawerItem());
        drawerItems.add(DrawerHelper.getPrimaryMenuItem(DrawerHelper.DrawerItem.ALL_FORUMS));

        List<Forum> forums = HiSettingsHelper.getInstance().getFreqForums();
        for (Forum forum : forums) {
            if (HiUtils.isForumValid(forum.getId())) {
                drawerItems.add(new PrimaryDrawerItem()
                        .withName(forum.getName())
                        .withIdentifier(forum.getId())
                        .withIcon(forum.getIcon()));
            }
        }

        drawerItems.add(new DividerDrawerItem());
        drawerItems.add(DrawerHelper.getPrimaryMenuItem(DrawerHelper.DrawerItem.WARRANTY));

        IDrawerItem settingsItem;
        if (TextUtils.isEmpty(HiSettingsHelper.getInstance().getNightTheme())) {
            settingsItem = DrawerHelper.getPrimaryMenuItem(DrawerHelper.DrawerItem.SETTINGS);
        } else {
            settingsItem = new SwitchDrawerItem()
                    .withName(R.string.title_drawer_setting)
                    .withIdentifier(Constants.DRAWER_SETTINGS)
                    .withIcon(GoogleMaterial.Icon.gmd_settings)
                    .withChecked(HiSettingsHelper.getInstance().isNightMode())
                    .withOnCheckedChangeListener(new OnCheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(IDrawerItem drawerItem, CompoundButton buttonView, boolean isChecked) {
                            if (HiSettingsHelper.getInstance().isNightMode() != isChecked) {
                                final DrawerLayout.DrawerListener nightModeDrawerListener = new DrawerLayout.DrawerListener() {
                                    @Override
                                    public void onDrawerSlide(View drawerView, float slideOffset) {
                                    }

                                    @Override
                                    public void onDrawerOpened(View drawerView) {
                                    }

                                    @Override
                                    public void onDrawerClosed(View drawerView) {
                                        mDrawer.getDrawerLayout().removeDrawerListener(this);
                                        recreateActivity();
                                    }

                                    @Override
                                    public void onDrawerStateChanged(int newState) {
                                    }
                                };
                                HiSettingsHelper.getInstance().setNightMode(isChecked);
                                mDrawer.getDrawerLayout().addDrawerListener(nightModeDrawerListener);
                                mDrawer.closeDrawer();
                            }
                        }
                    });
        }

        drawerItems.add(settingsItem);

        mDrawer = new DrawerBuilder()
                .withActivity(this)
                .withToolbar(mToolbar)
                .withAccountHeader(mAccountHeader)
                .withTranslucentStatusBar(true)
                .withDrawerItems(drawerItems)
                .withStickyFooterDivider(false)
                .withStickyFooterShadow(false)
                .withOnDrawerItemClickListener(new DrawerItemClickListener())
                // .addStickyDrawerItems(settingsItem)
                .build();

        mDrawer.getRecyclerView().setVerticalScrollBarEnabled(false);

        if (HiSettingsHelper.getInstance().isUsingLightTheme()) {
            MaterialDrawerColorManager.setSelectedColor(mDrawer, themePrimaryColor);
            MaterialDrawerColorManager.setSelectedIconColor(mDrawer, Color.WHITE);
            MaterialDrawerColorManager.setSelectedTextColor(mDrawer, Color.WHITE);
        }
    }

    private ProfileDrawerItem getProfileDrawerItem() {
        String username = LoginHelper.isLoggedIn() ? HiSettingsHelper.getInstance().getUsername() : "游客";
        String uid = LoginHelper.isLoggedIn() ? "UID : " + HiSettingsHelper.getInstance().getUid() : "点击头像登录";
        String avatarUrl = LoginHelper.isLoggedIn()
                ? HiUtils.getAvatarUrlByUid(HiSettingsHelper.getInstance().getUid())
                : (GlideHelper.DEFAULT_AVATAR_FILE != null ? GlideHelper.DEFAULT_AVATAR_FILE.getAbsolutePath() : "");
        return new ProfileDrawerItem()
                .withEmail(uid)
                .withName(username)
                .withIcon(avatarUrl);
    }

    public void updateFabGravity() {
        CoordinatorLayout.LayoutParams mainFabParams = (CoordinatorLayout.LayoutParams) mMainFab.getLayoutParams();
        CoordinatorLayout.LayoutParams notiFabParams = (CoordinatorLayout.LayoutParams) mNotiificationFab.getLayoutParams();

        if (HiSettingsHelper.getInstance().isFabLeftSide())
            mainFabParams.anchorGravity = Gravity.BOTTOM | Gravity.LEFT | Gravity.END;
        else
            mainFabParams.anchorGravity = Gravity.BOTTOM | Gravity.RIGHT | Gravity.END;

        if (HiSettingsHelper.getInstance().isFabAutoHide()) {
            mainFabParams.setBehavior(new FABHideOnScrollBehavior());
            notiFabParams.setBehavior(new FABHideOnScrollBehavior());
        } else {
            mainFabParams.setBehavior(null);
            notiFabParams.setBehavior(null);
            mMainFab.show();
        }
    }

    public static View getChildById(View view, int id) {
        ViewGroup vg = (ViewGroup) view;
        if (view == null)
            return null;

        for (int i=0; i<vg.getChildCount(); i++) {
            View v = vg.getChildAt(i);
            if (v != null && v.getId() == id)
                return v;
        }

        return null;
    }

    protected void assignNotificationViewActions(View view, int action) {
        if (action == SimpleListJob.TYPE_SMS) {
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    FragmentUtils.showSimpleListActivity(MainFrameActivity.this, false, SimpleListJob.TYPE_SMS);
                }
            });
        } else if (action == SimpleListJob.TYPE_THREAD_NOTIFY) {
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    FragmentUtils.showNotifyListActivity(MainFrameActivity.this, false, SimpleListJob.TYPE_THREAD_NOTIFY,
                            NotiHelper.getCurrentNotification().getTotalNotiCount() > 0 ? SimpleListJob.NOTIFY_UNREAD : SimpleListJob.NOTIFY_THREAD);
                }
            });
        }
    }

    public void updateAccountHeader() {
        if (mAccountHeader != null) {
            IProfile profile = getProfileDrawerItem();

            // mAccountHeader.updateProfile(profile);

            mAccountHeader.removeProfile(0);
            mAccountHeader.addProfile(profile, 0);

            // mAccountHeader.setActiveProfile(profile);

            // HACK: 确保信息提示控件可见
            View mies = mAccountHeader.getView();
            View ctrl = mies.findViewById(R.id.material_drawer_account_header_small_first);
            if (ctrl != null) {
                ctrl.setVisibility(View.VISIBLE);

                // 如果是有 badge 的情况，则 ctrl 此时事实上是 badge 自动创建的和原控件共有的父控件，
                // 需要找打真正的控件
                if (mMessageBadge != null) {
                    ctrl = getChildById(ctrl, R.id.material_drawer_account_header_small_first);
                    if (ctrl != null)
                        ctrl.setVisibility(View.VISIBLE);
                }

                assignNotificationViewActions(ctrl, SimpleListJob.TYPE_SMS);
            }

            ctrl = mies.findViewById(R.id.material_drawer_account_header_small_second);
            if (ctrl != null) {
                ctrl.setVisibility(View.VISIBLE);

                // 如果是有 badge 的情况，则 ctrl 此时事实上是 badge 自动创建的和原控件共有的父控件，
                // 需要找打真正的控件
                if (mNoticeBadge != null) {
                    ctrl = getChildById(ctrl, R.id.material_drawer_account_header_small_second);
                    if (ctrl != null)
                        ctrl.setVisibility(View.VISIBLE);
                }

                assignNotificationViewActions(ctrl, SimpleListJob.TYPE_THREAD_NOTIFY);
            }
        }
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();

        if (HiApplication.getSettingStatus() == HiApplication.RESTART) {
            HiApplication.setSettingStatus(HiApplication.IDLE);
            Utils.restartActivity(this);
        } else if (HiApplication.getSettingStatus() == HiApplication.RECREATE) {
            HiApplication.setSettingStatus(HiApplication.IDLE);
            recreateActivity();
        } else if (HiApplication.getSettingStatus() == HiApplication.RELOAD) {
            HiApplication.setSettingStatus(HiApplication.IDLE);
            updateAppBarScrollFlag();
            updateFabGravity();

            Fragment fg = getSupportFragmentManager().findFragmentByTag(ThreadListFragment.class.getName());
            if (fg instanceof ThreadListFragment) {
                ((ThreadListFragment) fg).notifyDataSetChanged();
            }
        } else {
            // if (!LoginHelper.isLoggedIn())
            //     showLoginDialog();
        }
    }

    @Override
    public void onDestroy() {
        if (mNetworkReceiver != null)
            unregisterReceiver(mNetworkReceiver);

        EventBus.getDefault().unregister(this);
        dismissLoginDialog();

        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        // getMenuInflater().inflate(R.menu.main_frame, menu);
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                UIUtils.hideSoftKeyboard(this);
                // popFragment();
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (mDrawer.isDrawerOpen()) {
            mDrawer.closeDrawer();
            return;
        }

        FragmentManager fm = getSupportFragmentManager();
        Fragment fragment = fm.findFragmentById(R.id.main_frame_container);

        if (fragment instanceof BaseFragment)
            if (((BaseFragment) fragment).onBackPressed())
                return;

        finishWithDefault();
    }

    private float mStartX;
    private float mStartY;

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        int action = ev.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mStartX = ev.getX();
                mStartY = ev.getY();
                break;
            case MotionEvent.ACTION_UP:
                float deltaX = ev.getX() - mStartX;
                float deltaY = Math.abs(ev.getY() - mStartY);

                if (deltaX >= DRAG_SENSITIVITY && deltaY < 0.5 * deltaX)
                    if (!mDrawer.isDrawerOpen())
                        mDrawer.openDrawer();

                break;
        }

        return super.dispatchTouchEvent(ev);
    }

    private class DrawerItemClickListener implements Drawer.OnDrawerItemClickListener {
        @Override
        public boolean onItemClick(View view, int position, IDrawerItem iDrawerItem) {
            if (iDrawerItem.getIdentifier() == Constants.DRAWER_NO_ACTION)
                return false;

            switch ((int) iDrawerItem.getIdentifier()) {
                case Constants.DRAWER_SEARCH:
                    FragmentUtils.showSimpleListActivity(MainFrameActivity.this, false, SimpleListJob.TYPE_SEARCH);
                    break;
                case Constants.DRAWER_NEW_POSTS:
                    FragmentUtils.showSimpleListActivity(MainFrameActivity.this, false, SimpleListJob.TYPE_NEW_POSTS);
                    break;
                case Constants.DRAWER_MYPOST:
                    FragmentUtils.showSimpleListActivity(MainFrameActivity.this, false, SimpleListJob.TYPE_MYPOST);
                    break;
                case Constants.DRAWER_FAVORITES:
                    FragmentUtils.showSimpleListActivity(MainFrameActivity.this, false, SimpleListJob.TYPE_FAVORITES);
                    break;
                case Constants.DRAWER_HISTORIES:
                    FragmentUtils.showSimpleListActivity(MainFrameActivity.this, false, SimpleListJob.TYPE_HISTORIES);
                    break;
                case Constants.DRAWER_SMS:
                    FragmentUtils.showSimpleListActivity(MainFrameActivity.this, false, SimpleListJob.TYPE_SMS);
                    break;
                case Constants.DRAWER_THREADNOTIFY:
                    FragmentUtils.showNotifyListActivity(MainFrameActivity.this, false, SimpleListJob.TYPE_THREAD_NOTIFY,
                            NotiHelper.getCurrentNotification().getTotalNotiCount() > 0 ? SimpleListJob.NOTIFY_UNREAD : SimpleListJob.NOTIFY_THREAD);
                    break;
                case Constants.DRAWER_SETTINGS:
                    Intent intent = new Intent(MainFrameActivity.this, SettingsActivity.class);
                    ActivityCompat.startActivity(MainFrameActivity.this, intent,
                            FragmentUtils.getAnimBundle(MainFrameActivity.this, false));
                    break;
                case Constants.DRAWER_ALL_FORUMS:
                    intent = new Intent(MainFrameActivity.this, SettingsActivity.class);
                    intent.putExtra(AllForumsFragment.TAG_KEY, AllForumsFragment.TAG_KEY);
                    ActivityCompat.startActivity(MainFrameActivity.this, intent,
                            FragmentUtils.getAnimBundle(MainFrameActivity.this, false));
                    break;
                case Constants.DRAWER_WARRANTY:
                    FragmentUtils.showWarrantyActivity(MainFrameActivity.this, false);
                    break;
                default:
                    int forumId = (int) iDrawerItem.getIdentifier();
                    FragmentUtils.showForum(getSupportFragmentManager(), forumId);
                    break;
            }

            return false;
        }
    }

    private class ProfileImageListener implements AccountHeader.OnAccountHeaderProfileImageListener {
        @Override
        public boolean onProfileImageClick(View view, IProfile profile, boolean current) {
            if (!LoginHelper.isLoggedIn()) {
                showLoginDialog();
                return true;
            }

            String username = HiSettingsHelper.getInstance().getUsername();
            String uid = HiSettingsHelper.getInstance().getUid();
            FragmentUtils.showUserInfoActivity(MainFrameActivity.this, false, uid, username);

            return true;
        }

        @Override
        public boolean onProfileImageLongClick(View view, IProfile profile, boolean current) {
            if (!LoginHelper.isLoggedIn())
                return false;

            Dialog dialog = new AlertDialog.Builder(MainFrameActivity.this)
                    .setTitle("退出登录？")
                    .setMessage("确认退出当前登录用户 <" + HiSettingsHelper.getInstance().getUsername() + "> ，并清除保存的登录信息？\n")
                    .setPositiveButton(getResources().getString(android.R.string.ok),
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    HiProgressDialog progressDialog = HiProgressDialog.show(MainFrameActivity.this, "正在退出...");
                                    LoginHelper.logout();
                                    updateAccountHeader();

                                    Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.main_frame_container);
                                    if (fragment instanceof ThreadListFragment) {
                                        ((ThreadListFragment) fragment).enterNotLoginState();
                                    }

                                    progressDialog.dismiss("已退出登录状态", 2000);
                                    new Handler().postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            showLoginDialog();
                                        }
                                    }, 2000);
                                }
                            })
                    .setNegativeButton(getResources().getString(android.R.string.cancel),
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            }).create();
            dialog.show();

            return true;
        }
    }

    private static class NetworkStateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            HiSettingsHelper.updateMobileNetworkStatus(context);
            EventBus.getDefault().post(new NetworkReadyEvent());
        }
    }

    public void updateDrawerBadge() {
        int smsCount = NotiHelper.getCurrentNotification().getSmsCount();
        int threadCount = NotiHelper.getCurrentNotification().getTotalNotiCount();

        if (mMessageBadge != null) {
            mMessageBadge.setBadgeCount(smsCount);
            mMessageBadge.setVisibility(smsCount == 0 ? View.INVISIBLE : View.VISIBLE);
        }

        if (mNoticeBadge != null) {
            mNoticeBadge.setBadgeCount(threadCount);
            mNoticeBadge.setVisibility(threadCount == 0 ? View.INVISIBLE : View.VISIBLE);
        }

        int threadNotifyIndex = mDrawer.getPosition(Constants.DRAWER_THREADNOTIFY);
        if (threadNotifyIndex != -1) {
            PrimaryDrawerItem drawerItem = (PrimaryDrawerItem) mDrawer.getDrawerItem(Constants.DRAWER_THREADNOTIFY);
            if (threadCount > 0) {
                drawerItem.withBadgeStyle(new BadgeStyle().withTextColor(Color.WHITE).withColorRes(R.color.md_red_700));
                mDrawer.updateBadge(Constants.DRAWER_THREADNOTIFY, new StringHolder(threadCount + ""));
            } else {
                drawerItem.withBadgeStyle(new BadgeStyle().withTextColor(Color.WHITE).withColorRes(R.color.background_grey));
                mDrawer.updateBadge(Constants.DRAWER_THREADNOTIFY, new StringHolder("0"));
            }
        }

        int smsNotifyIndex = mDrawer.getPosition(Constants.DRAWER_SMS);
        if (smsNotifyIndex != -1) {
            PrimaryDrawerItem drawerItem = (PrimaryDrawerItem) mDrawer.getDrawerItem(Constants.DRAWER_SMS);
            if (smsCount > 0) {
                drawerItem.withBadgeStyle(new BadgeStyle().withTextColor(Color.WHITE).withColorRes(R.color.md_red_700));
                mDrawer.updateBadge(Constants.DRAWER_SMS, new StringHolder(smsCount + ""));
            } else {
                drawerItem.withBadgeStyle(new BadgeStyle().withTextColor(Color.WHITE).withColorRes(R.color.background_grey));
                mDrawer.updateBadge(Constants.DRAWER_SMS, new StringHolder("0"));
            }
        }
    }

    void setDrawerSelection(int forumId) {
        if (mDrawer == null || mDrawer.isDrawerOpen())
            return;

        int position = mDrawer.getPosition(forumId);
        if (mDrawer.getCurrentSelectedPosition() == position)
            return;

        // NOTE: 对于 -1 的判断，为 FastAdapter 升级为 3.2.1 后引入的额外判断，如果不提前判断的话，
        // 则进入 setSelectionAtPosition 调用会引发异常
        if (position == -1)
            mDrawer.deselect();
        else
            mDrawer.setSelectionAtPosition(position, false);
    }

    public void setActionBarDisplayHomeAsUpEnabled(boolean showHomeAsUp) {
        if (showHomeAsUp) {
            mDrawer.getActionBarDrawerToggle().setDrawerIndicatorEnabled(false);
            if (getSupportActionBar() != null)
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        } else {
            if (getSupportActionBar() != null)
                getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            mDrawer.getActionBarDrawerToggle().setDrawerIndicatorEnabled(true);
        }
    }

    void syncActionBarState() {
        if (mDrawer != null)
            mDrawer.getActionBarDrawerToggle().syncState();
    }

    private void closeDrawer() {
        if (mDrawer != null && mDrawer.isDrawerOpen())
            mDrawer.closeDrawer();
    }

    private void recreateActivity() {
        ColorHelper.clear();
        int theme = HiUtils.getThemeValue(this,
                HiSettingsHelper.getInstance().getActiveTheme(),
                HiSettingsHelper.getInstance().getPrimaryColor());
        setTheme(theme);

        // avoid “RuntimeException: Performing pause of activity that is not resumed”
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    getWindow().setWindowAnimations(R.style.ThemeTransitionAnimation);
                    recreate();
                } catch (Exception e) {
                    Utils.restartActivity(MainFrameActivity.this);
                }
            }
        }, 5);
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(LoginEvent event) {
        if (event.mManual) {
            // clearBackStacks(true);
            Fragment fg = getSupportFragmentManager().findFragmentByTag(ThreadListFragment.class.getName());
            if (fg instanceof ThreadListFragment) {
                fg.setHasOptionsMenu(true);
                invalidateOptionsMenu();
                ((ThreadListFragment) fg).onRefresh();
            }
        }

        updateAccountHeader();
        dismissLoginDialog();
    }

    @SuppressWarnings("unused")
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onEvent(LogoutEvent event) {
        EventBus.getDefault().removeStickyEvent(event);
        UIUtils.toast("已退出登录");
        updateAccountHeader();
    }

    @SuppressWarnings("unused")
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onEvent(ForumChangedEvent event) {
        EventBus.getDefault().removeStickyEvent(event);
        if (event.mForumChanged)
            setupDrawer();

        if (HiUtils.isForumValid(event.mFid))
            FragmentUtils.showForum(getSupportFragmentManager(), event.mFid);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_CODE_STORAGE:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    UIUtils.toast("授权成功");

                break;

            case FilePickerDialog.EXTERNAL_READ_PERMISSION_GRANT:
                if (grantResults.length == 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED)
                    UIUtils.askForStoragePermission(this);

                break;
        }
    }

    public void showLoginDialog() {
        if (mLoginDialog == null || !mLoginDialog.isShowing()) {
            mLoginDialog = new LoginDialog(this);
            mLoginDialog.setTitle("用户登录");
            mLoginDialog.show();
        }
    }

    public void dismissLoginDialog() {
        if (mLoginDialog != null) {
            if (mLoginDialog.isShowing())
                mLoginDialog.dismiss();
            mLoginDialog = null;
        }
    }
}
