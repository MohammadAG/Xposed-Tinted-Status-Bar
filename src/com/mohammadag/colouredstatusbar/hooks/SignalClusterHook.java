package com.mohammadag.colouredstatusbar.hooks;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

import com.mohammadag.colouredstatusbar.ColourChangerMod;
import com.mohammadag.colouredstatusbar.Common;

import android.widget.ImageView;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.XposedHelpers.ClassNotFoundError;

public class SignalClusterHook {
	private ColourChangerMod mInstance;
	private XC_MethodHook mSignalClusterHook = new XC_MethodHook() {
		@Override
		protected void afterHookedMethod(MethodHookParam param) throws Throwable {
			for (String name : Common.SIGNAL_CLUSTER_ICON_NAMES) {
				try {
					ImageView view = (ImageView) XposedHelpers.getObjectField(param.thisObject, name);
					mInstance.addSystemIconView(view);
				} catch (NoSuchFieldError e) {
					XposedBridge.log("Couldn't find field " + name + "in class " + param.method.getClass().getName());
				}
			}
		}
	};
	
	public SignalClusterHook(ColourChangerMod instance, ClassLoader classLoader) {
		mInstance = instance;
		doHooks(classLoader);
	}

	private void doHooks(ClassLoader classLoader) {
		String className = "com.android.systemui.statusbar.SignalClusterView";
		String methodName = "onAttachedToWindow";
		try {
			Class<?> SignalClusterView = XposedHelpers.findClass(className, classLoader);

			try {
				findAndHookMethod(SignalClusterView, methodName, mSignalClusterHook);
			} catch (NoSuchMethodError e) {
				XposedBridge.log("Not hooking method " + className + "." + methodName);
			}
		} catch (ClassNotFoundError e) {
			// Really shouldn't happen, but we can't afford a crash here.
			XposedBridge.log("Not hooking class: " + className);
		}

		/* HTC Specific hook */
		if (!android.os.Build.MANUFACTURER.toLowerCase().contains("htc"))
			return;

		try {
			Class<?> HTCClusterView =
					XposedHelpers.findClass("com.android.systemui.statusbar.HtcGenericSignalClusterView", classLoader);

			findAndHookMethod(HTCClusterView, methodName, mSignalClusterHook);
		} catch (Throwable t) {}
	}
}
