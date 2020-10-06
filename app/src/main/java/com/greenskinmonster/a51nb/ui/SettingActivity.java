package com.greenskinmonster.a51nb.ui;

import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.Toolbar;

import com.greenskinmonster.a51nb.R;
import com.greenskinmonster.a51nb.ui.settings.AboutFragment;
import com.greenskinmonster.a51nb.ui.settings.AllForumsFragment;
import com.greenskinmonster.a51nb.ui.settings.BlacklistFragment;
import com.greenskinmonster.a51nb.ui.settings.PasswordFragment;
import com.greenskinmonster.a51nb.ui.settings.SettingsMainFragment;
import com.greenskinmonster.a51nb.ui.settings.SettingsNestedFragment;

/**
 * Created by GreenSkinMonster on 2017-06-16.
 */

public class SettingActivity extends SwipeBaseActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_post);
        mRootView = findViewById(R.id.main_activity_root_view);
        mAppBarLayout = (AppBarLayout) findViewById(R.id.appbar_layout);

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        FragmentManager fragmentManager = getSupportFragmentManager();
        if (fragmentManager.findFragmentById(R.id.main_frame_container) != null)
            return;

        Fragment fragment = null;

        Bundle arguments = getIntent().getExtras();
        if (arguments == null)
            fragment = new SettingsMainFragment();
        else if (arguments.containsKey(AboutFragment.TAG_KEY))
            fragment = new AboutFragment();
        else if (arguments.containsKey(BlacklistFragment.TAG_KEY))
            fragment = new BlacklistFragment();
        else if (arguments.containsKey(PasswordFragment.TAG_KEY))
            fragment = new PasswordFragment();
        else if (arguments.containsKey(AllForumsFragment.TAG_KEY))
            fragment = new AllForumsFragment();
        else
            fragment = new SettingsNestedFragment();

        if (arguments != null)
            fragment.setArguments(arguments);

        fragmentManager.beginTransaction()
                .add(R.id.main_frame_container, fragment).commit();
    }
}
