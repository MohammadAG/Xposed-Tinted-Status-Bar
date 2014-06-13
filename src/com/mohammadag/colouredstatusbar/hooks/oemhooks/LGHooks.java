package com.mohammadag.colouredstatusbar.hooks.oemhooks;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;

import java.util.Locale;

import android.annotation.SuppressLint;
import android.view.View;
import android.widget.ImageView;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.XposedHelpers.ClassNotFoundError;

public class LGHooks {
	public static void doHook(ClassLoader classLoader) {
		if (!android.os.Build.BRAND.toLowerCase(Locale.getDefault()).contains("lge"))
			return;

		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
			hookStatusBar(classLoader);
			hookNavigationBar(classLoader);
		}
	}

	private static void hookStatusBar(ClassLoader classLoader) {
		// REMOVE THE BACKGROUND ONLY
		try {
			Class<?> StatusBarBackGroundClss = findClass("com.lge.systemui.StatusBarBackground", classLoader);
			findAndHookMethod(StatusBarBackGroundClss, "applyMode", int.class, boolean.class, new XC_MethodHook() {
				@SuppressLint("NewApi")
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					int i = (Integer) param.args[0];
					if (i != 1 && i != 2) {
						XposedHelpers.setIntField(param.thisObject, "mMode", i);
						ImageView imageView = (ImageView) param.thisObject;
						imageView.setBackground(null);
						imageView.setVisibility(View.VISIBLE);
						param.setResult(null);
					}
				}
			});
		} catch (ClassNotFoundError e) {
			XposedBridge.log("TintedStatusBar: LG StatusBarBackground has not been found...skipping");
		}
	}

	private static void hookNavigationBar(ClassLoader classLoader) {
		try {
			Class<?> NavigationBackGroundClss = XposedHelpers.findClass("com.lge.systemui.navigationbar.NavigationBarBackground", classLoader);
			findAndHookMethod(NavigationBackGroundClss, "updateThemeResource", new XC_MethodHook() {
				@SuppressLint("NewApi")
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					ImageView imageView = (ImageView) param.thisObject;
					imageView.setBackground(null);
					param.setResult(null);
				}
			});

		} catch (ClassNotFoundError e) {
			XposedBridge.log("TintedStatusBar: LG NavigationBarBackground has not been found...skipping");
		}
	} 
}
