package com.mohammadag.colouredstatusbar.hooks;

import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;

public class HtcTransparencyHook {
	public static void doHook(ClassLoader classLoader) {
		if (!android.os.Build.MANUFACTURER.toLowerCase().contains("htc"))
			return;

		XposedHelpers.findAndHookMethod("com.android.systemui.statusbar.phone.PhoneStatusBar", classLoader,
				"setStatusBarBackground", int.class, XC_MethodReplacement.DO_NOTHING);
	}
}
