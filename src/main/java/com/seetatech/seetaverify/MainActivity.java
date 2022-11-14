package com.seetatech.seetaverify;

import android.content.res.Configuration;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.View;

import com.df.lib_seete6.utils.EnginHelper;
import com.seetatech.seetaverify.mvp.MainFragment;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate");
        initFaceEngin();
        setContentView(R.layout.activity_main);
        Fragment fragment = new MainFragment();
        getSupportFragmentManager().beginTransaction().replace(android.R.id.content, fragment).commitNow();
        this.setFinishOnTouchOutside(false);
    }

    private void initFaceEngin() {
        new Thread(() -> {
        }).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume");
        View decorView = getWindow().getDecorView();
        int uiOptions = decorView.getSystemUiVisibility() | View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "onPause");
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.i(TAG, "onConfigurationChanged");
    }

}
