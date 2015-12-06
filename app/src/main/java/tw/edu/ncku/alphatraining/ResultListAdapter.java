package tw.edu.ncku.alphatraining;

import android.database.DataSetObserver;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;

import java.util.ArrayList;

public abstract class ResultListAdapter<K> implements ListAdapter {
    ArrayList<K> list = new ArrayList<>();

    public void appendResult(K data){
        list.add(data);
    }

    @Override
    public void registerDataSetObserver(DataSetObserver observer) {

    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {

    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public K getItem(int position) {
        return list.get(position);
    }

    @Override
    public long getItemId(int position) {
        if(position >= list.size())
            throw new ArrayIndexOutOfBoundsException();
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public abstract View getView(int position, View convertView, ViewGroup parent);

    @Override
    public int getItemViewType(int position) {
        if(position >= list.size())
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
        if(position >= list.size())
            throw new ArrayIndexOutOfBoundsException();
        return true;
    }

    @Override
    public boolean isEmpty() {
        return list.isEmpty();
    }
}
