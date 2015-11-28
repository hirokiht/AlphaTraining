package tw.edu.ncku.alphatraining;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import java.util.Collections;
import java.util.List;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link DeviceSelectFragment.OnDeviceSelectedListener} interface
 * to handle interaction events.
 */
public class DeviceSelectFragment extends Fragment{
    final private static BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private Switch btSwitch;
    private Spinner deviceSpinner;
    private Button btSelectBtn;
    private static ArrayAdapter<BluetoothDevice> deviceArrayAdapter;
    private BluetoothDevice device;
    private OnDeviceSelectedListener mListener;
    final private static List<ScanFilter> filters = Collections.singletonList(
            new ScanFilter.Builder().setServiceUuid(new ParcelUuid(AdcManager.ADC_SERVICE_UUID)).build());
    final private static ScanCallback scanCb = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            if (deviceArrayAdapter != null && deviceArrayAdapter.getPosition(result.getDevice()) < 0) {
                deviceArrayAdapter.add(result.getDevice());
            }
        }
    };
    private BroadcastReceiver bcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,0) == BluetoothAdapter.STATE_ON)
                mBluetoothAdapter.getBluetoothLeScanner().startScan(filters, new ScanSettings.Builder().build(),scanCb);

        }
    };


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_device_select, container, false);
        btSwitch = (Switch) view.findViewById(R.id.btSwitch);
        deviceSpinner = (Spinner) view.findViewById(R.id.deviceSpinner);
        btSelectBtn = (Button) view.findViewById(R.id.btSelectBtn);
        btSelectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onDeviceSelected(deviceArrayAdapter.getItem(deviceSpinner.getSelectedItemPosition()));
            }
        });
        deviceSpinner.setAdapter(deviceArrayAdapter);
        deviceSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (view == null)
                    return;
                if (device != deviceArrayAdapter.getItem(position)) {
                    device = deviceArrayAdapter.getItem(position);
                    btSelectBtn.setEnabled(device != null);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnDeviceSelectedListener) {
            mListener = (OnDeviceSelectedListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnDeviceSelectedListener");
        }
        context.registerReceiver(bcastReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
        if(deviceArrayAdapter == null){
            deviceArrayAdapter = new ArrayAdapter<BluetoothDevice>(getContext(),R.layout.support_simple_spinner_dropdown_item){
                @Override
                public View getDropDownView(int position, View convertView, ViewGroup parent) {
                    BluetoothDevice device = getItem(position);
                    TextView textView = device == null? (TextView)getActivity().getLayoutInflater().inflate(
                            R.layout.support_simple_spinner_dropdown_item,parent,false) :
                            (TextView) super.getDropDownView(position, convertView, parent);
                    textView.setText(device == null? "(None)" :
                            device.getName() == null? device.getAddress() :
                            device.getName()+"("+device.getAddress()+")");
                    return textView;
                }

                @Override
                public View getView(int position, View convertView, ViewGroup parent) {
                    BluetoothDevice device = getItem(position);
                    TextView textView = device == null? new TextView(getContext()) :
                            (TextView) super.getView(position, convertView, parent);
                    textView.setText(device == null? "Select the Bluetooth Device: " :
                            device.getName() == null? device.getAddress() :
                                    device.getName()+"("+device.getAddress()+")");
                    return textView;
                }
            };
            deviceArrayAdapter.add(null);
        }
    }

    @Override
    public void onDetach() {
        getContext().unregisterReceiver(bcastReceiver);
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onResume(){
        super.onResume();
        if(mBluetoothAdapter == null){
            new AlertDialog.Builder(getContext()).setMessage(R.string.require_bt).
                    setNeutralButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            getActivity().finish();
                        }
            }).create().show();
        }else if(!mBluetoothAdapter.isEnabled()){
            ((MainActivity)getActivity()).toolbar.setTitle(R.string.require_bt);
            btSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if(isChecked){
                        mBluetoothAdapter.enable();
                        startDeviceSelect();
                    }
                }
            });
            btSwitch.setVisibility(View.VISIBLE);
        }else startDeviceSelect();
    }

    private void startDeviceSelect(){
        ((MainActivity)getActivity()).toolbar.setTitle(R.string.select_device);
        btSwitch.setVisibility(View.GONE);
        deviceSpinner.setVisibility(View.VISIBLE);
        btSelectBtn.setVisibility(View.VISIBLE);
        btSelectBtn.setEnabled(false);
        if(mBluetoothAdapter.isEnabled())
            mBluetoothAdapter.getBluetoothLeScanner().startScan(filters, new ScanSettings.Builder().build(), scanCb);
    }

    public interface OnDeviceSelectedListener {
        void onDeviceSelected(BluetoothDevice device);
    }
}
