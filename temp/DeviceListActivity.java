package hackathon.co.kr.neopen.temp;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import hackathon.co.kr.neopen.R;
import kr.neolab.sdk.util.NLog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 * This Activity appears as a dialog. It lists any paired devices and
 * devices detected in the area after discovery. When a device is chosen
 * by the user, the MAC address of the device is sent back to the parent
 * Activity in the token Intent.
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class DeviceListActivity extends AppCompatActivity {
    // Return Intent extra
    public static String EXTRA_DEVICE_SPP_ADDRESS = "device_spp_address";
    public static String EXTRA_DEVICE_LE_ADDRESS = "device_le_address";
    public static String EXTRA_IS_BLUETOOTH_LE = "is_bluetooth_le";

    // Member fields
    private BluetoothAdapter mBtAdapter;
    private Handler mHandler;
    private static final long SCAN_PERIOD = 10000;
    private BluetoothLeScanner mLeScanner;
    private ScanSettings mScanSetting;
    private List<ScanFilter> mScanFilters;
    private ArrayAdapter<String> mPairedDevicesArrayAdapter;
    private ArrayAdapter<String> mNewDevicesArrayAdapter;
    HashMap<String, String> temp = new HashMap<>();
    Button scanButton;
    Button scanLEButton;
    boolean is_le_scan = false;
    boolean isScanning = false;

    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            BluetoothDevice device = result.getDevice();
            if (device != null) {
                String sppAddress = changeAddressFromLeToSpp(result.getScanRecord().getBytes());
                String msg = device.getName() + "\n" + "[RSSI : " + result.getRssi() + "dBm]" + sppAddress;
                NLog.d("onLeScan " + msg);
                /**
                 * have to change adapter to BLE
                 */
                if (!temp.containsKey(sppAddress)) {
                    NLog.d("ACTION_FOUND onLeScan : " + device.getName() + " sppAddress : " + sppAddress + ", COD:" + device.getBluetoothClass());

                    PenClientCtrl.getInstance(DeviceListActivity.this).setLeMode(true);
                    if (PenClientCtrl.getInstance(DeviceListActivity.this).isAvailableDevice(result.getScanRecord().getBytes())) {
                        temp.put(sppAddress, device.getAddress());
                        mNewDevicesArrayAdapter.add(msg);
                    }
                } else {
                }
            }
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
            for (ScanResult scanResult : results) {
                NLog.d("ScanResult - Results", scanResult.toString());
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            NLog.d("Scan Failed", "Error Code : " + errorCode);
        }
    };

    private String changeAddressFromLeToSpp(byte[] data) {
        int index = 0;
        int size = 0;
        byte flag = 0;
        while (data.length > index) {
            size = data[index++];
            if (data.length <= index)
                return null;
            flag = data[index];
            if ((flag & 0xFF) == 0xFF) {
                ++index;
                byte[] mac = new byte[6];
                System.arraycopy(data, index, mac, 0, 6);
                StringBuilder sb = new StringBuilder(18);
                for (byte b : mac) {
                    if (sb.length() > 0)
                        sb.append(':');
                    sb.append(String.format("%02x", b));
                }
                String strMac = sb.toString().toUpperCase();
                return strMac;
            } else {
                index += size;
            }
        }
        return null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Setup the window

        setContentView(R.layout.device_list);

        // Set token CANCELED in case the user backs out
        setResult(Activity.RESULT_CANCELED);

        mHandler = new Handler();

        //Actionbar Home
        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);

        // Initialize the button to perform device discovery
        scanButton = (Button) findViewById(R.id.button_scan);
        scanButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                is_le_scan = false;
                doDiscovery(false);
                scanButton.setEnabled(false);
                scanLEButton.setEnabled(false);
            }
        });

        scanLEButton = (Button) findViewById(R.id.button_le_scan);
        scanLEButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                is_le_scan = true;
                isScanning = !isScanning;
                if (isScanning) {
                    temp.clear();
                    doDiscovery(true);
                    scanLEButton.setText("STOP");
                    scanButton.setEnabled(false);
                } else {
                    scanLEButton.setText(R.string.button_le_scan);
                    scanButton.setEnabled(true);
//                    scanButton.setVisibility( View.VISIBLE );
                    if (Build.VERSION.SDK_INT >= 21) {
                        mLeScanner.stopScan(mScanCallback);
                    }
                }
            }
        });


        // Initialize array adapters. One for already paired devices and
        // one for newly discovered devices
        mPairedDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.device_name);
        mNewDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.device_name);

        // Find and set up the ListView for paired devices
        ListView pairedListView = (ListView) findViewById(R.id.paired_devices);
        pairedListView.setAdapter(mPairedDevicesArrayAdapter);
        pairedListView.setOnItemClickListener(mDeviceClickListener);

        // Find and set up the ListView for newly discovered devices
        ListView newDevicesListView = (ListView) findViewById(R.id.new_devices);
        newDevicesListView.setAdapter(mNewDevicesArrayAdapter);
        newDevicesListView.setOnItemClickListener(mDeviceClickListener);

        // Register for broadcasts when a device is discovered
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(mReceiver, filter);

        // Register for broadcasts when discovery has finished
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(mReceiver, filter);

        // Get the local Bluetooth adapter
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        // Get a set of currently paired devices
        Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();

        // If there are paired devices, add each one to the ArrayAdapter
        if (pairedDevices.size() > 0) {
            findViewById(R.id.title_paired_devices).setVisibility(View.VISIBLE);

            for (BluetoothDevice device : pairedDevices) {
                mPairedDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
//                mPairedDevicesArrayAdapter.add(device.getName() +"\n M:"+device.getBluetoothClass().getMajorDeviceClass()+"D:"+device.getBluetoothClass().getDeviceClass());
            }
        } else {
            String noDevices = getResources().getText(R.string.none_paired).toString();
            mPairedDevicesArrayAdapter.add(noDevices);
        }
        getWindow().setStatusBarColor(getResources().getColor(R.color.color_3440ff));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT >= 21) {
            mLeScanner = mBtAdapter.getBluetoothLeScanner();
            mScanSetting = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build();
            mScanFilters = new ArrayList<>();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Make sure we're not doing discovery anymore
        if (mBtAdapter != null) {
            mBtAdapter.cancelDiscovery();
        }

        // Unregister broadcast listeners
        this.unregisterReceiver(mReceiver);
    }

    /**
     * Start device discover with the BluetoothAdapter
     */
    private void doDiscovery(boolean le) {
        NLog.d("doDiscovery()");

        // Indicate scanning in the title
        setProgressBarIndeterminateVisibility(true);
        setTitle(R.string.scanning);

        // Turn on sub-title for new devices
        findViewById(R.id.title_new_devices).setVisibility(View.VISIBLE);

        mNewDevicesArrayAdapter.clear();

        if (le) // scan btle
        {
//            mHandler.postDelayed(new Runnable() {
//                @Override
//                public void run() {
//                    if (Build.VERSION.SDK_INT < 21) {
//                        mBtAdapter.stopLeScan(mLeScanCallback);
//                    } else {
//                        mLeScanner.stopScan(mScanCallback);
//                    }
//                }
//            }, SCAN_PERIOD);
            if (Build.VERSION.SDK_INT < 21) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Not Supported BLE under 21");
                builder.setMessage("Android SDK [" + Build.VERSION.SDK_INT + "] does not support BLE in SDK");
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                });
                builder.setCancelable(false);
                builder.create().show();
                return;
            } else {
                mLeScanner.startScan(mScanFilters, mScanSetting, mScanCallback);
            }
        } else    // scan bt
        {
            // If we're already discovering, stop it
            if (mBtAdapter.isDiscovering()) {
                mBtAdapter.cancelDiscovery();
            }

//        mBtAdapter.startLeScan( callback );
            mBtAdapter.startDiscovery();
        }
    }


    // The on-click listener for all devices in the ListViews
    private OnItemClickListener mDeviceClickListener = new OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
            // Cancel discovery because it's costly and we're about to connect
            if (isScanning) {
                if (Build.VERSION.SDK_INT < 21) {
                } else {
                    mLeScanner.stopScan(mScanCallback);
                }
                mBtAdapter = null;
                mLeScanner = null;
                isScanning = !isScanning;
            } else {
                if (mBtAdapter.isDiscovering())
                    mBtAdapter.cancelDiscovery();
            }

            // Get the device MAC address, which is the last 17 chars in the View
            String info = ((TextView) v).getText().toString();
            String sppAddress = info.substring(info.length() - 17);
            NLog.d("[SdkSampleCode] select address : " + sppAddress);


            // Create the token Intent and include the MAC address
            Intent intent = new Intent();
            intent.putExtra(EXTRA_DEVICE_SPP_ADDRESS, sppAddress);
            intent.putExtra(EXTRA_DEVICE_LE_ADDRESS, temp.get(sppAddress));
            intent.putExtra(EXTRA_IS_BLUETOOTH_LE, is_le_scan);

            // Set token and finish this Activity
            setResult(Activity.RESULT_OK, intent);
            finish();
        }
    };

    // The BroadcastReceiver that listens for discovered devices and
    // changes the title when discovery is finished
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                // Get rssi value
                short rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);

                // If it's already paired, skip it, because it's been listed already
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    NLog.d("ACTION_FOUND SPP : " + device.getName() + " address : " + device.getAddress() + ", COD:" + device.getBluetoothClass());

                    mNewDevicesArrayAdapter.add(device.getName() + "\n" + "[RSSI : " + rssi + "dBm] " + device.getAddress());
//                    mNewDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress()+"\n Major"+device.getBluetoothClass().toString()+"\nDeviceClass()"+device.getBluetoothClass().getDeviceClass()+"device="+device.getType());

                }
                // When discovery is finished, change the Activity title
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                setProgressBarIndeterminateVisibility(false);
                setTitle(R.string.select_device);

                if (mNewDevicesArrayAdapter.getCount() == 0) {
                    String noDevices = getResources().getText(R.string.none_found).toString();
                    mNewDevicesArrayAdapter.add(noDevices);
                }
            }
        }
    };

}
