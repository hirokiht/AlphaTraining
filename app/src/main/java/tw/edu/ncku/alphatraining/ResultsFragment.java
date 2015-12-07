package tw.edu.ncku.alphatraining;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.TextView;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.LegendRenderer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link ResultsFragment.OnResultSendListener} interface
 * to handle interaction events.
 */
public class ResultsFragment extends Fragment implements View.OnClickListener{
    private static float baseline = 0f;
    private AbsListView resultList;
    private static DateFormat dateFormat;
    private static final ArrayList<Date> dates = new ArrayList<>();
    private static final ArrayList<float[]> rawData = new ArrayList<>();
    private static final ResultListAdapter<float[]> listAdapter = new ResultListAdapter<float[]>() {
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            int count = 0;
            float[] data = listAdapter.getItem(position);
            for(int i = 1 ; data.length > 1 && i < data.length ; i++)
                if(data[i] >= baseline && data[i-1] < baseline)
                    count++;
            TextView textView = new TextView(parent.getContext(),null,android.R.attr.textAppearanceLarge);
            textView.setIncludeFontPadding(true);
            textView.setText(dateFormat.format(dates.get(position))+" "+parent.getResources().getString(R.string.alpha_legend)+": "+count);
            if(!((AbsListView)parent).isItemChecked(position))
                return textView;
            GraphView graph = new GraphView(parent.getContext());
            graph.setTitle(textView.getText().toString());
            graph.setTitleTextSize(textView.getTextSize());
            graph.getViewport().setXAxisBoundsManual(true);
            LineGraphSeries<DataPoint> dataSeries = new LineGraphSeries<>();
            float[] values = list.get(position);
            for(int i = 0 ; i < values.length ; i++)
                dataSeries.appendData(new DataPoint(i/2f,values[i]), false, values.length);
            LineGraphSeries<DataPoint> baselineSeries = new LineGraphSeries<>(
                    new DataPoint[]{new DataPoint(0f,ResultsFragment.getBaseline()),
                            new DataPoint(dataSeries.getHighestValueX(),ResultsFragment.getBaseline())});
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                dataSeries.setColor(parent.getResources().getColor(R.color.colorPrimary,null));
                baselineSeries.setColor(parent.getResources().getColor(R.color.colorAccent,null));
            }else{
                //noinspection deprecation
                dataSeries.setColor(parent.getResources().getColor(R.color.colorPrimary));
                //noinspection deprecation
                baselineSeries.setColor(parent.getResources().getColor(R.color.colorAccent));
            }
            dataSeries.setTitle(parent.getContext().getString(R.string.alpha_legend));
            baselineSeries.setTitle(parent.getContext().getString(R.string.baseline_legend));
            graph.addSeries(dataSeries);
            graph.addSeries(baselineSeries);
            graph.getLegendRenderer().setVisible(true);
            graph.getLegendRenderer().setAlign(LegendRenderer.LegendAlign.TOP);
            graph.getViewport().setMinX(dataSeries.getLowestValueX());
            graph.getViewport().setMaxX(dataSeries.getHighestValueX());
            graph.setMinimumHeight(parent.getHeight()>parent.getWidth()?
                    parent.getWidth() / 2 : parent.getHeight()/2);
            return graph;
        }
    };

    private OnResultSendListener mListener;

    public ResultsFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_results, container, false);
        final FloatingActionButton fab = (FloatingActionButton) view.findViewById(R.id.sendActionBtn);
        resultList = (AbsListView) view.findViewById(R.id.resultList);
        fab.setOnClickListener(this);
        resultList.setAdapter(listAdapter);
        resultList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ((AbsListView) parent).setItemChecked(position, true);
            }
        });
        return view;
    }

    public void onClick(View view) {
        if (mListener != null) {
            final int i = resultList.getCheckedItemPosition();
            String energyStr = Arrays.toString(listAdapter.getItem(i));
            energyStr = energyStr.substring(1,energyStr.length()-1);
            String rawDataStr = Arrays.toString(rawData.get(i));
            rawDataStr = rawDataStr.substring(1,rawDataStr.length()-1);
            String stringResult = getString(R.string.timestamp)+","+dateFormat.format(dates.get(i))
                    +"\n"+getString(R.string.baseline)+","+baseline
                    +"\n"+getString(R.string.alpha_legend)+","+energyStr
                    +"\n"+getString(R.string.raw_data)+","+rawDataStr;
            mListener.onResultSend(stringResult);
        }
    }

    @SuppressLint("SimpleDateFormat")
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        dateFormat = new SimpleDateFormat(context.getString(R.string.date_format));
        if (context instanceof OnResultSendListener) {
            mListener = (OnResultSendListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnResultSendListener");
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState){
        super.onActivityCreated(savedInstanceState);
        ((MainActivity)getActivity()).navigationView.setCheckedItem(R.id.nav_result);
        //noinspection ConstantConditions
        ((MainActivity)getActivity()).getSupportActionBar().setTitle(R.string.getResult);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public static boolean isEmpty(){
        return rawData.isEmpty();
    }

    public static float getBaseline(){
        return baseline;
    }

    public static void setBaseline(float baseline){
        ResultsFragment.baseline = baseline;
    }

    public static void appendResult(float[] data, float[] result){
        dates.add(new Date());
        rawData.add(data);
        listAdapter.appendResult(result);
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
    public interface OnResultSendListener {
        void onResultSend(String result);
    }
}
