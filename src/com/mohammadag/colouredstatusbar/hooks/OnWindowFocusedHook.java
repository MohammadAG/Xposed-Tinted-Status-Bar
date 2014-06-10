package com.mohammadag.colouredstatusbar.hooks;

import java.io.ByteArrayOutputStream;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.content.res.XModuleResources;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.support.v4.app.NotificationCompat;
import android.view.View;

import com.mohammadag.colouredstatusbar.R;
import com.mohammadag.colouredstatusbar.SettingsHelper;
import com.mohammadag.colouredstatusbar.SettingsKeys;

import de.robv.android.xposed.XC_MethodHook;

public class OnWindowFocusedHook extends XC_MethodHook {
	private SettingsHelper mSettingsHelper;
	private XModuleResources mResources;

	public OnWindowFocusedHook(SettingsHelper helper, XModuleResources resources) {
		mSettingsHelper = helper;
		mResources = resources;
	}
	
	@Override
	protected void afterHookedMethod(MethodHookParam param) throws Throwable {
		mSettingsHelper.reload();
		if (!mSettingsHelper.getBoolean(SettingsKeys.ENABLE_AWESOME_AB_COLOR_PICKER, false))
			return;

		Activity activity = (Activity) param.thisObject;
		String packageName = activity.getPackageName();
		String activityName = activity.getLocalClassName();

		final TypedArray typedArray = activity.obtainStyledAttributes(new int[]{android.R.attr.actionBarSize});
		int actionBarSize = (int) typedArray.getDimension(0, 0);
		typedArray.recycle();

		// Get the top of the window, so we can crop the status bar out.
		Rect rect = new Rect();
		activity.getWindow().getDecorView().getWindowVisibleDisplayFrame(rect);
		int top = rect.top;

		View view = activity.getWindow().getDecorView();
		view.setDrawingCacheEnabled(true);
		Bitmap bitmap1 = view.getDrawingCache();
		if (bitmap1 == null) return;
		// Crop and compress the image so that we don't get a TransactionTooLargeException.
		Bitmap bitmap = Bitmap.createBitmap(bitmap1, 0, top, bitmap1.getWidth(), actionBarSize);
		ByteArrayOutputStream compressedBitmap = new ByteArrayOutputStream();
		bitmap.compress(Bitmap.CompressFormat.JPEG, 80, compressedBitmap);

		ComponentName cn = new ComponentName("com.mohammadag.colouredstatusbar",
				"com.mohammadag.colouredstatusbar.ScreenColorPickerActivity");
		Intent colorPickerIntent = new Intent().setComponent(cn);
		colorPickerIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		colorPickerIntent.putExtra("bitmap", compressedBitmap.toByteArray());
		colorPickerIntent.putExtra("pkg", packageName);

		PendingIntent colorActivityPendingIntent = PendingIntent.getActivity(activity, 0,
				colorPickerIntent.putExtra("title", activityName), PendingIntent.FLAG_UPDATE_CURRENT);
		PendingIntent colorAllPendingIntent = PendingIntent.getActivity(activity, 1,
				colorPickerIntent.putExtra("title", packageName), PendingIntent.FLAG_UPDATE_CURRENT);

		NotificationManager nm = (NotificationManager) activity.getSystemService(Context.NOTIFICATION_SERVICE);
		Notification notification = new NotificationCompat.Builder(activity)
				.setContentTitle(packageName)
				.setContentText(activityName)
				.setSmallIcon(android.R.drawable.sym_def_app_icon)
				.setStyle(new NotificationCompat.BigPictureStyle().bigPicture(bitmap))
				.addAction(android.R.drawable.ic_menu_add,
						mResources.getString(R.string.notification_add_activity), colorActivityPendingIntent)
				.addAction(android.R.drawable.ic_menu_add,
						mResources.getString(R.string.notification_add_app), colorAllPendingIntent)
				.build();
		nm.notify(1240, notification);
		view.setDrawingCacheEnabled(false);
	}
}
