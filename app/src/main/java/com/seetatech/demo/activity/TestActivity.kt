package com.seetatech.demo.activity

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.*
import android.hardware.Camera
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.df.seeta6.R
import com.seetatech.demo.camera.CameraCallbacks
import com.seetatech.demo.mvp.MainFragment
import com.seetatech.demo.mvp.PresenterImpl
import com.seetatech.demo.mvp.VerificationContract
import com.seetatech.demo.mvp.VerificationContract.Presenter
import kotlinx.android.synthetic.main.activity_test.*
import org.opencv.core.Mat
import org.opencv.core.Rect

class TestActivity : AppCompatActivity(), VerificationContract.View {
    companion object {
        private const val TAG = "TestActivity"
        private const val REQUEST_CAMERA_PERMISSION = 200
        val permissions =
            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA)
    }

    private var mPreviewSize: Camera.Size? = null
    private var mPresenter: Presenter? = null
    private var mPreviewScaleX = 1.0f
    private var mPreviewScaleY = 1.0f
    private val focusRect = android.graphics.Rect()
    private lateinit var mFaceRectPaint: Paint
    private lateinit var presenterImpl: PresenterImpl


    private val mCameraCallbacks: CameraCallbacks = object : CameraCallbacks {
        override fun onCameraUnavailable(errorCode: Int) {
            Log.e(MainFragment.TAG, "camera unavailable, reason=%d$errorCode")
            showCameraUnavailableDialog(errorCode)
        }

        override fun onPreviewFrame(data: ByteArray, camera: Camera) {
            if (mPreviewSize == null) {
                mPreviewSize = camera.parameters.previewSize
                mPreviewScaleX = cameraPreview.height.toFloat() / mPreviewSize!!.width
                mPreviewScaleY = cameraPreview.width.toFloat() / mPreviewSize!!.height
            }
            mPresenter?.detect(
                data, mPreviewSize!!.width, mPreviewSize!!.height, cameraPreview.cameraRotation
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test)
        cameraPreview.setCameraCallbacks(mCameraCallbacks)
        surfaceViewOverlap?.run {
            setZOrderOnTop(true)
            holder.setFormat(PixelFormat.TRANSLUCENT)
        }
        mFaceRectPaint = Paint()
        mFaceRectPaint.color = Color.argb(150, 0, 255, 0)
        mFaceRectPaint.strokeWidth = 3f
        mFaceRectPaint.style = Paint.Style.STROKE

        presenterImpl = PresenterImpl(this, this)

        requestCameraPermission()
        initView()
    }

    private fun initView() {
        btn_register.setOnClickListener { view12: View? ->
            //人脸注册
            val registeredName = et_registerName.text.toString()
            presenterImpl.startRegisterFace(true, registeredName)
        }
    }

    override fun onStop() {
        super.onStop()
        cameraPreview.onPause()
    }

    private fun requestCameraPermission() {
        Log.d(TAG, "requestCameraPermission() called")
        var allOk = true
        permissions.forEach {
            if (ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED) {
                allOk = false
                return@forEach
            }
        }
        if (allOk) {
            openCamera()
            return
        }
        ActivityCompat.requestPermissions(this, permissions, REQUEST_CAMERA_PERMISSION)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty()) {
            openCamera()
        }
    }

    private fun openCamera() {
        Log.d(TAG, "openCamera() called")
        cameraPreview.onResume(0)
    }

    override fun drawFaceRect(faceRect: Rect?) {
        if (!isActive) {
            return
        }
        val canvas: Canvas = surfaceViewOverlap.holder.lockCanvas() ?: return
        canvas.drawColor(0, PorterDuff.Mode.CLEAR)

        if (faceRect != null) {
            faceRect.x *= mPreviewScaleX.toInt()
            faceRect.y *= mPreviewScaleY.toInt()
            faceRect.width *= mPreviewScaleX.toInt()
            faceRect.height *= mPreviewScaleY.toInt()
            focusRect.left = faceRect.x
            focusRect.right = faceRect.x + faceRect.width
            focusRect.top = faceRect.y
            focusRect.bottom = faceRect.y + faceRect.height

            canvas.drawRect(focusRect, mFaceRectPaint)
        }
        surfaceViewOverlap.holder.unlockCanvasAndPost(canvas)
    }

    override fun drawFaceImage(faceBmp: Bitmap?) {
    }

    override fun toastMessage(msg: String?) {
        Log.d(TAG, "toastMessage() called with: msg = $msg")
    }

    override fun showCameraUnavailableDialog(errorCode: Int) {
        Log.d(TAG, "showCameraUnavailableDialog() called with: errorCode = $errorCode")
    }

    override fun setStatus(status: Int, matBgr: Mat?, faceRect: Rect?) {
        Log.d(
            TAG,
            "setStatus() called with: status = $status, matBgr = $matBgr, faceRect = $faceRect"
        )
    }

    override fun setName(name: String?, matBgr: Mat?, faceRect: Rect?) {
        Log.d(TAG, "setName() called with: name = $name, matBgr = $matBgr, faceRect = $faceRect")
    }

    override fun FaceRegister(tip: String?) {
        Log.d(TAG, "FaceRegister() called with: tip = $tip")
        //提示注册成功
        et_registerName.setText("")
        et_registerName.hint = "enter name"
        Toast.makeText(this, tip, Toast.LENGTH_LONG).show()
    }

    override fun showSimpleTip(tip: String?) {
        Log.d(TAG, "showSimpleTip() called with: tip = $tip")
    }

    override fun setBestImage(bitmap: Bitmap?) {
    }

    override fun setPresenter(presenter: Presenter?) {
        mPresenter = presenter
    }


    override fun isActive(): Boolean {
        return !isFinishing || !isDestroyed
    }
}