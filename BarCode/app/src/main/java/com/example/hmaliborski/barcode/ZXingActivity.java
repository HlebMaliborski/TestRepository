package com.example.hmaliborski.barcode;

import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.FrameLayout;

import com.google.zxing.Result;


public class ZXingActivity extends AppCompatActivity implements ZxingScanerViewCustom.ResultHandler {
    private ZxingScanerViewCustom mScannerView;
    private ZXingActivity zXingActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        zXingActivity = this;
        mScannerView = new ZxingScanerViewCustom(this);
        setContentView(R.layout.activity_zxing);
        FrameLayout preview = (FrameLayout) findViewById(R.id.zxingPreview);
        preview.addView(mScannerView);
    }

    double secondsOpening = 0;

    @Override
    protected void onResume() {
        super.onResume();
        mScannerView.setResultHandler(this);
        int cameraId = getCameraId();
        long startTime = System.nanoTime();
        mScannerView.startCamera(cameraId);
        boolean isFocusable = getIntent().getBooleanExtra("focus", true);
        PackageManager pm = getPackageManager();
        if(pm.hasSystemFeature(PackageManager.FEATURE_CAMERA) && pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_AUTOFOCUS)){
            mScannerView.setFocus(isFocusable);
        }
        if (secondsOpening == 0) {
            long endTime = System.nanoTime();
            long a = endTime - startTime;
            secondsOpening = (double) a / 1000000.0;
            secondsOpening = Math.floor(secondsOpening * 100) / 100;
            //secondsOpening = Math.round(secondsOpening);
        }
    }

    private int getCameraId() {
        int cameraCount;
        int camera = getIntent().getIntExtra("camera", 1);

        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        cameraCount = Camera.getNumberOfCameras();
        for (int camIdx = 0; camIdx < cameraCount; camIdx++) {
            Camera.getCameraInfo(camIdx, cameraInfo);
            if (cameraInfo.facing == camera) {
                try {
                    return camIdx;
                } catch (RuntimeException e) {
                }
            }
        }

        return 0;
    }

    @Override
    protected void onPause() {
        super.onPause();
        mScannerView.stopCamera();
    }

    boolean a = false;

    @Override
    public void handleResult(Result var1, String barCode, double seconds, long countOfImages, double timeBefore) {
        if (!a) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Scan Result");
            builder.setMessage("Code: " + var1.getText() +
                    System.lineSeparator() + "Processing time: " +  seconds + "ms" +
                    System.lineSeparator() + "Code type: " + barCode +
                    System.lineSeparator() + "Time for opening camera: " +  secondsOpening + "ms" +
                    System.lineSeparator() + "Dropped images: " + countOfImages +
                    System.lineSeparator() + "Time elapsed before successful scan : " + timeBefore + "ms");

            builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    a = false;
                /*mScannerView.setResultHandler(zXingActivity);
                int cameraId = getCameraId();
                mScannerView.startCamera(cameraId);*/
                }
            });
            builder.setCancelable(false);
            AlertDialog alert1 = builder.create();
            alert1.show();
            a = true;
        }
        //mScannerView.stopCamera();
        mScannerView.resumeCameraPreview(this);
    }
}
