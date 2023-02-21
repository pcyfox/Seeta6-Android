package com.seetatech.seetaverify;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.df.lib_seete6.Target;
import com.df.lib_seete6.view.FaceRecognitionListener;
import com.df.lib_seete6.view.FaceRecognitionView;

import org.opencv.core.Mat;
import org.opencv.core.Rect;

import java.io.File;

public class TestDialogActivity extends AppCompatActivity {

    private FaceDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_dialog);
        dialog = new FaceDialog(this);
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