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
import android.widget.TextView;
import android.widget.ToggleButton;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;


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

    private TextView timeText;
    private ToggleButton theButton;
    private GraphView energyGraph;
    private Runnable task;
    private LineGraphSeries<DataPoint> energySeries = new LineGraphSeries<>();

    public CaptureSessionFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_capture_session, container, false);
        timeText = (TextView) view.findViewById(R.id.timeText);
        theButton = (ToggleButton) view.findViewById(R.id.theButton);
        energyGraph = (GraphView) view.findViewById(R.id.energyGraph);
        timeText.setText(countDownSeconds/60+":"+String.format("%02d",countDownSeconds%60));
        theButton.setOnCheckedChangeListener(this);
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
            mListener.onSessionStart();
        }else{
            if(timer == null)
                mListener.onSessionFinish();
            else{
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
                energySeries.appendData(new DataPoint(energySeries.isEmpty() ? 0f :
                        energySeries.getHighestValueX() + 0.5f, datum*100), true, energyDataWindowSize);
                energyGraph.getViewport().setMinX(energySeries.getLowestValueX());
                energyGraph.getViewport().setMaxX(energySeries.getHighestValueX());
            }
        });
    }

    public void reset(){
        handler.post(task = new Runnable() {
            @Override
            public void run() {
                energySeries.resetData(new DataPoint[]{new DataPoint(0, 0)});
            }
        });
        if(timer != null) {
            timer.cancel();
            timer = null;
        }
        timeText.setText(countDownSeconds/60+":"+String.format("%02d",countDownSeconds%60));
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
        void onSessionStart();
        void onSessionStop();
        void onSessionFinish();
    }
}
