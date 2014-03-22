package com.mohammadag.colouredstatusbar.hooks;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import android.app.ActionBar;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.TextView;

import com.mohammadag.colouredstatusbar.ColourChangerMod;
import com.mohammadag.colouredstatusbar.Common;
import com.mohammadag.colouredstatusbar.SettingsHelper;
import com.mohammadag.colouredstatusbar.SettingsHelper.Tint;
import com.mohammadag.colouredstatusbar.StatusBarTintApi;
import com.mohammadag.colouredstatusbar.Utils;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers.ClassNotFoundError;

public class ActionBarHooks {
	private SettingsHelper mSettingsHelper;

	public ActionBarHooks(SettingsHelper settingsHelper) {
		mSettingsHelper = settingsHelper;

		try {
			Class<?> ActionBarImpl = findClass("com.android.internal.app.ActionBarImpl", null);
			findAndHookMethod(ActionBarImpl, "setBackgroundDrawable", Drawable.class, new XC_MethodHook() {
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					ActionBar actionBar = (ActionBar) param.thisObject;
					Drawable drawable = (Drawable) param.args[0];
					if (drawable != null) {
						int color = Utils.getMainColorFromActionBarDrawable(drawable);
						int defaultNormal = mSettingsHelper.getDefaultTint(Tint.ICON);
						int invertedIconTint = mSettingsHelper.getDefaultTint(Tint.ICON_INVERTED);
						ColourChangerMod.sendColorChangeIntent(color, Utils.getIconColorForColor(color, defaultNormal,
								invertedIconTint, mSettingsHelper.getHsvMax()), actionBar.getThemedContext());
					}
				}
			});

			findAndHookMethod(ActionBarImpl, "hide", new XC_MethodHook() {
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					ActionBar actionBar = (ActionBar) param.thisObject;
					Intent intent = new Intent(Common.INTENT_CHANGE_COLOR_NAME);

					int statusBarTint = mSettingsHelper.getDefaultTint(Tint.STATUS_BAR);
					int defaultNormal = mSettingsHelper.getDefaultTint(Tint.ICON);
					int invertedIconTint = mSettingsHelper.getDefaultTint(Tint.ICON_INVERTED);

					if (!intent.hasExtra(StatusBarTintApi.KEY_STATUS_BAR_ICON_TINT))
						intent.putExtra(StatusBarTintApi.KEY_STATUS_BAR_ICON_TINT,
								mSettingsHelper.getDefaultTint(Tint.ICON));

					if (mSettingsHelper.shouldReactToActionBarVisibility())
						ColourChangerMod.sendColorChangeIntent(statusBarTint, Utils.getIconColorForColor(statusBarTint, defaultNormal,
								invertedIconTint, mSettingsHelper.getHsvMax()), actionBar.getThemedContext());
				};
			});

			findAndHookMethod(ActionBarImpl, "show", new XC_MethodHook() {
				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					ActionBar actionBar = (ActionBar) param.thisObject;
					Object actionBarContainer = getObjectField(actionBar, "mContainerView");
					int actionBarTextColor = -2;
					try {
						TextView mTitleView = (TextView) getObjectField(
								getObjectField(actionBarContainer, "mActionBarView"), "mTitleView");
						if (mTitleView != null) {
							if (mTitleView.getVisibility() == View.VISIBLE) {
								actionBarTextColor = mTitleView.getCurrentTextColor();
							}
						}
					} catch (Throwable t) {

					}
					Drawable drawable = (Drawable) getObjectField(actionBarContainer, "mBackground");
					if (drawable == null)
						return;

					int color = Utils.getMainColorFromActionBarDrawable(drawable);
					int defaultNormal = mSettingsHelper.getDefaultTint(Tint.ICON);
					int invertedIconTint = mSettingsHelper.getDefaultTint(Tint.ICON_INVERTED);
					int iconTint;

					if (actionBarTextColor != -2) {
						iconTint = actionBarTextColor;
					} else {
						iconTint = Utils.getIconColorForColor(color, defaultNormal,
								invertedIconTint, mSettingsHelper.getHsvMax());
					}

					if (mSettingsHelper.shouldReactToActionBarVisibility())
						ColourChangerMod.sendColorChangeIntent(color, iconTint, actionBar.getThemedContext());
				}
			});
		} catch (ClassNotFoundError e) {

		} catch (NoSuchMethodError e) {

		}
	}
}
