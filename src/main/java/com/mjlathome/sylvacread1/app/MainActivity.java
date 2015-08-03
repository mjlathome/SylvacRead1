package com.mjlathome.sylvacread1.app;

import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.LinkedList;
import java.util.Queue;

public class MainActivity extends Activity {

    // private static final int REQUEST_ENABLE_BT = 1;

    private static final String TAG = "SylvacRead1";

    // queue read/write requests
    private Queue<BluetoothGattDescriptor> descriptorWriteQueue = new LinkedList<BluetoothGattDescriptor>();
    private Queue<BluetoothGattCharacteristic> characteristicReadQueue = new LinkedList<BluetoothGattCharacteristic>();
    // TODO use queue of characteristic and the value written so that this can be correctly returned instead of mLastWrite
    private Queue<BluetoothGattCharacteristic> characteristicWriteQueue = new LinkedList<BluetoothGattCharacteristic>();

    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private Handler mHandler;

    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;

    private SparseArray<BluetoothDevice> mDevices;

    private BluetoothGatt mConnectedGatt;

    // private MyGattCallback mMyGattCallback;
    // private MyLeScanCallback mLeScanCallback;

    private String mLastWrite = "";
    private boolean mCanWrite = true;

    private ProgressDialog mProgress;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics;

    private static final String DEVICE_NAME_BONDED   = "SY";
    private static final String DEVICE_NAME_UNBONDED = "SY289";

    BroadcastReceiver mReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_main);
