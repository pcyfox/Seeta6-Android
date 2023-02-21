package com.df.lib_seete6;

import com.seeta.sdk.FaceAntiSpoofing;
import com.seeta.sdk.SeetaImageData;

public interface ExtractFaceResultInterceptor {

    boolean onExtractFeats(float[] feats, FaceAntiSpoofing.Status status);
}
