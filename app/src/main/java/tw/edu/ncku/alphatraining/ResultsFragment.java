package tw.edu.ncku.alphatraining;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;

import java.text.SimpleDateFormat;
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
    private final static ResultListAdapter<String> listAdapter = new ResultListAdapter<>();

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
            String key = listAdapter.getKey(resultList.getCheckedItemPosition());
            float[] result = listAdapter.getItem(resultList.getCheckedItemPosition());
            String stringResult = Arrays.toString(result).substring(1);
            stringResult = stringResult.substring(0,stringResult.length()-1);
            stringResult = "Timestamp,"+key+"\nBaseline,"+baseline+"\nEnergy Data,"+stringResult;
            mListener.onResultSend(stringResult);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnResultSendListener) {
            mListener = (OnResultSendListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnResultSendListener");
        }
        ((MainActivity)getActivity()).navigationView.setCheckedItem(R.id.nav_result);
        //noinspection ConstantConditions
        ((MainActivity)getActivity()).getSupportActionBar().setTitle(R.string.getResult);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public static float getBaseline(){
        return baseline;
    }

    public static void setBaseline(float baseline){
        ResultsFragment.baseline = baseline;
    }

    @SuppressLint("SimpleDateFormat")
    public void appendResult(float[] result){
        listAdapter.appendResult(new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date()),result);
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
