package com.seetatech.seetaverify.mvp;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.text.TextUtils;
import android.util.Log;

import com.df.lib_seete6.config.AppConfig;
import com.df.lib_seete6.utils.FileUtils;
import com.seeta.sdk.FaceDetector;
import com.seeta.sdk.FaceLandmarker;
import com.seeta.sdk.FaceRecognizer;
import com.seeta.sdk.Property;
import com.seeta.sdk.SeetaDevice;
import com.seeta.sdk.SeetaImageData;
import com.seeta.sdk.SeetaModelSetting;
import com.seeta.sdk.SeetaPointF;
import com.seeta.sdk.SeetaRect;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


public class PresenterImpl implements VerificationContract.Presenter {

    static {
        System.loadLibrary("opencv_java3");
    }

    private static final String TAG = "PresenterImpl";
    private final VerificationContract.View mView;
    public static FaceDetector faceDetector = null;

    private static final int WIDTH = AppConfig.IMAGE_WIDTH;
    private static final int HEIGHT = AppConfig.IMAGE_HEIGHT;
    public SeetaImageData imageData = new SeetaImageData(WIDTH, HEIGHT, 3);

    private float thresh = 0.70f;

    public static FaceLandmarker faceLandmarker = null;
    public static FaceRecognizer faceRecognizer = null;

    public static class TrackingInfo {
        public Mat matBgr;
        public Mat matGray;
        public SeetaRect faceInfo = new SeetaRect();
        public Rect faceRect = new Rect();
        public long birthTime;
        public long lastProccessTime;
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

    private final Mat matNv21 = new Mat(AppConfig.CAMERA_PREVIEW_HEIGHT + AppConfig.CAMERA_PREVIEW_HEIGHT / 2, AppConfig.CAMERA_PREVIEW_WIDTH, CvType.CV_8UC1);

    public PresenterImpl(Context context, VerificationContract.View view) {
        mView = view;
        mView.setPresenter(this);

        File cacheDir = getInternalCacheDirectory(context, null);
        String modelPath = cacheDir.getAbsolutePath();
        Log.d("cacheDir", "" + modelPath);

        String fdModel = "face_detector.csta";
        String pdModel = "face_landmarker_pts5.csta";
        String frModel = "face_recognizer.csta";

        if (!isExists(modelPath, fdModel)) {
            File fdFile = new File(cacheDir + "/" + fdModel);
            FileUtils.copyFromAsset(context, fdModel, fdFile, false);
        }
        if (!isExists(modelPath, pdModel)) {
            File pdFile = new File(cacheDir + "/" + pdModel);
            FileUtils.copyFromAsset(context, pdModel, pdFile, false);
        }
        if (!isExists(modelPath, frModel)) {
            File frFile = new File(cacheDir + "/" + frModel);
            FileUtils.copyFromAsset(context, frModel, frFile, false);
        }

        String rootPath = cacheDir + "/";
        try {
            if (faceDetector == null || faceLandmarker == null || faceRecognizer == null) {
                faceDetector = new FaceDetector(new SeetaModelSetting(0, new String[]{rootPath + fdModel}, SeetaDevice.SEETA_DEVICE_AUTO));
                faceLandmarker = new FaceLandmarker(new SeetaModelSetting(0, new String[]{rootPath + pdModel}, SeetaDevice.SEETA_DEVICE_AUTO));
                faceRecognizer = new FaceRecognizer(new SeetaModelSetting(0, new String[]{rootPath + frModel}, SeetaDevice.SEETA_DEVICE_AUTO));
            }
            faceDetector.set(Property.PROPERTY_MIN_FACE_SIZE, 80);
        } catch (Exception e) {
            Log.e(TAG, "init exception:" + e);
        }

    }

    public boolean isExists(String path, String modelName) {
        File file = new File(path + "/" + modelName);
        return file.exists();
    }

    public static File getInternalCacheDirectory(Context context, String type) {
        File appCacheDir = null;
        if (TextUtils.isEmpty(type)) {
            appCacheDir = context.getCacheDir();// /data/data/app_package_name/cache
        } else {
            appCacheDir = new File(context.getFilesDir(), type);// /data/data/app_package_name/files/type
        }

        if (!appCacheDir.exists() && !appCacheDir.mkdirs()) {
            Log.e("getInternalDirectory", "getInternalDirectory fail ,the reason is make directory fail !");
        }
        return appCacheDir;
    }

