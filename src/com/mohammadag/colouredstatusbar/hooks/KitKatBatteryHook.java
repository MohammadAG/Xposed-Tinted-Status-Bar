package com.mohammadag.colouredstatusbar.hooks;

import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.ClassNotFoundError;

import android.content.res.Resources;
import android.view.View;

import com.mohammadag.colouredstatusbar.ColourChangerMod;
import com.mohammadag.colouredstatusbar.KitKatBattery;

import de.robv.android.xposed.XC_MethodHook;

public class KitKatBatteryHook {
	private ColourChangerMod mInstance;
	private static final String[] BATTERY_VIEWS_CLASSES = new String[]{
			"com.android.systemui.BatteryMeterView",
			"com.android.systemui.BatteryCircleMeterView",
			"com.android.systemui.BatteryPercentMeterView"
	};

	public KitKatBatteryHook(final ColourChangerMod instance, ClassLoader classLoader) {
		mInstance = instance;

		for (final String batteryViewClass : BATTERY_VIEWS_CLASSES) {
			try {
				Class<?> BatteryView = findClass(batteryViewClass, classLoader);
				findAndHookMethod(BatteryView, "onAttachedToWindow", new XC_MethodHook() {
					@Override
					protected void afterHookedMethod(MethodHookParam param) throws Throwable {
						View batteryView = (View) param.thisObject;
						int parentId = ((View) batteryView.getParent()).getId();
						Resources res = batteryView.getResources();
						if (parentId != res.getIdentifier("signal_battery_cluster", "id", "com.android.systemui"))
							return;

						int visibility = (Integer) callMethod(param.thisObject, "getVisibility");
						if (visibility == View.VISIBLE)
							mInstance.setKitKatBatteryView(new KitKatBattery(batteryView, batteryViewClass,
									instance.getSettingsHelper()));
					}
				});
			} catch (ClassNotFoundError ignored) {
			}
		}
	}
}
