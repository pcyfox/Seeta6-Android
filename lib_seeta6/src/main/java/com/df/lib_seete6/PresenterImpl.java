package com.df.lib_seete6;


import android.graphics.Bitmap;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.text.TextUtils;
import android.util.Log;

import com.df.lib_seete6.config.EnginConfig;
import com.df.lib_seete6.utils.EnginHelper;
import com.df.lib_seete6.utils.SeetaUtils;
import com.seeta.sdk.FaceAntiSpoofing;
import com.seeta.sdk.FaceRecognizer;
import com.seeta.sdk.SeetaImageData;
import com.seeta.sdk.SeetaPointF;
import com.seeta.sdk.SeetaRect;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.Map;


public class PresenterImpl implements SeetaContract.Presenter {

    private static final String TAG = "PresenterImpl";
    private SeetaContract.ViewInterface mView;

    private boolean needFaceRegister;
    private String registeredName;

    private String takePicPath = Environment.getExternalStorageDirectory().getAbsolutePath();
    private String takePciName;

    private SeetaImageData tempImageData;
    private Mat tempMatYUV;
    private volatile boolean isDetecting = false;
    private volatile boolean isDestroyed = false;
    private volatile boolean isSearchingFace = false;

    public static class TrackingInfo {
        public Mat matBgr;
        public Mat matGray;
        public SeetaRect faceInfo = new SeetaRect();
        public Rect faceRect = new Rect();
        public long birthTime;
        public long lastProcessTime;


        public void release() {
            if (matBgr != null) {
                matBgr.release();
                matBgr = null;
            }
            if (matGray != null) {
                matGray.release();
                matGray = null;
            }

        }
    }

    private final HandlerThread mFaceTrackThread;
    private final HandlerThread mFasThread;

    private int lastRotation;

    {
        mFaceTrackThread = new HandlerThread("FaceTrackThread", Process.THREAD_PRIORITY_MORE_FAVORABLE);
        mFasThread = new HandlerThread("FasThread", Process.THREAD_PRIORITY_MORE_FAVORABLE);
        mFaceTrackThread.start();
        mFasThread.start();
    }


    public PresenterImpl(SeetaContract.ViewInterface view) {
        mView = view;
    }


