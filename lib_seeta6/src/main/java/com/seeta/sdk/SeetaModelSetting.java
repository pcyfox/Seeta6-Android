package com.seeta.sdk;

public class SeetaModelSetting {
    public SeetaDevice device;
    public int id;
    public String[] model;

    public SeetaModelSetting(int id, String[] models, SeetaDevice dev) {
        this.id = id;
        this.device = dev;
        this.model = new String[models.length];
        System.arraycopy(models, 0, this.model, 0, models.length);
    }

    public SeetaModelSetting(String[] models) {
        this(0, models, SeetaDevice.SEETA_DEVICE_AUTO);
    }

}
