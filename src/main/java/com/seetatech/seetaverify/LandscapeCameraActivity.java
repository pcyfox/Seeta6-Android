package com.seetatech.seetaverify;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Toast;

import com.df.lib_seete6.SeetaContract;
import com.df.lib_seete6.view.FaceRectView;
import com.df.lib_seete6.PresenterImpl;
import com.df.lib_seete6.camera.CameraCallbacks;
import com.df.lib_seete6.camera.CameraPreview;
import com.df.lib_seete6.utils.EnginHelper;
import com.seeta.sdk.FaceAntiSpoofing;

import org.opencv.core.Mat;
import org.opencv.core.Rect;

import java.io.File;

public class LandscapeCameraActivity extends AppCompatActivity implements SeetaContract.ViewInterface {
    private CameraPreview cameraPreview;
    private FaceRectView faceRectView;

    private PresenterImpl presenter;

    private Camera.Size previewSize;
    private float previewScaleX = 1.0f;
    private float previewScaleY = 1.0f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lanscape_camera);
        cameraPreview = findViewById(R.id.camera_preview);
        faceRectView = findViewById(R.id.faceRectView);
        presenter = new PresenterImpl(this);

        cameraPreview.setCameraCallbacks(new CameraCallbacks() {
            @Override
            public void onCameraUnavailable(int errorCode) {

            }

            @Override
            public void onPreviewFrame(byte[] data, Camera camera) {
                if (previewSize == null) {
                    previewSize = camera.getParameters().getPreviewSize();
                    initEngin();
                    previewScaleY = (float) (cameraPreview.getHeight()) / previewSize.height;
                    previewScaleX = (float) (cameraPreview.getWidth()) / previewSize.width;
                }
                int orientation = cameraPreview.getCameraRotation();
                presenter.detect(data, previewSize.width, previewSize.height, orientation > 0 ? orientation : -1);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        cameraPreview.onResume(1,1);
    }

    @Override
    protected void onPause() {
        super.onPause();
        cameraPreview.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EnginHelper.getInstance().release();
    }

    @Override
    public void onOpenCameraError(int errorCode, String msg) {

    }

    @Override
    public void onTakePictureFinish(String path, String name) {

    }

    @Override
    public void drawFaceRect(Rect faceRect) {
        faceRectView.drawFaceRect(faceRect, previewScaleX, previewScaleY);
    }

    @Override
    public void drawFaceImage(Bitmap faceBmp) {
        faceRectView.drawBitmap(faceBmp, 0, 0, null);
    }

    @Override
    public void onDetectFinish(FaceAntiSpoofing.Status status, float similarity, String key, Mat matBgr, Rect faceRect) {

    }

    @Override
    public void onRegisterByFrameFaceFinish(boolean isSuccess, String tip) {

    }

    @Override
    public boolean isActive() {
        return !isFinishing() || !isDestroyed();
    }


    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_register:
                startRegisterFace();
                break;
            case R.id.btn_take_pic:
                presenter.takePicture("/sdcard/", "lDetect.jpg");
                break;
        }
    }


    private void startRegisterFace() {
        //String facePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/face.jpg";
        String facePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/bgr2.png";
        File faceFile = new File(facePath);
        new Thread(() -> {
            boolean ret = EnginHelper.getInstance().registerFace("PCY", faceFile);
            String tip = "注册" + (ret ? "成功" : "失败");
            runOnUiThread(() -> {
                Toast.makeText(this, tip, Toast.LENGTH_SHORT).show();
            });
        }).start();
    }

    private void initEngin() {
        new Thread(() -> {
            EnginHelper.getInstance().initEngine(this, true);
        }).start();
    }
}