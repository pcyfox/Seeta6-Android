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
        faceRecognitionView = findViewById(R.id.faceRecognitionView);
        assert faceRecognitionView != null;
//        faceRecognitionView.setInterceptor(new ExtractFaceResultInterceptor() {
//            @Override
//            public boolean onPrepare(SeetaRect rect) {
//                Log.d(TAG, "onPrepare() called with: rect = [" + rect + "]");
//                boolean isGoodFace = rect.width * rect.height > 200 * 200;
//                if (isGoodFace) {
//                    faceRecognitionView.pauseDetect();
//                }
//                return isGoodFace;
//            }
//
//            @Override
//            public boolean onExtractFeats(float[] feats, FaceAntiSpoofing.Status status) {
//                Log.d(TAG, "onExtractFeats() called with: feats = [" + feats + "], status = [" + status + "]");
//                return true;
//            }
//        });


        Button btnRegisterFace = findViewById(R.id.btnRegister);
        btnRegisterFace.setOnClickListener(v -> {
            faceRecognitionView.registerByFrame("SPTLT");
        });

    }

    @Override
    protected void onStart() {
        super.onStart();
        faceRecognitionView.init();
        new Thread(() -> {
            boolean initRet = faceRecognitionView.initEngin();
            Log.d(TAG, "onStart() initRet=" + initRet);
        }).start();
        faceRecognitionView.setFaceRecognitionListener(new FaceRecognitionListener() {
            @Override
            public void onDetectFinish(Target target, Mat matBgr, Rect faceRect) {
                Log.d(TAG, "onDetectFinish() called with: target = [" + target + "], matBgr = [" + matBgr + "], faceRect = [" + faceRect + "]");

            }

            @Override
            public void onTakePictureFinish(String path, String name) {
                Log.d(TAG, "onTakePictureFinish() called with: path = [" + path + "], name = [" + name + "]");

            }
        });

        faceRecognitionView.resumeCamera(0, 0);
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

