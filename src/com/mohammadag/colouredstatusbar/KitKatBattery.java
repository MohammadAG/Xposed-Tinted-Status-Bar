package com.mohammadag.colouredstatusbar;

import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;

import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.setIntField;
import static de.robv.android.xposed.XposedHelpers.setObjectField;

public class KitKatBattery {
	private static final String TYPE_NORMAL = "com.android.systemui.BatteryMeterView";
	private static final String TYPE_CIRCLE = "com.android.systemui.BatteryCircleMeterView";
	private static final String TYPE_PERCENT = "com.android.systemui.BatteryPercentMeterView";
	private final View batteryView;
	private final String type;
	private final SettingsHelper settingsHelper;

	public KitKatBattery(View batteryView, String type, SettingsHelper settingsHelper) {
		this.batteryView = batteryView;
		this.type = type;
		this.settingsHelper = settingsHelper;
	}

	public void updateBattery(int iconColor) {
		if (type.equals(TYPE_NORMAL)) {
			updateNormalBattery(iconColor);
		} else if (type.equals(TYPE_CIRCLE)) {
			updateCircleBattery(iconColor);
		} else if (type.equals(TYPE_PERCENT)) {
			updatePercentBattery(iconColor);
		}
		batteryView.invalidate();
	}

	private void updateNormalBattery(int iconColor) {
		boolean debug = settingsHelper.isDebugMode();

		try {
			final int[] colors = (int[]) getObjectField(batteryView, "mColors");
			colors[colors.length - 1] = iconColor;
			setObjectField(batteryView, "mColors", colors);
		} catch (NoSuchFieldError e) {
			if (debug) e.printStackTrace();
		}

		try {
			final Paint framePaint = (Paint) getObjectField(batteryView, "mFramePaint");
			framePaint.setColor(iconColor);
			framePaint.setAlpha(100);
		} catch (NoSuchFieldError e) {
			if (debug) e.printStackTrace();
		}

		try {
			final Paint boltPaint = (Paint) getObjectField(batteryView, "mBoltPaint");
			boltPaint.setColor(Utils.getIconColorForColor(iconColor, Color.WHITE, Color.BLACK, 0.7f));
			boltPaint.setAlpha(100);
		} catch (NoSuchFieldError e) {
			if (debug) e.printStackTrace();
		}

		try {
			final Paint textPaint = (Paint) getObjectField(batteryView, "mTextPaint");
			textPaint.setColor(Utils.getIconColorForColor(iconColor, Color.WHITE, Color.BLACK, 0.7f));
			textPaint.setAlpha(100);
		} catch (NoSuchFieldError e) {
			if (debug) e.printStackTrace();
		}

		try {
			setIntField(batteryView, "mChargeColor", iconColor);
		} catch (NoSuchFieldError e) {
			/* Beanstalk, not sure why the ROM changed this */
			try {
				setIntField(batteryView, "mBatteryColor", iconColor);
			} catch (NoSuchFieldError ignored) {
			}
			if (debug) e.printStackTrace();
		}
	}

	private void updateCircleBattery(int iconColor) {
		boolean debug = settingsHelper.isDebugMode();

		for (String field : new String[]{"mCircleTextColor", "mCircleTextChargingColor", "mCircleColor"}) {
			try {
				setIntField(batteryView, field, iconColor);
			} catch (NoSuchFieldError e) {
				if (debug) e.printStackTrace();
			}
		}

		try {
			final Paint paintSystem = (Paint) getObjectField(batteryView, "mPaintSystem");
			paintSystem.setColor(Utils.getIconColorForColor(iconColor, Color.BLACK, Color.WHITE, 0.7f));
			paintSystem.setAlpha(100);
		} catch (NoSuchFieldError e) {
			if (debug) e.printStackTrace();
		}
	}

	private void updatePercentBattery(int iconColor) {
		boolean debug = settingsHelper.isDebugMode();

		for (String field : new String[]{"mChargingColorBg", "mChargingColorDefault", "mChargingColorFg"}) {
			try {
				setIntField(batteryView, field, iconColor);
			} catch (NoSuchFieldError e) {
				if (debug) e.printStackTrace();
			}
		}

		for (String field : new String[]{"mPaintFontBg", "mPaintFontBg"}) {
			try {
				final Paint paintFont = (Paint) getObjectField(batteryView, field);
				paintFont.setColor(Utils.getIconColorForColor(iconColor, Color.BLACK, Color.WHITE, 0.7f));
				paintFont.setAlpha(100);
			} catch (NoSuchFieldError e) {
				if (debug) e.printStackTrace();
			}
		}
	}
}
