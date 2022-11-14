package com.seeta.sdk;

public class FaceAntiSpoofing {
    static {
        System.loadLibrary("SeetaFaceAntiSpoofingX600_java");
    }

    public enum Status {
        REAL, SPOOF, FUZZY, DETECTING,
    }

    public long impl = 0;

    private native void construct(SeetaModelSetting setting);

    public native void dispose();

    public FaceAntiSpoofing(SeetaModelSetting setting) {
        this.construct(setting);
    }

    protected void finalize() throws Throwable {
        super.finalize();
        this.dispose();
    }

    private native int PredictCore(SeetaImageData image, SeetaRect face, SeetaPointF[] landmarks);

    private native int PredictVideoCore(SeetaImageData image, SeetaRect face, SeetaPointF[] landmarks);

    public Status Predict(SeetaImageData image, SeetaRect face, SeetaPointF[] landmarks) {
        int status_num = this.PredictCore(image, face, landmarks);
        return Status.values()[status_num];
    }

    public Status PredictVideo(SeetaImageData image, SeetaRect face, SeetaPointF[] landmarks) {
        int status_num = this.PredictVideoCore(image, face, landmarks);
        return Status.values()[status_num];
    }

    public native void ResetVideo();

    public native void GetPreFrameScore(float[] clarity, float[] reality);

    public native void SetVideoFrameCount(int number);

    public native int GetVideoFrameCount();

    public native void SetThreshold(float clarity, float reality);

    public native void GetThreshold(float[] clarity, float[] reality);
}