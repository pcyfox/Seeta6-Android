package com.seetatech.seetaverify;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.df.lib_seete6.BuildConfig;
import com.df.lib_seete6.Target;
import com.df.lib_seete6.view.FaceRecognitionListener;
import com.df.lib_seete6.view.FaceRecognitionView;
import com.df.lib_seete6.view.FaceRegisterListener;

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
        faceRecognitionView.setFaceRegisterListener((isSuccess, key) -> {
            Log.d(TAG, "onRegisterFinish, isSuccess = " + isSuccess + ",key = " + key);
        });

        faceRecognitionView.setFaceRecognitionListener(new FaceRecognitionListener() {

            @Override
            public void onDetectFinish(Target target, Mat matBgr, Rect faceRect) {
                if (BuildConfig.DEBUG)
                    Log.d(TAG, "onDetectFinish() called with: target = [" + target + "], matBgr = [" + matBgr + "], faceRect = [" + faceRect + "]");
                tvName.setText("key:" + target.getKey() + "\nsimilarity:" + target.getSimilarity() + "\nstatus;" + target.getStatus());
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
        new Thread(() -> {
            faceRecognitionView.initEngin();
        }).start();
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
        if(!release){
            new Thread(() -> {
                try {
                    Thread.sleep(8000);
                    boolean release2 = faceRecognitionView.release();
                    Log.w(TAG, "---------------onDestroy() called release 2 ret=" + release2);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }).start();
        }
    }


    public void onBtnRegisterClick(View c) {
        String key = etRegister.getText().toString();
        if (key.isEmpty()) {
            Toast.makeText(this, "请输入Key", Toast.LENGTH_SHORT).show();
            return;
        }
        faceRecognitionView.resumeDetect();
        faceRecognitionView.registerByFrame(key);
    }

    public void onBtnRegisterFromLocalClick(View v) {
        faceRecognitionView.pauseDetect();
        v.postDelayed(() -> new Thread(() -> {
            boolean ret = faceRecognitionView.registerFace("李二狗", new File("/sdcard/test.png"));
            Log.d(TAG, "onBtnRegisterFromLocalClick() called with: ret = [" + ret + "]");
            if (ret) faceRecognitionView.resumeDetect();
        }).start(), 300);
    }

    public void onTakePicClick(View v) {
        faceRecognitionView.takePicture("/sdcard/", "test.png");
    }

    public void onFinishClick(View c) {
        faceRecognitionView.pauseDetect();
        c.postDelayed(() -> finish(), 2000);
    }

}