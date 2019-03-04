package com.example.bluetoothdemo;

import android.app.PendingIntent;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import no.nordicsemi.android.ble.BleManagerCallbacks;
import no.nordicsemi.android.support.v18.scanner.BluetoothLeScannerCompat;
import no.nordicsemi.android.support.v18.scanner.ScanCallback;
import no.nordicsemi.android.support.v18.scanner.ScanFilter;
import no.nordicsemi.android.support.v18.scanner.ScanResult;
import no.nordicsemi.android.support.v18.scanner.ScanSettings;

public class MainActivity extends AppCompatActivity implements BleManagerCallbacks, BlinkyManagerCallbacks {
    public final String TAG = "MainActivity";

    Context mContext = null;
    TextView data;
    private final List<BluetoothDevice> mDevices = new ArrayList<>();
    /** Flag set to true when scanner is active. */
    private boolean mScanning;
    private Handler mHandler;
    private final static long SCAN_DURATION = 5000;
    private boolean mDeviceConnected = false;

    private BlinkyManager mBlinkyManager;
    private BluetoothDevice mDevice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mContext = this;

        // Initialize the manager
        mBlinkyManager = new BlinkyManager(getApplication());
        mBlinkyManager.setGattCallbacks(MainActivity.this);

        final Button connect = findViewById(R.id.button);
        data = findViewById(R.id.data);
        mHandler = new Handler();

        connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connectNow();
            }
        });
    }


    private void connectNow() {
        if (mScanning) {
            // Extend scanning for some time more
            mHandler.removeCallbacks(mStopScanTask);
            mHandler.postDelayed(mStopScanTask, SCAN_DURATION);
            return;
        }

        Intent intent = new Intent(this, MyReceiver.class); // explicite intent
        intent.setAction("com.example.ACTION_FOUND");
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);


        BluetoothLeScannerCompat scanner = BluetoothLeScannerCompat.getScanner();
        ScanSettings settings = new ScanSettings.Builder()
                .setLegacy(false)
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setReportDelay(1000)
                .setPhy(ScanSettings.PHY_LE_ALL_SUPPORTED)
                .build();
        List<ScanFilter> filters = new ArrayList<>();
        ScanFilter filter = new ScanFilter.Builder().setDeviceAddress("00:0B:57:47:D5:EA").build();
        filters.add(filter);
        scanner.startScan(filters, settings, mScanCallback);

        // Setup timer that will stop scanning
        mHandler.postDelayed(mStopScanTask, SCAN_DURATION);
        mScanning = true;
    }

    public void stopLeScan() {
        if (!mScanning)
            return;

        final BluetoothLeScannerCompat scanner = BluetoothLeScannerCompat.getScanner();
        scanner.stopScan(mScanCallback);

        mHandler.removeCallbacks(mStopScanTask);
        mScanning = false;
    }

    private Runnable mStopScanTask = new Runnable() {
        @Override
        public void run() {
            MainActivity.this.stopLeScan();
        }
    };

    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(final int callbackType, final ScanResult result) {
            // empty
            Log.e("onScanResult","onScanResult : " + result.getDevice().getName() + " " + result.getDevice().getAddress());
        }

        @Override
        public void onBatchScanResults(final List<ScanResult> results) {
            final int size = mDevices.size();
            BluetoothDevice device;
            for (final ScanResult result : results) {
                device = result.getDevice();
                if(!mDeviceConnected){
                    mDevice = device;
                    reconnect();
                    data.setText(data.getText() +"\n"+ device.getAddress());
                }

                if (!mDevices.contains(device))
                    mDevices.add(device);

                if (size != mDevices.size()) {
                    if(!mDeviceConnected){
                        mDevice = device;
                        reconnect();
                    }
                    data.setText(data.getText() +"\n"+ device.getAddress());
                }
            }

        }

        @Override
        public void onScanFailed(final int errorCode) {
            // empty
        }
    };

    @Override
    protected void onPause() {
        super.onPause();
        stopLeScan();
        if(mDeviceConnected)
            disconnect();
    }

    /**
     * Reconnects to previously connected device.
     * If this device was not supported, its services were cleared on disconnection, so
     * reconnection may help.
     */
    public void reconnect() {
        if (mDevice != null) {
            mBlinkyManager.connect(mDevice)
                    .retry(3, 100)
                    .useAutoConnect(false)
                    .enqueue();
        }
    }

    /**
     * Disconnect from peripheral.
     */
    private void disconnect() {
        mDevice = null;
        mBlinkyManager.disconnect().enqueue();
    }

    @Override
    public void onLedStateChanged(@NonNull BluetoothDevice device, boolean on) {

    }



    @Override
    public void onDeviceConnecting(@NonNull BluetoothDevice device) {

    }

    @Override
    public void onDeviceConnected(@NonNull BluetoothDevice device) {
        mDeviceConnected = true;
    }

    @Override
    public void onDeviceDisconnecting(@NonNull BluetoothDevice device) {

    }

    @Override
    public void onDeviceDisconnected(@NonNull BluetoothDevice device) {
        mDeviceConnected = false;
    }

    @Override
    public void onLinkLossOccurred(@NonNull BluetoothDevice device) {
        mDeviceConnected = false;
    }

    @Override
    public void onServicesDiscovered(@NonNull BluetoothDevice device, boolean optionalServicesFound) {
        mBlinkyManager.send();
    }

    @Override
    public void onDeviceReady(@NonNull BluetoothDevice device) {

    }

    @Override
    public void onBondingRequired(@NonNull BluetoothDevice device) {

    }

    @Override
    public void onBonded(@NonNull BluetoothDevice device) {

    }

    @Override
    public void onBondingFailed(@NonNull BluetoothDevice device) {

    }

    @Override
    public void onError(@NonNull BluetoothDevice device, @NonNull String message, int errorCode) {

    }

    @Override
    public void onDeviceNotSupported(@NonNull BluetoothDevice device) {

    }
}

