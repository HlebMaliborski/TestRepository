package com.example.hmaliborski.barcode;

import android.content.DialogInterface;
import android.hardware.Camera;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.google.zxing.Result;

import me.dm7.barcodescanner.core.CameraWrapper;

public class ZXingActivity extends AppCompatActivity implements ZxingScanerViewCustom.ResultHandler {
    private ZxingScanerViewCustom mScannerView;
    private ZXingActivity zXingActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        zXingActivity = this;
        mScannerView = new ZxingScanerViewCustom(this);
/*        Camera camera = Camera.open(1);
        CameraWrapper wrapper = CameraWrapper.getWrapper(camera, 1);
        wrapper.mCamera.cancelAutoFocus();
        mScannerView.setupCameraPreview(wrapper);*/
        setContentView(mScannerView);
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
        mScannerView.setFocus(isFocusable);
        if (secondsOpening == 0) {
            long endTime = System.nanoTime();
            long a = endTime - startTime;
            secondsOpening = (double) a / 1000000.0;
            secondsOpening = Math.floor(secondsOpening * 100) / 100;
        }
    }

    private int getCameraId() {
        int cameraCount = 0;
        int camera = getIntent().getIntExtra("camera", 1);

        Camera cam = null;
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

    @Override
    public void handleResult(Result var1, String barCode, double seconds, long countOfImages, double timeBefore) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Scan Result");
        builder.setMessage("Code: " + var1.getText() +
                System.lineSeparator() + "Spent time in milliseconds for recognizing barcode: " + seconds +
                System.lineSeparator() + "Code type: " + barCode +
                System.lineSeparator() + "Time for opening camera in ms: " + secondsOpening +
                System.lineSeparator() + "Count of images before success : " + countOfImages +
                System.lineSeparator() + "Time before we capture success image : " + timeBefore);


        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                mScannerView.resumeCameraPreview(zXingActivity);
            }
        });
        builder.setCancelable(false);
        AlertDialog alert1 = builder.create();
        alert1.show();
    }
}
