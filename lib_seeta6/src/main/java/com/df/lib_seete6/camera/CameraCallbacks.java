package com.df.lib_seete6.camera;

import android.hardware.Camera;

@SuppressWarnings("deprecation")
public interface CameraCallbacks extends Camera.PreviewCallback{

    void onCameraUnavailable(int errorCode);
}
