package com.df.lib_seete6.camera;

import android.hardware.Camera;
import android.util.Log;

public final class CameraUtils {
    private CameraUtils() {
    }

    private static final String TAG = "CameraUtils";

    public static int findCameraId() throws CameraUnavailableException {
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
                Log.d(TAG, "findCameraId()  front:" + i);
                if (i > maxFrontCameraId) maxFrontCameraId = i;
            }
        }
        if (maxFrontCameraId >= 0) return maxFrontCameraId;
        return Camera.CameraInfo.CAMERA_FACING_BACK;
    }
}
