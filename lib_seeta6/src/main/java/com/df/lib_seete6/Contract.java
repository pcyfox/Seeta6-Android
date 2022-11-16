package com.df.lib_seete6;

import android.graphics.Bitmap;

import com.seeta.sdk.FaceAntiSpoofing;

import org.opencv.core.Mat;
import org.opencv.core.Rect;

public interface Contract {

    interface View {

        void drawFaceRect(Rect faceRect);

        void drawFaceImage(Bitmap faceBmp);

        void toastMessage(String msg);

        void showCameraUnavailableDialog(int errorCode);

        void setStatus(int status, Mat matBgr, Rect faceRect);

        void onDetectFinish(FaceAntiSpoofing.Status status, float similarity, String name, Mat matBgr, Rect faceRect);

        void onFaceRegisterFinish(boolean isSuccess, String tip);

        void showSimpleTip(String tip);

        void setBestImage(Bitmap bitmap);

        void setPresenter(Presenter presenter);

        boolean isActive();


    }

    interface Presenter {

        void takePicture(String path, String name);

        void startRegisterFrame(boolean needFaceRegister, String registeredName);

        void detect(byte[] data, int width, int height, int rotation);

        void destroy();

    }
}
