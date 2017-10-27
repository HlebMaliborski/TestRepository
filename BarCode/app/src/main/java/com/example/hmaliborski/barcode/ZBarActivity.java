package com.example.hmaliborski.barcode;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.hardware.Camera;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import net.sourceforge.zbar.Config;
import net.sourceforge.zbar.Image;
import net.sourceforge.zbar.ImageScanner;
import net.sourceforge.zbar.Symbol;
import net.sourceforge.zbar.SymbolSet;

import java.util.List;


public class ZBarActivity extends AppCompatActivity {
    private Camera mCamera;
    private Handler mAutoFocusHandler;
    private TextView mScanText;

    private ImageScanner scanner;

    private boolean barcodeScanned = false;
    private boolean previewing = true;
    boolean isFocusOn;

    static {
        System.loadLibrary("iconv");
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_zbar);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        initializeCamera();
        scanner = new ImageScanner();
        scanner.setConfig(0, Config.X_DENSITY, 3);

        CameraPreview mPreview = new CameraPreview(this, mCamera, previewCb, autoFocusCB, isFocusOn);
        FrameLayout preview = (FrameLayout) findViewById(R.id.cameraPreview);
        preview.addView(mPreview);

        mScanText = (TextView) findViewById(R.id.scanText);

        Button scanButton = (Button) findViewById(R.id.ScanButton);
        Button cancel = (Button) findViewById(R.id.cancelButton);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        scanButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (barcodeScanned) {
                    barcodeScanned = false;
                    mScanText.setText("Scanning...");
                    mCamera.setPreviewCallback(previewCb);
                    mCamera.startPreview();
                    previewing = true;
                    mCamera.autoFocus(autoFocusCB);
                }
            }
        });
    }

    private void initializeCamera() {
        int resolution = getIntent().getIntExtra("resolution", 0);
        int fps = getIntent().getIntExtra("fps", 15);
        isFocusOn = getIntent().getBooleanExtra("focus", false);
        mAutoFocusHandler = new Handler();
        mCamera = openFrontFacingCameraGingerbread();
        Camera.Parameters parameters = mCamera.getParameters();
        parameters.setPreviewFrameRate(fps);
        List<Camera.Size> sizes = parameters.getSupportedPictureSizes();
        Camera.Size size = sizes.get(resolution);
        parameters.setPictureSize(size.width, size.height);
        if (isFocusOn && !mCamera.getParameters().getFocusMode().equals("fixed")) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        } else {
            mCamera.cancelAutoFocus();
        }
        mCamera.setParameters(parameters);
    }

    public void onPause() {
        super.onPause();
        releaseCamera();
    }

    private double secondsOpening;

    private Camera openFrontFacingCameraGingerbread() {
        int cameraCount = 0;
        int camera = getIntent().getIntExtra("camera", 1);

        Camera cam = null;
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        cameraCount = Camera.getNumberOfCameras();
        for (int camIdx = 0; camIdx < cameraCount; camIdx++) {
            Camera.getCameraInfo(camIdx, cameraInfo);
            if (cameraInfo.facing == camera) {
                try {
                    long startTime = System.nanoTime();
                    cam = Camera.open(camIdx);
                    long endTime = System.nanoTime();
                    long a = endTime - startTime;
                    secondsOpening = (double) a / 1000000.0;
                } catch (RuntimeException e) {
                }
            }
        }

        return cam;
    }

    private void releaseCamera() {
        if (mCamera != null) {
            previewing = false;
            mCamera.setPreviewCallback(null);
            mCamera.release();
            mCamera = null;
        }
    }

    private Runnable doAutoFocus = new Runnable() {
        public void run() {
            if (previewing)
                mCamera.autoFocus(autoFocusCB);
        }
    };

    private long countOfImagesBeforeSuccess = 0;
    private long timeBeforeSuccess = 0;
    private double doubleTimeAfterSuccess = 0;

    Camera.PreviewCallback previewCb = new Camera.PreviewCallback() {
        public void onPreviewFrame(byte[] data, Camera camera) {
            if (timeBeforeSuccess == 0) {
                timeBeforeSuccess = System.nanoTime();
            }
            Camera.Parameters parameters = camera.getParameters();
            Camera.Size size = parameters.getPreviewSize();

            Image barcode = new Image(size.width, size.height, "Y800");
            barcode.setData(data);

            long startTime = System.nanoTime();
            countOfImagesBeforeSuccess++;
            int result = scanner.scanImage(barcode);
            if (result != 0) {
                long timeAfterSuccess = System.nanoTime() - timeBeforeSuccess;
                doubleTimeAfterSuccess = (double) timeAfterSuccess / 1000000.0;
                long endTime = System.nanoTime();
                long a = endTime - startTime;
                double seconds = (double) a / 1000000.0;
                //seconds = Math.round(seconds);
                seconds = Math.floor(seconds * 100) / 100;
                previewing = false;
                mCamera.setPreviewCallback(null);
                //mCamera.stopPreview();

                SymbolSet syms = scanner.getResults();
                for (Symbol sym : syms) {
                    mScanText.setText("barcode result " + sym.getData());
                    String scanResult = sym.getData().trim();
                    DataType type = DataType.getByValue(sym.getType());

                    showAlertDialog(scanResult, seconds, type.name());
                    barcodeScanned = true;
                }
            }
        }
    };


    // Mimic continuous auto-focusing
    Camera.AutoFocusCallback autoFocusCB = new Camera.AutoFocusCallback() {
        public void onAutoFocus(boolean success, Camera camera) {
            mAutoFocusHandler.postDelayed(doAutoFocus, 1000);
        }
    };

    private void showAlertDialog(String message, double seconds, String codeType) {
        //secondsOpening = Math.round(secondsOpening);
        secondsOpening = Math.floor(secondsOpening * 100) / 100;
        doubleTimeAfterSuccess = Math.floor(doubleTimeAfterSuccess * 100) / 100;
        //doubleTimeAfterSuccess = Math.round(doubleTimeAfterSuccess);

        new AlertDialog.Builder(this)
                .setTitle(getResources().getString(R.string.app_name))
                .setCancelable(false)
                .setMessage("Code: " + message +
                        System.lineSeparator() + "Processing time: " + seconds + "ms" +
                        System.lineSeparator() + "Code type: " + codeType +
                        System.lineSeparator() + "Time for opening camera: " + secondsOpening + "ms" +
                        System.lineSeparator() + "Dropped images: " + countOfImagesBeforeSuccess +
                        System.lineSeparator() + "Time elapsed before successful scan : " + doubleTimeAfterSuccess + "ms")

                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        countOfImagesBeforeSuccess = 0;
                        timeBeforeSuccess = 0;
                    }
                }).show();
    }

    private enum DataType {
        NONE(0),
        PARTIAL(1),
        EAN8(8),
        UPCE(9),
        ISBN10(10),
        UPCA(12),
        EAN13(13),
        ISBN13(14),
        I25(25),
        DATABAR(34),
        DATABAR_EXP(35),
        CODABAR(38),
        CODE39(39),
        PDF417(57),
        QRCODE(64),
        CODE93(93),
        CODE128(128);

        public final int value;

        private DataType(int i) {
            this.value = i;
        }

        public static DataType getByValue(int i) {
            for (DataType dt : DataType.values()) {
                if (dt.value == i) {
                    return dt;
                }
            }
            throw new IllegalArgumentException("no datatype with " + i + " exists");
        }
    }
}
