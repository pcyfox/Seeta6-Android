package com.seetatech.seetaverify;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.df.lib_seete6.view.FaceRecognitionListener;
import com.df.lib_seete6.view.FaceRecognitionView;
import com.seeta.sdk.FaceAntiSpoofing;

import org.opencv.core.Mat;
import org.opencv.core.Rect;

import java.io.File;

public class TestActivity extends AppCompatActivity {
    private static final String TAG = "TestActivity";
    private FaceRecognitionView faceRecognitionView;
    private EditText etRegister;
    private TextView tvName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_activty);
        faceRecognitionView = findViewById(R.id.faceRecognitionView);
        etRegister = findViewById(R.id.et_register_name);
        tvName = findViewById(R.id.tv_name);
        new Thread(() -> {
            faceRecognitionView.initEngin();
        }).start();

        faceRecognitionView.setFaceRecognitionListener(new FaceRecognitionListener() {

            @Override
            public void onDetectFinish(FaceAntiSpoofing.Status status, float similarity, String key, Mat matBgr, Rect faceRect) {
                Log.d(TAG, "onDetectFinish() called with: status = [" + status + "], similarity = [" + similarity + "], key = [" + key + "], matBgr = [" + matBgr + "], faceRect = [" + faceRect + "]");
                tvName.setText("key:" + key + "\nsimilarity:" + similarity + "\nstatus;" + status);
            }

            @Override
            public void onRegisterByFrameFaceFinish(boolean isSuccess, String tip) {
                Log.d(TAG, "onRegisterByFrameFaceFinish() called with: isSuccess = [" + isSuccess + "], tip = [" + tip + "]");
            }

            @Override
            public void onTakePictureFinish(String path, String name) {
                Log.d(TAG, "onTakePictureFinish() called");

            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        faceRecognitionView.resumeCamera(0, 0);
    }


    @Override
    protected void onPause() {
        super.onPause();
        faceRecognitionView.pauseCamera();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        boolean release = faceRecognitionView.release();
        Log.w(TAG, "---------------onDestroy() called release 1 ret=" + release);
        if (!release) {
            release = faceRecognitionView.release();
        }
        Log.w(TAG, "---------------onDestroy() called release 2 ret=" + release);
    }


    public void onBtnRegisterClick(View c) {
        String key = etRegister.getText().toString();
        if (key.isEmpty()) {
            return;
        }
        faceRecognitionView.registerByFrame(key);
    }

    public void onBtnRegisterFromLocalClick(View v) {
        faceRecognitionView.setStartDetected(false);
        v.postDelayed(() -> new Thread(() -> {
            boolean ret = faceRecognitionView.registerFace("?????????", new File("/sdcard/?????????.png"));
            Log.d(TAG, "onBtnRegisterFromLocalClick() called with: ret = [" + ret + "]");
            faceRecognitionView.setStartDetected(true);
        }).start(), 300);
    }

    public void onTakePicClick(View v) {
        faceRecognitionView.takePicture("/sdcard/", "?????????.png");
    }

    public void onFinishClick(View c) {
        faceRecognitionView.setStartDetected(false);
        c.postDelayed(() -> finish(), 2000);
    }

}