package com.mohammadag.colouredstatusbar.hooks;

import android.widget.ImageView;
import android.widget.TextView;

import com.mohammadag.colouredstatusbar.ColourChangerMod;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.XposedHelpers.ClassNotFoundError;

public class BatteryHooks {
	private ColourChangerMod mInstance;

	public BatteryHooks(ColourChangerMod instance, ClassLoader classLoader) {
		mInstance = instance;
		String className = "com.android.systemui.statusbar.policy.BatteryController";
		String addIconMethodName = "addIconView";
		String addLabelMethodName = "addLabelView";
		try {
			Class<?> BatteryController = XposedHelpers.findClass(className, classLoader);

			try {
				XposedHelpers.findAndHookMethod(BatteryController, addIconMethodName, ImageView.class,
						new XC_MethodHook() {
					@Override
					protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
						mInstance.addSystemIconView((ImageView) param.args[0]);
					}
				});
			} catch (NoSuchMethodError e) {
				XposedBridge.log("Not hooking method " + className + "." + addIconMethodName);
			}

			try {
				XposedHelpers.findAndHookMethod(BatteryController, addLabelMethodName, TextView.class,
						new XC_MethodHook() {
					@Override
					protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
						mInstance.addTextLabel((TextView) param.args[0]);
					}
				});
			} catch (NoSuchMethodError e) {
				XposedBridge.log("Not hooking method " + className + "." + addLabelMethodName);
			}

		} catch (ClassNotFoundError e) {
			// Really shouldn't happen, but we can't afford a crash here.
			XposedBridge.log("Not hooking class: " + className);
		}
	}
}
