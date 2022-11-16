/*
 * Copyright 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.df.lib_seete6.camera;

import android.content.Context;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.Nullable;

import com.df.lib_seete6.constants.ErrorCode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Camera preview that displays a {@link Camera}.
 * <p>
 * Handles basic lifecycle methods to display and stop the preview.
 * <p>
 * Implementation is based directly on the documentation at
 * http://developer.android.com/guide/topics/media/camera.html
 */
@SuppressWarnings("deprecation")
public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
    private static final int CAMERA_ID = 1;
    private static final String TAG = "CameraPreview";
    private final SurfaceHolder mHolder;
    @Nullable
    private Camera mCamera;
    @Nullable
    private Camera.CameraInfo mCameraInfo;
    private int mDisplayOrientation;
    private CameraCallbacks mCallbacks;
    private boolean isCreated;
    private Size wantPreViewSize = new Size(640, 480);

    private final OpenCameraAction mOpenCameraAction = new OpenCameraAction();

    private final Runnable mStartPreviewAction = new Runnable() {
        @Override
        public void run() {
            if (isCreated()) {
                startPreview(mHolder);
            }
        }
    };
    private int cameraRotation = 0;

    public CameraPreview(Context context) {
        this(context, null);
    }

    public CameraPreview(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CameraPreview(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mHolder = getHolder();
        mHolder.addCallback(this);
    }

    public Size getWantPreViewSize() {
        return wantPreViewSize;
    }

    public void setWantPreViewSize(Size wantPreViewSize) {
        this.wantPreViewSize = wantPreViewSize;
    }

    /**
     * Calculate the correct orientation for a {@link Camera} preview that is displayed on screen.
     * <p>
     * Implementation is based on the sample code provided in
     * {@link Camera#setDisplayOrientation(int)}.
     */
    public static int calculatePreviewOrientation(Camera.CameraInfo info, int rotation) {
        int degrees = 0;

        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }

        return result;
    }

    private void setCamera(Camera camera, Camera.CameraInfo cameraInfo, int displayOrientation) {
        Log.d(TAG, "setCamera() called with: camera = [" + camera + "], cameraInfo = [" + cameraInfo + "], displayOrientation = [" + displayOrientation + "]");
        mCamera = camera;
        mCameraInfo = cameraInfo;
        mDisplayOrientation = displayOrientation;
    }

    public void setCameraCallbacks(CameraCallbacks callbacks) {
        mCallbacks = callbacks;
    }

    public boolean isCreated() {
        return isCreated;
    }


    public Camera.Size findBestPreviewSize() {
        List<Camera.Size> sizes = mCamera.getParameters().getSupportedPreviewSizes();
        Map<Integer, Camera.Size> tempSize = new HashMap<>();

        for (Camera.Size size : sizes) {
            if (size.width == wantPreViewSize.getWidth() && size.height == wantPreViewSize.getHeight()) {
                return size;
            }
            int wTotal = wantPreViewSize.getHeight() * wantPreViewSize.getWidth();
            int total = size.width * size.height;
            tempSize.put(Math.abs(wTotal - total), size);

        }
        ArrayList<Integer> dSizeList = new ArrayList<>(tempSize.keySet());
        Collections.sort(dSizeList);
        Camera.Size expected = tempSize.get(dSizeList.get(0));
        return expected;
    }

    private void startPreview(SurfaceHolder holder) {
        wantPreViewSize = new Size(getWidth(), getHeight());
        // The Surface has been created, now tell the camera where to draw the preview.
        if (mCamera == null || mCameraInfo == null) {
            return;
        }
        try {
            mCamera.setPreviewDisplay(holder);
            Camera.Size expected = findBestPreviewSize();
            Log.i(TAG, "Preview size is w:" + expected.width + " h:" + expected.height);
            Camera.Parameters parameters = mCamera.getParameters();
            parameters.setPreviewSize(expected.width, expected.height);
            mCamera.setParameters(parameters);
            mCamera.startPreview();
            Log.i(TAG, "Camera preview started.");
            if (mCallbacks != null) {
                mCamera.setPreviewCallback(mCallbacks);
            }
        } catch (Exception e) {
            Log.i(TAG, "Error setting camera preview: " + e.getMessage());
            if (mCallbacks != null) {
                mCallbacks.onCameraUnavailable(ErrorCode.CAMERA_UNAVAILABLE_PREVIEW);
            }
        }
    }

    public void onResume(int rotation) {
        Log.d(TAG, "onResume() called with: rotation = [" + rotation + "]");
        mOpenCameraAction.setRotation(rotation);
        removeCallbacks(mOpenCameraAction);
        postDelayed(mOpenCameraAction, 400L);
    }

    public void onPause() {
        removeCallbacks(mStartPreviewAction);
        removeCallbacks(mOpenCameraAction);
        stopPreviewAndFreeCamera();
    }

    public int getCameraRotation() {
        return cameraRotation;
    }

    public int getPreviewWidth() {
        return -1;
    }

    public int getPreviewHeight() {
        return -1;
    }

    private void openCamera() throws CameraUnavailableException {
        if (Camera.getNumberOfCameras() > 0) {
            try {
                mCamera = Camera.open(CAMERA_ID);
                assert mCamera != null;
            } catch (Exception e) {
                throw new CameraUnavailableException(e);
            }
        } else {
            throw new CameraUnavailableException();
        }
    }

    private void stopPreviewAndFreeCamera() {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.setPreviewCallback(null);
            mCamera.release();
            mCamera = null;
        }
    }

    public void surfaceCreated(SurfaceHolder holder) {
        Log.i(TAG, "surfaceCreated");
        isCreated = true;
        removeCallbacks(mStartPreviewAction);
        postDelayed(mStartPreviewAction, 500);
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        // empty. Take care of releasing the Camera preview in your activity.
        Log.i(TAG, "surfaceDestroy");
        isCreated = false;
        removeCallbacks(mStartPreviewAction);
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        // If your preview can change or rotate, take care of those events here.
        // Make sure to stop the preview before resizing or reformatting it.
        if (mCamera == null || mCameraInfo == null) {
            return;
        }
        if (mHolder.getSurface() == null) {
            // preview surface does not exist
            Log.i(TAG, "Preview surface does not exist");
            return;
        }

        // stop preview before making changes
        try {
            mCamera.stopPreview();
            Log.i(TAG, "Preview stopped.");
        } catch (Exception e) {
            // ignore: tried to stop a non-existent preview
            Log.i(TAG, "Error starting camera preview: " + e.getMessage());
        }
        handleOrientation();
        removeCallbacks(mStartPreviewAction);
        postDelayed(mStartPreviewAction, 50);
    }

    private class OpenCameraAction implements Runnable {
        private int rotation = 0;

        public void setRotation(int rotation) {
            this.rotation = rotation;
        }

        @Override
        public void run() {
            try {
                openCamera();
                Camera.CameraInfo info = new Camera.CameraInfo();
                Camera.getCameraInfo(CAMERA_ID, info);
                setCamera(mCamera, info, rotation);
                removeCallbacks(mStartPreviewAction);

                handleOrientation();
                postDelayed(mStartPreviewAction, 50);
            } catch (Exception e) {
                e.printStackTrace();
                if (mCallbacks != null) {
                    mCallbacks.onCameraUnavailable(ErrorCode.CAMERA_UNAVAILABLE_ERROR);
                }
            }
        }
    }

    private void handleOrientation() {
        if (mCamera == null) {
            return;
        }
        cameraRotation = calculatePreviewOrientation(mCameraInfo, mDisplayOrientation);
        mCamera.setDisplayOrientation(cameraRotation);
    }

}