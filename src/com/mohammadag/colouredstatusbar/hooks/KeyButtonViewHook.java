package com.mohammadag.colouredstatusbar.hooks;

import static de.robv.android.xposed.XposedHelpers.findClass;

import android.view.KeyEvent;
import android.view.MotionEvent;

import com.mohammadag.colouredstatusbar.ColourChangerMod;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

public class KeyButtonViewHook {
	public KeyButtonViewHook(final ColourChangerMod instance, ClassLoader classLoader) {
		try {
			Class<?> KeyButtonView = findClass("com.android.systemui.statusbar.policy.KeyButtonView",
					classLoader);

			XposedHelpers.findAndHookMethod(KeyButtonView, "sendEvent", int.class, int.class, new XC_MethodHook() {
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					int action = (Integer) param.args[0];
					int flags = (Integer) param.args[1];
					int mCode = XposedHelpers.getIntField(param.thisObject, "mCode");

					if (mCode == KeyEvent.KEYCODE_HOME && 
							action == MotionEvent.ACTION_UP && flags == 0) {
						instance.onHomeKeyPressed();
					}
				}
			});
		} catch (Throwable t) {
			instance.log("Failed to hook KeyButtonView: " + t.getMessage());
		}
	}
}
