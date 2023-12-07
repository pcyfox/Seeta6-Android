package com.df.lib_seete6;

import android.graphics.Bitmap;

import androidx.annotation.Keep;

import com.df.lib_seete6.view.FaceRecognitionListener;
import com.df.lib_seete6.view.FaceRegisterListener;

import org.opencv.core.Rect;

@Keep
public interface SeetaContract {

    interface ViewInterface extends FaceRecognitionListener {

        void onOpenCameraError(int code, String message);

        void drawFaceRect(Rect faceRect);

        void drawFaceImage(Bitmap faceBmp);

        boolean isActive();

    }

    interface Presenter {

        void setFaceRegisterListener(FaceRegisterListener faceRegisterListener);

        void takePicture(String path, String name);

        void startRegisterFrame( String key);

        void detect(byte[] data, int width, int height, int rotation);

        boolean destroy();

    }
}
