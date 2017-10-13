package com.example.hmaliborski.barcode;

import android.content.Context;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;

import me.dm7.barcodescanner.core.BarcodeScannerView;
import me.dm7.barcodescanner.core.DisplayUtils;

public class ZxingScanerViewCustom extends BarcodeScannerView {
    private static final String TAG = "ZXingScannerView";
    private MultiFormatReader mMultiFormatReader;
    public static final List<BarcodeFormat> ALL_FORMATS = new ArrayList();
    private List<BarcodeFormat> mFormats;
    private ResultHandler mResultHandler;

    public ZxingScanerViewCustom(Context context) {
        super(context);
        this.initMultiFormatReader();
    }

    public ZxingScanerViewCustom(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.initMultiFormatReader();
    }

    public void setFormats(List<BarcodeFormat> formats) {
        this.mFormats = formats;
        this.initMultiFormatReader();
    }

    public void setResultHandler(ResultHandler resultHandler) {
        this.mResultHandler = resultHandler;
    }

    public Collection<BarcodeFormat> getFormats() {
        return this.mFormats == null ? ALL_FORMATS : this.mFormats;
    }

    private void initMultiFormatReader() {
        EnumMap hints = new EnumMap(DecodeHintType.class);
        hints.put(DecodeHintType.POSSIBLE_FORMATS, this.getFormats());
        this.mMultiFormatReader = new MultiFormatReader();
        this.mMultiFormatReader.setHints(hints);
    }

    Result var22;

    private long startTimeBefore = 0;
    private double doubleTimeBefore = 0;
    private long countOfImagesBefore = 0;

    public void onPreviewFrame(byte[] data, Camera camera) {
        countOfImagesBefore++;
        if (startTimeBefore == 0) {
            startTimeBefore = System.nanoTime();
        }
        if (this.mResultHandler != null) {
            try {
                Camera.Parameters e = camera.getParameters();
                Camera.Size size = e.getPreviewSize();
                int width = size.width;
                int height = size.height;
                if (DisplayUtils.getScreenOrientation(this.getContext()) == 1) {
                    byte[] rawResult = new byte[data.length];
                    int source = 0;

                    while (true) {
                        if (source >= height) {
                            source = width;
                            width = height;
                            height = source;
                            data = rawResult;
                            break;
                        }

                        for (int finalRawResult = 0; finalRawResult < width; ++finalRawResult) {
                            rawResult[finalRawResult * height + height - source - 1] = data[finalRawResult + source * width];
                        }

                        ++source;
                    }
                }

                long startTime = System.nanoTime();
                var22 = null;
                final PlanarYUVLuminanceSource var23 = this.buildLuminanceSource(data, width, height);

                if (var23 != null) {
                    BinaryBitmap var24 = new BinaryBitmap(new HybridBinarizer(var23));

                    try {
                        var22 = this.mMultiFormatReader.decodeWithState(var24);
                    } catch (ReaderException | NullPointerException | ArrayIndexOutOfBoundsException var17) {
                    } finally {
                        this.mMultiFormatReader.reset();
                    }
                }

                long endTime = System.nanoTime();
                long a = endTime - startTime;
                final double seconds = Math.floor(((double) a / 1000000.0) * 100) / 100;

                if (var22 != null) {
                    long endTimeBefore = System.nanoTime() - startTimeBefore;
                    doubleTimeBefore = (double) endTimeBefore / 1000000.0;
                    doubleTimeBefore = Math.floor(doubleTimeBefore * 100) / 100;
                    Handler handler = new Handler(Looper.getMainLooper());
                    handler.post(new Runnable() {
                        public void run() {
                            ResultHandler tmpResultHandler = mResultHandler;
                            ZxingScanerViewCustom.this.mResultHandler = null;
                            ZxingScanerViewCustom.this.stopCameraPreview();
                            if (tmpResultHandler != null) {
                                tmpResultHandler.handleResult(var22, var22.getBarcodeFormat().name(), seconds, countOfImagesBefore, doubleTimeBefore);
                                countOfImagesBefore = 0;
                                startTimeBefore = 0;

                            }
                        }
                    });
                } else {
                    camera.setOneShotPreviewCallback(this);
                }
            } catch (RuntimeException var21) {
                Log.e("ZXingScannerView", var21.toString(), var21);
            }
        }
    }

    public void resumeCameraPreview(ResultHandler resultHandler) {
        this.mResultHandler = resultHandler;
        super.resumeCameraPreview();
    }

    public PlanarYUVLuminanceSource buildLuminanceSource(byte[] data, int width, int height) {
        Rect rect = this.getFramingRectInPreview(width, height);
        if (rect == null) {
            return null;
        } else {
            PlanarYUVLuminanceSource source = null;

            try {
                source = new PlanarYUVLuminanceSource(data, width, height, rect.left, rect.top, rect.width(), rect.height(), false);
            } catch (Exception var7) {
            }

            return source;
        }
    }

    static {
        ALL_FORMATS.add(BarcodeFormat.UPC_A);
        ALL_FORMATS.add(BarcodeFormat.UPC_E);
        ALL_FORMATS.add(BarcodeFormat.EAN_13);
        ALL_FORMATS.add(BarcodeFormat.EAN_8);
        ALL_FORMATS.add(BarcodeFormat.RSS_14);
        ALL_FORMATS.add(BarcodeFormat.CODE_39);
        ALL_FORMATS.add(BarcodeFormat.CODE_93);
        ALL_FORMATS.add(BarcodeFormat.CODE_128);
        ALL_FORMATS.add(BarcodeFormat.ITF);
        ALL_FORMATS.add(BarcodeFormat.CODABAR);
        ALL_FORMATS.add(BarcodeFormat.QR_CODE);
        ALL_FORMATS.add(BarcodeFormat.DATA_MATRIX);
        ALL_FORMATS.add(BarcodeFormat.PDF_417);
    }

    public interface ResultHandler {
        void handleResult(Result var1, String barCode, double seconds, long countOfImages, double timeBefore);
    }

    public void setFocus(boolean isFocus) {
        this.setAutoFocus(isFocus);
    }
}
