package com.mohammadag.colouredstatusbar.hooks;

import android.widget.ImageView;

import com.mohammadag.colouredstatusbar.ColourChangerMod;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.XposedHelpers.ClassNotFoundError;

public class BluetoothControllerHook {
	private ColourChangerMod mInstance;

	public BluetoothControllerHook(ColourChangerMod instance, ClassLoader classLoader) {
		mInstance = instance;

		String className = "com.android.systemui.statusbar.policy.BluetoothController";
		String methodName = "addIconView";
		try {
			Class<?> BluetoothController = XposedHelpers.findClass(className, classLoader);

			XposedHelpers.findAndHookMethod(BluetoothController, methodName, ImageView.class, new XC_MethodHook() {
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					mInstance.addSystemIconView((ImageView) param.args[0]);
				}
			});
		} catch (ClassNotFoundError e) {
			mInstance.log("Not hooking class: " + className);
		} catch (NoSuchMethodError e) {
			mInstance.log("Not hooking method " + className + "." + methodName);
		}
	}
}
