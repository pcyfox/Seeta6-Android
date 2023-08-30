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
import com.seeta.sdk.FaceDetector;
import com.seeta.sdk.FaceLandmarker;
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

import java.util.Arrays;
import java.util.Map;


public class PresenterImpl implements SeetaContract.Presenter {

    private static final String TAG = "PresenterImpl";
    private SeetaContract.ViewInterface mView;

    private float[] feats = new float[1024];
    private SeetaPointF[] points = new SeetaPointF[5];

    private boolean needFaceRegister;
    private String registeredName;


    private String takePicPath = Environment.getExternalStorageDirectory().getAbsolutePath();
    private String takePciName;

    private SeetaImageData tempImageData;
    private Mat tempMatYUV;

    private volatile boolean isNeedDestroy = false;
    private volatile boolean isDetecting = false;
    private volatile boolean isDestroyed = false;
    private volatile boolean isSearchingFace = false;

    private int lastRotation;
    private ExtractFaceResultInterceptor interceptor;

    private HandlerThread mFaceTrackThread;
    private HandlerThread mFasThread;

    private Handler mFaceTrackingHandler;
    private Handler mFasHandler;

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

    {
        init();
    }

    private void init() {
        mFaceTrackThread = new HandlerThread("FaceTrackThread", Process.THREAD_PRIORITY_MORE_FAVORABLE);
        mFasThread = new HandlerThread("FasThread", Process.THREAD_PRIORITY_MORE_FAVORABLE);
        mFaceTrackThread.start();
        mFasThread.start();
        initHandler();
    }

    private boolean isDestroy() {
        return isNeedDestroy || isDestroyed || mView == null || !EnginHelper.getInstance().isInitOver();
    }

    private void initHandler() {
        mFaceTrackingHandler = new Handler(mFaceTrackThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                if (EnginHelper.getInstance().isRegistering()) {
                    Log.e(TAG, "detect() called fail,engin is  isRegistering!");
                    return;
                }

                if (isDestroy()) {
                    checkState();
                    return;
                }
                isDetecting = true;
                final TrackingInfo trackingInfo = (TrackingInfo) msg.obj;
                trackingInfo.matBgr.get(0, 0, tempImageData.data);
                FaceDetector faceDetector = EnginHelper.getInstance().getFaceDetector();
                if (faceDetector == null) return;
                SeetaRect[] faces = faceDetector.Detect(tempImageData);
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
                if (isDestroy()) return;
                if (enginConfig != null && enginConfig.isNeedFaceImage && limitX <= tempImageData.width && limitY <= tempImageData.height) {
                    Mat faceMatBGR = new Mat(trackingInfo.matBgr, trackingInfo.faceRect);
                    Imgproc.resize(faceMatBGR, faceMatBGR, new Size(tempImageData.height >> 1, tempImageData.width >> 1));
                    Mat faceMatBGRA = new Mat();
                    Imgproc.cvtColor(faceMatBGR, faceMatBGRA, Imgproc.COLOR_BGR2RGBA);
                    Bitmap faceBmp = Bitmap.createBitmap(faceMatBGR.width(), faceMatBGR.height(), Bitmap.Config.ARGB_8888);
                    Utils.matToBitmap(faceMatBGRA, faceBmp);
                    mView.drawFaceImage(faceBmp);
                    if (!faceBmp.isRecycled()) {
                        faceBmp.recycle();
                    }
                }
                isDetecting = false;
                if (!isSearchingFace) {
                    mFasHandler.removeMessages(0);
                    mFasHandler.obtainMessage(0, trackingInfo).sendToTarget();
                }
                checkState();
            }
        };

