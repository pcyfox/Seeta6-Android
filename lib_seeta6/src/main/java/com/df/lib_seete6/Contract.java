package com.df.lib_seete6;

import android.graphics.Bitmap;

import org.opencv.core.Mat;
import org.opencv.core.Rect;

public interface Contract {

    interface View {

        void drawFaceRect(Rect faceRect);

        void drawFaceImage(Bitmap faceBmp);

        void toastMessage(String msg);

        void showCameraUnavailableDialog(int errorCode);

        void setStatus(int status, Mat matBgr, Rect faceRect);

        void onDetectFinish(float similarity, String name, Mat matBgr, Rect faceRect);

        void FaceRegister(String tip);

        void showSimpleTip(String tip);

        void setBestImage(Bitmap bitmap);

        void setPresenter(Presenter presenter);

        boolean isActive();


    }

    interface Presenter {

        void startRegister(boolean needFaceRegister, String registeredName);

        void detect(byte[] data, int width, int height, int rotation);

        void destroy();

    }
}
