package com.mohammadag.colouredstatusbar.hooks.oemhooks;

import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getObjectField;

import java.lang.reflect.Method;
import java.util.Locale;

import android.view.View;

import com.mohammadag.colouredstatusbar.ColourChangerMod;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.XposedHelpers.ClassNotFoundError;

public class SamsungHook extends XC_MethodHook {
	private ColourChangerMod mInstance = null;

	public static void doHook(ColourChangerMod instance, ClassLoader loader) {
		if (!android.os.Build.MANUFACTURER.toLowerCase(Locale.getDefault()).contains("samsung"))
			return;

		Class<?> PhoneStatusBar = null;
		try {
			PhoneStatusBar = findClass("com.android.systemui.statusbar.phone.PhoneStatusBar", loader);
		} catch (ClassNotFoundError e) {
			// Bigger problems to worry about if this isn't found.
		}
		try {
			if (PhoneStatusBar != null) {
				Method m = XposedHelpers.findMethodExact(PhoneStatusBar, "transparentizeStatusBar", int.class);
				XposedBridge.hookMethod(m, new SamsungHook(instance));
			}
		} catch (NoSuchMethodError e) {
			// Not an S4
		}
	}

	private SamsungHook(ColourChangerMod instance) {
		mInstance = instance;
	}

	@Override
	protected void afterHookedMethod(MethodHookParam param) throws Throwable {
		// This is casted internally from a boolean to an int, not sure why,
		// could be a Handler thing.
		int isTransparent = (Integer) param.args[0];

		if (isTransparent == 0) {
			View statusBarView = (View) getObjectField(param.thisObject, "mStatusBarView");
			statusBarView.setBackgroundColor(mInstance.getLastStatusBarTint());
		} else if (isTransparent == 1) {

		}
	}
}
