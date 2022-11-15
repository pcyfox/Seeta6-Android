package com.seetatech.seetaverify;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.df.lib_seete6.utils.EnginHelper;

import java.io.File;

public class LaunchActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launch);
        this.setFinishOnTouchOutside(false);
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
        if (!faceFile.exists() || !faceFile.canRead()) {
            Toast.makeText(this, "访问文件失败！", Toast.LENGTH_SHORT).show();
            return;
        }
        new Thread(() -> {
            Bitmap faceBitmap = BitmapFactory.decodeFile(facePath);
            boolean ret = EnginHelper.getInstance().registerFace("PCY", faceBitmap);
            String tip = "注册" + (ret ? "成功" : "失败");
            runOnUiThread(() -> {
                Toast.makeText(this, tip, Toast.LENGTH_SHORT).show();
            });
        }).start();
    }
}
