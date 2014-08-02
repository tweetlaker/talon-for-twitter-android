package com.klinker.android.twitter_l.ui;

import android.app.AlarmManager;
import android.app.FragmentManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.view.ViewPager;
import android.transition.Explode;
import android.transition.Fade;
import android.transition.Slide;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.View;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import android.widget.Toolbar;
import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.adapters.MainDrawerArrayAdapter;
import com.klinker.android.twitter_l.adapters.TimelinePagerAdapter;
import com.klinker.android.twitter_l.data.sq_lite.DMDataSource;
import com.klinker.android.twitter_l.data.sq_lite.FavoriteUsersDataSource;
import com.klinker.android.twitter_l.data.sq_lite.FollowersDataSource;
import com.klinker.android.twitter_l.data.sq_lite.HomeDataSource;
import com.klinker.android.twitter_l.data.sq_lite.InteractionsDataSource;
import com.klinker.android.twitter_l.data.sq_lite.ListDataSource;
import com.klinker.android.twitter_l.data.sq_lite.MentionsDataSource;
import com.klinker.android.twitter_l.services.CatchupPull;
import com.klinker.android.twitter_l.services.TalonPullNotificationService;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.ui.compose.ComposeActivity;
import com.klinker.android.twitter_l.ui.drawer_activities.DrawerActivity;
import com.klinker.android.twitter_l.ui.setup.LoginActivity;
import com.klinker.android.twitter_l.ui.setup.TutorialActivity;
import com.klinker.android.twitter_l.utils.IOUtils;

public class MainActivity extends DrawerActivity {

    public static boolean isPopup;
    public static Context sContext;

