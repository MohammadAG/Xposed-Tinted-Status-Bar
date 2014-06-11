package com.mohammadag.colouredstatusbar.hooks.oemhooks;

import static de.robv.android.xposed.XposedHelpers.getObjectField;
import android.view.View;

import com.mohammadag.colouredstatusbar.ColourChangerMod;

import de.robv.android.xposed.XC_MethodHook;

public class TouchWizTransparencyHook extends XC_MethodHook {
	private ColourChangerMod mInstance = null;

	public TouchWizTransparencyHook(ColourChangerMod instance) {
		mInstance = instance;
	}

	@Override
	protected void afterHookedMethod(MethodHookParam param) throws Throwable {
		// This is casted internally from a boolean to an int, not sure why,
		// could be a Handler thing.
		int isTransparent = (Integer) param.args[0];

		if (isTransparent == 0) {
			mInstance.setTouchWizTransparentStatusBar(false);

			View statusBarView = (View) getObjectField(param.thisObject, "mStatusBarView");
			statusBarView.setBackgroundColor(mInstance.getLastStatusBarTint());
		} else if (isTransparent == 1) {
			mInstance.setTouchWizTransparentStatusBar(true);
		}
	}
}
