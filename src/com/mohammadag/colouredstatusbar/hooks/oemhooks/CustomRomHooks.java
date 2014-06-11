package com.mohammadag.colouredstatusbar.hooks.oemhooks;

import android.widget.TextView;

import com.mohammadag.colouredstatusbar.ColourChangerMod;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.XposedHelpers.ClassNotFoundError;

public class CustomRomHooks {
	public static void doHook(ClassLoader loader, final ColourChangerMod instance) {
		Class<?> Traffic = null;

		try {
			Traffic = XposedHelpers.findClass("com.android.systemui.statusbar.policy.NetworkTraffic", loader);
		} catch (ClassNotFoundError e) {
			try {
				Traffic = XposedHelpers.findClass("com.android.systemui.statusbar.policy.Traffic", loader);
			} catch (ClassNotFoundError e1) { }
		}

		if (Traffic != null) {
			XposedBridge.hookAllConstructors(Traffic, new XC_MethodHook() {
				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					if (param.args != null && param.args.length == 3) {
						if (param.thisObject instanceof TextView)
							instance.addTextLabel((TextView) param.thisObject);
					}
				}
			});
		}
	}
}
