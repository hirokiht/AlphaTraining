package tw.edu.ncku.alphatraining;

import android.database.DataSetObserver;
import android.os.Build;
import android.support.v4.util.SimpleArrayMap;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.LegendRenderer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

public class ResultListAdapter<K> implements ListAdapter {
    SimpleArrayMap<K,float[]> map = new SimpleArrayMap<>();

    public void appendResult(K key, float[] data){
        map.put(key,data);
    }

    public K getKey(int index){
        if(index >= map.size())
            throw new ArrayIndexOutOfBoundsException();
        return map.keyAt(index);
    }

    @Override
    public void registerDataSetObserver(DataSetObserver observer) {

    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {

    }

    @Override
    public int getCount() {
        return map.size();
    }

    @Override
    public float[] getItem(int position) {
        if(position >= map.size())
            throw new ArrayIndexOutOfBoundsException();
        return map.valueAt(position);
    }

    @Override
    public long getItemId(int position) {
        if(position >= map.size())
            throw new ArrayIndexOutOfBoundsException();
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        K key = map.keyAt(position);
        float[] values = map.valueAt(position);
        int count = 0;
        for(int i = 1 ; values.length > 1 && i < values.length ; i++)
            if(values[i] >= ResultsFragment.getBaseline() && values[i-1] < ResultsFragment.getBaseline())
                count++;
        TextView textView = new TextView(parent.getContext(),null,android.R.attr.textAppearanceLarge);
        textView.setIncludeFontPadding(true);
        textView.setText(key.toString()+" Alpha: "+count);
        if(!((AbsListView)parent).isItemChecked(position))
            return textView;
        GraphView graph = new GraphView(parent.getContext());
        graph.setTitle(textView.getText().toString());
        graph.setTitleTextSize(textView.getTextSize());
        graph.getViewport().setXAxisBoundsManual(true);
        LineGraphSeries<DataPoint> dataSeries = new LineGraphSeries<>();
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
        graph.setMinimumHeight(parent.getWidth() / 2);
        return graph;
    }

    @Override
    public int getItemViewType(int position) {
        if(position >= map.size())
            throw new ArrayIndexOutOfBoundsException();
        return 0;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public boolean areAllItemsEnabled() {
        return true;
    }

    @Override
    public boolean isEnabled(int position) {
        if(position >= map.size())
            throw new ArrayIndexOutOfBoundsException();
        return true;
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }
}
