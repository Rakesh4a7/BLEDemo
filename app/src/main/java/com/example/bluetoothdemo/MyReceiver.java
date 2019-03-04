package com.example.bluetoothdemo;

import android.bluetooth.le.BluetoothLeScanner;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;

import no.nordicsemi.android.support.v18.scanner.BluetoothLeScannerCompat;
import no.nordicsemi.android.support.v18.scanner.ScanResult;

/**
 * Created by Rakesh Gupta on 27-02-2019.
 */
public class MyReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent.hasExtra(BluetoothLeScannerCompat.EXTRA_LIST_SCAN_RESULT)){
            ArrayList<ScanResult> results = intent.getParcelableArrayListExtra(BluetoothLeScanner.EXTRA_LIST_SCAN_RESULT);

            for (final ScanResult result : results) {
                Log.e("onScanResult","onScanResult : " + result.getDevice().getName() + " " + result.getDevice().getAddress());
                Toast.makeText(context, result.getDevice().getName() + " " + result.getDevice().getAddress(), Toast.LENGTH_LONG).show();
            }
        }

      /*  StringBuilder sb = new StringBuilder();
        sb.append("Action: " + intent.getAction() + "\n");
        sb.append("URI: " + intent.getDataString() + "\n");
        String log = sb.toString();
        Log.d("MyReceiver", log);
        Toast.makeText(context, log, Toast.LENGTH_LONG).show();*/
    }
}
