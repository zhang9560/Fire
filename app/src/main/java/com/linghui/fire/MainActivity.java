package com.linghui.fire;

import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ImageSpan;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import com.linghui.fire.session.SessionChangedEvent;
import com.linghui.fire.session.SessionManager;
import com.linghui.fire.task.HallFragment;
import com.linghui.fire.task.AssignedTaskFragment;
import com.linghui.fire.message.MessageFragment;
import com.linghui.fire.settings.PersonalInfoFragment;
import com.linghui.fire.task.TaskAssignedEvent;
import com.linghui.fire.utils.CenteredImageSpan;
import com.linghui.fire.widget.SessionBaseActivity;
import com.squareup.otto.Subscribe;

public class MainActivity extends SessionBaseActivity implements NavigationView.OnNavigationItemSelectedListener {
    public static final String TAG = MainActivity.class.getSimpleName();

    private static final String HALL_FRAGMENT_TAG = "hall";
    private static final String TASK_FRAGMENT_TAG = "task";
    private static final String MESSAGE_FRAGMENT_TAG = "message";
    private static final String PERSONAL_INFO_FRAGMENT_TAG = "personal_info";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        loadFragment(HALL_FRAGMENT_TAG);

        SessionManager.getInstance().getEventBus().register(this);
        SessionManager.getInstance().syncUserInfo();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SessionManager.getInstance().getEventBus().unregister(this);
    }

    @Subscribe
    public void onSesseionChanged(SessionChangedEvent event) {
        refreshPoints();
    }

    @Subscribe
    public void onTaskAssigned(TaskAssignedEvent event) {
        updateTaskMenuItem(true);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.navigation_item_hall:
                mToolbar.setTitle(R.string.title_hall);
                loadFragment(HALL_FRAGMENT_TAG);
                break;
            case R.id.navigation_item_task:
                mToolbar.setTitle(R.string.title_task);
                updateTaskMenuItem(false);
                loadFragment(TASK_FRAGMENT_TAG);
                break;
            case R.id.navigation_item_message:
                loadFragment(MESSAGE_FRAGMENT_TAG);
                mToolbar.setTitle(R.string.title_message);
                break;
            case R.id.navigation_item_personal_info:
                loadFragment(PERSONAL_INFO_FRAGMENT_TAG);
                mToolbar.setTitle(R.string.title_personal_info);
                break;
        }

        if (mDrawerLayout.isDrawerOpen(mNavigationView)) {
            mDrawerLayout.closeDrawer(mNavigationView);
        }
        return true;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (mDrawerLayout.isDrawerOpen(mNavigationView)) {
                mDrawerLayout.closeDrawer(mNavigationView);
                return true;
            }
        }
        return super.onKeyUp(keyCode, event);
    }

    private void initViews() {
        mToolbar = (Toolbar)findViewById(R.id.toolbar);
        mToolbar.setTitle(R.string.title_hall);
        setSupportActionBar(mToolbar);

        mDrawerLayout = (DrawerLayout)findViewById(R.id.drawer);
        ActionBarDrawerToggle defaultActionBarDrawerToggle = new ActionBarDrawerToggle(
                this,
                mDrawerLayout,
                mToolbar,
                R.string.app_name,
                R.string.app_name
        );
        mDrawerLayout.setDrawerListener(defaultActionBarDrawerToggle);
        defaultActionBarDrawerToggle.syncState();

        mNavigationView = (NavigationView)findViewById(R.id.navigation_view);
        mNavigationView.setNavigationItemSelectedListener(this);

        View navigationHeaderView = mNavigationView.getHeaderView(0);
        mAvailablePointsText = (TextView)navigationHeaderView.findViewById(R.id.available_points);
        mEarnedPointsText = (TextView)navigationHeaderView.findViewById(R.id.earned_points);
        mIntroducedPointsText = (TextView)navigationHeaderView.findViewById(R.id.introduced_points);
        refreshPoints();
    }

    private void loadFragment(String tag) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        if (mCurrentFragmentTag != null) {
            Fragment currentFragment = getSupportFragmentManager().findFragmentByTag(mCurrentFragmentTag);
            if (currentFragment != null) {
                transaction.hide(currentFragment);

                // 这里主动调用AssignedTaskFragment, onPause()是为了在其被hide后
                // 停止任务倒计时
                if (mCurrentFragmentTag.equals(TASK_FRAGMENT_TAG)) {
                    currentFragment.onPause();
                }
            }
        }

        Fragment fragment = getSupportFragmentManager().findFragmentByTag(tag);
        if (fragment == null) {
            fragment = createFragmentByTag(tag);

            if (fragment != null) {
                transaction.add(R.id.content, fragment, tag);
            }
        } else {
            transaction.show(fragment);

            // 这里主动调用AssignedTaskFragment被重新显示出来时继续任务倒计时
            if (tag.equals(TASK_FRAGMENT_TAG)) {
                fragment.onResume();
            }
        }

        transaction.commit();
        mCurrentFragmentTag = tag;
    }

    private Fragment createFragmentByTag(String tag) {
        if (!TextUtils.isEmpty(tag)) {
            if (tag.equals(HALL_FRAGMENT_TAG)) {
                return new HallFragment();
            } else if (tag.equals(TASK_FRAGMENT_TAG)) {
                return new AssignedTaskFragment();
            } else if (tag.equals(MESSAGE_FRAGMENT_TAG)) {
                return new MessageFragment();
            } else if (tag.equals(PERSONAL_INFO_FRAGMENT_TAG)) {
                return new PersonalInfoFragment();
            }
        }

        return null;
    }

    private void refreshPoints() {
        mAvailablePointsText.setText(String.format("%.1f", SessionManager.getInstance().getAvailablePoints()));
        mIntroducedPointsText.setText(String.format("%.1f", SessionManager.getInstance().getIntroducedPoints()));
        mEarnedPointsText.setText(String.format("%.1f", SessionManager.getInstance().getEarnedPoints()));
    }

    private void updateTaskMenuItem(boolean showNewIcon) {
        if (showNewIcon) {
            ImageSpan imageSpan = new CenteredImageSpan(this, R.drawable.ic_new);
            SpannableStringBuilder builder = new SpannableStringBuilder(getString(R.string.title_task) + "     *");
            builder.setSpan(imageSpan, builder.length() - 1, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            mNavigationView.getMenu().findItem(R.id.navigation_item_task).setTitle(builder);
        } else {
            mNavigationView.getMenu().findItem(R.id.navigation_item_task).setTitle(R.string.title_task);
        }
    }

    private String mCurrentFragmentTag;

    private Toolbar mToolbar;
    private DrawerLayout mDrawerLayout;
    private NavigationView mNavigationView;

    private TextView mAvailablePointsText;
    private TextView mIntroducedPointsText;
    private TextView mEarnedPointsText;
}
