package com.df.lib_seete6;


import android.graphics.Bitmap;
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


public class PresenterImpl implements Contract.Presenter {

    private static final String TAG = "PresenterImpl";
    private Contract.View mView;
    private final EnginConfig enginConfig = EnginHelper.getInstance().getEnginConfig();

    private boolean needFaceRegister;
    private String registeredName;

    private String takePicPath;
    private String takePciName;

    private SeetaImageData tempImageData;
    private Mat tempMatNv21;

    public static class TrackingInfo {
        public Mat matBgr;
        public Mat matGray;
        public SeetaRect faceInfo = new SeetaRect();
        public Rect faceRect = new Rect();
        public long birthTime;
        public long lastProcessTime;
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


    public PresenterImpl(Contract.View view) {
        mView = view;
        mView.setPresenter(this);
    }


    private final Handler mFaceTrackingHandler = new Handler(mFaceTrackThread.getLooper()) {
        @Override
        public void handleMessage(Message msg) {
            if (mView == null) {
                return;
            }
            final TrackingInfo trackingInfo = (TrackingInfo) msg.obj;
            trackingInfo.matBgr.get(0, 0, tempImageData.data);
            SeetaRect[] faces = EnginHelper.getInstance().getFaceDetector().Detect(tempImageData);
            if (faces.length == 0) {
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

            mView.drawFaceRect(trackingInfo.faceRect);

            int limitX = trackingInfo.faceRect.x + trackingInfo.faceRect.width;
            int limitY = trackingInfo.faceRect.y + trackingInfo.faceRect.height;

            if (enginConfig.isNeedFaceImage && limitX < tempImageData.width && limitY < tempImageData.height) {
                Mat faceMatBGR = new Mat(trackingInfo.matBgr, trackingInfo.faceRect);
                Imgproc.resize(faceMatBGR, faceMatBGR, new Size(tempImageData.height / 2, tempImageData.width / 2));
                Mat faceMatBGRA = new Mat();
                Imgproc.cvtColor(faceMatBGR, faceMatBGRA, Imgproc.COLOR_BGR2RGBA);
                Bitmap faceBmp = Bitmap.createBitmap(faceMatBGR.width(), faceMatBGR.height(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(faceMatBGRA, faceBmp);
                mView.drawFaceImage(faceBmp);
            }

            mFasHandler.removeMessages(0);
            mFasHandler.obtainMessage(0, trackingInfo).sendToTarget();
        }
    };

    private final Handler mFasHandler = new Handler(mFasThread.getLooper()) {

        @Override
        public void handleMessage(Message msg) {
            if (mView == null) {
                return;
            }
            final TrackingInfo trackingInfo = (TrackingInfo) msg.obj;
            trackingInfo.matGray = new Mat();
            final Rect faceRect = trackingInfo.faceRect;
            trackingInfo.matBgr.get(0, 0, tempImageData.data);
            String targetName = "unknown";
            //注册人脸
            SeetaRect faceInfo = trackingInfo.faceInfo;
            if (faceInfo.width == 0) {
                return;
            }
            if (needFaceRegister) {
                if (EnginHelper.getInstance().startRegister(faceInfo, tempImageData, registeredName)) {
                    final String tip = registeredName + ",注册成功";
                    new Handler(Looper.getMainLooper()).post(() -> mView.onFaceRegisterFinish(true, tip));
                } else {
                    final String tip = registeredName + ",注册失败";
                    new Handler(Looper.getMainLooper()).post(() -> mView.onFaceRegisterFinish(false, tip));
                }
                needFaceRegister = false;
                registeredName = "";
            }

            //进行人脸识别
            float maxSimilarity = 0.0f;
            FaceAntiSpoofing.Status faceAntiSpoofingState = FaceAntiSpoofing.Status.UNKNOWN;//初始状态
            //特征点检测
            SeetaPointF[] points = new SeetaPointF[5];
            EnginHelper.getInstance().getFaceLandMarker().mark(tempImageData, faceInfo, points);
            //特征提取
            if (!EnginHelper.registerName2feats.isEmpty()) {//不空进行特征提取，并比对
                FaceRecognizer faceRecognizer = EnginHelper.getInstance().getFaceRecognizer();
                float[] feats = new float[faceRecognizer.GetExtractFeatureSize()];
                faceRecognizer.Extract(tempImageData, points, feats);
                for (Map.Entry<String, float[]> entry : EnginHelper.registerName2feats.entrySet()) {
                    float sim = faceRecognizer.CalculateSimilarity(feats, entry.getValue());
                    if (sim > maxSimilarity && sim > enginConfig.faceThresh) {
                        maxSimilarity = sim;
                        targetName = entry.getKey();
                        faceAntiSpoofingState = checkSpoofing(tempImageData, faceInfo, points);
                    }
                }
            }
            final String pickedName = targetName;
            final float similarity = maxSimilarity;
            final FaceAntiSpoofing.Status status = faceAntiSpoofingState;
            new Handler(Looper.getMainLooper()).post(() -> {
                if (mView != null) {
                    mView.onDetectFinish(status, similarity, pickedName, trackingInfo.matBgr, faceRect);
                }
            });
        }
    };

    /**
     * 活体检测
     */
    public FaceAntiSpoofing.Status checkSpoofing(SeetaImageData imageData, SeetaRect faceInfo, SeetaPointF[] points) {
        FaceAntiSpoofing faceAntiSpoofing = EnginHelper.getInstance().getFaceAntiSpoofing();
        if (faceAntiSpoofing == null) {
            return FaceAntiSpoofing.Status.UNKNOWN;
        }
        return faceAntiSpoofing.Predict(imageData, faceInfo, points);
    }

    private void initTempData(int width, int height, int rotation) {
        if (tempMatNv21 == null) {
            tempMatNv21 = new Mat(height + height / 2, width, CvType.CV_8UC1);
        }

        if (tempImageData == null || lastRotation != rotation) {
            if (rotation >= 90) {
                tempImageData = new SeetaImageData(height, width, 3);
            } else {
                tempImageData = new SeetaImageData(width, height, 3);
            }
        }
    }


    @Override
    public void detect(byte[] data, int width, int height, int rotation) {
        if (mView == null) {
            return;
        }

        if (!EnginHelper.getInstance().isInitOver()) {
            Log.d(TAG, "detect() called fail,engin is  not init!");
            return;
        }

        initTempData(width, height, rotation);

        tempMatNv21.put(0, 0, data);
        TrackingInfo trackingInfo = new TrackingInfo();
        trackingInfo.matBgr = new Mat(width, height, CvType.CV_8UC3);
        trackingInfo.birthTime = System.currentTimeMillis();
        trackingInfo.lastProcessTime = System.currentTimeMillis();
        Imgproc.cvtColor(tempMatNv21, trackingInfo.matBgr, Imgproc.COLOR_YUV2BGR_NV21);

        if (rotation > 0) {
            // 0表示90度，1表示180度，2表示270度
            int r = (rotation - 90) / 90;
            Core.rotate(trackingInfo.matBgr, trackingInfo.matBgr, r);
        }
//        Core.transpose(trackingInfo.matBgr, trackingInfo.matBgr);
//        Core.flip(trackingInfo.matBgr, trackingInfo.matBgr, 0);

        Core.flip(trackingInfo.matBgr, trackingInfo.matBgr, 1);

        if (isNeedTakePic()) {
            SeetaUtils.saveImage(trackingInfo.matBgr, takePicPath, takePciName);
            takePicPath = takePciName = null;
        }
        mFaceTrackingHandler.removeMessages(1);
        mFaceTrackingHandler.obtainMessage(1, trackingInfo).sendToTarget();
    }

    @Override
    public void takePicture(String path, String name) {
        this.takePciName = name;
        this.takePicPath = path;
    }

    @Override
    public void startRegisterFrame(boolean needFaceRegister, String registeredName) {
        this.needFaceRegister = needFaceRegister;
        this.registeredName = registeredName;
    }


    @Override
    public void destroy() {
        if (tempMatNv21 != null) {
            tempMatNv21.release();
            tempMatNv21 = null;
        }
        if (tempImageData != null) {
            tempImageData.data = null;
            tempImageData = null;
        }
        mFaceTrackThread.quitSafely();
        mFasThread.quitSafely();
        mView = null;

    }

    private boolean isNeedTakePic() {
        return !TextUtils.isEmpty(takePicPath) && !TextUtils.isEmpty(takePciName);
    }
}
