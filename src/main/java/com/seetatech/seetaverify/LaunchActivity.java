package com.seetatech.seetaverify;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.df.lib_seete6.utils.EnginHelper;

import java.io.File;

public class LaunchActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initEngin();
        setContentView(R.layout.activity_launch);
        requestPermission();
        this.setFinishOnTouchOutside(false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseEngine();
    }

    private void initEngin() {
        new Thread(() -> {
            EnginHelper.getInstance().initEngine(this, true);
        }).start();
    }

    private void releaseEngine() {
        EnginHelper.getInstance().release();
    }


    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btnStart:
                startActivity(new Intent(this, MainActivity.class));
                break;
            case R.id.btnRegisterFace:
                startRegisterFace();
                break;
        }
    }


    private void startRegisterFace() {
        String facePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/face.jpg";
        File faceFile = new File(facePath);
        new Thread(() -> {
            boolean ret = EnginHelper.getInstance().registerFace("PCY", faceFile);
            String tip = "注册" + (ret ? "成功" : "失败");
            runOnUiThread(() -> {
                Toast.makeText(this, tip, Toast.LENGTH_SHORT).show();
            });
        }).start();
    }

    private void requestPermission() {
        // 先判断有没有权限
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.CAMERA}, 200);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length != 2) {
            getWindow().getDecorView().postDelayed(() -> {
                requestPermission();
            }, 15000);
        }
    }
}
