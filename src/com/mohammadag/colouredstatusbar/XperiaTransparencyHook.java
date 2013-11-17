package com.mohammadag.colouredstatusbar;

import static de.robv.android.xposed.XposedHelpers.getObjectField;
import android.view.View;

import com.nineoldandroids.view.ViewHelper;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

/* I'm going to document my findings here as it seems to be easy to get lost
 * with these methods...
 * 
 * Whenever setSystemUiVisibility is called, swapViews is called on both
 * the status bar and the navigation bar.
 * It seems pretty messed up for Sony not to have included a boolean to
 * toggle this behaviour, as their code is quite organized like that.
 * 
 * Anyway. swapViews takes 4 arguments, here's the method as it would look
 * like if we had the source code:
 * private void swapViews(View layout, int outViewId, int inViewId, long duration)
 * 
 * layout is either mStatusBarWindow, or mNavigationBarView.
 * In our hook, we should do nothing when the layout is mNavigationBarView, since
 * we only handle the status bar in this mod for now. We might need to add this
 * in the future if we add nav bar tinting.
 * 
 * outViewId and inViewId are obtained with the method:
 * getBackgroundViewId(boolean bool1, boolean bool2, boolean bool3, boolean bool4)
 * 
 * Sadly, the parameter names seem to have been lost here, so I can't figure this one out.
 * TODO: Hook this method and find out what the parameter values are.
 * 
 * After this is done, the method isOpaque(int) called, which returns true/false based
 * on whether or not the id is equal to R.id.transparent_bg.
 * Here's a representation of the method:
 * 
 * private boolean isOpaque(int viewId) {
 *     return (viewId != R.id.transparent_bg) && (viewId != 0);
 * }
 * 
 * Here's what we do in the hook: We hook the method swapViews.
 * If the layout passed is mStatusBarWindow, we continue to check
 * what the passed outView and inView are, if the inView to be shown
 * is opaque, we back away, if it's not, we allow it to be set, since
 * this makes for better consistency with Sony's existing UI.
 */

public class XperiaTransparencyHook extends XC_MethodHook {
	@Override
	protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
		View layout = (View) param.args[0];
		View statusBarWindow = (View) getObjectField(param.thisObject, "mStatusBarWindow");

		if (layout.equals(statusBarWindow)) {
			int outViewId = (Integer) param.args[1];
			int inViewId = (Integer) param.args[2];

			View outView = layout.findViewById(outViewId);
			View inView = layout.findViewById(inViewId);

            ViewHelper.setAlpha(outView,0f);
            ViewHelper.setAlpha(inView,0f);

			boolean isOpaque = (Boolean) XposedHelpers.callMethod(param.thisObject, "isOpaque", inViewId);

			if (isOpaque)
				param.setResult(null);
		}
	}
}
