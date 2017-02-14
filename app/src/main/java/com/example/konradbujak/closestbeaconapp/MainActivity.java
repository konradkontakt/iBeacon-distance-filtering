package com.example.konradbujak.closestbeaconapp;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.MifareUltralight;
import android.os.CountDownTimer;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.kontakt.sdk.android.ble.configuration.ActivityCheckConfiguration;
import com.kontakt.sdk.android.ble.configuration.ScanMode;
import com.kontakt.sdk.android.ble.configuration.ScanPeriod;
import com.kontakt.sdk.android.ble.connection.OnServiceReadyListener;
import com.kontakt.sdk.android.ble.filter.ibeacon.IBeaconFilter;
import com.kontakt.sdk.android.ble.filter.ibeacon.IBeaconFilters;
import com.kontakt.sdk.android.ble.manager.ProximityManager;
import com.kontakt.sdk.android.ble.manager.ProximityManagerFactory;
import com.kontakt.sdk.android.ble.manager.listeners.ScanStatusListener;
import com.kontakt.sdk.android.ble.manager.listeners.simple.SimpleIBeaconListener;
import com.kontakt.sdk.android.ble.manager.listeners.simple.SimpleScanStatusListener;
import com.kontakt.sdk.android.ble.manager.listeners.simple.SimpleSecureProfileListener;
import com.kontakt.sdk.android.ble.rssi.RssiCalculators;
import com.kontakt.sdk.android.ble.spec.EddystoneFrameType;
import com.kontakt.sdk.android.common.KontaktSDK;
import com.kontakt.sdk.android.common.profile.IBeaconDevice;
import com.kontakt.sdk.android.common.profile.IBeaconRegion;
import com.kontakt.sdk.android.common.profile.ISecureProfile;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private ProximityManager KontaktManager;
    final CountDownTimer counter1 = new CountDownTimer(5000, 1) {
        @Override
        public void onTick(long millisUntilFinished) {

        }

        @Override
        public void onFinish() {
            stopScan();
        }
    };
    String TAG = "MyActivity";
    MySimpleArrayAdapter adapter;
    //Replace (Your Secret API key) with your API key aquierd from the Kontakt.io Web Panel
    public static String API_KEY = "Your Secret API key";
    ArrayList<String> uids = new ArrayList<>();
    ArrayList<Double> distances = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        adapter = new MySimpleArrayAdapter(this);
        onetimeconfiguration();
    }
    @Override
    protected void onStart() {
        checkPermissionAndStart();
        super.onStop();
    }
    @Override
    protected void onStop() {
        KontaktManager.stopScanning();
        super.onStop();
    }
    @Override
    protected void onDestroy() {
        KontaktManager.disconnect();
        KontaktManager = null;
        super.onDestroy();
    }

    private void checkPermissionAndStart() {
        int checkSelfPermissionResult = ContextCompat.checkSelfPermission(this, Arrays.toString(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.READ_EXTERNAL_STORAGE}));
        if (PackageManager.PERMISSION_GRANTED == checkSelfPermissionResult) {
            //already granted
            Log.d(TAG,"Permission already granted");
            adapter();
            startScan();
        }
        else {
            //request permission
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.READ_EXTERNAL_STORAGE}, 100);
            Log.d(TAG,"Permission request called");
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (100 == requestCode) {
                Log.d(TAG,"Permission granted");
                adapter();
                startScan();
            }
        } else
        {
            Log.d(TAG,"Permission not granted");
            showToast("Kontakt.io SDK require this permission");
        }
    }
    public void onetimeconfiguration(){
        sdkInitialise();
        configureProximityManager();
        setListeners();
        setFilters();
    }
    public void sdkInitialise()
    {
        KontaktSDK.initialize(API_KEY);
        if (KontaktSDK.isInitialized())
            Log.v(TAG, "SDK initialised");
    }
    private void configureProximityManager() {
        KontaktManager = ProximityManagerFactory.create(this);
        KontaktManager.configuration()
                .deviceUpdateCallbackInterval(TimeUnit.SECONDS.toMillis(1))
                .rssiCalculator(RssiCalculators.newLimitedMeanRssiCalculator(5))
                .resolveShuffledInterval(3)
                .scanMode(ScanMode.BALANCED)
                .scanPeriod(ScanPeriod.RANGING);
    }
    private void setListeners()
    {
        KontaktManager.setScanStatusListener(createScanStatusListener());
        KontaktManager.setIBeaconListener(iBeaconListener());
        Log.d(TAG,"Listeners Configured");
    }
    // Toasts on device
    private void showToast(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }
    private ScanStatusListener createScanStatusListener() {
        return new SimpleScanStatusListener() {
            @Override
            public void onScanStart()
            {
                Log.d(TAG,"Scanning started");
                showToast("Scanning started");
            }
            @Override
            public void onScanStop()
            {
                Log.d(TAG,"Scanning stopped");
                showToast("Scanning stopped");
            }
        };
    }
    private void setFilters(){
        IBeaconFilter customIBeaconFilter = new IBeaconFilter() {
            @Override
            public boolean apply(IBeaconDevice iBeaconDevice) {
                // So here we set the max distance from a beacon to 1m
                return iBeaconDevice.getDistance()<1;
            }
        };

        KontaktManager.filters().iBeaconFilter(customIBeaconFilter);
    }
    private void adapter()
    {
        ListView lista = (ListView) findViewById(R.id.CustomListView);
        lista.setAdapter(adapter);
        lista.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                // TODO

            }
        });
    }
    private SimpleIBeaconListener iBeaconListener()
    {
        return new SimpleIBeaconListener(){
            @Override
            public void onIBeaconDiscovered(IBeaconDevice ibeacon, IBeaconRegion region) {

                if (ibeacon.isShuffled()) {
                    String uid = "Shuffled Beacon";
                    double distance = ibeacon.getDistance();
                    uids.add(uid);
                    distances.add(distance);
                    adapter.updateList(distances, uids);
                }
                else {
                    String uid = ibeacon.getUniqueId();
                    double distance = ibeacon.getDistance();
                    uids.add(uid);
                    distances.add(distance);
                    adapter.updateList(distances, uids);
                }
            }
        };
    }
    private void stopScan(){
            KontaktManager.stopScanning();
            super.onStop();
        }
    private void startScan() {
        KontaktManager.connect(new OnServiceReadyListener()
        {
            @Override
            public void onServiceReady() {
                KontaktManager.startScanning();
            }
        });
    }
}
