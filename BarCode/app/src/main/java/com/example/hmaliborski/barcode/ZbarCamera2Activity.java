package com.example.hmaliborski.barcode;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Camera;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.net.wifi.aware.Characteristics;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Size;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.widget.FrameLayout;
import android.widget.Toast;

import net.sourceforge.zbar.Config;
import net.sourceforge.zbar.ImageScanner;
import net.sourceforge.zbar.Symbol;
import net.sourceforge.zbar.SymbolSet;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ZbarCamera2Activity extends AppCompatActivity {
    String mCameraId;
    TextureView textureView;
    CameraDevice mCameraDevice;
    Handler handler;
    CaptureRequest.Builder builder;
    android.util.Size mPreviewSize;
    ImageReader mImageReader;
    static SparseIntArray ORIENTATIONS = new SparseIntArray();
    private ImageScanner scanner;

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 0);
        ORIENTATIONS.append(Surface.ROTATION_90, 90);
        ORIENTATIONS.append(Surface.ROTATION_180, 180);
        ORIENTATIONS.append(Surface.ROTATION_270, 270);
    }

    static class CompareSize implements Comparator<android.util.Size> {
        @Override
        public int compare(android.util.Size size, android.util.Size t1) {
            return Long.signum((long) size.getWidth() * size.getHeight() /
                    (long) t1.getWidth() * t1.getHeight());
        }
    }

    CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            mCameraDevice = cameraDevice;
            startPreview();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            mCameraDevice.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int i) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
    };

    TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
            setupCamera(i, i1);
            connectToCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

        }
    };

    static {
        System.loadLibrary("iconv");
    }

    private void closeCamera() {
        if (mCameraDevice != null) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
    }

    private void connectToCamera() {
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                try {
                    cameraManager.openCamera(mCameraId, stateCallback, handler);
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            } else if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                Toast.makeText(this, "Camera needs to permissions", Toast.LENGTH_SHORT).show();
                requestPermissions(new String[]{Manifest.permission.CAMERA}, 1);
            }
        } else {
            try {
                cameraManager.openCamera(mCameraId, stateCallback, handler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
    }

    private void setupCamera(int width, int heght) {
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            for (String cameraId : cameraManager.getCameraIdList()) {
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
                if (characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT) {
                    continue;
                } else {
                    StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                    int deviceOrientation = getWindowManager().getDefaultDisplay().getRotation();
                    int totalRotation = sensorToDeviceRotation(characteristics, deviceOrientation);
                    boolean swapRotation = totalRotation == 90 || totalRotation == 270;
                    int rotateWidth = width;
                    int rotateHeight = heght;
                    if (swapRotation) {
                        rotateWidth = heght;
                        rotateHeight = width;
                    }
                    mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class), rotateWidth, rotateHeight);
                    mImageReader = ImageReader.newInstance(mPreviewSize.getWidth(), mPreviewSize.getHeight(), ImageFormat.YUV_420_888, 3);
                    mImageReader.setOnImageAvailableListener(mImageAvailable, handler);
                    mCameraId = cameraId;
                    return;
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    CustomThread thread = new CustomThread();

    class CustomThread extends Thread {
        @Override
        public void run() {
            Looper.prepare();
            handler = new Handler();

            Looper.loop();
        }
    }

    private void startBackgroundThread() {
        thread.start();
    }

    private void stopBackgroundThread() {
        thread = null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_zbar_camera2);
        startBackgroundThread();
        scanner = new ImageScanner();
        scanner.setConfig(0, Config.X_DENSITY, 3);
        scanner.setConfig(0, Config.Y_DENSITY, 3);

        textureView = (TextureView) findViewById(R.id.textureView);
    }

    @Override
    protected void onResume() {
        super.onResume();
        setup();
    }

    private void setup() {
        if (textureView.isAvailable()) {
            setupCamera(textureView.getWidth(), textureView.getHeight());
            connectToCamera();
        } else {
            textureView.setSurfaceTextureListener(textureListener);
        }
    }

    @Override
    protected void onPause() {
        closeCamera();
        //stopBackgroundThread();
        super.onPause();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            }
        }
    }

    private static int sensorToDeviceRotation(CameraCharacteristics characteristics, int deviceOrientation) {
        int sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        deviceOrientation = ORIENTATIONS.get(sensorOrientation);
        return (sensorOrientation + deviceOrientation + 360) % 360;
    }

    private static android.util.Size chooseOptimalSize(android.util.Size[] choices, int width, int height) {
        List<android.util.Size> bigEnough = new ArrayList<>();
        for (android.util.Size option : choices) {
            if (option.getHeight() == option.getWidth() * height / width &&
                    option.getWidth() >= width && option.getHeight() >= height) {
                bigEnough.add(option);
            }
            if (bigEnough.size() > 0) {
                return Collections.min(bigEnough, new CompareSize());
            } else {
                return choices[0];
            }
        }
        return null;
    }

    private void startPreview() {
        SurfaceTexture surfaceTexture = textureView.getSurfaceTexture();
        surfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
        Surface previwSurface = new Surface(surfaceTexture);
        List surfaces = new ArrayList<>();
        try {
            builder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            builder.addTarget(previwSurface);
            Surface secondSurface = mImageReader.getSurface();
            builder.addTarget(secondSurface);
            surfaces.add(previwSurface);
            surfaces.add(secondSurface);

            mCameraDevice.createCaptureSession(surfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    try {
                        cameraCaptureSession.setRepeatingRequest(builder.build(), null, handler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Toast.makeText(getApplicationContext(), "Unable to setup camera preview", Toast.LENGTH_SHORT).show();
                }
            }, null);
        } catch (CameraAccessException e) {

        }
    }

    ImageReader.OnImageAvailableListener mImageAvailable = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            long startTime = System.nanoTime();

            Image image = reader.acquireLatestImage();
            if (image != null) {
                final Image.Plane[] planes = image.getPlanes();
                if (planes.length > 0) {
                    ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                    byte[] bytes = new byte[buffer.capacity()];
                    buffer.get(bytes);
                    net.sourceforge.zbar.Image barcode = new net.sourceforge.zbar.Image(mPreviewSize.getWidth(), mPreviewSize.getHeight(), "Y800");
                    barcode.setData(bytes);
                    int result = scanner.scanImage(barcode);
                    if (result != 0) {
                        long endTime = System.nanoTime();
                        long a = endTime - startTime;
                        double seconds = (double) a / 1000000.0;

                        SymbolSet syms = scanner.getResults();
                        for (Symbol sym : syms) {
                            String scanResult = sym.getData().trim();
                            DataType type = DataType.getByValue(sym.getType());
                            if (!i)
                                showAlertDialog(scanResult, seconds, type.name());
                        }
                    }
                }
            }

            if (image == null)
                return;

            image.close();
        }
    };

    static boolean i = false;

    private void showAlertDialog(String message, double seconds, String codeType) {

        i = true;
        new AlertDialog.Builder(this)
                .setTitle(getResources().getString(R.string.app_name))
                .setCancelable(false)
                .setMessage("Code: " + message + System.lineSeparator() + "Spent time in milliseconds: " + seconds +
                        System.lineSeparator() + "Code type: " + codeType)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        i = false;
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
