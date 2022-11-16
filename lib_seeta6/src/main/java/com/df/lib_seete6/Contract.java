package com.df.lib_seete6;

import android.graphics.Bitmap;

import com.seeta.sdk.FaceAntiSpoofing;

import org.opencv.core.Mat;
import org.opencv.core.Rect;

public interface Contract {

    interface View {
        void onOpenCameraError(int errorCode);

        void drawFaceRect(Rect faceRect);

        void drawFaceImage(Bitmap faceBmp);

        void onDetectFinish(FaceAntiSpoofing.Status status, float similarity, String name, Mat matBgr, Rect faceRect);

        void onRegisterFaceFinish(boolean isSuccess, String tip);

        boolean isActive();


    }

    interface Presenter {

        void takePicture(String path, String name);

        void startRegisterFrame(boolean needFaceRegister, String registeredName);

        void detect(byte[] data, int width, int height, int rotation);

        void destroy();

    }
}
