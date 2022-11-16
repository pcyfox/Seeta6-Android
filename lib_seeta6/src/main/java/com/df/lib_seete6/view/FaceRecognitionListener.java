package com.df.lib_seete6.view;

import com.seeta.sdk.FaceAntiSpoofing;

import org.opencv.core.Mat;
import org.opencv.core.Rect;

public interface FaceRecognitionListener {
    void onOpenCameraError(int code, String message);

    void onDetectFinish(FaceAntiSpoofing.Status status, float similarity, String key, Mat matBgr, Rect faceRect);

    void onRegisterByFrameFaceFinish(boolean isSuccess, String tip);

    void onTakePictureFinish();
}
