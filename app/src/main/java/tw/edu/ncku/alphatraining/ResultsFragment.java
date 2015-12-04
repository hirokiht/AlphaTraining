package tw.edu.ncku.alphatraining;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link ResultsFragment.OnResultSendListener} interface
 * to handle interaction events.
 */
public class ResultsFragment extends Fragment {
    private float baseline = 0f;

    private OnResultSendListener mListener;

    public ResultsFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_results, container, false);
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed() {
        if (mListener != null) {
            mListener.onResultSend("");
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

    public float getBaseline(){
        return baseline;
    }

    public void setBaseline(float baseline){
        this.baseline = baseline;
    }

    public void appendResult(float[] result){

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
