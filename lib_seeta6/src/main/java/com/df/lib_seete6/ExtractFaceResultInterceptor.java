package com.df.lib_seete6;

import com.seeta.sdk.FaceAntiSpoofing;
import com.seeta.sdk.SeetaImageData;
import com.seeta.sdk.SeetaRect;

public interface ExtractFaceResultInterceptor {

    boolean onPrepare(SeetaRect rect);

    boolean onExtractFeats(float[] feats, FaceAntiSpoofing.Status status);
}