//        setProgressBarIndeterminate(true);

        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (BluetoothDevice.ACTION_UUID.equals(action)) {
                    Log.d(TAG, "ACTION_UUID received");
                    Log.d(TAG, "ACTION_UUID Has " + BluetoothDevice.EXTRA_UUID + ": " + intent.hasExtra(BluetoothDevice.EXTRA_UUID));
                    if (intent.hasExtra(BluetoothDevice.EXTRA_DEVICE)) {
                        BluetoothDevice btd = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                        Log.d(TAG, "ACTION_UUID btd name: " + btd.getName());
                    }
                    if (intent.hasExtra(BluetoothDevice.EXTRA_UUID)) {
                        Parcelable[] uuidExtra = intent.getParcelableArrayExtra(BluetoothDevice.EXTRA_UUID);
                        Log.d(TAG, "ACTION_UUID UUIDs: " + uuidExtra);
                    }
                    /*
                    BluetoothDevice btd = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                    StringBuilder sb = new StringBuilder();
                    List<String> uuids = new ArrayList<String>(uuidExtra.length);
                    if(uuidExtra != null) {
                        for (int i = 0; i < uuidExtra.length; i++) {
                            sb.append(uuidExtra[i].toString()).append(',');
                            uuids.add(uuidExtra[i].toString());
                        }
                    }
                    Log.d(TAG, "ACTION_UUID received for " + btd.getName() + " uuids: " + sb.toString());
                    */
                }
            }
        };

        // Register the BroadcastReceiver
        IntentFilter filter1 = new IntentFilter(BluetoothDevice.ACTION_UUID);
        registerReceiver(mReceiver,filter1);

        // extract Bluetooth adapter under Android 4.3+
        BluetoothManager manager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        mBluetoothAdapter = manager.getAdapter();

        mDevices = new SparseArray<BluetoothDevice>();

        /*
         * A progress dialog will be needed while the connection process is
         * taking place
         */
        mProgress = new ProgressDialog(this);
        mProgress.setIndeterminate(true);
        mProgress.setCancelable(false);


        mHandler = new Handler();
        // scanLeDevice(true);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Ensures Bluetooth is available on the device and it is enabled. If not,
        // displays a dialog requesting user permission to enable Bluetooth.
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            // startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            startActivity(enableBtIntent);
            finish();
            return;
        }

        // Check for Bluetooth LE Support.  In production, the manifest entry will keep this
        // from installing on these devices, but this will allow test devices or other
        // sideloads to report whether or not the feature exists.
        // NOTE: Not really needed as included in Manifest.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "No Bluetooth LE Support.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

    }

    @Override
    protected void onPause() {
        super.onPause();

        //Make sure dialog is hidden
        //mProgress.dismiss();
        //Cancel any scans in progress
        //mHandler.removeCallbacks(mStopRunnable);
        //mHandler.removeCallbacks(mStartRunnable);
        //mBluetoothAdapter.stopLeScan(this);
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void onClickScanBT(View view) {
        scanLeDevice(true);
        return;
    }

    public void onClickGetId(View view) {
        Log.d(TAG, "onClickGetId: Write Char = " + writeCharacteristic("ID?\r"));
        return;
    }

    public void onClickSetZero(View view) {
        Log.d(TAG, "onClickSetZero: Write Char = " + writeCharacteristic("SET\r"));
        return;
    }

    public void onClickGetValue(View view) {
        Log.d(TAG, "onClickGetValue: Write Char = " + writeCharacteristic("?\r"));
        return;
    }

    public void onClickSetMm(View view) {
        Log.d(TAG, "onClickSetMm: Write Char = " + writeCharacteristic("MM\r"));
        return;
    }

    public void onClickGetBattery(View view) {
        Log.d(TAG, "onClickGetBattery: Write Char = " + writeCharacteristic("BAT?\r"));
        return;
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                }
            }, SCAN_PERIOD);

            // does not work correctly
            // UUID[] uuidService = { UUID.fromString(SylvacGattAttributes.DATA_RECEIVED_FROM_INSTRUMENT) };

            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
            // does not work correctly
            // mBluetoothAdapter.startLeScan(uuidService, mLeScanCallback);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
    }

    /*
     * In this callback, we've created a bit of a state machine to enforce that only
     * one characteristic be read or written at a time until all of our sensors
     * are enabled and we are registered to get notifications.
     */
    private BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status,
                                            int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG, "Connected to GATT server.");
                Log.i(TAG, "Attempting to start service discovery:" +
                        mConnectedGatt.discoverServices());

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(TAG, "Disconnected from GATT server.");
            }
        }

        @Override
        // New services discovered
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "onServicesDiscovered GATT_SUCCESS: " + status);
                Log.d(TAG, "onServicesDiscovered Services = " + gatt.getServices());
                displayGattServices(mConnectedGatt.getServices());
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        // Result of a characteristic read operation
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "onCharacteristicRead GATT_SUCCESS: " + characteristic);
                Log.d(TAG, "onCharacteristicRead UUID = " + characteristic.getUuid());

                // For all other profiles, writes the data formatted in HEX.
                final byte[] data = characteristic.getValue();
                if (data != null && data.length > 0) {
                    final StringBuilder stringBuilder = new StringBuilder(data.length);
                    for(byte byteChar : data)
                        stringBuilder.append(String.format("%02X ", byteChar));

                    Log.d(TAG, "onCharacteristicRead value = " + new String(data) + "\n" + stringBuilder.toString());
                }
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);

            Log.d(TAG, "onCharacteristicChanged char = : " + characteristic);
            Log.d(TAG, "onCharacteristicChanged UUID = " + characteristic.getUuid());

            if (characteristic.getUuid().equals(UUID.fromString(SylvacGattAttributes.ANSWER_TO_REQUEST_OR_CMD_FROM_INSTRUMENT))) {
                Log.d(TAG, "onCharacteristicChanged last write = " + mLastWrite);
                Log.d(TAG, "onCharacteristicChanged getValue() = " + new String(characteristic.getValue()));
            }

            // For all other profiles, writes the data formatted in HEX.
            final byte[] data = characteristic.getValue();
            Log.d(TAG, "onCharacteristicChanged value = " + new String(data) + "\n" + byteArrayToString(data));

            /*
            final byte[] data = characteristic.getValue();
            if (data != null && data.length > 0) {
                final StringBuilder stringBuilder = new StringBuilder(data.length);
                for(byte byteChar : data)
                    stringBuilder.append(String.format("%02X ", byteChar));

                Log.d(TAG, "onCharacteristicChanged value = " + new String(data) + "\n" + stringBuilder.toString());
            }
            */
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "onCharacteristicWrite GATT_SUCCESS: " + status);
                Log.d(TAG, "onCharacteristicWrite UUID = " + characteristic.getUuid());
                Log.i(TAG, "onCharacteristicWrite Char Value = " + characteristic.getValue().toString());
            } else {
                Log.w(TAG, "onCharacteristicWrite received: " + status);
            }
        }

        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
            super.onReliableWriteCompleted(gatt, status);
            Log.d(TAG, "onReliableWriteCompleted(" + status + ")");
        }
    };

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi,
                                     final byte[] scanRecord) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.d(TAG, "fetch = " + device.fetchUuidsWithSdp());
                            Log.d(TAG, "UUID = " + device.getUuids());
                            Log.d(TAG, "Name = " + device.getName());
                            Log.d(TAG, "Type = " + device.getType());
                            Log.d(TAG, "BT Class = " + device.getBluetoothClass());
                            Log.d(TAG, "Address = " + device.getAddress());
                            Log.d(TAG, "String = " + device.toString());

                            List<UUID> uuids = parseUUIDs(scanRecord);
                            Log.d(TAG, "UUIDs parsed = " + uuids.toString());
                            if(device.getName().equals(DEVICE_NAME_BONDED)) {
                                Log.d(TAG, "mLeScanCallback: stopLeScan");
                                mBluetoothAdapter.stopLeScan(mLeScanCallback);
                                Log.d(TAG, "mLeScanCallback: connectGatt");
                                mConnectedGatt = device.connectGatt(MainActivity.this, false, mGattCallback);
                            }

                            // mLeDeviceListAdapter.addDevice(device);
                            // mLeDeviceListAdapter.notifyDataSetChanged();
                        }
                    });
                }
            };

    // parseUUIDs - extract UUIDs from the content of the advertisement record offered by the remote device
    // called from BLE callback method.
    // see: http://stackoverflow.com/questions/18019161/startlescan-with-128-bit-uuids-doesnt-work-on-native-android-ble-implementation/21986475#21986475
    private List<UUID> parseUUIDs(final byte[] advertisedData) {
        List<UUID> uuids = new ArrayList<UUID>();

        int offset = 0;
        while (offset < (advertisedData.length - 2)) {
            int len = advertisedData[offset++];
            if (len == 0)
                break;

            int type = advertisedData[offset++];
            switch (type) {
                case 0x02: // Partial list of 16-bit UUIDs
                case 0x03: // Complete list of 16-bit UUIDs
                    while (len > 1) {
                        int uuid16 = advertisedData[offset++];
                        uuid16 += (advertisedData[offset++] << 8);
                        len -= 2;
                        uuids.add(UUID.fromString(String.format(
                                "%08x-0000-1000-8000-00805f9b34fb", uuid16)));
                    }
                    break;
                case 0x06:// Partial list of 128-bit UUIDs
                case 0x07:// Complete list of 128-bit UUIDs
                    // Loop through the advertised 128-bit UUID's.
                    while (len >= 16) {
                        try {
                            // Wrap the advertised bits and order them.
                            ByteBuffer buffer = ByteBuffer.wrap(advertisedData,
                                    offset++, 16).order(ByteOrder.LITTLE_ENDIAN);
                            long mostSignificantBit = buffer.getLong();
                            long leastSignificantBit = buffer.getLong();
                            uuids.add(new UUID(leastSignificantBit,
                                    mostSignificantBit));
                        } catch (IndexOutOfBoundsException e) {
                            // Defensive programming.
                            Log.e(TAG, e.toString());
                            continue;
                        } finally {
                            // Move the offset to read the next uuid.
                            offset += 15;
                            len -= 16;
                        }
                    }
                    break;
                default:
                    offset += (len - 1);
                    break;
            }
        }

        return uuids;
    }

    // Demonstrates how to iterate through the supported GATT
    // Services/Characteristics.
    // In this sample, we populate the data structure that is bound to the
    // ExpandableListView on the UI.
    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;

        // clear BLE command queues
        descriptorWriteQueue.clear();
        characteristicReadQueue.clear();
        characteristicWriteQueue.clear();

        for (BluetoothGattService service : gattServices) {
            Log.d(TAG, "Found service: " + service.getUuid());
            Log.d(TAG, "Included service(s): " + service.getIncludedServices());

            // skip if not Sylvac Metrology service
            if (!service.getUuid().equals(UUID.fromString(SylvacGattAttributes.SYLVAC_METROLOGY_SERVICE))) {
                continue;
            }

            for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                Log.d(TAG, "Found characteristic: " + characteristic.getUuid());
                Log.d(TAG, "Descriptor: " + characteristic.getDescriptors());
                Log.d(TAG, "Properties: " + characteristic.getProperties());

                for (BluetoothGattDescriptor descriptor : characteristic.getDescriptors()) {
                    Log.d(TAG, "Found descriptor: " + descriptor.getUuid());
                    Log.d(TAG, "Value: " + descriptor.getValue());
                    Log.d(TAG, "Permissions: " + descriptor.getPermissions());
                }

                if(hasProperty(characteristic,
                        BluetoothGattCharacteristic.PROPERTY_READ)) {
                    Log.d(TAG, "Found Read characteristic: " + characteristic.getUuid());
                    // TODO before queue - remove later
                    // mConnectedGatt.readCharacteristic(characteristic);
                    // TODO read here not required - remove later
                    // readCharacteristic(characteristic);
                }

                if(hasProperty(characteristic,
                        BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) {
                    Log.d(TAG, "Found Write No Resp characteristic: " + characteristic.getUuid());
                }

                if(hasProperty(characteristic,
                        BluetoothGattCharacteristic.PROPERTY_INDICATE)) {
                    Log.d(TAG, "Found indication for characteristic: " + characteristic.getUuid());

                    // enable indication on the Sylvac data received (from instrument) characteristic only
                    if(characteristic.getUuid().equals(UUID.fromString(SylvacGattAttributes.DATA_RECEIVED_FROM_INSTRUMENT))) {
                        Log.d(TAG, "Register indication for characteristic: " + characteristic.getUuid());
                        Log.d(TAG, "Register Success = " + mConnectedGatt.setCharacteristicNotification(characteristic, true));

                        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                                UUID.fromString(SylvacGattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);

                        // TODO before queue - remove later
                        // mConnectedGatt.writeDescriptor(descriptor);
                        writeGattDescriptor(descriptor);
                    }
                }

                if(hasProperty(characteristic,
                        BluetoothGattCharacteristic.PROPERTY_NOTIFY)) {
                    Log.d(TAG, "Found notification for characteristic: " + characteristic.getUuid());

                    // enable notify on the Sylvac answer to request or cmd (from instrument) characteristic only
                    if(characteristic.getUuid().equals(UUID.fromString(SylvacGattAttributes.ANSWER_TO_REQUEST_OR_CMD_FROM_INSTRUMENT))) {
                        Log.d(TAG, "Register notification for characteristic: " + characteristic.getUuid());
                        Log.d(TAG, "Register Success = " + mConnectedGatt.setCharacteristicNotification(characteristic, true));

                        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                                UUID.fromString(SylvacGattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        // descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);

                        // TODO before queue - remove later
                        // mConnectedGatt.writeDescriptor(descriptor);
                        writeGattDescriptor(descriptor);
                    }
                }
            }
        }
    }

    public static boolean hasProperty(BluetoothGattCharacteristic
                                                   characteristic, int property) {
        int prop = characteristic.getProperties() & property;
        return prop == property;
    }

    // dequeue next BLE command
    private boolean dequeueBleCommand() {

        // handle asynchronous BLE callbacks via queues
        // GIVE PRECEDENCE to descriptor writes.  They must all finish first?
        if (descriptorWriteQueue.size() > 0) {
            return mConnectedGatt.writeDescriptor(descriptorWriteQueue.element());
        } else if (characteristicReadQueue.size() > 0) {
            return mConnectedGatt.readCharacteristic(characteristicReadQueue.element());
        } else if (characteristicWriteQueue.size() > 0) {
            return mConnectedGatt.writeCharacteristic(characteristicWriteQueue.element());
        } else {
            return true;
        }
    }

    // queue Gatt Descriptor writes
    private boolean writeGattDescriptor(BluetoothGattDescriptor d){
        boolean success = false;

        // check Bluetooth GATT connected
        if (mConnectedGatt == null) {
            Log.e(TAG, "lost connection");
            return false;
        }

        //put the descriptor into the write queue
        success = descriptorWriteQueue.add(d);

        // execute BLE command immediately if there is nothing else queued up
        if((descriptorWriteQueue.size() == 1) && (characteristicReadQueue.size() == 0) && (characteristicWriteQueue.size() == 0)) {
            return mConnectedGatt.writeDescriptor(d);
        } else {
            return success;
        }
    }

    // queue BLE characteristic writes
    private boolean writeCharacteristic(BluetoothGattCharacteristic c) {
        boolean success = false;

        // check Bluetooth GATT connected
        if (mConnectedGatt == null) {
            Log.e(TAG, "lost connection");
            return false;
        }

        // BluetoothGattService s = mBluetoothGatt.getService(UUID.fromString(kYourServiceUUIDString));
        // BluetoothGattCharacteristic c = s.getCharacteristic(UUID.fromString(characteristicName));

        //put the characteristic into the read queue
        success = characteristicWriteQueue.add(c);

        // execute BLE command immediately if there is nothing else queued up
        if((descriptorWriteQueue.size() == 0) && (characteristicReadQueue.size() == 0) && (characteristicWriteQueue.size() == 1)) {
            return mConnectedGatt.writeCharacteristic(c);
        }
        else {
            return success;
        }
    }

    public boolean writeCharacteristic(String value) {

        // check Bluetooth GATT connected
        if (mConnectedGatt == null) {
            Log.e(TAG, "lost connection");
            return false;
        }

        /*
        // check write is allowed
        if (mCanWrite == false) {
            Log.e(TAG, "write not allowed");
            return false;
        }
        */

        // extract the Service
        BluetoothGattService gattService = mConnectedGatt.getService(UUID.fromString(SylvacGattAttributes.SYLVAC_METROLOGY_SERVICE));
        if (gattService == null) {
            Log.e(TAG, "service not found");
            return false;
        }

        // extract the Characteristic
        BluetoothGattCharacteristic gattChar = gattService.getCharacteristic(UUID.fromString(SylvacGattAttributes.DATA_REQUEST_OR_CMD_TO_INSTRUMENT));
        if (gattChar == null) {
            Log.e(TAG, "characteristic not found");
            return false;
        }

        // set the Characteristic
        if (gattChar.setValue(value) == false) {
            Log.e(TAG, "characteristic set failed");
            return false;
        }

        // write the Characteristic
        if (mConnectedGatt.writeCharacteristic(gattChar) == false) {
            Log.e(TAG, "characteristic write failed");
            return false;
        }

        mLastWrite = value;
       // mCanWrite = false;
        return true;
    }

    public boolean readCharacteristic() {
        // check Bluetooth GATT connected
        if (mConnectedGatt == null) {
            Log.e(TAG, "lost connection");
            return false;
        }

        // extract the Service
        BluetoothGattService gattService = mConnectedGatt.getService(UUID.fromString(SylvacGattAttributes.SYLVAC_METROLOGY_SERVICE));
        if (gattService == null) {
            Log.e(TAG, "service not found");
            return false;
        }

        // extract the Characteristic
        BluetoothGattCharacteristic gattChar = gattService.getCharacteristic(UUID.fromString(SylvacGattAttributes.ANSWER_TO_REQUEST_OR_CMD_FROM_INSTRUMENT));
        // BluetoothGattCharacteristic gattChar = gattService.getCharacteristic(uuidChar);
        if (gattChar == null) {
            Log.e(TAG, "characteristic not found");
            return false;
        }

        // read the Characteristic
        return mConnectedGatt.readCharacteristic(gattChar);
    }

    public String byteArrayToString(byte[] data) {
        if (data != null && data.length > 0) {
            final StringBuilder stringBuilder = new StringBuilder(data.length);
            for(byte byteChar : data)
                stringBuilder.append(String.format("%02X ", byteChar));

            return stringBuilder.toString();
        }
        else {
            return "";
        }
    }

}
