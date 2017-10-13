package com.example.hmaliborski.barcode;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.scandit.barcodepicker.BarcodePicker;
import com.scandit.barcodepicker.OnScanListener;
import com.scandit.barcodepicker.ProcessFrameListener;
import com.scandit.barcodepicker.ScanSession;
import com.scandit.barcodepicker.ScanSettings;
import com.scandit.barcodepicker.ScanditLicense;
import com.scandit.recognition.Barcode;
import com.scandit.recognition.SymbologySettings;

import java.util.Locale;

public class ScanditActivity extends AppCompatActivity implements OnScanListener, ProcessFrameListener {
    public static final String sScanditSdkAppKey = "A1JHbQ6ySFWqQ3dnlJ0JXrT8LgAQE0qsK1uhVk0FYsA";
    private final int CAMERA_PERMISSION_REQUEST = 0;

    private BarcodePicker mBarcodePicker;
    private boolean mPaused = true;
    private Toast mToast = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ScanditLicense.setAppKey(sScanditSdkAppKey);
        initializeAndStartBarcodeScanning();
    }

    @Override
    public void didScan(ScanSession scanSession) {
        String message = "";

        for (Barcode code : scanSession.getNewlyRecognizedCodes()) {
            String data = code.getData();
            String cleanData = data;
            if (data.length() > 30) {
                cleanData = data.substring(0, 25) + "[...]";
            }
            if (message.length() > 0) {
                message += "\n\n\n";
            }
            message = "Code: " + cleanData;
            message += "\n\n" + "Code type: " + code.getSymbologyName().toUpperCase(Locale.US);
        }
        mBarcodePicker.pauseScanning();
        showAlertDialog(message);
    }

    public void initializeAndStartBarcodeScanning() {
        // Switch to full screen.
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        requestWindowFeature(Window.FEATURE_NO_TITLE);


        // The scanning behavior of the barcode picker is configured through scan
        // settings. We start with empty scan settings and enable a very generous
        // set of symbologies. In your own apps, only enable the symbologies you
        // actually need.
        ScanSettings settings = ScanSettings.create();
        int[] symbologiesToEnable = new int[]{
                Barcode.SYMBOLOGY_EAN13,
                Barcode.SYMBOLOGY_EAN8,
                Barcode.SYMBOLOGY_UPCA,
                Barcode.SYMBOLOGY_DATA_MATRIX,
                Barcode.SYMBOLOGY_QR,
                Barcode.SYMBOLOGY_CODE39,
                Barcode.SYMBOLOGY_CODE128,
                Barcode.SYMBOLOGY_INTERLEAVED_2_OF_5,
                Barcode.SYMBOLOGY_UPCE
        };
        for (int sym : symbologiesToEnable) {
            settings.setSymbologyEnabled(sym, true);
        }

        // Some 1d barcode symbologies allow you to encode variable-length data. By default, the
        // Scandit BarcodeScanner SDK only scans barcodes in a certain length range. If your
        // application requires scanning of one of these symbologies, and the length is falling
        // outside the default range, you may need to adjust the "active symbol counts" for this
        // symbology. This is shown in the following few lines of code.

        SymbologySettings symSettings = settings.getSymbologySettings(Barcode.SYMBOLOGY_CODE39);
        short[] activeSymbolCounts = new short[]{
                7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20
        };
        symSettings.setActiveSymbolCounts(activeSymbolCounts);
        // For details on defaults and how to calculate the symbol counts for each symbology, take
        // a look at http://docs.scandit.com/stable/c_api/symbologies.html.


        // Prefer the back-facing camera, is there is any.

        settings.setCameraFacingPreference(getIntent().getIntExtra("camera", 1));
        settings.setWorkingRange(ScanSettings.WORKING_RANGE_STANDARD);
        // Some Android 2.3+ devices do not support rotated camera feeds. On these devices, the
        // barcode picker emulates portrait mode by rotating the scan UI.
        boolean emulatePortraitMode = !BarcodePicker.canRunPortraitPicker();
        if (emulatePortraitMode) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }

        BarcodePicker picker = new BarcodePicker(this, settings);
        picker.setFocusable(false);
        picker.setFocusableInTouchMode(false);
        setContentView(picker);
        mBarcodePicker = picker;
        // Register listener, in order to be notified about relevant events
        // (e.g. a successfully scanned bar code).
        mBarcodePicker.setOnScanListener(this);
        mBarcodePicker.setProcessFrameListener(this);
        mBarcodePicker.getOverlayView().setBeepEnabled(false);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mBarcodePicker.stopScanning();
        mPaused = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        mPaused = false;
        mBarcodePicker.startScanning();
    }

    @Override
    public void onBackPressed() {
        mBarcodePicker.stopScanning();
        finish();
    }

    private void showAlertDialog(String message) {

        new AlertDialog.Builder(this)
                .setTitle(getResources().getString(R.string.app_name))
                .setCancelable(false)
                .setMessage(message)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        mBarcodePicker.resumeScanning();
                    }
                }).show();
    }

    @Override
    public void didProcess(byte[] bytes, int i, int i1, ScanSession scanSession) {

    }
}
