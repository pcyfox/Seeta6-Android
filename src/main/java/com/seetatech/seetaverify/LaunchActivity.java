package com.seetatech.seetaverify;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.df.lib_seete6.utils.CachedImage;
import com.df.lib_seete6.utils.EnginHelper;

public class LaunchActivity extends AppCompatActivity {

    private ImageView imageView;
    private TextView tvSilentScore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launch);
        imageView = findViewById(R.id.best_image);
        tvSilentScore = findViewById(R.id.silentScore);
        this.setFinishOnTouchOutside(false);
    }

    public void onClick(View view) {
        imageView.setImageBitmap(null);
        tvSilentScore.setText("");
        startActivityForResult(new Intent(this, MainActivity.class), 1);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (CachedImage.imageAfterBlink != null) {
                imageView.setImageBitmap(CachedImage.imageAfterBlink);
                CachedImage.imageAfterBlink = null;
            }
        } else if (resultCode == RESULT_FIRST_USER) {
            imageView.setImageBitmap(null);
            tvSilentScore.setText("");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
