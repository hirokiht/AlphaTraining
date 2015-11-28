package tw.edu.ncku.alphatraining;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.UUID;

import tw.edu.ncku.androidlibrary.BleManager;

public class AdcManager extends BleManager {
    public static final UUID ADC_SERVICE_UUID = UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb");
    private static final UUID SAMPLE_PERIOD_UUID = UUID.fromString("0000fff1-0000-1000-8000-00805f9b34fb");
    private static final UUID REALTIME_DATA_UUID = UUID.fromString("0000fff4-0000-1000-8000-00805f9b34fb");
    private static final UUID BUFFERED_DATA_UUID = UUID.fromString("0000fff2-0000-1000-8000-00805f9b34fb");
    private static final UUID BYTE_BUFFERED_UUID = UUID.fromString("0000fff3-0000-1000-8000-00805f9b34fb");
    private static final UUID CHAR_CONFIG_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private BluetoothGattService adcService;
    private BluetoothGattCharacteristic samplingPeriodChar, realtimeDataChar, bufferedDataChar, byteBufferChar;

    private short samplingPeriod = -1;
    private AdcListener adcListener;

    public interface AdcListener{
        void onConnectionStateChange(int newState);
        void onSamplingPeriodChanged(short sampling_period);
        void onDataReceived(short data);
        void onDataBufferReceived(byte[] buffer);
        void onDataBufferReceived(short[] buffer);
    }

    public AdcManager(Context ctx, AdcListener listener){
        super(ctx);
        adcListener = listener;
        setBleCallback(new BleListener() {
            @Override
            public void onConnectionStateChange(int newState) {
                if(newState == BluetoothProfile.STATE_CONNECTED){
                    adcService = btGatt.getService(ADC_SERVICE_UUID);
                    samplingPeriodChar = adcService.getCharacteristic(SAMPLE_PERIOD_UUID);
                    realtimeDataChar = adcService.getCharacteristic(REALTIME_DATA_UUID);
                    bufferedDataChar = adcService.getCharacteristic(BUFFERED_DATA_UUID);
                    byteBufferChar = adcService.getCharacteristic(BYTE_BUFFERED_UUID);
                    if(samplingPeriodChar != null)
                        btGatt.readCharacteristic(samplingPeriodChar);  //loads samplingPeriod when done
                    else pollRequests();
                }else if(newState == BluetoothProfile.STATE_DISCONNECTED){
                    adcService = null;
                    samplingPeriodChar = realtimeDataChar = bufferedDataChar = byteBufferChar = null;
                    samplingPeriod = -1;
                }
                adcListener.onConnectionStateChange(newState);
            }

            @Override
            public void onCharacteristicRead(BluetoothGattCharacteristic characteristic) {
                if(characteristic != samplingPeriodChar)
                    return;
                if(characteristic.getValue().length == 1)
                    samplingPeriod = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8,0).shortValue();
                else Log.d("onCharacteristicRead", "Sampling Period Characteristics Read Callback byte array length is not 1!");
            }

            @Override
            public void onCharacteristicWrite(BluetoothGattCharacteristic characteristic) {
                if(characteristic != samplingPeriodChar)
                    return;
                if(characteristic.getValue().length == 1) {
                    samplingPeriod = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0).shortValue();
                    adcListener.onSamplingPeriodChanged(samplingPeriod);
                }else Log.d("onCharacteristicWrite","Sampling Period Characteristics Write Callback byte array length is not 1!");
            }

            @Override
            public void onCharacteristicUpdate(BluetoothGattCharacteristic characteristic) {
                ByteBuffer buffer = ByteBuffer.wrap(characteristic.getValue());
                if(characteristic == realtimeDataChar && buffer.capacity() == 2)
                    adcListener.onDataReceived(buffer.getShort());
                else if(characteristic == bufferedDataChar && buffer.capacity() == 16) {
                    short[] array = new short[8];
                    buffer.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(array);
                    for(int i = 0 ; i < array.length ; i++)
                        if((array[i]&0x800) > 0)
                            array[i] |= 0xF000;
                    adcListener.onDataBufferReceived(array);
                }else if(characteristic == byteBufferChar && buffer.capacity() == 16)
                    adcListener.onDataBufferReceived(buffer.array());
                else Log.d("onCharacteristicChanged","Invalid characteristic or buffer size received!");
            }
        });
    }

    public short getSamplingPeriod(){
        return samplingPeriod;
    }

    private boolean readSamplingPeriod(){
        if(adcService == null)
            throw new UnsupportedOperationException("Device doesn't provide ADC Service!");
        if(samplingPeriodChar == null)
            throw new UnsupportedOperationException("Device doesn't support Sampling Period Operations!");
        BtRequest request = new BtRequest() {
            @Override
            public boolean execute() {
                busy = true;
                return btGatt.readCharacteristic(samplingPeriodChar);
            }
        };
        return busy? btRequests.offer(request) : request.execute();
    }

    public boolean saveSamplingPeriod(final short sampling_period){
        if(adcService == null || samplingPeriodChar == null)
            throw new UnsupportedOperationException("Device doesn't support Sampling Period Operations!");
        BtRequest request = new BtRequest() {
            @Override
            public boolean execute() {
                busy = true;
                samplingPeriodChar.setValue(sampling_period, BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                return btGatt.writeCharacteristic(samplingPeriodChar);
            }
        };
        return busy? btRequests.offer(request) : request.execute();
    }

    public boolean setRealtimeAdcNotification(final boolean enable){
        if(adcService == null || realtimeDataChar == null)
            throw new UnsupportedOperationException("Device doesn't support Realtime ADC!");
        final BluetoothGattDescriptor descriptor = realtimeDataChar.getDescriptor(CHAR_CONFIG_UUID);
        if(descriptor == null)
            throw new UnsupportedOperationException("Realtime ADC doesn't support notification!");
        BtRequest request = new BtRequest() {
            @Override
            public boolean execute() {
                busy = true;
                btGatt.setCharacteristicNotification(realtimeDataChar,enable);
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                return btGatt.writeDescriptor(descriptor);
            }
        };
        return busy? btRequests.offer(request) : request.execute();
    }

    public boolean setBuffered12bitAdcNotification(final boolean enable){
        if(adcService == null || bufferedDataChar == null)
            throw new UnsupportedOperationException("Device doesn't support Buffered 12bit ADC!");
        final BluetoothGattDescriptor descriptor = bufferedDataChar.getDescriptor(CHAR_CONFIG_UUID);
        if(descriptor == null)
            throw new UnsupportedOperationException("Buffered 12bit ADC doesn't support notification!");
        BtRequest request = new BtRequest() {
            @Override
            public boolean execute() {
                busy = true;
                btGatt.setCharacteristicNotification(bufferedDataChar,enable);
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                return btGatt.writeDescriptor(descriptor);
            }
        };
        return busy? btRequests.offer(request) : request.execute();
    }

    public boolean setBuffered8bitAdcNotification(final boolean enable){
        if(adcService == null || byteBufferChar == null)
            throw new UnsupportedOperationException("Device doesn't support Buffered 8bit ADC!");
        final BluetoothGattDescriptor descriptor = byteBufferChar.getDescriptor(CHAR_CONFIG_UUID);
        if(descriptor == null)
            throw new UnsupportedOperationException("Buffered 8bit ADC doesn't support notification!");
        BtRequest request = new BtRequest() {
            @Override
            public boolean execute() {
                busy = true;
                btGatt.setCharacteristicNotification(byteBufferChar,enable);
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                return btGatt.writeDescriptor(descriptor);
            }
        };
        return busy? btRequests.offer(request) : request.execute();
    }
}
