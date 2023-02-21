package com.seetatech.seetaverify;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDialog;

import com.df.lib_seete6.Target;
import com.df.lib_seete6.view.FaceRecognitionListener;
import com.df.lib_seete6.view.FaceRecognitionView;

import org.opencv.core.Mat;
import org.opencv.core.Rect;

public class FaceDialog extends AppCompatDialog {
    private static final String TAG = "FaceDialog";
    private FaceRecognitionView faceRecognitionView;

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
        faceRecognitionView = findViewById(R.id.faceRecognitionView);
        assert faceRecognitionView != null;
        faceRecognitionView.setInterceptor((feats, status) -> false);
        faceRecognitionView.setFaceRecognitionListener(new FaceRecognitionListener() {
            @Override
            public void onDetectFinish(Target target, Mat matBgr, Rect faceRect) {
                Log.d(TAG, "onDetectFinish() called with: target = [" + target + "], matBgr = [" + matBgr + "], faceRect = [" + faceRect + "]");

            }

            @Override
            public void onRegisterByFrameFaceFinish(boolean isSuccess, String tip) {
                Log.d(TAG, "onRegisterByFrameFaceFinish() called with: isSuccess = [" + isSuccess + "], tip = [" + tip + "]");

            }

            @Override
            public void onTakePictureFinish(String path, String name) {
                Log.d(TAG, "onTakePictureFinish() called with: path = [" + path + "], name = [" + name + "]");

            }
        });

        Button btnRegisterFace = findViewById(R.id.btnRegister);
        btnRegisterFace.setOnClickListener(v -> {
            faceRecognitionView.registerByFrame("SPTLT");
        });

    }

    @Override
    protected void onStart() {
        super.onStart();
        new Thread(() -> {
            boolean initRet = faceRecognitionView.initEngin();
            Log.d(TAG, "onStart() initRet=" + initRet);
        }).start();

        faceRecognitionView.resumeCamera(0, 0);
    }

    @Override
    public void dismiss() {
        faceRecognitionView.pauseCamera();

        faceRecognitionView.postDelayed(() -> {
            boolean ret = faceRecognitionView.release();
            Log.d(TAG, "dismiss() release ret=" + ret);
            super.dismiss();
        }, 500);
    }
}

