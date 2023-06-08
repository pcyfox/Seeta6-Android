package com.seetatech.seetaverify;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.df.lib_seete6.utils.EnginHelper;

public class TestDialogActivity extends AppCompatActivity {

    private FaceDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_dialog);
        dialog = new FaceDialog(this);
        dialog.setOnDismissListener(log -> {
//            if (EnginHelper.getInstance().isInitOver()) {
//                getWindow().getDecorView().postDelayed(() -> {
//                    dialog.getFaceRecognitionView().release();
//                }, 1800);
//            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

    }


    @Override
    protected void onPause() {
        super.onPause();
    }

    public void onClick(View v) {
        dialog.show();
    }
}