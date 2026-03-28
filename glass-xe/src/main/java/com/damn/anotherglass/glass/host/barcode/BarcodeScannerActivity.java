package com.damn.anotherglass.glass.host.barcode;

import android.app.Activity;
import android.content.Intent;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.Toast;

import com.damn.anotherglass.glass.host.R;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

import java.io.IOException;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class BarcodeScannerActivity extends Activity implements SurfaceHolder.Callback, Camera.PreviewCallback {

    public static final String EXTRA_SCAN_RESULT = "scan_result";

    private static final String TAG = "BarcodeScannerActivity";

    /** Minimum milliseconds between successive decode attempts. */
    private static final long DECODE_INTERVAL_MS = 500;

    /** Finish with no result after this many milliseconds if nothing was scanned. */
    private static final long SCAN_TIMEOUT_MS = 60_000;

    private Camera mCamera;

    private final ExecutorService mDecodeExecutor = Executors.newSingleThreadExecutor();

    private final AtomicBoolean mDecodeInProgress = new AtomicBoolean(false);

    private final AtomicBoolean mResultFound = new AtomicBoolean(false);

    private final AtomicLong mLastDecodeTime = new AtomicLong(0);

    private final MultiFormatReader mReader = new MultiFormatReader();

    private final Handler mTimeoutHandler = new Handler();

    private final Runnable mTimeoutRunnable = () -> {
        if (!mResultFound.get()) {
            setResult(RESULT_CANCELED);
            finish();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_barcode_scanner);

        SurfaceView surfaceView = findViewById(R.id.surface_view);

        Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);
        hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
        mReader.setHints(hints);

        SurfaceHolder holder = surfaceView.getHolder();
        holder.addCallback(this);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        mTimeoutHandler.postDelayed(mTimeoutRunnable, SCAN_TIMEOUT_MS);
    }

    @Override
    protected void onPause() {
        releaseCamera();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        mTimeoutHandler.removeCallbacks(mTimeoutRunnable);
        mDecodeExecutor.shutdownNow();
        super.onDestroy();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        startCamera(holder);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // no-op
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        releaseCamera();
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if (mResultFound.get()) {
            return;
        }

        long now = System.currentTimeMillis();
        if (now - mLastDecodeTime.get() < DECODE_INTERVAL_MS) {
            return;
        }

        if (!mDecodeInProgress.compareAndSet(false, true)) {
            return;
        }

        mLastDecodeTime.set(now);

        Camera.Size size = camera.getParameters().getPreviewSize();
        final int width = size.width;
        final int height = size.height;
        final byte[] copy = Arrays.copyOf(data, data.length);

        mDecodeExecutor.execute(() -> {
            String decoded = decode(copy, width, height);
            runOnUiThread(() -> {
                mDecodeInProgress.set(false);
                if (!TextUtils.isEmpty(decoded)) {
                    onCodeDetected(decoded);
                }
            });
        });
    }

    private void startCamera(SurfaceHolder holder) {
        try {
            mCamera = Camera.open();
            if (mCamera == null) {
                Toast.makeText(this, R.string.msg_camera_unavailable, Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            mCamera.setPreviewDisplay(holder);
            mCamera.setPreviewCallback(this);
            mCamera.startPreview();
        } catch (IOException e) {
            Log.e(TAG, "Failed to start camera", e);
            Toast.makeText(this, R.string.msg_camera_unavailable, Toast.LENGTH_SHORT).show();
            finish();
        } catch (RuntimeException e) {
            Log.e(TAG, "Camera runtime error", e);
            Toast.makeText(this, R.string.msg_camera_unavailable, Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void releaseCamera() {
        if (mCamera == null) {
            return;
        }
        try {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
        } catch (RuntimeException e) {
            Log.w(TAG, "Failed to stop camera preview", e);
        }
        mCamera.release();
        mCamera = null;
        mDecodeInProgress.set(false);
    }

    private String decode(byte[] data, int width, int height) {
        Result result = tryDecode(data, width, height);
        if (result != null) {
            return result.getText();
        }

        // Fallback for rotated frames from older camera drivers.
        byte[] rotated = rotateYuv420(data, width, height);
        //noinspection SuspiciousNameCombination
        result = tryDecode(rotated, height, width);
        return result != null ? result.getText() : null;
    }

    private Result tryDecode(byte[] data, int width, int height) {
        try {
            mReader.reset();
            return mReader.decodeWithState(new BinaryBitmap(new HybridBinarizer(
                    new YuvLuminanceSource(data, width, height)
            )));
        } catch (NotFoundException ignored) {
            return null;
        } catch (Exception e) {
            Log.v(TAG, "Decode failed", e);
            return null;
        }
    }

    private void onCodeDetected(String result) {
        if (!mResultFound.compareAndSet(false, true)) {
            return;
        }

        mTimeoutHandler.removeCallbacks(mTimeoutRunnable);
        Intent data = new Intent().putExtra(EXTRA_SCAN_RESULT, result);
        setResult(RESULT_OK, data);
        finish();
    }

    private static byte[] rotateYuv420(byte[] data, int width, int height) {
        byte[] rotated = new byte[data.length];
        int index = 0;
        for (int x = 0; x < width; x++) {
            for (int y = height - 1; y >= 0; y--) {
                rotated[index++] = data[y * width + x];
            }
        }
        return rotated;
    }

    private static class YuvLuminanceSource extends com.google.zxing.LuminanceSource {

        private final byte[] mYuvData;

        private final int mDataWidth;

        private final int mDataHeight;

        YuvLuminanceSource(byte[] yuvData, int dataWidth, int dataHeight) {
            super(dataWidth, dataHeight);
            mYuvData = yuvData;
            mDataWidth = dataWidth;
            mDataHeight = dataHeight;
        }

        @Override
        public byte[] getRow(int y, byte[] row) {
            if (y < 0 || y >= getHeight()) {
                throw new IllegalArgumentException("Requested row is outside the image: " + y);
            }
            int width = getWidth();
            if (row == null || row.length < width) {
                row = new byte[width];
            }
            System.arraycopy(mYuvData, y * mDataWidth, row, 0, width);
            return row;
        }

        @Override
        public byte[] getMatrix() {
            int width = getWidth();
            int height = getHeight();
            if (width == mDataWidth && height == mDataHeight) {
                return mYuvData;
            }

            byte[] matrix = new byte[width * height];
            for (int y = 0; y < height; y++) {
                System.arraycopy(mYuvData, y * mDataWidth, matrix, y * width, width);
            }
            return matrix;
        }

        @Override
        public boolean isCropSupported() {
            return false;
        }
    }
}


