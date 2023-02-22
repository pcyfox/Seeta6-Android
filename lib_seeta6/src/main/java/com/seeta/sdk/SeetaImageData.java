package com.seeta.sdk;

//TODO build native
public class SeetaImageData {
    public byte[] data;
    public int width;
    public int height;
    public int channels;

    public SeetaImageData(int width, int height, int channels) {
        this.data = new byte[width * height * channels];
        this.width = width;
        this.height = height;
        this.channels = channels;
    }

    public SeetaImageData(int width, int height) {
        this(width, height, 1);
    }

    public void clear() {
        data = null;
        width = 0;
        height = 0;
        channels = 0;
    }
}