package tw.edu.ncku.alphatraining;

import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ToggleButton;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {
    private ToneGenerator toneGenerator = new ToneGenerator(AudioManager.STREAM_NOTIFICATION,ToneGenerator.MAX_VOLUME);
    private ToggleButton theButton = null;
    private TextView timeText = null;
    private ProgressBar timeProgress = null;
    private CountDownTimer timer;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        timeText = (TextView) findViewById(R.id.timeText);
        theButton = (ToggleButton) findViewById(R.id.theButton);
        timeProgress = (ProgressBar) findViewById(R.id.progressBar);
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

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
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
        int id = item.getItemId();

        if (id == R.id.nav_init) {
            timeProgress.setMax(120);
            timeProgress.setProgress(120);
            timeText.setText("120");
            theButton.setEnabled(true);
        } else if (id == R.id.nav_capture) {

        } else if (id == R.id.nav_result) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public void onClick(View view){
        if(theButton.isChecked()) {
            timer = new CountDownTimer(timeProgress.getMax()*1000,1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    timeText.setText(Long.toString(millisUntilFinished/1000));
                    timeProgress.setProgress((int)millisUntilFinished/1000);
                }

                @Override
                public void onFinish() {
                    toneGenerator.startTone(ToneGenerator.TONE_PROP_ACK);
                    timeText.setText("0");
                    timeProgress.setProgress(0);
                    AlphaAnimation anim = new AlphaAnimation(0.0f, 1.0f);
                    anim.setDuration(40); //You can manage the blinking time with this parameter
                    anim.setStartOffset(160);
                    anim.setRepeatMode(Animation.REVERSE);
                    anim.setRepeatCount(5);
                    timeText.startAnimation(anim);
                    timer = null;
                }
            };
            timer.start();
        }else{
            if(timer != null)
                timer.cancel();
            timeProgress.setProgress(timeProgress.getMax());
            timeText.setText(Integer.toString(timeProgress.getMax()));
        }
    }
}
