package com.df.lib_seete6.utils;

import static com.df.lib_seete6.utils.FileUtils.getInternalCacheDirectory;
import static com.df.lib_seete6.utils.FileUtils.isExists;

import android.content.Context;
import android.util.Log;

import com.df.lib_seete6.config.EnginConfig;
import com.seeta.sdk.FaceAntiSpoofing;
import com.seeta.sdk.FaceDetector;
import com.seeta.sdk.FaceLandmarker;
import com.seeta.sdk.FaceRecognizer;
import com.seeta.sdk.Property;
import com.seeta.sdk.SeetaModelSetting;

import org.opencv.core.CvType;
import org.opencv.core.Mat;

import java.io.File;

import kotlin.jvm.Volatile;

public class EnginHelper {

    private static final String TAG = "EnginHelper";

    static {
        System.loadLibrary("opencv_java3");
    }

    private EnginHelper() {
    }

    private EnginConfig enginConfig = new EnginConfig();
    private final static EnginHelper instance = new EnginHelper();

    public static EnginHelper getInstance() {
        return instance;
    }

    public final Mat matNv21 = new Mat(enginConfig.CAMERA_PREVIEW_HEIGHT + enginConfig.CAMERA_PREVIEW_HEIGHT / 2, enginConfig.CAMERA_PREVIEW_WIDTH, CvType.CV_8UC1);

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

    public boolean isInitOver() {
        return isInitOver;
    }

    public void setEnginConfig(EnginConfig enginConfig) {
        this.enginConfig = enginConfig;
    }

    public EnginConfig getEnginConfig() {
        return enginConfig;
    }

    public void initEngine(Context context, boolean needCheckSpoofing) {
        String faceModelPath = getInternalCacheDirectory(context, "face").getAbsolutePath();
        String fasModelPath = getInternalCacheDirectory(context, "fas").getAbsolutePath();

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
            Log.e(TAG, "PresenterImpl() init fail,can't find face models");
            return;
        }

        try {
            String rootPath = faceModelPath + "/";
            if (faceDetector == null || faceLandMarker == null || faceRecognizer == null) {
                faceDetector = new FaceDetector(new SeetaModelSetting(new String[]{rootPath + fdModel}));
                faceLandMarker = new FaceLandmarker(new SeetaModelSetting(new String[]{rootPath + pdModel}));
                faceRecognizer = new FaceRecognizer(new SeetaModelSetting(new String[]{rootPath + frModel}));
            }
            faceDetector.set(Property.PROPERTY_MIN_FACE_SIZE, enginConfig.MIN_FACE_SIZE);

            if (faceAntiSpoofing == null && needCheckSpoofing) {
                File fasModelPathFile = new File(fasModelPath);
                String[] fasModels = fasModelPathFile.list();
                if (fasModels == null || fasModels.length == 0) {
                    Log.e(TAG, "PresenterImpl() init fail,can't find fas models");
                    return;
                }
                rootPath = fasModelPath + "/";
                faceAntiSpoofing = new FaceAntiSpoofing(new SeetaModelSetting(new String[]{rootPath + fasModel1, rootPath + fasModel2}));
                faceAntiSpoofing.SetThreshold(enginConfig.FAS_CLARITY, enginConfig.FAS_THRESH);
            }
            isInitOver = true;
            Log.e(TAG, "-----------init over--------------");

        } catch (Exception e) {
            Log.e(TAG, "init exception:" + e);
        }
    }

    public void release() {
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
