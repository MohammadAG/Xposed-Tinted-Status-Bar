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
		findAndHookMethod(Window.class, "makeActive", new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				Window window = (Window) param.thisObject;
				if ((window.getAttributes().flags & LayoutParams.FLAG_DIM_BEHIND) == LayoutParams.FLAG_DIM_BEHIND) {
					XposedHelpers.setAdditionalInstanceField(window, "isDialog", true);
					Intent intent = new Intent(INTENT_DIM_CHANGED);
					intent.putExtra(KEY_DIM_AMOUNT, window.getAttributes().dimAmount);
					window.getContext().sendBroadcast(intent);
				}
			}
		});

		findAndHookMethod(Window.class, "destroy", new XC_MethodHook() {
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				Window window = (Window) param.thisObject;
				boolean isDialog = (Boolean) XposedHelpers.getAdditionalInstanceField(window, "isDialog");
				if (isDialog) {
					Intent intent = new Intent(INTENT_DIM_CHANGED);
					intent.putExtra(KEY_DIM_AMOUNT, 0);
					window.getContext().sendBroadcast(intent);
				}
			};
		});
	}
}
