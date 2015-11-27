package tw.edu.ncku.androidlibrary;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.os.Build;

import java.util.LinkedList;
import java.util.Queue;

public class BleManager {
    protected interface BtRequest{
        boolean execute();
    }

    protected BluetoothGatt btGatt;
    protected BluetoothDevice device;
    protected Queue<BtRequest> btRequests = new LinkedList<>();
    protected boolean busy = true;
    private BleListener bleCallback;
    private Context ctx;

    public interface BleListener {
        void onConnectionStateChange(int newState);
        void onCharacteristicRead(BluetoothGattCharacteristic characteristic);
        void onCharacteristicWrite(BluetoothGattCharacteristic characteristic);
        void onCharacteristicUpdate(BluetoothGattCharacteristic characteristic);
    }

    public final BluetoothGattCallback btGattCb = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if(newState == BluetoothProfile.STATE_CONNECTED){
                gatt.discoverServices();
            }else if(newState == BluetoothProfile.STATE_DISCONNECTED){
                setDevice(null);
                bleCallback.onConnectionStateChange(newState);
            }else bleCallback.onConnectionStateChange(newState);
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            bleCallback.onConnectionStateChange(BluetoothProfile.STATE_CONNECTED);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            bleCallback.onCharacteristicRead(characteristic);
            pollRequests();
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status){
            super.onCharacteristicWrite(gatt, characteristic, status);
            bleCallback.onCharacteristicWrite(characteristic);
            pollRequests();
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            bleCallback.onCharacteristicUpdate(characteristic);
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorRead(gatt, descriptor, status);
            pollRequests();
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            pollRequests();
        }
    };

    public BleManager(Context context){
        ctx = context;
    }

    public interface BleDiscoveryListener{
        void onDeviceDiscovered(BluetoothDevice device);
    }

    public static void discoverDevices(final BleDiscoveryListener listener) {
        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        if(btAdapter == null || !btAdapter.isEnabled())
            return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if(btAdapter.getBluetoothLeScanner() != null)
                btAdapter.getBluetoothLeScanner().startScan(new ScanCallback() {
                    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
                    @Override
                    public void onScanResult(int callbackType, ScanResult result) {
                        super.onScanResult(callbackType, result);
                        listener.onDeviceDiscovered(result.getDevice());
                    }
                });
            else throw new UnsupportedOperationException("getBluetoothLeScanner() == null");
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            //noinspection deprecation
            btAdapter.startLeScan(new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
                    listener.onDeviceDiscovered(device);
                }
            });
        } else throw new UnsupportedOperationException("Only support Android 4.3 and above!");
    }

    public void setDevice(BluetoothDevice device){
        if(btGatt != null && device != this.device)
            btGatt.close();
        if(this.device == null) {
            this.device = device;
            btGatt = this.device != null? this.device.connectGatt(ctx, false, btGattCb) : null;
            return;
        }
        this.device = device;
        btRequests.clear();
        busy = true;
        btGatt = this.device != null? this.device.connectGatt(ctx, false, btGattCb) : null;
        if(btGatt == null)
            bleCallback.onConnectionStateChange(BluetoothProfile.STATE_DISCONNECTED);
    }

    protected void setBleCallback(BleListener cb){
        bleCallback = cb;
    }

    protected void pollRequests(){
        if(btRequests.isEmpty())
            busy = false;
        else btRequests.poll().execute();
    }

}
