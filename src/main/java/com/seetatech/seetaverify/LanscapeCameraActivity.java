package com.seetatech.seetaverify;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.os.Bundle;

import com.df.lib_seete6.Contract;
import com.seeta.sdk.FaceAntiSpoofing;

import org.opencv.core.Mat;
import org.opencv.core.Rect;

public class LanscapeCameraActivity extends AppCompatActivity implements Contract.View {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lanscape_camera);
    }

    @Override
    public void drawFaceRect(Rect faceRect) {

    }

    @Override
    public void drawFaceImage(Bitmap faceBmp) {

    }

    @Override
    public void toastMessage(String msg) {

    }

    @Override
    public void showCameraUnavailableDialog(int errorCode) {

    }

    @Override
    public void setStatus(int status, Mat matBgr, Rect faceRect) {

    }

    @Override
    public void onDetectFinish(FaceAntiSpoofing.Status status, float similarity, String name, Mat matBgr, Rect faceRect) {

    }

    @Override
    public void onFaceRegisterFinish(boolean isSuccess, String tip) {

    }

    @Override
    public void showSimpleTip(String tip) {

    }

    @Override
    public void setBestImage(Bitmap bitmap) {

    }

    @Override
    public void setPresenter(Contract.Presenter presenter) {

    }

    @Override
    public boolean isActive() {
        return false;
    }
}