package com.df.lib_seete6;

import com.seeta.sdk.FaceAntiSpoofing;

public class Target {
    private float similarity;
    private String key;
    private FaceAntiSpoofing.Status status;

    public Target(float similarity, String key) {
        this.similarity = similarity;
        this.key = key;
    }

    public float getSimilarity() {
        return similarity;
    }

    public void setSimilarity(float similarity) {
        this.similarity = similarity;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public FaceAntiSpoofing.Status getStatus() {
        return status;
    }

    public void setStatus(FaceAntiSpoofing.Status status) {
        this.status = status;
    }


    @Override
    public String toString() {
        return "Target{" +
                "similarity=" + similarity +
                ", key='" + key + '\'' +
                ", status=" + status +
                '}';
    }
}
