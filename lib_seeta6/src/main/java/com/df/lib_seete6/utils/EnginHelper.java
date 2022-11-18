package com.df.lib_seete6.utils;

import static com.df.lib_seete6.utils.FileUtils.getInternalCacheDirectory;
import static com.df.lib_seete6.utils.FileUtils.isExists;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.TextUtils;
import android.util.Log;

import com.df.lib_seete6.config.EnginConfig;
import com.seeta.sdk.FaceAntiSpoofing;
import com.seeta.sdk.FaceDetector;
import com.seeta.sdk.FaceLandmarker;
import com.seeta.sdk.FaceRecognizer;
import com.seeta.sdk.Property;
import com.seeta.sdk.SeetaImageData;
import com.seeta.sdk.SeetaModelSetting;
import com.seeta.sdk.SeetaPointF;
import com.seeta.sdk.SeetaRect;

import org.opencv.core.CvType;
import org.opencv.core.Mat;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import kotlin.jvm.Volatile;

public class EnginHelper {
    static {
        System.loadLibrary("opencv_java3");
    }

    private static final String TAG = "EnginHelper";
    private EnginConfig enginConfig;

    private final static EnginHelper instance = new EnginHelper();

    private EnginHelper() {
    }

    public static EnginHelper getInstance() {
        return instance;
    }

    public static Map<String, float[]> registerName2feats = new HashMap<>();

    private FaceDetector faceDetector = null;
    private FaceLandmarker faceLandMarker = null;
    private FaceRecognizer faceRecognizer = null;
    private FaceAntiSpoofing faceAntiSpoofing = null;
    @Volatile
    private boolean isInitOver = false;

    public FaceDetector getFaceDetector() {
        return faceDetector;
    }

    public FaceLandmarker getFaceLandMarker() {
        return faceLandMarker;
    }

    public FaceRecognizer getFaceRecognizer() {
        return faceRecognizer;
    }

    public FaceAntiSpoofing getFaceAntiSpoofing() {
        return faceAntiSpoofing;
    }

    public EnginConfig getEnginConfig() {
        return enginConfig;
    }


    public boolean isInitOver() {
        return isInitOver;
    }


    public boolean initEngine(Context context, EnginConfig enginConfig) {
        Log.d(TAG, "initEngine() called with:  enginConfig = [" + enginConfig + "]");
        this.enginConfig = enginConfig;

        String faceModelPath;
        String fasModelPath;
        String modelRootDir = enginConfig.modelRootDir;

        if (TextUtils.isEmpty(modelRootDir)) {
            faceModelPath = getInternalCacheDirectory(context, "face").getAbsolutePath();
            fasModelPath = getInternalCacheDirectory(context, "fas").getAbsolutePath();
        } else {
            File modelRootFile = new File(modelRootDir);
            if (!FileUtils.makeDirs(modelRootDir)) {
                Log.e(TAG, "initEngine() fail, can't create path:" + modelRootDir);
                return false;
            }
            if (!modelRootFile.isDirectory()) {
                Log.e(TAG, "initEngine() fail, root path is not a dir:" + modelRootDir);
                return false;
            }
            faceModelPath = modelRootDir + "/face";
            fasModelPath = modelRootDir + "/fas";
            FileUtils.makeDirs(faceModelPath);
            FileUtils.makeDirs(fasModelPath);
        }

        String fdModel = "face_detector.csta";
        String pdModel = "face_landmarker_pts5.csta";
        String frModel = "face_recognizer.csta";

        String fasModel1 = "fas_first.csta";
        String fasModel2 = "fas_second.csta";

        if (!isExists(faceModelPath, fdModel)) {
            File fdFile = new File(faceModelPath + "/" + fdModel);
            FileUtils.copyFromAsset(context, fdModel, fdFile, false);
        }
        if (!isExists(faceModelPath, pdModel)) {
            File pdFile = new File(faceModelPath + "/" + pdModel);
            FileUtils.copyFromAsset(context, pdModel, pdFile, false);
        }
        if (!isExists(faceModelPath, frModel)) {
            File frFile = new File(faceModelPath + "/" + frModel);
            FileUtils.copyFromAsset(context, frModel, frFile, false);
        }

        if (!isExists(fasModelPath, fasModel1)) {
            File fasModel1File = new File(fasModelPath + "/" + fasModel1);
            FileUtils.copyFromAsset(context, fasModel1, fasModel1File, false);
        }

        if (!isExists(fasModelPath, fasModel2)) {
            File fasModel2FIle = new File(fasModelPath + "/" + fasModel2);
            FileUtils.copyFromAsset(context, fasModel2, fasModel2FIle, false);
        }

        File faceModelPathFile = new File(faceModelPath);
        String[] faceModels = faceModelPathFile.list();
        if (faceModels == null || faceModels.length == 0) {
            Log.e(TAG, "init fail,can't find face models");
            return false;
        }


        try {
            String rootPath = faceModelPath + "/";
            if (faceDetector == null || faceLandMarker == null || faceRecognizer == null) {
                faceDetector = new FaceDetector(new SeetaModelSetting(new String[]{rootPath + fdModel}));
                faceLandMarker = new FaceLandmarker(new SeetaModelSetting(new String[]{rootPath + pdModel}));
                faceRecognizer = new FaceRecognizer(new SeetaModelSetting(new String[]{rootPath + frModel}));
            }
            faceDetector.set(Property.PROPERTY_MIN_FACE_SIZE, enginConfig.minFaceSize);

            if (faceAntiSpoofing == null && enginConfig.isNeedCheckSpoofing) {
                File fasModelPathFile = new File(fasModelPath);
                String[] fasModels = fasModelPathFile.list();
                if (fasModels == null || fasModels.length == 0) {
                    Log.e(TAG, "init fail,can't find fas models");
                    return false;
                }
                rootPath = fasModelPath + "/";
                faceAntiSpoofing = new FaceAntiSpoofing(new SeetaModelSetting(new String[]{rootPath + fasModel1, rootPath + fasModel2}));
                faceAntiSpoofing.SetThreshold(enginConfig.fasClarity, enginConfig.fasThresh);
            }
            isInitOver = true;
            Log.e(TAG, "-----------init over--------------");
        } catch (Exception e) {
            Log.e(TAG, "init exception:" + e);
        }

        return isInitOver;
    }