        mFasHandler = new Handler(mFasThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                if (EnginHelper.getInstance().isRegistering()) {
                    Log.e(TAG, "detect() called fail,engin is  isRegistering!");
                    return;
                }

                if (isDestroy() || isDetecting) {
                    checkState();
                    return;
                }

                final TrackingInfo trackingInfo = (TrackingInfo) msg.obj;
                trackingInfo.matBgr.get(0, 0, tempImageData.data);
                SeetaRect faceInfo = trackingInfo.faceInfo;

                if (faceInfo.width == 0 || interceptor != null && !interceptor.onPrepare(faceInfo)) {
                    trackingInfo.release();
                    return;
                }

                //注册人脸
                if (needFaceRegister) {
                    if (EnginHelper.getInstance().startRegister(faceInfo, tempImageData, registeredName)) {
                        final String tip = registeredName + ",注册成功";
                        new Handler(Looper.getMainLooper()).post(() -> {
                            if (mView != null)
                                mView.onRegisterByFrameFaceFinish(true, tip);
                        });
                    } else {
                        final String tip = registeredName + ",注册失败";
                        new Handler(Looper.getMainLooper()).post(() -> {
                            if (mView != null)
                                mView.onRegisterByFrameFaceFinish(false, tip);
                        });
                    }
                    needFaceRegister = false;
                    registeredName = "";
                }


                if (EnginHelper.registerName2feats.isEmpty() && interceptor == null) {
                    trackingInfo.release();
                    return;
                }

                FaceAntiSpoofing.Status faceAntiSpoofingState = null;
                //特征点检测
                if (isDestroy()) return;
                FaceLandmarker landMarker = EnginHelper.getInstance().getFaceLandMarker();
                if (landMarker == null || tempImageData == null || points == null) return;
                isSearchingFace = true;
                landMarker.mark(tempImageData, faceInfo, points);
                FaceRecognizer faceRecognizer = EnginHelper.getInstance().getFaceRecognizer();
                if (faceRecognizer == null) return;
                int fSize = faceRecognizer.GetExtractFeatureSize();
                if (fSize == 0) {
                    trackingInfo.release();
                    isSearchingFace = false;
                    return;
                }

                if (feats.length >= fSize) {
                    Arrays.fill(feats, 0);
                } else {
                    feats = new float[fSize];
                }

                if (isDestroy()) {
                    trackingInfo.release();
                    cancelSearchTaskOnce();
                    isSearchingFace = false;
                    return;
                }
                //特征提取
                if (isDestroy() || feats == null || tempImageData == null || points == null) {
                    isSearchingFace = false;
                    return;
                }
                faceRecognizer.extract(tempImageData, points, feats);
                if (isNeedDestroy || isDestroyed) {
                    trackingInfo.release();
                    cancelSearchTaskOnce();
                    isSearchingFace = false;
                    checkState();
                    return;
                }

                if (interceptor != null) {
                    faceAntiSpoofingState = checkSpoofing(tempImageData, faceInfo, points);
                    if (interceptor.onExtractFeats(feats, faceAntiSpoofingState)) {
                        trackingInfo.release();
                        cancelSearchTaskOnce();
                        isSearchingFace = false;
                        checkState();
                        return;
                    }
                }

                final Target target = findTarget(faceRecognizer, feats);
                if (target == null) {
                    cancelSearchTaskOnce();
                    isSearchingFace = false;
                    checkState();
                    return;
                }

                if (faceAntiSpoofingState == null) {
                    faceAntiSpoofingState = checkSpoofing(tempImageData, faceInfo, points);
                }

                target.setStatus(faceAntiSpoofingState);
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (mView != null) {
                        final Rect faceRect = trackingInfo.faceRect;
                        mView.onDetectFinish(target, trackingInfo.matBgr, faceRect);
                    }
                });

                trackingInfo.release();
                cancelSearchTaskOnce();
                isSearchingFace = false;
                checkState();
            }
        };

    }

    public PresenterImpl(SeetaContract.ViewInterface view) {
        mView = view;
        resume(view);
    }

    public void resume(SeetaContract.ViewInterface view) {
        this.mView = view;
        if (isDestroyed) {
            init();
        }
        isDestroyed = false;
        isSearchingFace = false;
        isDetecting = false;
        isNeedDestroy = false;
    }


    public void setInterceptor(ExtractFaceResultInterceptor interceptor) {
        this.interceptor = interceptor;
    }

    private void cancelSearchTaskOnce() {
        isSearchingFace = false;
        checkState();
    }


    public Target findTarget(FaceRecognizer faceRecognizer, float[] feats) {
        if (EnginHelper.registerName2feats.isEmpty()) {
            return null;
        }
        if (isDestroy()) return null;
        final EnginConfig enginConfig = EnginHelper.getInstance().getEnginConfig();
        //不空进行特征提取，并比对
        for (Map.Entry<String, float[]> entry : EnginHelper.registerName2feats.entrySet()) {
            float sim = faceRecognizer.CalculateSimilarity(feats, entry.getValue());
            if (sim >= enginConfig.faceThresh) {
                return new Target(sim, entry.getKey());
            }
        }
        return null;
    }


    /**
     * 活体检测
     */
    public FaceAntiSpoofing.Status checkSpoofing(SeetaImageData imageData, SeetaRect faceInfo, SeetaPointF[] points) {
        if (isDestroy()) return FaceAntiSpoofing.Status.UNKNOWN;
        FaceAntiSpoofing faceAntiSpoofing = EnginHelper.getInstance().getFaceAntiSpoofing();
        if (faceAntiSpoofing == null || isDestroyed || imageData == null || imageData.data.length == 0 | isDestroy()) {
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
        if (!EnginHelper.getInstance().isInitOver()) {
            Log.e(TAG, "detect() called fail,engin is  not init!");
            return;
        }

        if (EnginHelper.getInstance().isRegistering()) {
            Log.e(TAG, "detect() called fail,engin is  isRegistering!");
            return;
        }

        if (isDestroy() || !mView.isActive()) {
            checkState();
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
            // 0表示90度，1表示180度，2表示270度
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

        if (isNeedDestroy || isDestroyed || mView == null || !mView.isActive()) {
            trackingInfo.release();
            destroy();
            return;
        }

        mFaceTrackingHandler.removeMessages(1);
        mFaceTrackingHandler.obtainMessage(1, trackingInfo).sendToTarget();
    }


    private void checkState() {
        if (isNeedDestroy && !isDetecting && !isSearchingFace) {
            destroy();
        }
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
        isNeedDestroy = true;
        if (isDestroyed) return true;
        Log.d(TAG, "destroy() called isDetecting=" + isDestroyed + ",isSearchingFace=" + isSearchingFace);
        if (isDetecting || isSearchingFace) {
            mFaceTrackThread.quit();
            mFasThread.quit();
            return false;
        }

        isDestroyed = true;
        isNeedDestroy = false;
        mView = null;

        mFaceTrackThread.quit();
        mFasThread.quit();

        if (tempImageData != null) {
            tempImageData.clear();
            tempImageData = null;
        }

        if (tempMatYUV != null) {
            tempMatYUV.release();
            tempMatYUV = null;
        }
        feats = null;
        points = null;
        Log.d(TAG, "--------------destroy() over!--------------");
        return true;
    }

    private boolean isNeedTakePic() {
        return !TextUtils.isEmpty(takePicPath) && !TextUtils.isEmpty(takePciName);
    }
}
