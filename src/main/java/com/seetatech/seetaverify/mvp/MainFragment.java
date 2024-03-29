package com.seetatech.seetaverify.mvp;

import android.Manifest;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.df.lib_seete6.PresenterImpl;
import com.df.lib_seete6.SeetaContract;
import com.df.lib_seete6.Target;
import com.df.lib_seete6.camera.CameraCallbacks;
import com.df.lib_seete6.camera.CameraPreview2;
import com.df.lib_seete6.view.FaceRectView;
import com.seetatech.seetaverify.R;

import org.opencv.core.Mat;


@SuppressWarnings("deprecation")
public class MainFragment extends Fragment implements SeetaContract.ViewInterface {
    public static final String TAG = "MainFragment";
    private static final int REQUEST_CAMERA_PERMISSION = 200;

    CameraPreview2 mCameraPreview;
    protected FaceRectView mOverlap;
    TextView txtTips;
    TextView tvfacestatus;
    EditText edit_name;
    Button btn_register;
    Button btn_take_pic;

    private SeetaContract.Presenter mPresenter;
    private AlertDialog mCameraUnavailableDialog;
    private Camera.Size mPreviewSize;

    private float mPreviewScaleX = 1.0f;
    private float mPreviewScaleY = 1.0f;

    private final CameraCallbacks mCameraCallbacks = new CameraCallbacks() {
        @Override
        public void onCameraUnavailable(int errorCode) {
            Log.e(TAG, "camera unavailable, reason=%d" + errorCode);
            onOpenCameraError(errorCode, "");
        }

        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            if (mPreviewSize == null) {
                mPreviewSize = camera.getParameters().getPreviewSize();
                mPreviewScaleX = (float) (mCameraPreview.getHeight()) / mPreviewSize.width;
                mPreviewScaleY = (float) (mCameraPreview.getWidth()) / mPreviewSize.height;
            }
            mPresenter.detect(data, mPreviewSize.width, mPreviewSize.height, 360 - mCameraPreview.getCameraRotation());
        }
    };


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate");
        mPresenter = new PresenterImpl(this);
        mPresenter.setFaceRegisterListener(((isSuccess, key) -> {
            //提示注册成功
            Toast.makeText(getContext(), key + ",isSuccess:" + isSuccess, Toast.LENGTH_LONG).show();
            //还原EditView
            edit_name.setText("");
            edit_name.setHint("enter name");
        }));

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView");
        return inflater.inflate(R.layout.fragment_main, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        Log.i(TAG, "onViewCreated");
        super.onViewCreated(view, savedInstanceState);
        mCameraPreview = getView().findViewById(R.id.camera_preview);
        mOverlap = getView().findViewById(R.id.surfaceViewOverlap);
        txtTips = getView().findViewById(R.id.txt_name);
        tvfacestatus = getView().findViewById(R.id.txt_state);
        edit_name = getView().findViewById(R.id.et_registername);
        btn_register = getView().findViewById(R.id.btn_register);
        btn_take_pic = getView().findViewById(R.id.btn_take_pic);
        mCameraPreview.setCameraCallbacks(mCameraCallbacks);

        btn_register.setOnClickListener(view12 -> {
            //人脸注册
            String registeredName = edit_name.getText().toString();
            mPresenter.startRegisterFrame(registeredName);
        });
        edit_name.setOnClickListener(view1 -> edit_name.setFocusable(true));
        btn_take_pic.setOnClickListener(v -> mPresenter.takePicture("/sdcard/", "detect.jpg"));
    }

    @WorkerThread
    @Override
    public void drawFaceRect(org.opencv.core.Rect faceRect) {
        if (!isActive()) {
            return;
        }
        mOverlap.drawFaceRect(faceRect, mPreviewScaleX, mPreviewScaleY);
    }

    @WorkerThread
    @Override
    public void drawFaceImage(Bitmap faceBmp) {
        if (!isActive()) {
            return;
        }
        mOverlap.drawBitmap(faceBmp, 0, 0, null);
    }

    @Override
    public void onOpenCameraError(int errorCode, String msg) {
        if (mCameraUnavailableDialog == null) {
            mCameraUnavailableDialog = new AlertDialog.Builder(getActivity()).setTitle("摄像头不可用").setMessage(getContext().getString(R.string.please_restart_device_or_app, errorCode)).setPositiveButton("重试", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    getActivity().runOnUiThread(() -> getActivity().recreate());
                }
            }).setNegativeButton("退出", (dialog, which) -> getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    getActivity().finish();
                }
            })).create();
        }
        if (!mCameraUnavailableDialog.isShowing()) {
            mCameraUnavailableDialog.show();
        }
    }

    @Override
    public void onTakePictureFinish(String path, String name) {

    }


    @Override
    public void onDetectFinish(Target target, Mat matBgr, org.opencv.core.Rect faceRect) {
        //展示名称
        if (!isActive()) {
            return;
        }
        switch (target.getStatus()) {
            case DETECTING:
                tvfacestatus.setText("检测中");
                break;
            case REAL:
                tvfacestatus.setText("真人脸");
                break;
            case SPOOF:
                tvfacestatus.setText("假人脸");
                break;
            case FUZZY:
                tvfacestatus.setText("图像过于模糊");
                break;
        }
        txtTips.setText(target.getKey());
    }


    @Override
    public boolean isActive() {
        return getView() != null && isAdded() && !isDetached();
    }


    @Override
    public void onDestroyView() {
        mPresenter.destroy();
        super.onDestroyView();
    }

    @SuppressWarnings("unused")
    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            getActivity().recreate();
        }
    }

}
