package com.mohammadag.colouredstatusbar.drawables;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.NinePatchDrawable;

public class OverlayDrawable extends ColorDrawable {
	private Paint mPaint;
	private NinePatchDrawable mNpd;
	private Mode mMode;
	private int mColor;

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

		canvas.drawRect(getBounds(), mPaint);
		if (mMode == Mode.GRADIENT) {
			mNpd.setBounds(getBounds());
			mNpd.draw(canvas);
		} else if (mMode == Mode.SEMI_TRANSPARENT) {
			mPaint.setColor(Color.argb(20, 0, 0, 0));
			canvas.drawRect(getBounds(), mPaint);
			mPaint.setColor(mColor);
		}
	}

	public void setColor(int color) {
		mColor = color;
		mPaint.setColor(color);
		invalidateSelf();
	}

	public void setMode(Mode mode) {
		if (mode == mMode)
			return;

		mMode = mode;
		invalidateSelf();
	}
}
