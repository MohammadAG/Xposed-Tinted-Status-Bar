package com.mohammadag.colouredstatusbar.hooks;

import android.view.View;

import com.mohammadag.colouredstatusbar.ColourChangerMod;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

public class NavigationBarHook {
	private static final int NAVIGATION_HINT_BACK_ALT;
	private static final int STATUS_BAR_DISABLE_RECENT;
	private ColourChangerMod mInstance;
	
	static {
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
			NAVIGATION_HINT_BACK_ALT = 1 << 0;
			STATUS_BAR_DISABLE_RECENT = 0x01000000;
		} else {
			NAVIGATION_HINT_BACK_ALT = 1 << 3;
			STATUS_BAR_DISABLE_RECENT = 0x01000000;
		}
	}

	public NavigationBarHook(ColourChangerMod instance, ClassLoader classLoader) {
		mInstance = instance;
		/* Thanks to GravityBox, big parts of this are from it */
		findAndHookMethod("com.android.systemui.statusbar.phone.NavigationBarView",
				classLoader, "setNavigationIconHints", int.class, boolean.class,
				new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				View mNavigationBarView = (View) param.thisObject;
				final int iconHints = XposedHelpers.getIntField(mNavigationBarView, "mNavigationIconHints");
				final int disabledFlags = XposedHelpers.getIntField(mNavigationBarView, "mDisabledFlags");
				boolean keyboardUp = !((disabledFlags & STATUS_BAR_DISABLE_RECENT) != 0) && 
						(iconHints & NAVIGATION_HINT_BACK_ALT) != 0;

				mInstance.onKeyboardVisible(keyboardUp);
			}
		});
	}

}
