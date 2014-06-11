package com.mohammadag.colouredstatusbar.hooks.oemhooks;

import java.util.Locale;

import android.view.View;
import android.widget.RelativeLayout;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;

public class HtcTransparencyHook {
	private static final int MODE_OPAQUE = 0;
	private static final int MODE_TRANSLUCENT = 2;

	protected static void transitionTo(Object thisObject, int mode, boolean animate) {
		int mMode = XposedHelpers.getIntField(thisObject, "mMode");
		if (mMode == mode)
			return;

		int oldMode = mMode;
		XposedHelpers.setIntField(thisObject, "mMode", mode);
		XposedHelpers.callMethod(thisObject, "onTransition", oldMode, mode, animate);
	}

	public static void doHook(ClassLoader classLoader) {
		if (!android.os.Build.MANUFACTURER.toLowerCase(Locale.getDefault()).contains("htc"))
			return;

		try {
			XposedHelpers.findAndHookMethod("com.android.systemui.statusbar.phone.PhoneStatusBar", classLoader,
					"setStatusBarBackground", int.class, XC_MethodReplacement.DO_NOTHING);
		} catch (Throwable t) {
			de.robv.android.xposed.XposedBridge.log("Failed to do HTC-specific hook: " + t.getMessage());
		}

		/*
		 * Sense 6 decided to replace the gradient with a custom opaque view, luckily
		 * it's not that complex, but Xposed (or Java reflection to be precise) can't
		 * be used to invoke the superclass's overriden method, so we turn to our own ways.
		 */
		try {
			XposedHelpers.findAndHookMethod("com.android.systemui.statusbar.phone.PhoneStatusBarTransitions", classLoader,
					"transitionTo", int.class, boolean.class, new XC_MethodReplacement() {
				@Override
				protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
					int mode = (Integer) param.args[0];
					boolean animate = (Boolean) param.args[1];

					transitionTo(param.thisObject, mode, animate);
					return null;
				}
			});
		} catch (Throwable t) {

		}

		try {
			XposedHelpers.findAndHookMethod("com.android.systemui.statusbar.phone.NavigationBarTransitions", classLoader,
					"transitionTo", int.class, boolean.class, new XC_MethodReplacement() {
				@Override
				protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
					int mode = (Integer) param.args[0];
					boolean animate = (Boolean) param.args[1];
					boolean mVertical = XposedHelpers.getBooleanField(param.thisObject, "mVertical");

					XposedHelpers.setIntField(param.thisObject, "mRequestedMode", mode);

					if (mVertical && mode == MODE_TRANSLUCENT) {
						mode = MODE_OPAQUE;
					}

					transitionTo(param.thisObject, mode, animate);
					return null;
				}
			});
		} catch (Throwable t) {

		}
	}

	public static void doBlinkFeedHooks(ClassLoader classLoader) {
		XposedHelpers.findAndHookMethod("com.htc.launcher.hotseat.Hotseat", classLoader,
				"initViews", new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				View view = (View) XposedHelpers.getObjectField(param.thisObject, "m_BackgroundGradient");
				view.setVisibility(View.GONE);
			}
		});
	}

	public static void doLockscreenHooks(ClassLoader classLoader) {
		XposedHelpers.findAndHookMethod("com.htc.lockscreen.ui.footer.ButtonFooter", classLoader,
				"init", new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				RelativeLayout layout = (RelativeLayout) param.thisObject;
				layout.getChildAt(0).setVisibility(View.INVISIBLE);
			}
		});
	}
}
