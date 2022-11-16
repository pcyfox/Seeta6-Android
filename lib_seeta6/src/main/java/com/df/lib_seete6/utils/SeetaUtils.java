package com.df.lib_seete6.utils;

import android.graphics.Bitmap;

import com.seeta.sdk.SeetaImageData;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class SeetaUtils {

    public static void saveImage(Mat bgr, String path, String imageName) {
        Mat rgba = bgr.clone();
        Imgproc.cvtColor(rgba, rgba, Imgproc.COLOR_BGR2RGBA);
        Bitmap mBitmap = null;
        mBitmap = Bitmap.createBitmap(rgba.cols(), rgba.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(rgba, mBitmap);
        File f = new File(path, imageName);
        if (f.exists()) {
            f.delete();
        }
        try {
            FileOutputStream out = new FileOutputStream(f);
            mBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 转换生成SeetaImageData
     *
     * @param bitmap
     * @return
     */
    public static SeetaImageData convertToSeetaImageData(Bitmap bitmap) {
        Bitmap bmp_src = bitmap.copy(Bitmap.Config.ARGB_8888, true); // true is RGBA
        //SeetaImageData大小与原图像一致，但是通道数为3个通道即BGR
        SeetaImageData imageData = new SeetaImageData(bmp_src.getWidth(), bmp_src.getHeight(), 3);
        imageData.data = getPixelsBGR(bmp_src);
        return imageData;
    }

    /**
     * 提取图像中的BGR像素
     *
     * @param image
     * @return
     */
    public static byte[] getPixelsBGR(Bitmap image) {
        // calculate how many bytes our image consists of
        int bytes = image.getByteCount();
        ByteBuffer buffer = ByteBuffer.allocate(bytes); // Create a new buffer
        image.copyPixelsToBuffer(buffer); // Move the byte data to the buffer
        byte[] temp = buffer.array(); // Get the underlying array containing the data.
        byte[] pixels = new byte[(temp.length / 4) * 3]; // Allocate for BGR
        // Copy pixels into place
        for (int i = 0; i < temp.length / 4; i++) {
            pixels[i * 3] = temp[i * 4 + 2];        //B
            pixels[i * 3 + 1] = temp[i * 4 + 1];    //G
            pixels[i * 3 + 2] = temp[i * 4];       //R
        }
        return pixels;
    }
}
