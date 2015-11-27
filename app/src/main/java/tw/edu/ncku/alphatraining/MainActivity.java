package tw.edu.ncku.alphatraining;

import android.Manifest;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        InitFragment.OnInitFragmentInteractionListener, DeviceSelectFragment.OnDeviceSelectedListener {
    private final static Fragment initFragment = new InitFragment();
    private final static Fragment deviceSelectFragment = new DeviceSelectFragment();
    private final FragmentManager fragmentManager = getSupportFragmentManager();
    private final static int REQUEST_COARSE_LOCATION = 1;
    private  DrawerLayout drawer;
    private ActionBarDrawerToggle toggle;
    private ProgressDialog progressDialog = null;
    private static boolean waitingPermission = false;
    private final AdcManager adcManager = new AdcManager(this, new AdcManager.AdcListener() {
        @Override
        public void onConnectionStateChange(int newState) {
            if(newState == BluetoothProfile.STATE_CONNECTED){
                if(progressDialog != null)
                    progressDialog.dismiss();
            }
        }

        @Override
        public void onSamplingPeriodChanged(short sampling_period) {

        }

        @Override
        public void onDataReceived(short data) {

        }

        @Override
        public void onDataBufferReceived(byte[] buffer) {

        }

        @Override
        public void onDataBufferReceived(short[] buffer) {

        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_COARSE_LOCATION);
            waitingPermission = true;
        }else startDeviceSelect();
        Log.d("MainActivity","After request permission");
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
    }

    @Override
    protected void onResume(){
        super.onResume();
        if(!waitingPermission)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED)
                finish();
            else startDeviceSelect();
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
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        final int id = item.getItemId();
        if (id == R.id.nav_init) {
            fragmentManager.beginTransaction().replace(R.id.content_frame, initFragment).commit();
        } else if (id == R.id.nav_capture) {

        } else if (id == R.id.nav_result) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults){
        if(requestCode == REQUEST_COARSE_LOCATION && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            waitingPermission = false;
        else finish();
    }

    @Override
    public void onInitStart() {
        Log.d("MainActivity", "Init onInitStart");
    }

    @Override
    public void onInitCancel() {
        Log.d("MainActivity", "Init onInitCancel");

    }

    @Override
    public void onInitFinish() {
        Log.d("MainActivity", "Init onInitFinish");

    }

    @Override
    public void onDeviceSelected(BluetoothDevice device) {
        progressDialog = ProgressDialog.show(this, "Please Wait","Connecting...");
        Log.d("MainActivity", "Device Selected: " + device);
        fragmentManager.beginTransaction().replace(R.id.content_frame, initFragment).commit();
        drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
        toggle.setDrawerIndicatorEnabled(true);
        adcManager.setDevice(device);
    }

    private void startDeviceSelect(){
        fragmentManager.beginTransaction().replace(R.id.content_frame, deviceSelectFragment).commit();
        drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        toggle.setDrawerIndicatorEnabled(false);
    }
}
