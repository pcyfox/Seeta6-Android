package com.seetatech.seetaverify.mvp;

import android.Manifest;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceView;
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

import com.df.lib_seete6.Contract;
import com.df.lib_seete6.FaceRectView;
import com.df.lib_seete6.PresenterImpl;
import com.df.lib_seete6.camera.CameraCallbacks;
import com.df.lib_seete6.camera.CameraPreview2;
import com.seeta.sdk.FaceAntiSpoofing;
import com.seetatech.seetaverify.R;

import org.opencv.core.Mat;

import butterknife.BindView;
import butterknife.ButterKnife;


@SuppressWarnings("deprecation")
public class MainFragment extends Fragment implements Contract.View {
    public static final String TAG = "MainFragment";
    private static final int REQUEST_CAMERA_PERMISSION = 200;

    @BindView(R.id.camera_preview)
    CameraPreview2 mCameraPreview;

    @BindView(R.id.surfaceViewOverlap)
    protected FaceRectView mOverlap;

    @BindView(R.id.txt_name)
    TextView txtTips;


    @BindView(R.id.txt_state)
    TextView tvfacestatus;

    @BindView(R.id.et_registername)
    EditText edit_name;

    @BindView(R.id.btn_register)
    Button btn_register;

    private Contract.Presenter mPresenter;
    private AlertDialog mCameraUnavailableDialog;
    private Camera.Size mPreviewSize;

    private final Rect focusRect = new Rect();


    private float mPreviewScaleX = 1.0f;
    private float mPreviewScaleY = 1.0f;

    private final CameraCallbacks mCameraCallbacks = new CameraCallbacks() {
        @Override
        public void onCameraUnavailable(int errorCode) {
            Log.e(TAG, "camera unavailable, reason=%d" + errorCode);
            showCameraUnavailableDialog(errorCode);
        }

        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            if (mPreviewSize == null) {
                mPreviewSize = camera.getParameters().getPreviewSize();
                mPreviewScaleX = (float) (mCameraPreview.getHeight()) / mPreviewSize.width;
                mPreviewScaleY = (float) (mCameraPreview.getWidth()) / mPreviewSize.height;
            }
            mPresenter.detect(data, mPreviewSize.width, mPreviewSize.height, mCameraPreview.getCameraRotation());
        }
    };


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate");
        mPresenter = new PresenterImpl(this);
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
        ButterKnife.bind(this, view);
        mCameraPreview.setCameraCallbacks(mCameraCallbacks);
        btn_register.setOnClickListener(view12 -> {
            //人脸注册
            String registeredName = edit_name.getText().toString();
            mPresenter.startRegisterFrame(true, registeredName);
        });

        edit_name.setOnClickListener(view1 -> edit_name.setFocusable(true));
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
    public void toastMessage(String msg) {
        if (!TextUtils.isEmpty(msg)) {
            Toast.makeText(getActivity(), msg, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void showCameraUnavailableDialog(int errorCode) {
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
    public void setStatus(int status, Mat matBgr, org.opencv.core.Rect faceRect) {
        Log.i(TAG, "setStatus " + status);

    }

    @Override
    public void onDetectFinish(FaceAntiSpoofing.Status status, float similarity, String name, Mat matBgr, org.opencv.core.Rect faceRect) {
        //展示名称
        if (!isActive()) {
            return;
        }
        switch (status) {
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

        txtTips.setText(name);
    }

    @Override
    public void showSimpleTip(String tip) {
        Toast.makeText(getContext(), tip, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onFaceRegisterFinish(boolean isSuccess, String tip) {
        //提示注册成功
        Toast.makeText(getContext(), tip, Toast.LENGTH_LONG).show();
        //还原EditView
        edit_name.setText("");
        edit_name.setHint("enter name");
    }

    @Override
    public void setBestImage(Bitmap bitmap) {

    }

    @Override
    public void setPresenter(Contract.Presenter presenter) {
        this.mPresenter = presenter;
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
