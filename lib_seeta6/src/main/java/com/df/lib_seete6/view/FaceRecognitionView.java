package com.df.lib_seete6.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.df.lib_seete6.ExtractFaceResultInterceptor;
import com.df.lib_seete6.SeetaContract;
import com.df.lib_seete6.PresenterImpl;
import com.df.lib_seete6.Target;
import com.df.lib_seete6.camera.CameraCallbacks;
import com.df.lib_seete6.camera.CameraPreview;
import com.df.lib_seete6.camera.CameraUtils;
import com.df.lib_seete6.config.EnginConfig;
import com.df.lib_seete6.utils.EnginHelper;

import org.opencv.core.Mat;
import org.opencv.core.Rect;

import java.io.File;

public class FaceRecognitionView extends FrameLayout implements SeetaContract.ViewInterface {
    private static final String TAG = "FaceRecognitionView";

    public FaceRecognitionView(@NonNull Context context) {
        super(context);
    }

    public FaceRecognitionView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public FaceRecognitionView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public FaceRecognitionView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    private CameraPreview cameraPreview;
    private FaceRectView faceRectView;
    private final PresenterImpl presenter;
    private Camera.Size previewSize;
    private float previewScaleX = 1.0f;
    private float previewScaleY = 1.0f;
    private long detectCount = 0L;
    private volatile boolean isStartDetected = false;
    private volatile boolean isInit = false;
    private final Object lock = new Object();
    private FaceRecognitionListener faceRecognitionListener;
    private FaceRegisterListener faceRegisterListener;

    {
        presenter = new PresenterImpl(this);
    }

    public void init() {
        if (isInit) return;
        presenter.setFaceRegisterListener(faceRegisterListener);
        faceRectView = new FaceRectView(getContext());
        cameraPreview = new CameraPreview(getContext());
        cameraPreview.setCameraCallbacks(new CameraCallbacks() {
            @Override
            public void onCameraUnavailable(int errorCode) {
                onOpenCameraError(errorCode, "");
            }

            @Override
            public void onPreviewFrame(byte[] data, Camera camera) {
                synchronized (lock) {
                    if (detectCount++ <= 3) {
                        Log.d(TAG, "onPreviewFrame() called call in first isStartDetected= [" + isStartDetected + "],detectCount=" + detectCount);
                    }
                    if (!isStartDetected) {
                        return;
                    }

                    if (previewSize == null) {
                        previewSize = camera.getParameters().getPreviewSize();
                        previewScaleY = (float) (cameraPreview.getHeight()) / previewSize.height;
                        previewScaleX = (float) (cameraPreview.getWidth()) / previewSize.width;
                    }

                    int orientation = cameraPreview.getCameraRotation();

                    presenter.detect(data, previewSize.width, previewSize.height, orientation > 0 ? orientation : -1);
                }
            }
        });
        presenter.resume(this);
        addView(cameraPreview, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        addView(faceRectView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        isInit = true;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        init();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        isStartDetected = false;
    }

    @Override
    public void drawFaceRect(Rect faceRect) {
        if (faceRectView == null) return;
        faceRectView.drawFaceRect(faceRect, previewScaleX, previewScaleY);
    }

    @Override
    public void drawFaceImage(Bitmap faceBmp) {
        if (faceRectView == null) return;
        faceRectView.drawBitmap(faceBmp, 0, 0, null);
    }

    @Override
    public void onOpenCameraError(int code, String message) {
        Log.d(TAG, "onOpenCameraError() called with: code = [" + code + "], message = [" + message + "]");
    }

    @Override
    public void onDetectFinish(Target target, Mat matBgr, Rect faceRect) {
        if (faceRecognitionListener != null) {
            faceRecognitionListener.onDetectFinish(target, matBgr, faceRect);
        }
    }


    @Override
    public void onTakePictureFinish(String path, String name) {
        if (faceRecognitionListener != null) {
            faceRecognitionListener.onTakePictureFinish(path, name);
        }
    }

    @Override
    public boolean isActive() {
        return isEnabled();
    }

    public PresenterImpl getPresenter() {
        return presenter;
    }

    public CameraPreview getCameraPreview() {
        return cameraPreview;
    }

    public FaceRectView getFaceRectView() {
        return faceRectView;
    }

    public void setFaceRegisterListener(FaceRegisterListener faceRegisterListener) {
        this.faceRegisterListener = faceRegisterListener;
        presenter.setFaceRegisterListener(faceRegisterListener);
    }

    public void setFaceRecognitionListener(FaceRecognitionListener faceRecognitionListener) {
        this.faceRecognitionListener = faceRecognitionListener;
    }

    public void takePicture(String path, String name) {
        presenter.takePicture(path, name);
    }

    public void registerByFrame(String key) {
        presenter.startRegisterFrame(key);
    }

    public boolean registerFace(String key, File faceFile) {
        return EnginHelper.getInstance().registerFace(key, faceFile);
    }

    public boolean initEngin() {
        return initEngin(new EnginConfig());
    }

    public boolean initEngin(EnginConfig config) {
        Log.d(TAG, "initEngin() called with: config = [" + config + "]");
        if (EnginHelper.getInstance().isInitOver()) {
            return true;
        }
        return EnginHelper.getInstance().initEngine(getContext(), config);
    }

    public void resumeDetect() {
        Log.d(TAG, "resumeDetect() called");
        synchronized (lock) {
            if (presenter != null) {
                presenter.resume(this);
            }
            isStartDetected = true;
        }
    }

    public void pauseDetect() {
        Log.d(TAG, "pauseDetect() called");
        synchronized (lock) {
            isStartDetected = false;
        }
    }

    public void setInterceptor(ExtractFaceResultInterceptor interceptor) {
        if (presenter != null) {
            presenter.setInterceptor(interceptor);
        }
    }

    public boolean isStartDetected() {
        synchronized (lock) {
            return isStartDetected;
        }
    }

    public boolean release() {
        pauseCamera();
        pauseDetect();
        faceRectView = null;
        if (presenter.destroy()) {
            return EnginHelper.getInstance().release();
        }
        isInit = false;
        return false;
    }

    /**
     * @param rotation 0:90度 1：180度 2：270度
     * @param cameraId :Camera.CameraInfo.CAMERA_FACING_FRONT,Camera.CameraInfo.CAMERA_FACING_BACK,
     */
    public void open(int cameraId, int rotation) {
        cameraPreview.open(cameraId, rotation);
    }

    public void open(int orientation) {
        cameraPreview.open(orientation);
    }

    public void open() {
        int orientation = cameraPreview.findCamera().second.orientation;
        int dro = 360 - orientation;
        if (dro > 0) {
            int r = (dro / 90) % 90;
            cameraPreview.open(r);
        } else {
            cameraPreview.open();
        }

    }

    public void pauseCamera() {
        cameraPreview.onPause();
    }

}
