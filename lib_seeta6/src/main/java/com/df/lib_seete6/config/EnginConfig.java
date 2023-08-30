package com.df.lib_seete6.config;


import android.os.Environment;

import androidx.annotation.Keep;

@Keep
public class EnginConfig {

    public String modelRootDir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Android/data/Seeta6";

    public int minFaceSize = 100;
    public float faceThresh = 0.70f;

    public float fasClarity = 0.30f;
    public float fasThresh = 0.80f;

    public boolean isNeedCheckSpoofing = true;
    public boolean isNeedFaceImage = false;

    public boolean isNeedFlipLeftToRight = true; //左右翻转
    public boolean isNeedFlipUpToDown = false;//上下翻转


    @Override
    public String toString() {
        return "EnginConfig{" + "modelRootDir='" + modelRootDir + '\'' + ", minFaceSize=" + minFaceSize + ", faceThresh=" + faceThresh + ", fasClarity=" + fasClarity + ", fasThresh=" + fasThresh + ", isNeedCheckSpoofing=" + isNeedCheckSpoofing + ", isNeedFaceImage=" + isNeedFaceImage + '}';
    }
}
