package com.mohammadag.colouredstatusbar.hooks;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.UserHandle;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

public class WindowManagerServiceHooks extends XC_MethodHook {
	public static final String INTENT_DIM_CHANGED = "com.mohammadag.colouredstatusbar.DIM_LAYER_CHANGED";
	public static final String KEY_DIM_LAYER = "dimLayer";
	public static final String KEY_DIM_AMOUNT = "dimAmount";
	public static final String KEY_TARGET_ALPHA = "targetAlpha";
	private boolean mDimmed;

	private WindowManagerServiceHooks() {

	}

	public static void doHook(ClassLoader loader) {
		try {
			XposedHelpers.findAndHookMethod("com.android.server.wm.WindowManagerService",
					loader, "handleFlagDimBehind", "com.android.server.wm.WindowState",
					int.class, int.class, new WindowManagerServiceHooks());
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}
	
	@Override
	protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
		final WindowManager.LayoutParams attrs = (LayoutParams) XposedHelpers.getObjectField(param.args[0], "mAttrs");
		boolean isDisplayedLw = (Boolean) XposedHelpers.callMethod(param.args[0], "isDisplayedLw");
		boolean isExiting = XposedHelpers.getBooleanField(param.args[0], "mExiting");
		boolean dimming = false;
		if ((attrs.flags & WindowManager.LayoutParams.FLAG_DIM_BEHIND) != 0) {
			if (isDisplayedLw && !isExiting)
				dimming = true;
		} else {
			if (isDisplayedLw && !isExiting)
				dimming = false;
		}
		
		if (mDimmed = dimming)
			return;
		
		boolean bootComplete = XposedHelpers.getBooleanField(param.thisObject, "mSystemBooted");
		if (!bootComplete)
			return;
		
		mDimmed = dimming;
		
		// Send a broadcast somehow...
	}

	@SuppressLint("NewApi")
	public static void sendBroadcast(Context context, Intent intent) {
		int currentapiVersion = android.os.Build.VERSION.SDK_INT;
		if (currentapiVersion >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
			context.sendBroadcastAsUser(intent, 
					(UserHandle) XposedHelpers.getStaticObjectField(UserHandle.class, "CURRENT"));
		} else {
			context.sendBroadcast(intent);
		}
	}
}
