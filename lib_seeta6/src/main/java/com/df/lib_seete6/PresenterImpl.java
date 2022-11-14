package com.df.lib_seete6;


import android.graphics.Bitmap;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.util.Log;

import com.df.lib_seete6.config.EnginConfig;
import com.df.lib_seete6.utils.EnginHelper;
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

import java.util.HashMap;
import java.util.Map;


public class PresenterImpl implements Contract.Presenter {

    private static final String TAG = "PresenterImpl";
    private Contract.View mView;

    private EnginConfig enginConfig = new EnginConfig();

    private final int WIDTH = enginConfig.IMAGE_WIDTH;
    private final int HEIGHT = enginConfig.IMAGE_HEIGHT;

    public SeetaImageData imageData = new SeetaImageData(WIDTH, HEIGHT, 3);

    private boolean needFaceRegister;
    private String registeredName;

    public static class TrackingInfo {
        public Mat matBgr;
        public Mat matGray;
        public SeetaRect faceInfo = new SeetaRect();
        public Rect faceRect = new Rect();
        public long birthTime;
        public long lastProcessTime;
        public static Map<String, float[]> name2feats = new HashMap<>();
    }

    private final HandlerThread mFaceTrackThread;
    private final HandlerThread mFasThread;

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

    public void setEnginConfig(EnginConfig enginConfig) {
        this.enginConfig = enginConfig;
    }

