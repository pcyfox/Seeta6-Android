package com.df.lib_seete6;

import com.seeta.sdk.SeetaImageData;

public interface ExtractFaceResultInterceptor {
    boolean onExtract(SeetaImageData result);
}
