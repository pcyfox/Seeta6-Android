package com.df.lib_seete6.config;


public class EnginConfig {
    public int minFaceSize = 80;
    public float faceThresh = 0.70f;

    public float fasClarity = 0.30f;
    public float fasThresh = 0.80f;

    public boolean isNeedCheckSpoofing = true;
    public boolean isNeedFaceImage = false;

    @Override
    public String toString() {
        return "EnginConfig{" +
                "minFaceSize=" + minFaceSize +
                ", faceThresh=" + faceThresh +
                ", fasClarity=" + fasClarity +
                ", fasThresh=" + fasThresh +
                ", needCheckSpoofing=" + isNeedCheckSpoofing +
                '}';
    }
}
