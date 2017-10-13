package com.example.hmaliborski.barcode;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.Camera;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private IntentIntegrator mQrScan;
    private boolean isFocusOnZBar = false, isFocusOnZXing = false, isFocusonScandit = false;
    private int fps = 15;
    private int resolution = 0;
    private int camera = 0, zBarCamera = 0, zXingCamera = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeSpinners();
        initializeToggle();

        Button zBarButton = (Button) findViewById(R.id.zbar);
        //Button zXingButton = (Button) findViewById(R.id.zxing);
        Button zXingCustomButton = (Button) findViewById(R.id.zxing2);
        //Button zXingCamera2Button = (Button) findViewById(R.id.zxingCam);
        Button scanditButton = (Button) findViewById(R.id.scanditBarCode);
        mQrScan = new IntentIntegrator(this);
        scanditButton.setOnClickListener(this);
        zBarButton.setOnClickListener(this);
        //zXingButton.setOnClickListener(this);
        zXingCustomButton.setOnClickListener(this);
        //zXingCamera2Button.setOnClickListener(this);
    }

    private void initializeToggle() {
        ToggleButton zXingToggle = (ToggleButton) findViewById(R.id.zxingToggle);
        zXingToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                isFocusOnZXing = b;
            }
        });

        ToggleButton zBarToggle = (ToggleButton) findViewById(R.id.zbarToggle);
        zBarToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                isFocusOnZBar = b;
            }
        });

        ToggleButton scanditToggle = (ToggleButton) findViewById(R.id.scanditToggle);
        scanditToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                isFocusonScandit = b;
            }
        });
    }

    private boolean isBackCameraExists() {
        int numCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numCameras; i++) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);
            if (Camera.CameraInfo.CAMERA_FACING_BACK == info.facing) {
                return true;
            }
        }
        return false;
    }

    private void initializeSpinners() {
        Spinner zxingCameraSpinner = (Spinner) findViewById(R.id.zxingCameraSpinner);
        ArrayAdapter<CharSequence> zxingCameraSpinnerAdapter = ArrayAdapter.createFromResource(this,
                R.array.cameras, android.R.layout.simple_spinner_item);
        zxingCameraSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        zxingCameraSpinner.setAdapter(zxingCameraSpinnerAdapter);
        zxingCameraSpinner.setSelection(0);


        Spinner zbarCameraSpinner = (Spinner) findViewById(R.id.zbarCameraSpinner);
        ArrayAdapter<CharSequence> zbarCameraSpinnerAdapter = ArrayAdapter.createFromResource(this,
                R.array.cameras, android.R.layout.simple_spinner_item);
        zbarCameraSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        zbarCameraSpinner.setAdapter(zbarCameraSpinnerAdapter);
        zbarCameraSpinner.setSelection(0);


        Spinner cameraSpinner = (Spinner) findViewById(R.id.cameraSpinner);
        ArrayAdapter<CharSequence> cameraAdapter = ArrayAdapter.createFromResource(this,
                R.array.cameras, android.R.layout.simple_spinner_item);
        cameraAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        cameraSpinner.setAdapter(cameraAdapter);
        cameraSpinner.setSelection(0);


        if (!isBackCameraExists()) {
            zbarCameraSpinner.setEnabled(false);
            cameraSpinner.setEnabled(false);
            zxingCameraSpinner.setEnabled(false);
        } else {
            zxingCameraSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                    switch (i) {
                        case 0:
                            zXingCamera = 0;
                            break;

                        case 1:
                            zXingCamera = 1;
                            break;
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {

                }
            });

            zbarCameraSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                    switch (i) {
                        case 0:
                            zBarCamera = 0;
                            break;

                        case 1:
                            zBarCamera = 1;
                            break;
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {

                }
            });

            cameraSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                    switch (i) {
                        case 0:
                            camera = 0;
                            break;

                        case 1:
                            camera = 1;
                            break;
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {

                }
            });
        }

        Spinner zBarResolutionSpinner = (Spinner) findViewById(R.id.zbarResolution);
        ArrayAdapter<CharSequence> adapterResolution = ArrayAdapter.createFromResource(this,
                R.array.resolution, android.R.layout.simple_spinner_item);
        adapterResolution.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        zBarResolutionSpinner.setAdapter(adapterResolution);
        zBarResolutionSpinner.setSelection(0);
        zBarResolutionSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                switch (i) {
                    case 0:
                        resolution = 0;
                        break;
                    case 1:
                        resolution = 1;
                        break;
                    case 2:
                        resolution = 2;
                        break;
                    case 3:
                        resolution = 3;
                        break;
                    case 4:
                        resolution = 4;
                        break;
                    case 5:
                        resolution = 5;
                        break;
                    case 6:
                        resolution = 6;
                        break;
                    case 7:
                        resolution = 7;
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        Spinner zBarFpsSpinner = (Spinner) findViewById(R.id.zbarSpinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.planets_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        zBarFpsSpinner.setAdapter(adapter);
        zBarFpsSpinner.setSelection(0);
        zBarFpsSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                switch (i) {
                    case 0:
                        fps = 15;
                        break;
                    case 1:
                        fps = 20;
                        break;
                    case 2:
                        fps = 25;
                        break;
                    case 3:
                        fps = 30;
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        long startTime = System.nanoTime();
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        long endTime = System.nanoTime();
        if (result != null) {
            long a = endTime - startTime;
            double seconds = (double) a / 1000000000.0;
            //if qrcode has nothing in it
            if (result.getContents() == null) {
                Toast.makeText(this, "Result Not Found", Toast.LENGTH_LONG).show();
            } else {
                //if qr contains data
                try {
                    //converting the data to json
                    JSONObject obj = new JSONObject(result.getContents());
                    //setting values to textviews
                    showAlertDialog(obj.getString("name") + " " + obj.getString("address"), seconds);
                } catch (JSONException e) {
                    e.printStackTrace();
                    //if control comes here
                    //that means the encoded format not matches
                    //in this case you can display whatever data is available on the qrcode
                    //to a toast
                    showAlertDialog(result.getContents(), seconds);
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.zbar:
                Intent intent = new Intent(view.getContext(), ZBarActivity.class);
                intent.putExtra("focus", isFocusOnZBar);
                intent.putExtra("fps", fps);
                intent.putExtra("resolution", resolution);
                if (isBackCameraExists())
                    intent.putExtra("camera", zBarCamera);
                startActivity(intent);
                break;

/*            case R.id.zxing:
                mQrScan.setBeepEnabled(false);
                mQrScan.initiateScan();
                break;*/

            case R.id.zxing2:
                Intent intent1 = new Intent(view.getContext(), ZXingActivity.class);
                intent1.putExtra("focus", isFocusOnZXing);
                intent1.putExtra("resolution", resolution);
                if (isBackCameraExists())
                    intent1.putExtra("camera", zXingCamera);
                startActivity(intent1);
                break;

/*            case R.id.zxingCam:
                Intent intent2 = new Intent(view.getContext(), ZbarCamera2Activity.class);
                startActivity(intent2);
                break;*/

            case R.id.scanditBarCode:
                Intent intent3 = new Intent(view.getContext(), ScanditActivity.class);
                if (isBackCameraExists())
                    intent3.putExtra("camera", camera);
                intent3.putExtra("focus", isFocusonScandit);
                startActivity(intent3);
                break;
        }
    }

    private void showAlertDialog(String message, double seconds) {
        new AlertDialog.Builder(this)
                .setTitle(getResources().getString(R.string.app_name))
                .setCancelable(false)
                .setMessage("Code: " + message + System.lineSeparator() + "Spent time: " + seconds)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                    }
                }).show();
    }
}
