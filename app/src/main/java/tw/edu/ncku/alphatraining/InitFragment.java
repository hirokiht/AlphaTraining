package tw.edu.ncku.alphatraining;

import android.content.Context;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

public class InitFragment extends Fragment implements CompoundButton.OnCheckedChangeListener{
    private static ToneGenerator toneGenerator = new ToneGenerator(AudioManager.STREAM_NOTIFICATION,ToneGenerator.MAX_VOLUME);
    private ToggleButton theButton = null;
    private TextView timeText = null;
    private ProgressBar timeProgress = null;
    private static CountDownTimer timer;
    private GraphView graphView;
    private final Handler handler = new Handler();
    private Runnable task;
    private LineGraphSeries<DataPoint> rawDataSeries = new LineGraphSeries<>();
    private int dataSize = 0;   //used to count avg
    private float totalEnergy = 0f;
    private static final int rawDataWindowSize =10 * 1000 / MainActivity.SAMPLING_PERIOD;

    private final static int countDownSeconds = 120;

    private OnInitFragmentInteractionListener mListener;

    public InitFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_init, container, false);
        timeText = (TextView) view.findViewById(R.id.timeText);
        theButton = (ToggleButton) view.findViewById(R.id.theButton);
        timeProgress = (ProgressBar) view.findViewById(R.id.progressBar);
        timeProgress.setMax(countDownSeconds);
        timeProgress.setProgress(countDownSeconds);
        timeText.setText(Integer.toString(countDownSeconds));
        theButton.setOnCheckedChangeListener(this);
        graphView = (GraphView) view.findViewById(R.id.rawDataGraph);
        graphView.addSeries(rawDataSeries);
        graphView.getViewport().setXAxisBoundsManual(true);
        graphView.getGridLabelRenderer().setPadding(32);
        return view;
    }

    @Override
    public void onPause(){
        handler.removeCallbacks(task);
        super.onPause();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnInitFragmentInteractionListener) {
            mListener = (OnInitFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnInitFragmentInteractionListener");
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState){
        super.onActivityCreated(savedInstanceState);
        ((MainActivity)getActivity()).navigationView.setCheckedItem(R.id.nav_init);
        //noinspection ConstantConditions
        ((MainActivity)getActivity()).getSupportActionBar().setTitle(R.string.getBaseline);
        setRetainInstance(true);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if(isChecked && timer == null) {
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
                    theButton.setChecked(false);
                }
            };
            timer.start();
            mListener.onInitStart();
        }else if(!isChecked){
            if(timeProgress.getProgress() == 0)
                mListener.onInitFinish(totalEnergy/dataSize);
            else mListener.onInitCancel();
            if(timer != null)
                timer.cancel();
            timeProgress.setProgress(timeProgress.getMax());
            timeText.setText(Integer.toString(timeProgress.getMax()));
        }
    }

    public void addEnergyData(final float energy){
        if(timer != null) {
            totalEnergy += energy;
            dataSize++;
        }
    }

    public void appendRawData(final float datum){
        if(timer != null)
            handler.post(new Runnable() {
                @Override
                public void run() {
                    rawDataSeries.appendData(new DataPoint(rawDataSeries.isEmpty() ? 0f :
                            rawDataSeries.getHighestValueX() + 0.5f, datum*100), true, rawDataWindowSize);
                    graphView.getViewport().setMinX(rawDataSeries.getLowestValueX());
                    graphView.getViewport().setMaxX(rawDataSeries.getHighestValueX());
                }
            });
    }

    public void appendRawData(final float[] data){
        if(timer != null)
            handler.post(task = new Runnable() {
                @Override
                public void run() {
                    for(final double datum : data)
                        rawDataSeries.appendData(new DataPoint(rawDataSeries.isEmpty() ? 0f :
                            rawDataSeries.getHighestValueX() + 0.5f, datum*100), true, rawDataWindowSize);
                    graphView.getViewport().setMinX(rawDataSeries.getLowestValueX());
                    graphView.getViewport().setMaxX(rawDataSeries.getHighestValueX());
                }
            });
    }

    public void resetData(){
        totalEnergy = 0f;
        dataSize = 0;
        handler.post(task = new Runnable() {
            @Override
            public void run() {
                rawDataSeries.resetData(new DataPoint[]{});
            }
        });
    }

    public interface OnInitFragmentInteractionListener {
        void onInitStart();
        void onInitCancel();
        void onInitFinish(float avg);
    }
}
