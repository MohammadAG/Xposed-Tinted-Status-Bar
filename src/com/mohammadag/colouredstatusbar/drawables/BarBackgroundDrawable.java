package com.mohammadag.colouredstatusbar.drawables;

import android.animation.TimeInterpolator;
import android.content.Context;
import android.content.res.XModuleResources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.view.animation.LinearInterpolator;

public class BarBackgroundDrawable extends Drawable {
	private final int mOpaque;
	private final int mSemiTransparent;
	private Drawable mGradient;
	private final TimeInterpolator mInterpolator;

	private int mMode = -1;
	private boolean mAnimating;
	private long mStartTime;
	private long mEndTime;

	private int mGradientAlpha;
	private int mColor;

	private int mGradientAlphaStart;
	private int mColorStart;

	private static final boolean DEBUG_COLORS = false;

	public static final int MODE_OPAQUE = 0;
	public static final int MODE_SEMI_TRANSPARENT = 1;
	public static final int MODE_TRANSLUCENT = 2;
	public static final int MODE_LIGHTS_OUT = 3;

	public static final int LIGHTS_IN_DURATION = 250;
	public static final int LIGHTS_OUT_DURATION = 750;
	public static final int BACKGROUND_DURATION = 200;

	private static final int OPAQUE_COLOR = Color.parseColor("#ff000000");
	private static final int SEMI_TRANSPARENT = Color.parseColor("#66000000");

	public BarBackgroundDrawable(Context context, XModuleResources res, int gradientResourceId) {
		if (DEBUG_COLORS) {
			mOpaque = 0xff0000ff;
			mSemiTransparent = 0x7f0000ff;
		} else {
			mOpaque = OPAQUE_COLOR;
			mSemiTransparent = SEMI_TRANSPARENT;
		}

		/* This might cause reboots when this is upgraded */
		try {
			mGradient = res.getDrawable(gradientResourceId);
		} catch (Throwable t) {
			mGradient = null;
		}
		mInterpolator = new LinearInterpolator();
		applyModeBackground(MODE_TRANSLUCENT, MODE_TRANSLUCENT, false);
	}

	@Override
	public void setAlpha(int alpha) {
		// noop
	}

	@Override
	public void setColorFilter(ColorFilter cf) {
		// noop
	}

	@Override
	protected void onBoundsChange(Rect bounds) {
		super.onBoundsChange(bounds);
		mGradient.setBounds(bounds);
	}

	public void applyModeBackground(int oldMode, int newMode, boolean animate) {
		if (mMode == newMode) return;
		mMode = newMode;
		mAnimating = animate;
		if (animate) {
			long now = SystemClock.elapsedRealtime();
			mStartTime = now;
			mEndTime = now + BACKGROUND_DURATION;
			mGradientAlphaStart = mGradientAlpha;
			mColorStart = mColor;
		}
		invalidateSelf();
	}

	@Override
	public int getOpacity() {
		return PixelFormat.TRANSLUCENT;
	}

	public void finishAnimation() {
		if (mAnimating) {
			mAnimating = false;
			invalidateSelf();
		}
	}

	@Override
	public void draw(Canvas canvas) {
		int targetGradientAlpha = 0, targetColor = 0;
		if (mMode == MODE_TRANSLUCENT) {
			targetGradientAlpha = 0xff;
		} else if (mMode == MODE_SEMI_TRANSPARENT) {
			targetColor = mSemiTransparent;
		} else {
			targetColor = mOpaque;
		}
		if (!mAnimating) {
			mColor = targetColor;
			mGradientAlpha = targetGradientAlpha;
		} else {
			final long now = SystemClock.elapsedRealtime();
			if (now >= mEndTime) {
				mAnimating = false;
				mColor = targetColor;
				mGradientAlpha = targetGradientAlpha;
			} else {
				final float t = (now - mStartTime) / (float)(mEndTime - mStartTime);
				final float v = Math.max(0, Math.min(mInterpolator.getInterpolation(t), 1));
				mGradientAlpha = (int)(v * targetGradientAlpha + mGradientAlphaStart * (1 - v));
				mColor = Color.argb(
						(int)(v * Color.alpha(targetColor) + Color.alpha(mColorStart) * (1 - v)),
						(int)(v * Color.red(targetColor) + Color.red(mColorStart) * (1 - v)),
						(int)(v * Color.green(targetColor) + Color.green(mColorStart) * (1 - v)),
						(int)(v * Color.blue(targetColor) + Color.blue(mColorStart) * (1 - v)));
			}
		}
		if (mGradientAlpha > 0 && mGradient != null) {
			mGradient.setAlpha(mGradientAlpha);
			mGradient.draw(canvas);
		}
		if (Color.alpha(mColor) > 0) {
			canvas.drawColor(mColor);
		}
		if (mAnimating) {
			invalidateSelf();  // keep going
		}
	}
}
