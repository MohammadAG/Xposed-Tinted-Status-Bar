package com.mohammadag.colouredstatusbar;

import de.robv.android.xposed.XposedHelpers;

public final class MiuiV5Support {
	public static final boolean IS_MIUIV5;
	
	static {
		Class<?> SystemProperties = XposedHelpers.findClass("android.os.SystemProperties", null);

		String miuiversion = (String) XposedHelpers.callStaticMethod(SystemProperties,
				"get", "ro.miui.ui.version.name");
		if (miuiversion.equalsIgnoreCase("V5")) {
			IS_MIUIV5 = true;
		} else {
			IS_MIUIV5 = false;
		}
	}
}
