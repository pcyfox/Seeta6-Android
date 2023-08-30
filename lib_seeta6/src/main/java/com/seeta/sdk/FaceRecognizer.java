package com.seeta.sdk;

import com.df.lib_seete6.utils.EnginHelper;

public class FaceRecognizer {
    private static final String TAG = "FaceRecognizer";

    static {
        System.loadLibrary("SeetaFaceRecognizer600_java");
    }

    public long impl = 0;

    private native void construct(SeetaModelSetting setting);

    public FaceRecognizer(SeetaModelSetting setting) {
        this.construct(setting);
    }

    public native void dispose();

    protected void finalize() throws Throwable {
        super.finalize();
        this.dispose();
    }


    public boolean extract(SeetaImageData image, SeetaPointF[] points, float[] features) {
        if (image == null || points == null || features == null) return false;
        if (!EnginHelper.getInstance().isInitOver()) return false;
        return Extract(image, points, features);
    }

    public native int GetCropFaceWidth();

    public native int GetCropFaceHeight();

    public native int GetCropFaceChannels();

    public native int GetExtractFeatureSize();

    public native boolean CropFace(SeetaImageData image, SeetaPointF[] points, SeetaImageData face);

    public native boolean ExtractCroppedFace(SeetaImageData face, float[] features);

    public native boolean Extract(SeetaImageData image, SeetaPointF[] points, float[] features);

    public native float CalculateSimilarity(float[] features1, float[] features2);
}
