package com.seetatech.seetaverify;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Toast;

import com.df.lib_seete6.Contract;
import com.df.lib_seete6.FaceRectView;
import com.df.lib_seete6.PresenterImpl;
import com.df.lib_seete6.camera.CameraCallbacks;
import com.df.lib_seete6.camera.CameraPreview;
import com.df.lib_seete6.config.EnginConfig;
import com.df.lib_seete6.utils.EnginHelper;
import com.seeta.sdk.FaceAntiSpoofing;

import org.opencv.core.Mat;
import org.opencv.core.Rect;

import java.io.File;

public class LandscapeCameraActivity extends AppCompatActivity implements Contract.View {
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
                    previewScaleX = (float) (cameraPreview.getHeight()) / previewSize.width;
                    previewScaleY = (float) (cameraPreview.getWidth()) / previewSize.height;
                }
                int orientation = cameraPreview.getCameraRotation();
                presenter.detect(data, previewSize.width, previewSize.height, orientation > 0 ? orientation : -1);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        cameraPreview.onResume(1);
    }

    @Override
    protected void onPause() {
        super.onPause();
        cameraPreview.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        EnginHelper.getInstance().release();
    }

    @Override
    public void drawFaceRect(Rect faceRect) {
        faceRectView.drawFaceRect(faceRect, previewScaleX, previewScaleY);
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


    public void onClick(View v) {
        startRegisterFace();
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