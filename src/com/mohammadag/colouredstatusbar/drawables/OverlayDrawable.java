package com.mohammadag.colouredstatusbar.drawables;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.NinePatchDrawable;

public class OverlayDrawable extends ColorDrawable {
	private Paint mPaint;
	private NinePatchDrawable mNpd;
	private Mode mMode;
	private int mColor;
	private int mOverrideColor = -3;
	private int mOpacity;
	private float mDimAmount;
	private boolean mIsKitkatTransparency = false;
	private ValueAnimator mAnimator;

	public enum Mode {
		SEMI_TRANSPARENT, GRADIENT, COLOR, UNKNOWN
	}

	public OverlayDrawable(Resources res, int color, int resId) {
		mColor = color;
		mPaint = new Paint();
		mPaint.setColor(color);
		mNpd = (NinePatchDrawable) res.getDrawable(resId);
	}

	@Override
	public void draw(Canvas canvas) {
		super.draw(canvas);

		if (mOverrideColor != -3)
			mPaint.setColor(mOverrideColor);
		else
			mPaint.setColor(mColor);

		if (mDimAmount > 0) {
			mPaint.setAlpha((int) ((1 - mDimAmount) * 255));
		} else {
			mPaint.setAlpha(255);
		}

		if (mMode == Mode.SEMI_TRANSPARENT) {
			mPaint.setAlpha(mPaint.getAlpha() - mOpacity);
		}

		canvas.drawRect(getBounds(), mPaint);
		if (mMode == Mode.GRADIENT || (mMode == Mode.COLOR && mIsKitkatTransparency)) {
			mNpd.setBounds(getBounds());
			mNpd.draw(canvas);
		}
	}

	public void setColor(int color) {
		mColor = color;
		invalidateSelf();
	}

	public void setOverrideColor(final int color) {
		int animateFrom;
		final int animateTo;
		if (mOverrideColor != -3) {
			animateFrom = mOverrideColor;
		} else {
			animateFrom = mColor;
		}

		if (color == -3) {
			animateTo = mColor;
		} else {
			animateTo = color;
		}
		if (mAnimator != null && mAnimator.isRunning()) {
			mAnimator.cancel();
			animateFrom = mOverrideColor;
			mAnimator = null;
		}
		mAnimator = ValueAnimator.ofObject(new ArgbEvaluator(), animateFrom, animateTo);
		mAnimator.addUpdateListener(new AnimatorUpdateListener() {
			@Override
			public void onAnimationUpdate(ValueAnimator animator) {
				mOverrideColor = (Integer) animator.getAnimatedValue();
				invalidateSelf();
			}
		});
		mAnimator.addListener(new AnimatorListener() {
			@Override
			public void onAnimationStart(Animator animation) { }

			@Override
			public void onAnimationRepeat(Animator animation) { }

			@Override
			public void onAnimationEnd(Animator animation) {
				if (color == -3) {
					mOverrideColor = -3;
					mColor = animateTo;
					invalidateSelf();
				}
				mAnimator = null;
			}

			@Override
			public void onAnimationCancel(Animator animation) { }
		});
		mAnimator.start();
	}

	public void setMode(Mode mode, int opacity) {
		if (mode == mMode && mOpacity == opacity)
			return;

		mMode = mode;
		mOpacity = opacity;
		invalidateSelf();
	}

	public void setIsTransparentCauseOfKitKatApi(boolean isKitkatTransparency) {
		mIsKitkatTransparency = isKitkatTransparency;
		invalidateSelf();
	}

	public void setDimAmount(float alphaPercent) {
		if (mIsKitkatTransparency)
			return;

		mDimAmount = alphaPercent;
		invalidateSelf();
	}
}
