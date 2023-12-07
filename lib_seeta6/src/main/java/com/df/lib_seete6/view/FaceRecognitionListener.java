package com.df.lib_seete6.view;

import com.df.lib_seete6.Target;
import com.seeta.sdk.FaceAntiSpoofing;

import org.opencv.core.Mat;
import org.opencv.core.Rect;

public interface FaceRecognitionListener {

    void onDetectFinish(Target target, Mat matBgr, Rect faceRect);


    void onTakePictureFinish(String path, String name);
}