    private final Handler mFaceTrackingHandler = new Handler(mFaceTrackThread.getLooper()) {
        @Override
        public void handleMessage(Message msg) {
            final TrackingInfo trackingInfo = (TrackingInfo) msg.obj;
            trackingInfo.matBgr.get(0, 0, imageData.data);
            SeetaRect[] faces = EnginHelper.getInstance().getFaceDetector().Detect(imageData);
            if (faces.length == 0) {
                mView.drawFaceRect(null);
                return;
            }

            trackingInfo.faceInfo.x = 0;
            trackingInfo.faceInfo.y = 0;
            trackingInfo.faceInfo.width = 0;
            trackingInfo.faceInfo.height = 0;

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
            if (limitX < WIDTH && limitY < HEIGHT) {
                Mat faceMatBGR = new Mat(trackingInfo.matBgr, trackingInfo.faceRect);
                Imgproc.resize(faceMatBGR, faceMatBGR, new Size(200, 240));
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
            final TrackingInfo trackingInfo = (TrackingInfo) msg.obj;
            trackingInfo.matGray = new Mat();
            final Rect faceRect = trackingInfo.faceRect;
            trackingInfo.matBgr.get(0, 0, imageData.data);
            String targetName = "unknown";
            //注册人脸

            SeetaRect faceInfo = trackingInfo.faceInfo;
            if (needFaceRegister) {
                startRegister(faceInfo);
            }
            //进行人脸识别
            float maxSimilarity = 0.0f;
            FaceAntiSpoofing.Status faceAntiSpoofingState = FaceAntiSpoofing.Status.DETECTING;//初始状态
            if (faceInfo.width != 0) {
                //特征点检测
                SeetaPointF[] points = new SeetaPointF[5];
                EnginHelper.getInstance().getFaceLandMarker().mark(imageData, faceInfo, points);
                //特征提取
                if (!TrackingInfo.name2feats.isEmpty()) {//不空进行特征提取，并比对
                    FaceRecognizer faceRecognizer = EnginHelper.getInstance().getFaceRecognizer();
                    float[] feats = new float[faceRecognizer.GetExtractFeatureSize()];
                    faceRecognizer.Extract(imageData, points, feats);
                    for (String name : TrackingInfo.name2feats.keySet()) {
                        float sim = faceRecognizer.CalculateSimilarity(feats, TrackingInfo.name2feats.get(name));
                        if (sim > maxSimilarity && sim > enginConfig.FACE_THRESH) {
                            maxSimilarity = sim;
                            targetName = name;
                            //活体检测
                            FaceAntiSpoofing faceAntiSpoofing = EnginHelper.getInstance().getFaceAntiSpoofing();
                            if (faceAntiSpoofing != null) {
                                faceAntiSpoofingState = faceAntiSpoofing.Predict(imageData, faceInfo, points);
                            }
                        }
                    }
                }
            }

            final String pickedName = targetName;
            final float similarity = maxSimilarity;
            final FaceAntiSpoofing.Status status = faceAntiSpoofingState;
            new Handler(Looper.getMainLooper()).post(() -> mView.onDetectFinish(status, similarity, pickedName, trackingInfo.matBgr, faceRect));
        }
    };


    private boolean startRegister(SeetaRect faceInfo) {
        boolean canRegister = true;
        FaceRecognizer faceRecognizer = EnginHelper.getInstance().getFaceRecognizer();
        float[] feats = new float[faceRecognizer.GetExtractFeatureSize()];
        if (faceInfo.width == 0) {
            return false;
        }
        //特征点检测
        SeetaPointF[] points = new SeetaPointF[5];
        EnginHelper.getInstance().getFaceLandMarker().mark(imageData, faceInfo, points);
        //特征提取
        faceRecognizer.Extract(imageData, points, feats);
        if ("".equals(registeredName)) {
            canRegister = false;
            final String tip = "注册名称不能为空";
            new Handler(Looper.getMainLooper()).post(() -> mView.showSimpleTip(tip));
        }
        for (String key : TrackingInfo.name2feats.keySet()) {
            if (key.equals(registeredName)) {
                needFaceRegister = false;
                canRegister = false;
                final String tip = registeredName + ",已经注册";
                new Handler(Looper.getMainLooper()).post(() -> mView.showSimpleTip(tip));
            }
        }
        //进行人脸的注册
        if (canRegister) {
            needFaceRegister = false;
            TrackingInfo.name2feats.put(registeredName, feats);
            final String tip = registeredName + ",已经注册成功";
            new Handler(Looper.getMainLooper()).post(() -> mView.FaceRegister(tip));
        }
        return canRegister;
    }

    @Override
    public void detect(byte[] data, int width, int height, int rotation) {
        if (!EnginHelper.getInstance().isInitOver()) {
            Log.d(TAG, "detect() called fail,engin is  not init!");
            return;
        }
        TrackingInfo trackingInfo = new TrackingInfo();
        EnginHelper.matNv21.put(0, 0, data);
        trackingInfo.matBgr = new Mat(enginConfig.CAMERA_PREVIEW_HEIGHT, enginConfig.CAMERA_PREVIEW_WIDTH, CvType.CV_8UC3);
        trackingInfo.matGray = new Mat();
        Imgproc.cvtColor(EnginHelper.matNv21, trackingInfo.matBgr, Imgproc.COLOR_YUV2BGR_NV21);

        Core.transpose(trackingInfo.matBgr, trackingInfo.matBgr);
        Core.flip(trackingInfo.matBgr, trackingInfo.matBgr, 0);
        Core.flip(trackingInfo.matBgr, trackingInfo.matBgr, 1);

        Imgproc.cvtColor(trackingInfo.matBgr, trackingInfo.matGray, Imgproc.COLOR_BGR2GRAY);
        trackingInfo.birthTime = System.currentTimeMillis();
        trackingInfo.lastProcessTime = System.currentTimeMillis();

        mFaceTrackingHandler.removeMessages(1);
        mFaceTrackingHandler.obtainMessage(1, trackingInfo).sendToTarget();
    }

    @Override
    public void startRegister(boolean needFaceRegister, String registeredName) {
        this.needFaceRegister = needFaceRegister;
        this.registeredName = registeredName;
    }


    @Override
    public void destroy() {
        mFaceTrackThread.quitSafely();
        mFasThread.quitSafely();
        mView = null;
    }
}
