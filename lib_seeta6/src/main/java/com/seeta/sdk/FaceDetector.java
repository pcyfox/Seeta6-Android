package com.seeta.sdk;

public class FaceDetector {
    static {
        System.loadLibrary("SeetaFaceDetector600_java");
    }
    //for native
    public long impl = 0;

    private native void construct(SeetaModelSetting setting) throws Exception;

    public FaceDetector(SeetaModelSetting setting) throws Exception {
        this.construct(setting);
    }

    public native void dispose();

    protected void finalize() throws Throwable {
        super.finalize();
        this.dispose();
    }

    public native SeetaRect[] Detect(SeetaImageData image);

    public native void set(Property property, double value);

    public native double get(Property property);
}
