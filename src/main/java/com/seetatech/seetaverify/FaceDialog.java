package com.seetatech.seetaverify;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDialog;

import com.df.lib_seete6.Target;
import com.df.lib_seete6.view.FaceRecognitionListener;
import com.df.lib_seete6.view.FaceRecognitionView;
import com.df.lib_seete6.view.FaceRegisterListener;

import org.opencv.core.Mat;
import org.opencv.core.Rect;

public class FaceDialog extends AppCompatDialog {
    private static final String TAG = "FaceDialog";
    private FaceRecognitionView faceRecognitionView;

    public FaceRecognitionView getFaceRecognitionView() {
        return faceRecognitionView;
    }

    public FaceDialog(@NonNull Context context) {
        super(context);
    }

    public FaceDialog(@NonNull Context context, int theme) {
        super(context, theme);
    }

    protected FaceDialog(@NonNull Context context, boolean cancelable, @Nullable OnCancelListener cancelListener) {
        super(context, cancelable, cancelListener);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_face);
        getWindow().setDimAmount(0f);
        faceRecognitionView = findViewById(R.id.faceRecognitionView);
        assert faceRecognitionView != null;
        Button btnRegisterFace = findViewById(R.id.btnRegister);
        btnRegisterFace.setOnClickListener(v -> {
            faceRecognitionView.registerByFrame("西门庆");
        });

    }

    @Override
    protected void onStart() {
        super.onStart();
        new Thread(() -> {
            boolean initRet = faceRecognitionView.initEngin();
            Log.d(TAG, "onStart() initRet=" + initRet);
        }).start();


        faceRecognitionView.init();
        faceRecognitionView.setFaceRecognitionListener(new FaceRecognitionListener() {
            @Override
            public void onDetectFinish(Target target, Mat matBgr, Rect faceRect) {
                Log.d(TAG, "onDetectFinish() called with: target = [" + target + "], matBgr = [" + matBgr + "], faceRect = [" + faceRect + "]");
                toast("识别到人脸:" + target.getKey() + ",similarity:" + target.getSimilarity());
            }

            @Override
            public void onTakePictureFinish(String path, String name) {
                Log.d(TAG, "onTakePictureFinish() called with: path = [" + path + "], name = [" + name + "]");
            }
        });
        faceRecognitionView.setFaceRegisterListener((isSuccess, key) -> {
            Log.d(TAG, "onRegisterFinish() called with: isSuccess = [" + isSuccess + "], key = [" + key + "]");
            toast("注册人脸:" + key + ",isSuccess:" + isSuccess);
        });
        faceRecognitionView.open();
        faceRecognitionView.resumeDetect();
    }


    private void toast(String tsxt) {
        if (faceRecognitionView == null) return;
        faceRecognitionView.post(
                () -> Toast.makeText(faceRecognitionView.getContext(), tsxt, Toast.LENGTH_SHORT).show());
    }

    @Override
    public void dismiss() {
        faceRecognitionView.postDelayed(() -> {
            boolean ret = faceRecognitionView.release();
            Log.d(TAG, "dismiss() release ret=" + ret);
            super.dismiss();
        }, 500);
    }
}

