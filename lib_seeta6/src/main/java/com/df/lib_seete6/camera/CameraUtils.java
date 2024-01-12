package com.df.lib_seete6.camera;

import android.hardware.Camera;
import android.util.Log;
import android.util.Pair;

public final class CameraUtils {
    private CameraUtils() {
    }

    private static final String TAG = "CameraUtils";

    public static Pair<Integer, Camera.CameraInfo> findCamera() throws CameraUnavailableException {
        int numberOfCameras = Camera.getNumberOfCameras();
        if (numberOfCameras <= 0) {
            Log.d(TAG, "findCameraId() called failed,not found camera!");
            throw new CameraUnavailableException();
        }

        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        int maxFrontCameraId = -1;
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                Log.d(TAG, "findCameraId()  front camera id:" + i + ", orientation:" + cameraInfo.orientation);
                if (i > maxFrontCameraId) maxFrontCameraId = i;
            }
        }
        if (maxFrontCameraId >= 0) return new Pair<>(maxFrontCameraId, cameraInfo);

        return new Pair<>(0, cameraInfo);
    }
}
