package com.mohammadag.colouredstatusbar.hooks;

import static de.robv.android.xposed.XposedHelpers.findClass;
import android.content.res.Resources;
import android.view.View;

import com.mohammadag.colouredstatusbar.ColourChangerMod;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

public class KitKatBatteryHook {
	private ColourChangerMod mInstance;

	public KitKatBatteryHook(ColourChangerMod instance, ClassLoader classLoader) {
		mInstance = instance;

		Class<?> BatteryMeterView = findClass("com.android.systemui.BatteryMeterView", classLoader);
		XposedBridge.hookAllConstructors(BatteryMeterView, new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				if (param.args.length != 3)
					return;

				View batteryView = (View) param.thisObject;
				int id = batteryView.getId();
				Resources res = batteryView.getResources();

				if (id != res.getIdentifier("battery", "id", "com.android.systemui"))
					return;

				mInstance.setKitKatBatteryView(batteryView);
			}
		});
	}
}
