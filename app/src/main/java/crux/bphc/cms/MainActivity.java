package crux.bphc.cms;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import app.Constants;
import app.MyApplication;
import crux.bphc.cms.fragments.ForumFragment;
import crux.bphc.cms.fragments.MyCoursesFragment;
import crux.bphc.cms.fragments.SearchCourseFragment;
import crux.bphc.cms.service.NotificationService;
import helper.MyFileManager;
import helper.UserAccount;
import helper.UserUtils;

import static app.Constants.API_URL;
import static app.Constants.TOKEN;
import static crux.bphc.cms.service.NotificationService.NOTIFICATION_CHANNEL_UPDATES;
import static crux.bphc.cms.service.NotificationService.NOTIFICATION_CHANNEL_UPDATES_BUNDLE;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final int MY_PERMISSIONS_REQUEST_WRITE_STORAGE = 1001;
    private UserAccount mUserAccount;
    private Fragment fragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (MyApplication.getInstance().isDarkModeEnabled()) {
            setTheme(R.style.AppTheme_NoActionBar_Dark);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mUserAccount = new UserAccount(this);
        Constants.TOKEN = mUserAccount.getToken();

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close) {
            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                View view = getCurrentFocus();
                if (view != null) {
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                }
            }
        };



        drawer.setDrawerListener(toggle);
        toggle.syncState();

        // Set the nav panel up
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        View headerView = navigationView.getHeaderView(0);
        TextView username = headerView.findViewById(R.id.username);
        TextView fullName = headerView.findViewById(R.id.firstname);
        username.setText(mUserAccount.getUsername());
        fullName.setText(mUserAccount.getFirstName());

        // Set the fragments up
        pushView(MyCoursesFragment.newInstance(TOKEN), "My Courses", true);
        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            Fragment frag = getSupportFragmentManager().findFragmentById(R.id.content_main);
            if (frag instanceof MyCoursesFragment){
                navigationView.setCheckedItem(R.id.my_courses);
            }
        });

        askPermission();
        createNotificationChannels(); // initialize channels before starting background service
        NotificationService.startService(this, false);
        resolveDeepLink();
        getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

    }


    // Create channels for devices running Oreo and above; Can be safely called even if channel exists
    void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create the "Updates Bundle" channel, which is channel for summary notifications that bundles thr
            // individual notifications
            NotificationChannel service = new NotificationChannel(NOTIFICATION_CHANNEL_UPDATES_BUNDLE,
                    "New Content Bundle",
                    NotificationManager.IMPORTANCE_DEFAULT);
            service.setDescription("A default priority channel that bundles all the notifications");

            // Create the "Updates" channel which has the low importance level
            NotificationChannel updates = new NotificationChannel(NOTIFICATION_CHANNEL_UPDATES,
                    "New Content",
                    NotificationManager.IMPORTANCE_LOW);
            updates.setDescription("All low importance channel that relays all the updates.");

            NotificationManager nm = getSystemService(NotificationManager.class);
            // create both channels
            nm.createNotificationChannel(service);
            nm.createNotificationChannel(updates);
        }
    }

    private void resolveDeepLink() {
        String pathString = getIntent().getStringExtra("path");
        if (pathString != null) {
            if (pathString.contains("view.php")) {
                String[] ids = pathString.split("=");
                try {
                    int id = Integer.parseInt(ids[ids.length - 1]);
                    if (id == 1) {
                        //todo open site news
                    } else {
                        Intent intent = new Intent(this, CourseDetailActivity.class);
                        intent.putExtra("id", id);
                        startActivity(intent);
                        return;
                    }

                } catch (NumberFormatException e) {

                }
            }
        }
    }


    private void askPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                AlertDialog.Builder dialog;

                if (MyApplication.getInstance().isDarkModeEnabled()) {
                    dialog = new AlertDialog.Builder(this,R.style.Theme_AppCompat_Dialog_Alert);
                } else {
                    dialog = new AlertDialog.Builder(this,R.style.Theme_AppCompat_Light_Dialog_Alert);
                }

                dialog.setMessage("Need Write permissions to seamlessly Download Files...");
                dialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                MY_PERMISSIONS_REQUEST_WRITE_STORAGE);
                    }
                });
                dialog.show();

            } else {


                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_REQUEST_WRITE_STORAGE);

            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == MY_PERMISSIONS_REQUEST_WRITE_STORAGE) {
            if (grantResults.length <= 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                askPermission();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void pushView(Fragment fragment, String tag, boolean rootFrag){
        if  (rootFrag){
            clearBackStack();
        }
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.content_main, fragment, tag);
        if (!rootFrag){
            transaction.addToBackStack(null);
        }
        transaction.commit();
        this.fragment = fragment;
    }


    private AlertDialog askToLogout() {
        AlertDialog.Builder alertDialog;

        if (MyApplication.getInstance().isDarkModeEnabled()) {
            alertDialog = new AlertDialog.Builder(MainActivity.this,R.style.Theme_AppCompat_Dialog_Alert);
        } else {
            alertDialog = new AlertDialog.Builder(MainActivity.this,R.style.Theme_AppCompat_Light_Dialog_Alert);
        }

        alertDialog.setMessage("Are you sure you want to Logout?");
        alertDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                logout();
            }
        });
        alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });
        return alertDialog.create();
    }

    public void logout() {
        UserUtils.logout(this);
        startActivity(new Intent(this, TokenActivity.class));
        finish();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.

        int id = item.getItemId();
        switch (id) {
            case R.id.my_courses:
                pushView(MyCoursesFragment.newInstance(TOKEN), "My Courses", true);
                break;
            case R.id.site_news:
                pushView(ForumFragment.newInstance(TOKEN), "Site News", false);
                break;
            case R.id.course_search:
                pushView(SearchCourseFragment.newInstance(TOKEN), "Course Search", false);
                break;
            case R.id.website:
                MyFileManager.showInWebsite(this, API_URL + "my/");
                break;

            case R.id.settings:
                Intent intent1 = new Intent(this, SettingsActivity.class);
                startActivity(intent1);
                break;
            case R.id.nav_share:
                final String appPackageName = BuildConfig.APPLICATION_ID;
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT,
                        "Check out the CMS App: https://play.google.com/store/apps/details?id=" + appPackageName);
                sendIntent.setType("text/plain");
                startActivity(sendIntent);
                break;
            case R.id.issue:
                MyFileManager.showInWebsite(this, Constants.getFeedbackURL(mUserAccount.getFirstName(), mUserAccount.getUsername()));
                break;
            case R.id.about:
                startActivity(new Intent(this, InfoActivity.class));
                break;
            case R.id.logout:
                askToLogout().show();
                break;
        }
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void clearBackStack() {
        for (int i = 0; i < getSupportFragmentManager().getBackStackEntryCount(); ++i) {
            getSupportFragmentManager().popBackStack();
        }
    }
}
