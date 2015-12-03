package tw.edu.ncku.alphatraining;

import android.content.Context;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.LegendRenderer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.util.ArrayList;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link CaptureSessionFragment.SessionFragmentListener} interface
 * to handle interaction events.
 */
public class CaptureSessionFragment extends Fragment implements CompoundButton.OnCheckedChangeListener {
    private static ToneGenerator toneGenerator = new ToneGenerator(AudioManager.STREAM_NOTIFICATION,ToneGenerator.MAX_VOLUME);
    private SessionFragmentListener mListener;

    private final Handler handler = new Handler();
    private CountDownTimer timer;
    private final static int countDownSeconds = 6*60;
    private static final int energyDataWindowSize = 10 * 1000 / MainActivity.SAMPLING_PERIOD;
    private static final int graphScale = 100;

    private TextView timeText;
    private ToggleButton theButton;
    private GraphView energyGraph;
    private Runnable task;
    private LineGraphSeries<DataPoint> energySeries = new LineGraphSeries<>(),
        baselineSeries = new LineGraphSeries<>();
    private ArrayList<Float> energyData = new ArrayList<>(countDownSeconds*2);
    private float baseline = 0f;
    private int alphaCount = 0;


    @SuppressWarnings("deprecation")
    public CaptureSessionFragment() {
        energySeries.setTitle("Alpha Energy");
        baselineSeries.setTitle("Baseline Energy");
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_capture_session, container, false);
        timeText = (TextView) view.findViewById(R.id.timeText);
        theButton = (ToggleButton) view.findViewById(R.id.theButton);
        energyGraph = (GraphView) view.findViewById(R.id.energyGraph);
        timeText.setText(countDownSeconds / 60 + ":" + String.format("%02d", countDownSeconds % 60));
        theButton.setOnCheckedChangeListener(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            energySeries.setColor(getResources().getColor(R.color.colorPrimary,null));
            baselineSeries.setColor(getResources().getColor(R.color.colorAccent,null));
        }else{
            //noinspection deprecation
            energySeries.setColor(getResources().getColor(R.color.colorPrimary));
            //noinspection deprecation
            baselineSeries.setColor(getResources().getColor(R.color.colorAccent));
        }
        energyGraph.addSeries(energySeries);
        energyGraph.addSeries(baselineSeries);
        energyGraph.getViewport().setXAxisBoundsManual(true);
        energyGraph.getGridLabelRenderer().setPadding(32);
        energyGraph.getLegendRenderer().setVisible(true);
        energyGraph.getLegendRenderer().setAlign(LegendRenderer.LegendAlign.TOP);
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
        if (context instanceof SessionFragmentListener) {
            mListener = (SessionFragmentListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement SessionFragmentListener");
        }
        ((MainActivity)getActivity()).navigationView.setCheckedItem(R.id.nav_capture);
        //noinspection ConstantConditions
        ((MainActivity)getActivity()).getSupportActionBar().setTitle(R.string.startSession);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if(isChecked) {
            timer = new CountDownTimer(countDownSeconds*1000,1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    timeText.setText(millisUntilFinished / 60000+":"+String.format("%02d",millisUntilFinished/1000%60));
                }

                @Override
                public void onFinish() {
                    toneGenerator.startTone(ToneGenerator.TONE_PROP_ACK);
                    timeText.setText("00:00");
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
            alphaCount = 0;
            baseline = mListener.onSessionStart()*1.5f;
            energyData.clear();
            handler.post(task = new Runnable() {
                @Override
                public void run() {
                    energySeries.resetData(new DataPoint[]{new DataPoint(0, 0)});
                    baselineSeries.resetData(new DataPoint[]{new DataPoint(0, baseline*graphScale)});
                }
            });
        }else{
            if(timer == null) {
                float[] data = new float[energyData.size()];
                for(int i = 0 ; i < data.length ; i++)
                    data[i] = energyData.get(i);
                mListener.onSessionFinish(data,alphaCount);
            }else{
                mListener.onSessionStop();
                timer.cancel();
            }
            timeText.setText(countDownSeconds/60+":"+String.format("%02d",countDownSeconds%60));
        }
    }

    public void appendEnergyData(final float datum){
        handler.post(task = new Runnable() {
            @Override
            public void run() {
                baselineSeries.appendData(new DataPoint(baselineSeries.isEmpty() ? 0f :
                        baselineSeries.getHighestValueX() + 0.5f, baseline*graphScale), true, energyDataWindowSize);
                energySeries.appendData(new DataPoint(energySeries.isEmpty() ? 0f :
                        energySeries.getHighestValueX() + 0.5f, datum*graphScale), true, energyDataWindowSize);
                energyGraph.getViewport().setMinX(energySeries.getLowestValueX());
                energyGraph.getViewport().setMaxX(energySeries.getHighestValueX());
            }
        });
        energyData.add(datum);
        if(datum >= baseline)
            alphaCount++;
    }

    public void reset() {
        handler.post(task = new Runnable() {
            @Override
            public void run() {
                energySeries.resetData(new DataPoint[]{new DataPoint(0, 0)});
                baselineSeries.resetData(new DataPoint[]{new DataPoint(0, baseline*graphScale)});
            }
        });
        if(timer != null) {
            timer.cancel();
            timer = null;
        }
        timeText.setText(countDownSeconds / 60 + ":" + String.format("%02d",countDownSeconds%60));
        theButton.setChecked(false);
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface SessionFragmentListener {
        float onSessionStart(); //return baseline
        void onSessionStop();
        void onSessionFinish(@NonNull float[] data, int alphaCount);
    }
}
