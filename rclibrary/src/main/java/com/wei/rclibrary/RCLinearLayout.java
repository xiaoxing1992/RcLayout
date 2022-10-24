package com.wei.rclibrary;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.Checkable;
import android.widget.LinearLayout;
import com.wei.rclibrary.helper.RCAttrs;
import com.wei.rclibrary.helper.RCHelper;
import skin.support.widget.SkinCompatBackgroundHelper;
import skin.support.widget.SkinCompatSupportable;

public class RCLinearLayout extends LinearLayout
        implements Checkable, RCAttrs, SkinCompatSupportable {

    private SkinCompatBackgroundHelper mBackgroundTintHelper;
    RCHelper mRCHelper;

    public RCLinearLayout(Context context) {
        this(context, null);
    }

    public RCLinearLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RCLinearLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mRCHelper = new RCHelper();
        mRCHelper.initAttrs(context, this, attrs);
        mBackgroundTintHelper = new SkinCompatBackgroundHelper(this);
        mBackgroundTintHelper.loadFromAttributes(attrs, defStyle);
    }

    @Override public void setBackgroundResource(int resId) {
        super.setBackgroundResource(resId);
        if (mBackgroundTintHelper != null) {
            mBackgroundTintHelper.onSetBackgroundResource(resId);
        }
    }

    @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mRCHelper.onSizeChanged(this, w, h);
    }

    @Override protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
    }

    @Override public void dispatchDraw(Canvas canvas) {
        canvas.saveLayer(mRCHelper.mLayer, null, Canvas.ALL_SAVE_FLAG);
        super.dispatchDraw(canvas);
        mRCHelper.onClipDraw(canvas);
        canvas.restore();
    }

    @Override public void draw(Canvas canvas) {
        if (mRCHelper.mClipBackground) {
            canvas.save();
            canvas.clipPath(mRCHelper.mClipPath);
            super.draw(canvas);
            canvas.restore();
        } else {
            super.draw(canvas);
        }
    }

    @Override public boolean dispatchTouchEvent(MotionEvent ev) {
        int action = ev.getAction();
        if (action == MotionEvent.ACTION_DOWN && !mRCHelper.mAreaRegion.contains((int) ev.getX(),
                (int) ev.getY())) {
            return false;
        }
        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_UP) {
            refreshDrawableState();
        } else if (action == MotionEvent.ACTION_CANCEL) {
            setPressed(false);
            refreshDrawableState();
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override public void invalidate() {
        if (null != mRCHelper) mRCHelper.refreshRegion(this);
        super.invalidate();
    }

    public boolean isClipBackground() {
        return mRCHelper.mClipBackground;
    }

    public boolean isRoundAsCircle() {
        return mRCHelper.mRoundAsCircle;
    }

    public float getTopLeftRadius() {
        return mRCHelper.radii[0];
    }

    public float getTopRightRadius() {
        return mRCHelper.radii[2];
    }

    public float getBottomLeftRadius() {
        return mRCHelper.radii[4];
    }

    public float getBottomRightRadius() {
        return mRCHelper.radii[6];
    }

    public int getStrokeWidth() {
        return mRCHelper.mStrokeWidth;
    }

    public int getStrokeColor() {
        return mRCHelper.mStrokeColor;
    }

    @Override public void setClipBackground(boolean clipBackground) {
        mRCHelper.mClipBackground = clipBackground;
        invalidate();
    }

    @Override public void setRoundAsCircle(boolean roundAsCircle) {
        mRCHelper.mRoundAsCircle = roundAsCircle;
        invalidate();
    }

    @Override public void setRadius(int radius) {
        for (int i = 0; i < mRCHelper.radii.length; i++) {
            mRCHelper.radii[i] = radius;
        }
        invalidate();
    }

    @Override public void setTopLeftRadius(int topLeftRadius) {
        mRCHelper.radii[0] = topLeftRadius;
        mRCHelper.radii[1] = topLeftRadius;
        invalidate();
    }

    @Override public void setTopRightRadius(int topRightRadius) {
        mRCHelper.radii[2] = topRightRadius;
        mRCHelper.radii[3] = topRightRadius;
        invalidate();
    }

    @Override public void setBottomLeftRadius(int bottomLeftRadius) {
        mRCHelper.radii[6] = bottomLeftRadius;
        mRCHelper.radii[7] = bottomLeftRadius;
        invalidate();
    }

    @Override public void setBottomRightRadius(int bottomRightRadius) {
        mRCHelper.radii[4] = bottomRightRadius;
        mRCHelper.radii[5] = bottomRightRadius;
        invalidate();
    }

    @Override public void setStrokeWidth(int strokeWidth) {
        mRCHelper.mStrokeWidth = strokeWidth;
        invalidate();
    }

    @Override public void setStrokeColor(int strokeColor) {
        mRCHelper.mStrokeColor = strokeColor;
        invalidate();
    }

    //--- Selector 支持 ----------------------------------------------------------------------------

    @Override protected void drawableStateChanged() {
        super.drawableStateChanged();
        mRCHelper.drawableStateChanged(this);
    }

    @Override public void setChecked(boolean checked) {
        if (mRCHelper.mChecked != checked) {
            mRCHelper.mChecked = checked;
            refreshDrawableState();
            if (mRCHelper.mOnCheckedChangeListener != null) {
                mRCHelper.mOnCheckedChangeListener.onCheckedChanged(this, mRCHelper.mChecked);
            }
        }
    }

    @Override public boolean isChecked() {
        return mRCHelper.mChecked;
    }

    @Override public void toggle() {
        setChecked(!mRCHelper.mChecked);
    }

    public void setOnCheckedChangeListener(RCHelper.OnCheckedChangeListener listener) {
        mRCHelper.mOnCheckedChangeListener = listener;
    }

    @Override public void applySkin() {
        if (mBackgroundTintHelper != null) {
            mBackgroundTintHelper.applySkin();
        }
    }
}

