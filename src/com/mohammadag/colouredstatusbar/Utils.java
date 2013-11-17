package com.mohammadag.colouredstatusbar;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.UserHandle;

import de.robv.android.xposed.XposedHelpers;

public class Utils {
	private static Object mUserHandle;

	public static ActivityInfo[] getActivityList(Context context, String packageName) throws NameNotFoundException {
		PackageManager pm = context.getPackageManager();
		PackageInfo info = pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
		ActivityInfo[] list = info.activities;
		return list;
	}

	public static String removePackageName(String string, String packageName) {
		return string.replace(packageName + ".", "");
	}

	public static Bitmap drawableToBitmap(Drawable drawable) throws IllegalArgumentException {
		if (drawable instanceof BitmapDrawable) {
			return ((BitmapDrawable)drawable).getBitmap();
		}
		Bitmap bitmap;

		try {
			bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
					drawable.getIntrinsicHeight(), Config.ARGB_8888);
			Canvas canvas = new Canvas(bitmap); 
			drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
			drawable.draw(canvas);
		} catch (IllegalArgumentException e) {
			throw e;
		}

		return bitmap;
	}

	public static int getMainColorFromActionBarDrawable(Drawable drawable) throws IllegalArgumentException {
		Bitmap bitmap = drawableToBitmap(drawable);
		int pixel = bitmap.getPixel(0, 5);
		int red = Color.red(pixel);
		int blue = Color.blue(pixel);
		int green = Color.green(pixel);
		return Color.rgb(red, green, blue);
	}

	// Thanks to GermainZ for the suggestion
	// http://forum.xda-developers.com/showpost.php?p=46102053&postcount=153
	public static int getIconColorForColor(int color, float hsvMaxValue) {
		float[] hsv = new float[3];
		Color.colorToHSV(color, hsv);
		float value = hsv[2];
		if (value > hsvMaxValue) {
			return Color.BLACK;
		} else {
			return Color.WHITE;
		}
	}

	/* Helper method, on API 17 this method uses sendBroadcastAsUser to prevent
	 * system warnings in logcat.
	 */
	@SuppressLint("NewApi")
    public static void sendOrderedBroadcast(Context context, Intent intent) {
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
			if (mUserHandle == null) {
				try {
					mUserHandle = XposedHelpers.getStaticObjectField(UserHandle.class, "CURRENT");
				} catch (Throwable t) {
					context.sendOrderedBroadcast(intent, null);
					return;
				}
			}
			context.sendOrderedBroadcastAsUser(intent, (UserHandle) mUserHandle, null, null, null, 0, null, null);
		} else {
			context.sendOrderedBroadcast(intent, null);
		}
	}

	public static String convertToARGB(int color) {
		String alpha = Integer.toHexString(Color.alpha(color));
		String red = Integer.toHexString(Color.red(color));
		String green = Integer.toHexString(Color.green(color));
		String blue = Integer.toHexString(Color.blue(color));

		if (alpha.length() == 1) {
			alpha = "0" + alpha;
		}

		if (red.length() == 1) {
			red = "0" + red;
		}

		if (green.length() == 1) {
			green = "0" + green;
		}

		if (blue.length() == 1) {
			blue = "0" + blue;
		}

		return "#" + alpha + red + green + blue;
	}
	
	@SuppressLint("WorldReadableFiles")
	@SuppressWarnings("deprecation")
	public static final SharedPreferences getSharedPreferences(Context context) {
		return context.getSharedPreferences(Common.PREFS, Context.MODE_WORLD_READABLE);
	}
}
