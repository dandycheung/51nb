package com.greenskinmonster.a51nb.ui;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
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
import com.greenskinmonster.a51nb.ui.widget.FABHideOnScrollBehavior;
import com.greenskinmonster.a51nb.ui.widget.HiProgressDialog;
import com.greenskinmonster.a51nb.ui.widget.LoginDialog;
import com.greenskinmonster.a51nb.ui.widget.OnSingleClickListener;
import com.greenskinmonster.a51nb.utils.ColorHelper;
import com.greenskinmonster.a51nb.utils.Constants;
import com.greenskinmonster.a51nb.utils.DrawerHelper;
import com.greenskinmonster.a51nb.utils.HiUtils;
import com.greenskinmonster.a51nb.utils.UIUtils;
import com.greenskinmonster.a51nb.utils.Utils;
import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.materialdrawer.AccountHeader;
import com.mikepenz.materialdrawer.AccountHeaderBuilder;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.holder.BadgeStyle;
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
import com.vanniktech.emoji.EmojiHandler;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

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

        // Create the AccountHeader
        mAccountHeader = new AccountHeaderBuilder()
                .withActivity(this)
                .withHeaderBackground(R.drawable.header)
                .withCompactStyle(true)
                .withDividerBelowHeader(false)
                .withSelectionListEnabled(false)
                .addProfiles(getProfileDrawerItem())
                .withOnAccountHeaderProfileImageListener(new ProfileImageListener())
                .build();

        ArrayList<IDrawerItem> drawerItems = new ArrayList<>();
        drawerItems.add(DrawerHelper.getPrimaryMenuItem(DrawerHelper.DrawerItem.SEARCH));
        drawerItems.add(DrawerHelper.getPrimaryMenuItem(DrawerHelper.DrawerItem.SMS));
        drawerItems.add(DrawerHelper.getPrimaryMenuItem(DrawerHelper.DrawerItem.THREAD_NOTIFY));
        drawerItems.add(DrawerHelper.getPrimaryMenuItem(DrawerHelper.DrawerItem.NEW_POSTS));

        Set<String> freqMenuIds = HiSettingsHelper.getInstance().getFreqMenus();
        Collection<IDrawerItem> subItems = new ArrayList<>();
        if (freqMenuIds.contains("" + DrawerHelper.DrawerItem.MY_POST.id))
            drawerItems.add(DrawerHelper.getPrimaryMenuItem(DrawerHelper.DrawerItem.MY_POST));
        else
            subItems.add(DrawerHelper.getSecondaryMenuItem(DrawerHelper.DrawerItem.MY_POST));

        if (freqMenuIds.contains("" + DrawerHelper.DrawerItem.MY_FAVORITES.id))
            drawerItems.add(DrawerHelper.getPrimaryMenuItem(DrawerHelper.DrawerItem.MY_FAVORITES));
        else
            subItems.add(DrawerHelper.getSecondaryMenuItem(DrawerHelper.DrawerItem.MY_FAVORITES));

        if (freqMenuIds.contains("" + DrawerHelper.DrawerItem.HISTORIES.id))
            drawerItems.add(DrawerHelper.getPrimaryMenuItem(DrawerHelper.DrawerItem.HISTORIES));
        else
            subItems.add(DrawerHelper.getSecondaryMenuItem(DrawerHelper.DrawerItem.HISTORIES));

        if (freqMenuIds.contains("" + DrawerHelper.DrawerItem.WARRANTY.id))
            drawerItems.add(DrawerHelper.getPrimaryMenuItem(DrawerHelper.DrawerItem.WARRANTY));
        else
            subItems.add(DrawerHelper.getSecondaryMenuItem(DrawerHelper.DrawerItem.WARRANTY));

        if (subItems.size() > 0)
            drawerItems.add(new ExpandableDrawerItem()
                            .withName(R.string.title_drawer_expandable)
                            .withIcon(GoogleMaterial.Icon.gmd_more_horiz)
                            .withIdentifier(Constants.DRAWER_NO_ACTION)
                            .withSelectable(false)
                            .withSubItems(subItems.toArray(new IDrawerItem[subItems.size()])
                            ));

        drawerItems.add(new DividerDrawerItem());
        if (TextUtils.isEmpty(HiSettingsHelper.getInstance().getNightTheme())) {
            drawerItems.add(DrawerHelper.getPrimaryMenuItem(DrawerHelper.DrawerItem.SETTINGS));
        } else {
            drawerItems.add(new SwitchDrawerItem()
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
                    }));
        }
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

        mDrawer = new DrawerBuilder()
                .withActivity(this)
                .withToolbar(mToolbar)
                .withAccountHeader(mAccountHeader)
                .withTranslucentStatusBar(true)
                .withDrawerItems(drawerItems)
                .withStickyFooterDivider(false)
                .withStickyFooterShadow(false)
                .withOnDrawerItemClickListener(new DrawerItemClickListener())
                .build();

        mDrawer.getRecyclerView().setVerticalScrollBarEnabled(false);
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

    public void updateAccountHeader() {
        if (mAccountHeader != null) {
            mAccountHeader.removeProfile(0);
            mAccountHeader.addProfile(getProfileDrawerItem(), 0);
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
                    Intent intent = new Intent(MainFrameActivity.this, SettingActivity.class);
                    ActivityCompat.startActivity(MainFrameActivity.this, intent,
                            FragmentUtils.getAnimBundle(MainFrameActivity.this, false));
                    break;
                case Constants.DRAWER_ALL_FORUMS:
                    intent = new Intent(MainFrameActivity.this, SettingActivity.class);
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
            if (LoginHelper.isLoggedIn()) {
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
                                        if (fragment != null && fragment instanceof ThreadListFragment) {
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
            } else {
                showLoginDialog();
            }
            return false;
        }

        @Override
        public boolean onProfileImageLongClick(View view, IProfile profile, boolean current) {
            return false;
        }
    }

    private class NetworkStateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            HiSettingsHelper.updateMobileNetworkStatus(context);
            EventBus.getDefault().post(new NetworkReadyEvent());
        }
    }

    public void updateDrawerBadge() {
        int smsCount = NotiHelper.getCurrentNotification().getSmsCount();
        int threadCount = NotiHelper.getCurrentNotification().getTotalNotiCount();

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
        if (mDrawer != null && !mDrawer.isDrawerOpen()) {
            int position = mDrawer.getPosition(forumId);
            if (mDrawer.getCurrentSelectedPosition() != position)
                mDrawer.setSelectionAtPosition(position, false);
        }
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
                                           @NonNull String permissions[], @NonNull int[] grantResults) {
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
