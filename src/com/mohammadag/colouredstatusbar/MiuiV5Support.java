package com.mohammadag.colouredstatusbar;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import de.robv.android.xposed.XposedBridge;

import android.os.SystemProperties;
import android.util.Log;

public final class MiuiV5Support {
	public static final boolean IS_MIUIV5;
	
	static {
		String miuiversion = SystemProperties.get("ro.miui.ui.version.name");
		if (miuiversion.equalsIgnoreCase("V5")) {
			IS_MIUIV5 = true;
		} else {
			IS_MIUIV5 = false;
		}

		XposedBridge.log("Is miui v5 system? " + IS_MIUIV5);
	}
}
