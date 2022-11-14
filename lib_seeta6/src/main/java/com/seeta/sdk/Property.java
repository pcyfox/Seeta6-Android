package com.seeta.sdk;


public enum Property {
    PROPERTY_MIN_FACE_SIZE(0), PROPERTY_THRESHOLD(1), PROPERTY_MAX_IMAGE_WIDTH(2), PROPERTY_MAX_IMAGE_HEIGHT(3), PROPERTY_NUMBER_THREADS(4);
    private final int value;

    private Property(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}