    private final Handler mFaceTrackingHandler = new Handler(mFaceTrackThread.getLooper()) {
        @Override
        public void handleMessage(Message msg) {
            final TrackingInfo trackingInfo = (TrackingInfo) msg.obj;
            trackingInfo.matBgr.get(0, 0, imageData.data);

            SeetaRect[] faces = faceDetector.Detect(imageData);
            trackingInfo.faceInfo.x = (int) 0;
            trackingInfo.faceInfo.y = 0;
            trackingInfo.faceInfo.width = 0;
            trackingInfo.faceInfo.height = 0;

            if (faces.length != 0) {
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
                trackingInfo.lastProccessTime = System.currentTimeMillis();

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

            } else {
                mView.drawFaceRect(null);
            }
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
            MainFragment mainFragment = (MainFragment) mView;
            if (mainFragment.needFaceRegister) {
                boolean canRegister = true;
                float[] feats = new float[faceRecognizer.GetExtractFeatureSize()];
                if (trackingInfo.faceInfo.width != 0) {
                    //特征点检测
                    SeetaPointF[] points = new SeetaPointF[5];
                    faceLandmarker.mark(imageData, trackingInfo.faceInfo, points);
                    //特征提取
                    faceRecognizer.Extract(imageData, points, feats);
                    if ("".equals(mainFragment.registeredName)) {
                        canRegister = false;
                        final String tip = "注册名称不能为空";
                        new Handler(Looper.getMainLooper()).post(() -> mView.showSimpleTip(tip));
                    }
                    for (String key : trackingInfo.name2feats.keySet()) {
                        if (key.equals(mainFragment.registeredName)) {
                            canRegister = false;
                            final String tip = mainFragment.registeredName + "已经注册";
                            new Handler(Looper.getMainLooper()).post(() -> mView.showSimpleTip(tip));
                        }
                    }
                }

                //进行人脸的注册
                if (canRegister) {
                    trackingInfo.name2feats.put(mainFragment.registeredName, feats);
                    final String tip = mainFragment.registeredName + "名称已经注册成功";
                    new Handler(Looper.getMainLooper()).post(() -> mView.FaceRegister(tip));
                }
            }

            //进行人脸识别
            if (trackingInfo.faceInfo.width != 0) {
                //特征点检测
                SeetaPointF[] points = new SeetaPointF[5];
                faceLandmarker.mark(imageData, trackingInfo.faceInfo, points);

                //特征提取
                if (!trackingInfo.name2feats.isEmpty()) {//不空进行特征提取，并比对
                    float[] feats = new float[faceRecognizer.GetExtractFeatureSize()];
                    faceRecognizer.Extract(imageData, points, feats);
                    int galleryNum = trackingInfo.name2feats.size();
                    float maxSimilarity = 0.0f;
                    for (String name : trackingInfo.name2feats.keySet()) {
                        float sim = faceRecognizer.CalculateSimilarity(feats, trackingInfo.name2feats.get(name));
                        if (sim > maxSimilarity && sim > thresh) {
                            maxSimilarity = sim;
                            targetName = name;
                        }
                    }
                }
            }

            final String pickedName = targetName;
            Log.e("recognized name:", pickedName);
            new Handler(Looper.getMainLooper()).post(() -> mView.setName(pickedName, trackingInfo.matBgr, faceRect));
        }
    };

    @Override
    public void detect(byte[] data, int width, int height, int rotation) {
        TrackingInfo trackingInfo = new TrackingInfo();
        matNv21.put(0, 0, data);

        trackingInfo.matBgr = new Mat(AppConfig.CAMERA_PREVIEW_HEIGHT, AppConfig.CAMERA_PREVIEW_WIDTH, CvType.CV_8UC3);
        trackingInfo.matGray = new Mat();
        Imgproc.cvtColor(matNv21, trackingInfo.matBgr, Imgproc.COLOR_YUV2BGR_NV21);

        Core.transpose(trackingInfo.matBgr, trackingInfo.matBgr);
        Core.flip(trackingInfo.matBgr, trackingInfo.matBgr, 0);
        Core.flip(trackingInfo.matBgr, trackingInfo.matBgr, 1);

        Imgproc.cvtColor(trackingInfo.matBgr, trackingInfo.matGray, Imgproc.COLOR_BGR2GRAY);

        trackingInfo.birthTime = System.currentTimeMillis();
        trackingInfo.lastProccessTime = System.currentTimeMillis();

        mFaceTrackingHandler.removeMessages(1);
        mFaceTrackingHandler.obtainMessage(1, trackingInfo).sendToTarget();
    }

    public void saveImage(Mat bgr, String path, String imageName) {
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
            Log.i(TAG, "已经保存");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void destroy() {
        mFaceTrackThread.quitSafely();
        mFasThread.quitSafely();
    }
}