    public static ImageButton sendButton;
    public static LinearLayout sendLayout;
    public static boolean showIsRunning = false;
    public static boolean hideIsRunning = false;
    public static Handler sendHandler;
    public static Runnable showSend = new Runnable() {
        @Override
        public void run() {
            if (sendLayout.getVisibility() == View.GONE && !showIsRunning) {

                sContext.sendBroadcast(new Intent("com.klinker.android.twitter.SHOW_TOAST"));

                Animation anim = AnimationUtils.loadAnimation(sContext, R.anim.fab_in);
                anim.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                        showIsRunning = true;
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        sendLayout.setVisibility(View.VISIBLE);
                        showIsRunning = false;
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });
                sendLayout.startAnimation(anim);
            }
        }
    };
    public static Runnable hideSend = new Runnable() {
        @Override
        public void run() {
            if (sendLayout.getVisibility() == View.VISIBLE && !hideIsRunning) {

                sContext.sendBroadcast(new Intent("com.klinker.android.twitter.HIDE_TOAST"));

                Animation anim = AnimationUtils.loadAnimation(sContext, R.anim.fab_out);
                anim.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                        hideIsRunning = true;
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        sendLayout.setVisibility(View.GONE);
                        hideIsRunning = false;
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });
                sendLayout.startAnimation(anim);
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.v("talon_starting", "starting main activity");

        MainActivity.sendHandler = new Handler();

        context = this;
        sContext = this;
        sharedPrefs = context.getSharedPreferences("com.klinker.android.twitter_world_preferences",
                Context.MODE_WORLD_READABLE + Context.MODE_WORLD_WRITEABLE);
        DrawerActivity.settings = AppSettings.getInstance(context);

        try {
            requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        } catch (Exception e) {

        }

        sharedPrefs.edit().putBoolean("refresh_me", getIntent().getBooleanExtra("from_notification", false)).commit();

        setUpTheme();
        setUpWindow();
        setContentView(R.layout.main_activity);
        mViewPager = (ViewPager) findViewById(R.id.pager);
        setUpDrawer(0, getResources().getString(R.string.timeline));

        MainActivity.sendLayout = (LinearLayout) findViewById(R.id.send_layout);
        MainActivity.sendHandler.postDelayed(showSend, 1000);
        MainActivity.sendButton = (ImageButton) findViewById(R.id.send_button);
        MainActivity.sendLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                getWindow().setExitTransition(new Explode());

                Intent compose = new Intent(context, ComposeActivity.class);
                startActivity(compose);
            }
        });
        MainActivity.sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MainActivity.sendLayout.performClick();
            }
        });

        actionBar = getActionBar();
        /*Toolbar toolbar = new Toolbar(context);
        toolbar.setTitle("Timeline");
        toolbar.setNavigationIcon(R.drawable.ic_drawer_dark);
        toolbar.inflateMenu(R.menu.main_activity);
        setActionBar(toolbar);*/
        actionBar.setTitle(getResources().getString(R.string.timeline));

        if (!settings.isTwitterLoggedIn) {
            Intent login = new Intent(context, LoginActivity.class);
            startActivity(login);
        } /*else if (!sharedPrefs.getBoolean("setup_v_two", false) && !PreferenceManager.getDefaultSharedPreferences(context).getBoolean("setup_v_two", false)) {
            Intent setupV2 = new Intent(context, Version2Setup.class);
            startActivity(setupV2);
        }*/

        mSectionsPagerAdapter = new TimelinePagerAdapter(getFragmentManager(), context, sharedPrefs, getIntent().getBooleanExtra("from_launcher", false));

        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.setOverScrollMode(View.OVER_SCROLL_NEVER);
        mViewPager.setCurrentItem(mSectionsPagerAdapter.getCount() - 3);

        if (getIntent().getBooleanExtra("from_launcher", false)) {
            actionBar.setTitle(mSectionsPagerAdapter.getPageTitle(getIntent().getIntExtra("launcher_page", 0)));
        }

        mViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            public void onPageScrollStateChanged(int state) {
            }

            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                if (!actionBar.isShowing()) {
                    actionBar.show();

                    if (translucent) {
                        statusBar.setVisibility(View.VISIBLE);
                    }
                }
                MainActivity.sendHandler.post(showSend);
            }

            public void onPageSelected(int position) {

                String title = "" + mSectionsPagerAdapter.getPageTitle(position);

                if (title.equals(getResources().getString(R.string.mentions))) {
                    MainDrawerArrayAdapter.current = 1;
                } else if (title.equals(getResources().getString(R.string.direct_messages))) {
                    MainDrawerArrayAdapter.current = 2;
                } else if (title.equals(getResources().getString(R.string.timeline))) {
                    MainDrawerArrayAdapter.current = 0;
                } else {
                    MainDrawerArrayAdapter.current = -1;
                }

                drawerList.invalidateViews();

                actionBar.setTitle(title);
            }
        });

        mViewPager.setOffscreenPageLimit(4);

        if (getIntent().getBooleanExtra("tutorial", false) && !sharedPrefs.getBoolean("done_tutorial", false)) {
            getIntent().putExtra("tutorial", false);
            sharedPrefs.edit().putBoolean("done_tutorial", true).commit();
            registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    try {
                        Log.v("tutorial_activity", "close drawer");
                        mDrawerLayout.closeDrawer(Gravity.LEFT);
                        unregisterReceiver(this);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }, new IntentFilter(TutorialActivity.ACTION_CLOSE_DRAWER));

            registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    try {
                        Log.v("tutorial_activity", "open drawer");
                        mDrawerLayout.openDrawer(Gravity.LEFT);
                        unregisterReceiver(this);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }, new IntentFilter(TutorialActivity.ACTION_OPEN_DRAWER));

            registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    try {
                        Log.v("tutorial_activity", "page left");
                        mViewPager.setCurrentItem(mViewPager.getCurrentItem() - 1, true);
                        unregisterReceiver(this);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }, new IntentFilter(TutorialActivity.ACTION_PAGE_LEFT));

            registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    try {
                        Log.v("tutorial_activity", "page right");
                        mViewPager.setCurrentItem(mViewPager.getCurrentItem() + 1, true);
                        unregisterReceiver(this);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }, new IntentFilter(TutorialActivity.ACTION_PAGE_RIGHT));

            startActivity(new Intent(context, TutorialActivity.class));
            overridePendingTransition(0, 0);
        }

        setLauncherPage();

        if (getIntent().getBooleanExtra("from_drawer", false)) {
            mViewPager.setCurrentItem(getIntent().getIntExtra("page_to_open", 3));
        }

        Log.v("talon_starting", "ending on create");
    }

    public void setLauncherPage() {
        // do nothing here
    }

    public void setUpWindow() {
        // nothing here, will be overrode
        MainActivity.isPopup = false;

        if ((getIntent().getFlags() & 0x00002000) != 0) {
            MainActivity.isPopup = true;
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        recreate();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!actionBar.isShowing()) {
            actionBar.show();
            if (settings.theme == AppSettings.THEME_DARK) {
                getWindow().setStatusBarColor(getResources().getColor(R.color.darkest_primary));
            } else {
                getWindow().setStatusBarColor(getResources().getColor(R.color.darker_primary));
            }
        }

        Log.v("talon_starting", "onResume()");

        getWindow().setExitTransition(new Explode());

        MainActivity.showIsRunning = false;
        MainActivity.hideIsRunning = false;
        MainActivity.sendHandler.postDelayed(showSend, 1000);

        if (sharedPrefs.getBoolean("open_a_page", false)) {
            sharedPrefs.edit().putBoolean("open_a_page", false).commit();
            int page = sharedPrefs.getInt("open_what_page", 3);
            String title = "" + mSectionsPagerAdapter.getPageTitle(page);
            actionBar.setTitle(title);
            mViewPager.setCurrentItem(page);
        }

        if (sharedPrefs.getBoolean("open_interactions", false)) {
            sharedPrefs.edit().putBoolean("open_interactions", false).commit();
            mDrawerLayout.openDrawer(Gravity.END);
        }

        Log.v("talon_starting", "ending onResume()");
    }

    @Override
    public void onDestroy() {
        try {
            HomeDataSource.getInstance(context).close();
        } catch (Exception e) { }
        try {
            MentionsDataSource.getInstance(context).close();
        } catch (Exception e) { }
        try {
            DMDataSource.getInstance(context).close();
        } catch (Exception e) { }
        try {
            ListDataSource.getInstance(context).close();
        } catch (Exception e) { }
        try {
            FollowersDataSource.getInstance(context).close();
        } catch (Exception e) { }
        try {
            FavoriteUsersDataSource.getInstance(context).close();
        } catch (Exception e) { }
        try {
            InteractionsDataSource.getInstance(context).close();
        } catch (Exception e) { }

        super.onDestroy();
    }

    public static boolean caughtstarting = false;

    @Override
    public void onStart() {
        super.onStart();

        MainActivity.isPopup = false;

        Log.v("talon_starting", "main activity starting");

        sharedPrefs = getSharedPreferences("com.klinker.android.twitter_world_preferences",
                Context.MODE_WORLD_READABLE + Context.MODE_WORLD_WRITEABLE);

        // check for night mode switching
        int theme = AppSettings.getCurrentTheme(sharedPrefs);

        if (!getWindow().hasFeature(Window.FEATURE_ACTION_BAR_OVERLAY) ||
                sharedPrefs.getBoolean("launcher_frag_switch", false) ||
                (theme != settings.theme && !settings.addonTheme && settings.nightMode)) {

            sharedPrefs.edit().putBoolean("launcher_frag_switch", false)
                              .putBoolean("dont_refresh", true).commit();

            AppSettings.invalidate();

            Log.v("talon_theme", "no action bar overlay found, recreating");

            finish();
            overridePendingTransition(0, 0);
            startActivity(getRestartIntent());
            overridePendingTransition(0, 0);

            MainActivity.caughtstarting = true;

            // return so that it doesn't start the background refresh, that is what caused the dups.
            sharedPrefs.edit().putBoolean("dont_refresh_on_start", true).commit();
            return;
        } else {
            sharedPrefs.edit().putBoolean("dont_refresh", false)
                              .putBoolean("should_refresh", true).commit();

        }

        if(DrawerActivity.settings.pushNotifications) {
            if (!TalonPullNotificationService.isRunning) {
                context.startService(new Intent(context, TalonPullNotificationService.class));
            }
        } else {
            context.sendBroadcast(new Intent("com.klinker.android.twitter.STOP_PUSH_SERVICE"));
        }

        // cancel the alarm to start the catchup service
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pendingIntent = PendingIntent.getService(context, 236, new Intent(context, CatchupPull.class), 0);
        am.cancel(pendingIntent); // cancel the old one, then start the new one in 1 min

        // clear the pull unread
        sharedPrefs.edit().putInt("pull_unread", 0).commit();

        new Thread(new Runnable() {
            @Override
            public void run() {
                if (sharedPrefs.getBoolean("version_2_5", true)) {
                    sharedPrefs.edit().putBoolean("version_2_5", false).commit();
                    IOUtils.trimCache(context);
                }
            }
        }).start();

        /*new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1000);
                } catch (Exception e) {

                }
                NotificationUtils.refreshNotification(context);
            }
        }).start();*/


        Log.v("talon_starting", "ending onStart()");
    }

    public Intent getRestartIntent() {
        return new Intent(context, MainActivity.class);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        if (settings.floatingCompose) {
            menu.getItem(2).setVisible(false); // hide the compose button here
        }

        return true;
    }

}