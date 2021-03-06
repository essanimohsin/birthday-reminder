package com.example.weilun.birthdayreminder;

import android.app.AlarmManager;
import android.app.LoaderManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TabLayout;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        UpComingBirthdayFragment.Countable,
        ContactListFragment.Refreshable,
        QuoteFragment.Receivable,
        LoaderManager.LoaderCallbacks<JSONObject> {

    public static final int TEST_NOTIFICATION_ID = 1;
    private static final int BACKUP_LOADER_ID = 1;
    private static final String WELCOME_MESSAGE = "welcomeScreen";
    private TabLayout tabLayout;
    private List<Quote> quotes;
    private SimpleFragmentPageAdapter adapter;
    private String LOG_TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        showWelcomeMessage();
        startNotification();

        ViewPager viewPager = (ViewPager) findViewById(R.id.view_pager);
        adapter = new SimpleFragmentPageAdapter(this, getSupportFragmentManager());
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(1);

        tabLayout = (TabLayout) findViewById(R.id.tab);
        tabLayout.setupWithViewPager(viewPager);
        tabLayout.getTabAt(1).setIcon(R.drawable.ic_contacts_white_24dp);
        tabLayout.getTabAt(2).setIcon(R.drawable.ic_format_quote_white_24dp);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, AddBirthdayActivity.class);
                if (intent.resolveActivity(getPackageManager()) != null)
                    startActivity(intent);
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
    }

    @Override
    public void getCount(int count) {
        if (count != 0)
            tabLayout.getTabAt(0).setText(getString(R.string.tab_title_upcoming) + " (" + count + ")");
        else
            tabLayout.getTabAt(0).setText(getString(R.string.tab_title_upcoming));
    }

    @Override
    public void onRefresh() {
        ((UpComingBirthdayFragment) adapter.getRegisteredFragment(0)).onRefreshView();
    }

    @Override
    public void onReceive(List<Quote> quotes) {
        this.quotes = quotes;
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_backup) {
            backupToCloud();
            Log.v("backupToCloud", "trigger from menu");
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        {
            if (id == R.id.test_noti) {
                testNotification();
            } else if (id == R.id.random_quote) {
                showQuoteDialog();
            } else if (id == R.id.nav_backup) {
                backupToCloud();
                Log.v("backupToCloud", "trigger from navigationbar");
            } else if (id == R.id.about) {
                showWelcomeDialog();
            }
            DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
            drawer.closeDrawer(GravityCompat.START);
            return true;
        }
    }

    /**
     * helper method to show quote of the day dialog
     */
    private void showQuoteDialog() {
        if (quotes == null) {
            Toast.makeText(this, R.string.no_quote_found, Toast.LENGTH_SHORT).show();
            return;
        }
        int max = quotes.size();
        final Quote quote = quotes.get(new Random().nextInt(max)); // ((max - min) + 1) + min
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(quote.getQuote()).setTitle(quote.getAuthor())
                .setPositiveButton(R.string.btn_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setNeutralButton(R.string.dialog_copy_text, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                        ClipData clip = ClipData.newPlainText(getString(R.string.dialog_copy_text), quote.getQuote());
                        clipboard.setPrimaryClip(clip);
                        Toast.makeText(MainActivity.this, getString(R.string.copied_success), Toast.LENGTH_SHORT).show();
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * helper method to show welcome message when user first launch the app
     */
    private void showWelcomeMessage() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        Boolean welcomeScreenShowed = sharedPreferences.getBoolean(WELCOME_MESSAGE, false);
        if (!welcomeScreenShowed) {
            showWelcomeDialog();
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(WELCOME_MESSAGE, true);
            editor.commit();
        }
    }

    /**
     * helper method to show welcoem dialog
     */
    private void showWelcomeDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.welcome_title)
                .setMessage(R.string.welcome_message)
                .setPositiveButton(R.string.btn_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).show();
    }

    /**
     * helper method to init the notification service
     */
    private void startNotification() {
        AlarmManager alarmManager;

        Calendar calendar = Calendar.getInstance();
        Calendar now = Calendar.getInstance();
        Date date = new Date();
        now.setTime(date);
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, 8);
        calendar.set(Calendar.MINUTE, 00);
        calendar.set(Calendar.SECOND, 00);

        Log.v(LOG_TAG, "Time: " + calendar.getTimeInMillis() + "");

        if (calendar.before(now)) {
            calendar.add(Calendar.DATE, 1);
        }

        Intent intent = new Intent(this, NotiReceiver.class);
        PendingIntent alarmIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), AlarmManager.INTERVAL_DAY, alarmIntent);

        Log.v(LOG_TAG, "Alarm started");
    }

    /**
     * helper method to test notification
     */
    private void testNotification() {
        Notification.Builder builder = new Notification.Builder(this);
        builder.setContentTitle(getString(R.string.notification_title))
                .setContentText(getString(R.string.test_noti))
                .setAutoCancel(true)
                .setSmallIcon(R.drawable.ic_announcement_black_24dp)
                .setColor(getResources().getColor(R.color.colorPrimary))
                .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                        R.mipmap.birthday_icon_launcher))
                .setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE);
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(TEST_NOTIFICATION_ID, builder.build());
    }

    /**
     * helper mehtod do perform backup to cloud
     */
    private void backupToCloud() {
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            getLoaderManager().restartLoader(BACKUP_LOADER_ID, null, this).forceLoad();
        } else {
            Toast.makeText(this, getString(R.string.no_network), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public Loader<JSONObject> onCreateLoader(int id, Bundle args) {
        Log.v(LOG_TAG, "BackupLoader Created");
        return new BackupLoader(this);
    }

    @Override
    public void onLoadFinished(Loader<JSONObject> loader, JSONObject data) {
        Loader<JSONObject> backupLoader = getLoaderManager().getLoader(BACKUP_LOADER_ID);
        BackupLoader backupLoader1 = (BackupLoader) backupLoader;
        if (backupLoader1.progressDialogIsShow())
            backupLoader1.stopProgressDialog();
        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setMessage(String.format(getString(R.string.backup_success)
                , extraCodeFromJSON(data)));
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, getString(R.string.btn_ok),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        alertDialog.show();
    }

    @Override
    public void onLoaderReset(Loader<JSONObject> loader) {
    }

    /**
     * helper method to convert response in JSONObject form to int
     *
     * @param jsonObj
     * @return response code
     */
    private int extraCodeFromJSON(JSONObject jsonObj) {
        try {
            return jsonObj.getInt("recordsSynced");
        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage());
            return 0;
        }
    }
}