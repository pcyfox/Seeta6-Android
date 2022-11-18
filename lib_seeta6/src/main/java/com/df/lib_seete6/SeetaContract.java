package com.df.lib_seete6;

import android.graphics.Bitmap;

import com.df.lib_seete6.view.FaceRecognitionListener;

import org.opencv.core.Rect;

public interface SeetaContract {

    interface ViewInterface extends FaceRecognitionListener {

        void onOpenCameraError(int code, String message);

        void drawFaceRect(Rect faceRect);

        void drawFaceImage(Bitmap faceBmp);

        boolean isActive();

    }

    interface Presenter {

        void takePicture(String path, String name);

        void startRegisterFrame(boolean needFaceRegister, String registeredName);

        void detect(byte[] data, int width, int height, int rotation);

        void destroy();

    }
}
