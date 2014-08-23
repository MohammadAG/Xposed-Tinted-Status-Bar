package com.mohammadag.colouredstatusbar.hooks;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import android.content.Intent;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

public class WindowDimHooks {
	public static final String INTENT_DIM_CHANGED = "com.mohammadag.colouredstatusbar.DIM_LAYER_CHANGED";
	public static final String KEY_DIM_AMOUNT = "dimAmount";

	public static void doHook() {
		findAndHookMethod("com.android.internal.policy.impl.PhoneWindow$DecorView", null,
				"onWindowFocusChanged", boolean.class, new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				Window window = (Window) XposedHelpers.getObjectField(param.thisObject, "this$0");
				if ((window.getAttributes().flags & LayoutParams.FLAG_DIM_BEHIND) == LayoutParams.FLAG_DIM_BEHIND) {
					boolean focused = (Boolean) param.args[0];
					Intent intent = new Intent(INTENT_DIM_CHANGED);
					intent.putExtra(KEY_DIM_AMOUNT, focused ? window.getAttributes().dimAmount : 0f);
					window.getContext().sendBroadcast(intent);
				}
			}
		});
	}
}
