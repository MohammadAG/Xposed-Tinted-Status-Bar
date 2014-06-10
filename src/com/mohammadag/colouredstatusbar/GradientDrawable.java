package com.mohammadag.colouredstatusbar;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.NinePatchDrawable;

public class GradientDrawable extends ColorDrawable {
	private Paint mPaint;
	private NinePatchDrawable mNpd;

	public GradientDrawable(Resources res, int color, int resId) {
		mPaint = new Paint();
		mPaint.setColor(color);
		mNpd = (NinePatchDrawable) res.getDrawable(resId);
	}

	@Override
	public void draw(Canvas canvas) {
		super.draw(canvas);

		canvas.drawRect(getBounds(), mPaint);
		mNpd.setBounds(getBounds());
		mNpd.draw(canvas);
	}

	public void setColor(int color) {
		mPaint.setColor(color);
		invalidateSelf();
	}
}