    private final Handler mFaceTrackingHandler = new Handler(mFaceTrackThread.getLooper()) {
        @Override
        public void handleMessage(Message msg) {
            if (isDestroyed || mView == null) {
                return;
            }
            isDetecting = true;
            final TrackingInfo trackingInfo = (TrackingInfo) msg.obj;
            trackingInfo.matBgr.get(0, 0, tempImageData.data);
            SeetaRect[] faces = EnginHelper.getInstance().getFaceDetector().Detect(tempImageData);

            if (faces.length == 0) {
                if (mView != null) {
                    mView.drawFaceRect(null);
                }
                trackingInfo.release();
                isDetecting = false;
                return;
            }
            int maxIndex = 0;
            double maxWidth = 0;
            for (int i = 0; i < faces.length; ++i) {
                if (faces[i].width > maxWidth) {
                    maxIndex = i;
                    maxWidth = faces[i].width;
                }
            }

            trackingInfo.faceInfo = faces[maxIndex];
            trackingInfo.faceRect.x = faces[maxIndex].x;
            trackingInfo.faceRect.y = faces[maxIndex].y;
            trackingInfo.faceRect.width = faces[maxIndex].width;
            trackingInfo.faceRect.height = faces[maxIndex].height;
            trackingInfo.lastProcessTime = System.currentTimeMillis();
            //draw face rect
            mView.drawFaceRect(trackingInfo.faceRect);

            int limitX = trackingInfo.faceRect.x + trackingInfo.faceRect.width;
            int limitY = trackingInfo.faceRect.y + trackingInfo.faceRect.height;

            final EnginConfig enginConfig = EnginHelper.getInstance().getEnginConfig();
            if (enginConfig != null && enginConfig.isNeedFaceImage && limitX < tempImageData.width && limitY < tempImageData.height) {
                Mat faceMatBGR = new Mat(trackingInfo.matBgr, trackingInfo.faceRect);
                Imgproc.resize(faceMatBGR, faceMatBGR, new Size(tempImageData.height / 2, tempImageData.width / 2));
                Mat faceMatBGRA = new Mat();
                Imgproc.cvtColor(faceMatBGR, faceMatBGRA, Imgproc.COLOR_BGR2RGBA);
                Bitmap faceBmp = Bitmap.createBitmap(faceMatBGR.width(), faceMatBGR.height(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(faceMatBGRA, faceBmp);
                mView.drawFaceImage(faceBmp);
            }
            if (!isSearchingFace) {
                mFasHandler.removeMessages(0);
                mFasHandler.obtainMessage(0, trackingInfo).sendToTarget();
            }
            isDetecting = false;
        }
    };

    private final Handler mFasHandler = new Handler(mFasThread.getLooper()) {

        @Override
        public void handleMessage(Message msg) {
            if (isDetecting || isDestroyed || mView == null) {
                return;
            }

            final TrackingInfo trackingInfo = (TrackingInfo) msg.obj;
            trackingInfo.matBgr.get(0, 0, tempImageData.data);
            String targetName = "unknown";
            SeetaRect faceInfo = trackingInfo.faceInfo;
            if (faceInfo.width == 0) {

                return;
            }

            //????????????
            if (needFaceRegister) {
                if (EnginHelper.getInstance().startRegister(faceInfo, tempImageData, registeredName)) {
                    final String tip = registeredName + ",????????????";
                    new Handler(Looper.getMainLooper()).post(() -> mView.onRegisterByFrameFaceFinish(true, tip));
                } else {
                    final String tip = registeredName + ",????????????";
                    new Handler(Looper.getMainLooper()).post(() -> mView.onRegisterByFrameFaceFinish(false, tip));
                }
                needFaceRegister = false;
                registeredName = "";
            }

            if (EnginHelper.registerName2feats.isEmpty()) {
                return;
            }
            isSearchingFace = true;
            float maxSimilarity = 0.0f;
            FaceAntiSpoofing.Status faceAntiSpoofingState = FaceAntiSpoofing.Status.UNKNOWN;//????????????
            SeetaPointF[] points = new SeetaPointF[5];
            //???????????????
            EnginHelper.getInstance().getFaceLandMarker().mark(tempImageData, faceInfo, points);
            FaceRecognizer faceRecognizer = EnginHelper.getInstance().getFaceRecognizer();
            int fSize = faceRecognizer.GetExtractFeatureSize();
            if (fSize == 0) {
                return;
            }
            isSearchingFace = true;
            float[] feats = new float[fSize];
            //????????????
            faceRecognizer.Extract(tempImageData, points, feats);
            final EnginConfig enginConfig = EnginHelper.getInstance().getEnginConfig();
            //????????????????????????????????????
            for (Map.Entry<String, float[]> entry : EnginHelper.registerName2feats.entrySet()) {
                float sim = faceRecognizer.CalculateSimilarity(feats, entry.getValue());
                if (sim >= enginConfig.faceThresh) {
                    maxSimilarity = sim;
                    targetName = entry.getKey();
                    faceAntiSpoofingState = checkSpoofing(tempImageData, faceInfo, points);
                }
            }

            final String pickedName = targetName;
            final float similarity = maxSimilarity;
            final FaceAntiSpoofing.Status status = faceAntiSpoofingState;
            new Handler(Looper.getMainLooper()).post(() -> {
                if (mView != null) {
                    final Rect faceRect = trackingInfo.faceRect;
                    mView.onDetectFinish(status, similarity, pickedName, trackingInfo.matBgr, faceRect);
                }
                trackingInfo.release();
            });

            isSearchingFace = false;
        }
    };

    /**
     * ????????????
     */
    public FaceAntiSpoofing.Status checkSpoofing(SeetaImageData imageData, SeetaRect faceInfo, SeetaPointF[] points) {
        FaceAntiSpoofing faceAntiSpoofing = EnginHelper.getInstance().getFaceAntiSpoofing();
        if (faceAntiSpoofing == null || isDestroyed || imageData == null || imageData.data.length == 0) {
            return FaceAntiSpoofing.Status.UNKNOWN;
        }
        return faceAntiSpoofing.Predict(imageData, faceInfo, points);
    }

    private void initTempData(int width, int height, int rotation) {
        if (tempMatYUV == null) {
            tempMatYUV = new Mat(height + height / 2, width, CvType.CV_8UC1);
        }
        if (tempImageData == null || lastRotation != rotation) {
            if (rotation >= 90) {
                tempImageData = new SeetaImageData(height, width, 3);
            } else {
                tempImageData = new SeetaImageData(width, height, 3);
            }
            lastRotation = rotation;
        }
    }


    @Override
    public void detect(byte[] data, int width, int height, int rotation) {
        if (isDestroyed || mView == null || !mView.isActive()) {
            return;
        }
        if (!EnginHelper.getInstance().isInitOver()) {
            Log.d(TAG, "detect() called fail,engin is  not init!");
            return;
        }
        initTempData(width, height, rotation);

        tempMatYUV.put(0, 0, data);
        TrackingInfo trackingInfo = new TrackingInfo();
        trackingInfo.matBgr = new Mat(width, height, CvType.CV_8UC3);
        trackingInfo.birthTime = System.currentTimeMillis();
        trackingInfo.lastProcessTime = System.currentTimeMillis();
        Imgproc.cvtColor(tempMatYUV, trackingInfo.matBgr, Imgproc.COLOR_YUV2BGR_NV21);

        if (rotation > 0) {
            // 0??????90??????1??????180??????2??????270???
            int r = (rotation - 90) / 90;
            Core.rotate(trackingInfo.matBgr, trackingInfo.matBgr, r);
        }

        EnginConfig config = EnginHelper.getInstance().getEnginConfig();
        if (config.isNeedFlipUpToDown) {
            Core.flip(trackingInfo.matBgr, trackingInfo.matBgr, 0);
        }

        if (config.isNeedFlipLeftToRight) {
            Core.flip(trackingInfo.matBgr, trackingInfo.matBgr, 1);
        }

        if (isNeedTakePic()) {
            SeetaUtils.saveImage(trackingInfo.matBgr, takePicPath, takePciName);
            mView.onTakePictureFinish(takePicPath, takePciName);
            takePicPath = null;
            takePciName = null;
        }

        mFaceTrackingHandler.removeMessages(1);
        mFaceTrackingHandler.obtainMessage(1, trackingInfo).sendToTarget();
    }


    public void takePicture(String name) {
        this.takePciName = name;
    }

    @Override
    public void takePicture(String path, String name) {
        this.takePicPath = path;
        this.takePciName = name;
    }

    @Override
    public void startRegisterFrame(boolean needFaceRegister, String registeredName) {
        this.needFaceRegister = needFaceRegister;
        this.registeredName = registeredName;
    }


    @Override
    public boolean destroy() {
        if (isDetecting || isSearchingFace) {
            return false;
        }

        mView = null;
        isDestroyed = true;

        mFaceTrackThread.quitSafely();
        mFasThread.quitSafely();

        if (tempImageData != null) {
            tempImageData.data = null;
            tempImageData = null;
        }

        if (tempMatYUV != null) {
            tempMatYUV.release();
            tempMatYUV = null;
        }
        return true;
    }

    private boolean isNeedTakePic() {
        return !TextUtils.isEmpty(takePicPath) && !TextUtils.isEmpty(takePciName);
    }
}
