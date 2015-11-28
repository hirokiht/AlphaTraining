package tw.edu.ncku.alphatraining;

import android.Manifest;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.Toast;

import org.jtransforms.fft.FloatFFT_1D;

import java.util.ArrayDeque;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        InitFragment.OnInitFragmentInteractionListener, DeviceSelectFragment.OnDeviceSelectedListener {
    private static final String TAG = "MainActivity";
    private final static InitFragment initFragment = new InitFragment();
    private final static Fragment deviceSelectFragment = new DeviceSelectFragment();
    private final FragmentManager fragmentManager = getSupportFragmentManager();
    private final static int REQUEST_COARSE_LOCATION = 1;
    private  DrawerLayout drawer;
    protected Toolbar toolbar;
    private ActionBarDrawerToggle toggle;
    private ProgressDialog progressDialog = null;
    private static boolean waitingPermission = false;
    private final static ArrayDeque<Float> queue = new ArrayDeque<>(32);
    public final static short SAMPLING_PERIOD = 1000/32;   //sample at 32Hz, sampling period is ms resolution
    private float totalEnergy = 0f, baseline = 0f;
    private int dataSize = 0;   //used to count avg
    private final static int BEGIN_FREQ = 8, END_FREQ = 12;
    private final float alpha = 0.54f, beta = 0.46f;    //parameters for hamming window
    private final static float[] windowFunction = new float[32];

    public MainActivity(){
        super();
        if(windowFunction[0] == 0f)
            for(int i = 0 ; i < windowFunction.length ; i++)
                windowFunction[i] = alpha-beta*(float)Math.cos(2*Math.PI*i/(windowFunction.length-1));
    }

    private final AdcManager adcManager = new AdcManager(this, new AdcManager.AdcListener() {
        @Override
        public void onConnectionStateChange(int newState) {
            if(newState == BluetoothProfile.STATE_CONNECTED){
                if(progressDialog != null)
                    progressDialog.dismiss();
                adcManager.saveSamplingPeriod(SAMPLING_PERIOD);
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
            float[] data = new float[buffer.length];
            for(int i = 0 ; i < data.length ; i++)
                data[i] = (float)buffer[i]/2048f;
            initFragment.appendRawData(data);
            for(float d : data)
                queue.push(d);
            if(queue.size() >= 32){
                processQueue();
            }
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

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
        Log.d(TAG, "After request permission");
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

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        final int id = item.getItemId();
        toolbar.setTitle(item.getTitle());
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
        queue.clear();
        totalEnergy = 0f;
        dataSize = 0;
        initFragment.resetRawData();
        adcManager.setBuffered12bitAdcNotification(true);
    }

    @Override
    public void onInitCancel() {
        adcManager.setBuffered12bitAdcNotification(false);
    }

    @Override
    public void onInitFinish() {
        adcManager.setBuffered12bitAdcNotification(false);
        baseline = totalEnergy/dataSize;
        Log.d(TAG, "Baseline: "+baseline);
        Toast.makeText(MainActivity.this, "Baseline Energy: "+ baseline, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDeviceSelected(BluetoothDevice device) {
        progressDialog = ProgressDialog.show(this, "Please Wait","Connecting...");
        Log.d(TAG, "Device Selected: " + device);
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

    private void processQueue(){
        float[] data = new float[queue.size()];
        Float[] qData = queue.toArray(new Float[data.length]);
        for(int i = 0 ; i < data.length ; i++)
            data[i++] = qData[i] == null ? Float.NaN : qData[i]*windowFunction[i];
        while(queue.size() != 16)
            queue.pop();
        FloatFFT_1D fft = new FloatFFT_1D(data.length);
        fft.realForward(data);
        float[] fftResult = new float[data.length/2+1];
        fftResult[0] = data[0];
        fftResult[fftResult.length-1] = data[1];
        for(int i = 2 ; i < data.length ; i+=2)
            fftResult[i>>1] = (float) Math.sqrt(data[i]*data[i]+data[i+1]*data[i+1]);
        for(int i = BEGIN_FREQ ; i < END_FREQ ; i++)
            totalEnergy += fftResult[i];
        dataSize++;
    }
}
