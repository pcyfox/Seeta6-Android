package com.df.lib_seete6.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.df.lib_seete6.SeetaContract;
import com.df.lib_seete6.PresenterImpl;
import com.df.lib_seete6.camera.CameraCallbacks;
import com.df.lib_seete6.camera.CameraPreview;
import com.df.lib_seete6.config.EnginConfig;
import com.df.lib_seete6.utils.EnginHelper;
import com.seeta.sdk.FaceAntiSpoofing;

import org.opencv.core.Mat;
import org.opencv.core.Rect;

import java.io.File;

public class FaceRecognitionView extends FrameLayout implements SeetaContract.ViewInterface {

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
    private PresenterImpl presenter;
    private Camera.Size previewSize;
    private float previewScaleX = 1.0f;
    private float previewScaleY = 1.0f;
    private volatile boolean isStartDetected = true;
    private FaceRecognitionListener faceRecognitionListener;

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        cameraPreview = new CameraPreview(getContext());
        cameraPreview.setCameraCallbacks(new CameraCallbacks() {
            @Override
            public void onCameraUnavailable(int errorCode) {
                onOpenCameraError(errorCode, "");
            }

            @Override
            public void onPreviewFrame(byte[] data, Camera camera) {
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
        });

        faceRectView = new FaceRectView(getContext());
        addView(cameraPreview, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        addView(faceRectView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        presenter = new PresenterImpl(this);
    }


    @Override
    public void drawFaceRect(Rect faceRect) {
        faceRectView.drawFaceRect(faceRect, previewScaleX, previewScaleY);
    }

    @Override
    public void drawFaceImage(Bitmap faceBmp) {
        faceRectView.drawBitmap(faceBmp, 0, 0, null);
    }

    @Override
    public void onOpenCameraError(int code, String message) {

    }

    @Override
    public void onDetectFinish(FaceAntiSpoofing.Status status, float similarity, String key, Mat matBgr, Rect faceRect) {
        if (faceRecognitionListener != null) {
            faceRecognitionListener.onDetectFinish(status, similarity, key, matBgr, faceRect);
        }
    }

    @Override
    public void onRegisterByFrameFaceFinish(boolean isSuccess, String tip) {
        if (faceRecognitionListener != null) {
            faceRecognitionListener.onRegisterByFrameFaceFinish(isSuccess, tip);
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

    public CameraPreview getCameraPreview() {
        return cameraPreview;
    }

    public FaceRectView getFaceRectView() {
        return faceRectView;
    }

    public void setFaceRecognitionListener(FaceRecognitionListener faceRecognitionListener) {
        this.faceRecognitionListener = faceRecognitionListener;
    }

    public void takePicture(String path, String name) {
        presenter.takePicture(path, name);
    }

    public void registerByFrame(String key) {
        presenter.startRegisterFrame(true, key);
    }

    public boolean registerFace(String key, File faceFile) {
        return EnginHelper.getInstance().registerFace(key, faceFile);
    }

    public boolean initEngin() {
        return initEngin(new EnginConfig());
    }

    public boolean initEngin(EnginConfig config) {
        return EnginHelper.getInstance().initEngine(getContext(), config);
    }

    public boolean isStartDetected() {
        return isStartDetected;
    }

    public void setStartDetected(boolean startDetected) {
        isStartDetected = startDetected;
    }

    public boolean release() {
        isStartDetected = false;
        if (presenter.destroy()) {
            EnginHelper.getInstance().release();
            return true;
        }
        return false;
    }

    /**
     * @param rotation 0:90??? 1???180??? 2???270???
     * @param cameraId :Camera.CameraInfo.CAMERA_FACING_FRONT,Camera.CameraInfo.CAMERA_FACING_BACK,
     */
    public void resumeCamera(int rotation, int cameraId) {
        cameraPreview.onResume(rotation, cameraId);
    }

    public void pauseCamera() {
        cameraPreview.onPause();
    }

}
