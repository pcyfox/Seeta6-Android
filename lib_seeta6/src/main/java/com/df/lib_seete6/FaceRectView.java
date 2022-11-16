package com.df.lib_seete6;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.WorkerThread;

public class FaceRectView extends SurfaceView {
    private Paint mFaceRectPaint;
    private final Rect focusRect = new Rect();
    private SurfaceHolder mOverlapHolder;

    public FaceRectView(Context context) {
        super(context);
    }

    public FaceRectView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FaceRectView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public FaceRectView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    {
        mFaceRectPaint = new Paint();
        mFaceRectPaint.setColor(Color.argb(150, 0, 255, 0));
        mFaceRectPaint.setStrokeWidth(3);
        mFaceRectPaint.setStyle(Paint.Style.STROKE);
    }

    private boolean isActive() {
        return isActivated() || mOverlapHolder != null;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mOverlapHolder = getHolder();
        mOverlapHolder.setFormat(PixelFormat.TRANSLUCENT);
        setZOrderOnTop(true);
    }


    public Paint getFaceRectPaint() {
        return mFaceRectPaint;
    }

    public void setFaceRectPaint(Paint mFaceRectPaint) {
        this.mFaceRectPaint = mFaceRectPaint;
    }


    public void drawBitmap(Bitmap bitmap, float left, float top, Paint paint) {
        if (!isActive()) {
            return;
        }
        Canvas canvas = mOverlapHolder.lockCanvas();
        if (canvas == null) {
            return;
        }
        canvas.drawColor(0, PorterDuff.Mode.CLEAR);
        if (bitmap != null && !bitmap.isRecycled()) {
            canvas.drawBitmap(bitmap, left, top, paint == null ? mFaceRectPaint : paint);
        }
        mOverlapHolder.unlockCanvasAndPost(canvas);
    }

    @WorkerThread
    public void drawFaceRect(org.opencv.core.Rect faceRect, float scaleX, float scaleY) {
        if (!isActive()) {
            return;
        }
        Canvas canvas = mOverlapHolder.lockCanvas();
        if (canvas == null) {
            return;
        }
        canvas.drawColor(0, PorterDuff.Mode.CLEAR);
        if (faceRect != null) {
            faceRect.x *= scaleX;
            faceRect.y *= scaleY;
            faceRect.width *= scaleX;
            faceRect.height *= scaleY;

            focusRect.left = faceRect.x;
            focusRect.right = faceRect.x + faceRect.width;

            focusRect.top = faceRect.y;
            focusRect.bottom = faceRect.y + faceRect.height;
            canvas.drawRect(focusRect, mFaceRectPaint);
        }
        mOverlapHolder.unlockCanvasAndPost(canvas);
    }
}