    public boolean initEngine(Context context, boolean needCheckSpoofing) {
        EnginConfig config = new EnginConfig();
        config.isNeedCheckSpoofing = needCheckSpoofing;
        return initEngine(context, config);
    }

    public static boolean isRegistered(String registeredName) {
        for (String key : registerName2feats.keySet()) {
            if (key.equals(registeredName)) {
                return true;
            }
        }
        return false;
    }

    public boolean registerFace(String key, File faceFile) {
        if (!isInitOver) {
            return false;
        }
        if (faceFile == null || !faceFile.exists() || !faceFile.canRead() || faceFile.length() == 0 || !faceFile.isFile()) {
            return false;
        }
        Bitmap faceBitmap = BitmapFactory.decodeFile(faceFile.getAbsolutePath());
        return registerFace(key, faceBitmap);
    }

    public boolean registerFace(String key, Bitmap faceBitmap) {
        if (!isInitOver) {
            return false;
        }
        SeetaImageData imageData = SeetaUtils.convertToSeetaImageData(faceBitmap);
        SeetaRect[] rectArray = EnginHelper.getInstance().getFaceDetector().Detect(imageData);
        if (rectArray == null || rectArray.length != 1) {
            return false;
        }
        boolean ret = startRegister(rectArray[0], imageData, key);
        faceBitmap.recycle();
        return ret;
    }

    public boolean startRegister(SeetaRect faceInfo, SeetaImageData imageData, String registeredName) {
        if ("".equals(registeredName)) {
            return false;
        }
        if (EnginHelper.isRegistered(registeredName)) {
            return false;
        }
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
        //进行人脸的注册
        EnginHelper.registerName2feats.put(registeredName, feats);
        return true;
    }


    public void release() {
        isInitOver = false;

        if (faceDetector != null) {
            faceDetector.dispose();
            faceDetector = null;
        }

        if (faceRecognizer != null) {
            faceRecognizer.dispose();
            faceRecognizer = null;
        }

        if (faceLandMarker != null) {
            faceLandMarker.dispose();
            faceLandMarker = null;
        }
        if (faceAntiSpoofing != null) {
            faceAntiSpoofing.dispose();
            faceAntiSpoofing = null;
        }
    }
}
