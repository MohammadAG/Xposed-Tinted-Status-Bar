package com.mohammadag.colouredstatusbar.hooks;

import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.getStaticIntField;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.XModuleResources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.mohammadag.colouredstatusbar.R;
import com.mohammadag.colouredstatusbar.SettingsHelper;
import com.mohammadag.colouredstatusbar.SettingsHelper.Tint;
import com.mohammadag.colouredstatusbar.drawables.IgnoredColorDrawable;
import com.mohammadag.colouredstatusbar.SettingsKeys;
import com.mohammadag.colouredstatusbar.StatusBarTintApi;
import com.mohammadag.colouredstatusbar.Utils;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class ActivityOnResumeHook extends XC_MethodHook {
	private SettingsHelper mSettingsHelper;
	private XModuleResources mResources;

	/* Floating Window Intent ID */
	public static final int FLAG_FLOATING_WINDOW = 0x00002000;

	private static final String KK_TRANSPARENT_COLOR_STRING = "#66000000";
	private static final int KITKAT_TRANSPARENT_COLOR = Color.parseColor(KK_TRANSPARENT_COLOR_STRING);

	public void log(String text) {
		if (mSettingsHelper.isDebugMode())
			XposedBridge.log("TintedStatusBar: " + text);
	}

	public ActivityOnResumeHook(SettingsHelper helper, XModuleResources resources) {
		mSettingsHelper = helper;
		mResources = resources;
	}

	@Override
	protected void afterHookedMethod(MethodHookParam param) throws Throwable {
		Activity activity = (Activity) param.thisObject;
		String packageName = activity.getPackageName();
		String activityName = activity.getLocalClassName();
		Intent activityIntent = activity.getIntent();

		mSettingsHelper.reload();

		if (mSettingsHelper.getBoolean(SettingsKeys.TOAST_ACTIVITY_NAMES, false)) {
			String tosatText = mResources.getString(R.string.toast_text_package_name, packageName);
			tosatText += "\n";
			tosatText += mResources.getString(R.string.toast_text_activity_name, activityName);
			Toast.makeText(activity, tosatText, Toast.LENGTH_SHORT).show();
		}

		if (!mSettingsHelper.isEnabled(packageName, activityName))
			return;

		if (activityIntent != null
				&& (activityIntent.getFlags() & FLAG_FLOATING_WINDOW) == FLAG_FLOATING_WINDOW)
			return;

		// From Xposed SwipeBack by PeterCxy
		// https://github.com/LOSP/SwipeBack/blob/master/src/us/shandian/mod/swipeback/hook/ModSwipeBack.java
		int isFloating = getStaticIntField(findClass("com.android.internal.R.styleable", null), "Window_windowIsFloating");
		if (activity.getWindow().getWindowStyle().getBoolean(isFloating, false))
			return;

		if (mSettingsHelper.getBoolean(SettingsKeys.ALLOW_API_CHANGES, true)) {
			PackageManager pm = activity.getPackageManager();
			ApplicationInfo info = pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA);
			if (info.metaData != null) {
				Bundle metadata = info.metaData;
				if (metadata.containsKey(StatusBarTintApi.METADATA_OVERRIDE_COLORS)) {
					return;
				}
			}
		}

		String statusBarTint = mSettingsHelper.getTintColor(packageName, activityName, true);
		String iconTint = mSettingsHelper.getIconColors(packageName, activityName, true);

		String navigationBarTint = mSettingsHelper.getNavigationBarTint(packageName, activityName, false);
		String navBarIconTint = mSettingsHelper.getNavigationBarIconTint(packageName, activityName, false);

		boolean overridingStatusBar = false;
		boolean overridingNavBar = false;
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT
				&& mSettingsHelper.shouldRespectKitKatApi()) {
			int flags = activity.getWindow().getAttributes().flags;
			if ((flags & WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
					== WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS) {
				log("Activity has status bar transclucency, overriding color to 66000000");
				statusBarTint = KK_TRANSPARENT_COLOR_STRING;
				overridingStatusBar = true;
			}

			if ((flags & WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
					== WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION) {
				log("Activity has nav bar transclucency, overriding color to 66000000");
				navigationBarTint = KK_TRANSPARENT_COLOR_STRING;
				overridingNavBar = true;
			}
		}

		int navigationBarTintColor = 0;
		int navigationBarIconTintColor = 0;

		try {
			navigationBarTintColor = Color.parseColor(Utils.addHashIfNeeded(navigationBarTint));
		} catch (Throwable t) {
		}

		try {
			navigationBarIconTintColor = Color.parseColor(Utils.addHashIfNeeded(navBarIconTint));
		} catch (Throwable t) {
		}

		int color = 0;
		int actionBarTextColor = -2;
		boolean colorHandled = false;

		if (Utils.hasActionBar() && !overridingStatusBar) {
			ActionBar actionBar = activity.getActionBar();
			if (actionBar != null) {
				// If it's not showing, we shouldn't detect it.
				if (actionBar.isShowing()) {
					FrameLayout container = (FrameLayout) XposedHelpers.getObjectField(actionBar, "mContainerView");
					if (container != null) {
						Drawable backgroundDrawable = (Drawable) XposedHelpers.getObjectField(container, "mBackground");
						if (backgroundDrawable != null) {
							try {
								color = Utils.getMainColorFromActionBarDrawable(backgroundDrawable);
								colorHandled = true;
								if (!mSettingsHelper.shouldAlwaysReverseTint()
										&& mSettingsHelper.shouldReverseTintAbColor(packageName)) {
									actionBar.setBackgroundDrawable(new IgnoredColorDrawable(color));
								}
							} catch (IllegalArgumentException e) {
							}
							container.invalidate();
						}

						try {
							TextView mTitleView = (TextView) getObjectField(
									getObjectField(container, "mActionBarView"), "mTitleView");
							if (mTitleView != null) {
								if (mTitleView.getVisibility() == View.VISIBLE) {
									actionBarTextColor = mTitleView.getCurrentTextColor();
								}
							}
						} catch (Throwable t) {

						}
					}
				}
			}
		}

		int statusBarTintColor = color;
		int iconTintColor;

		if (statusBarTint != null) {
			try {
				statusBarTintColor = Color.parseColor(statusBarTint);
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			}
		}

		int defaultNormal = mSettingsHelper.getDefaultTint(Tint.ICON);
		int invertedIconTint = mSettingsHelper.getDefaultTint(Tint.ICON_INVERTED);

		if (iconTint == null) {
			if (actionBarTextColor != -2) {
				iconTintColor = actionBarTextColor;
			} else {
				iconTintColor = Utils.getIconColorForColor(statusBarTintColor,
						defaultNormal, invertedIconTint, mSettingsHelper.getHsvMax());
			}
		} else {
			iconTintColor = Color.parseColor(iconTint);
		}

		Intent intent = new Intent(StatusBarTintApi.INTENT_CHANGE_COLOR_NAME);

		if (statusBarTint != null)
			intent.putExtra(StatusBarTintApi.KEY_STATUS_BAR_TINT, statusBarTintColor);

		if (iconTint != null)
			intent.putExtra(StatusBarTintApi.KEY_STATUS_BAR_ICON_TINT, iconTintColor);

		if (colorHandled == true) {
			if (!intent.hasExtra(StatusBarTintApi.KEY_STATUS_BAR_TINT))
				intent.putExtra(StatusBarTintApi.KEY_STATUS_BAR_TINT, overridingStatusBar ? KITKAT_TRANSPARENT_COLOR : color);
			if (!intent.hasExtra(StatusBarTintApi.KEY_STATUS_BAR_ICON_TINT))
				intent.putExtra(StatusBarTintApi.KEY_STATUS_BAR_ICON_TINT, overridingStatusBar ? Color.WHITE : iconTintColor);
		}

		/* We failed to get a colour, fall back to the defaults */
		if (!intent.hasExtra(StatusBarTintApi.KEY_STATUS_BAR_TINT))
			intent.putExtra(StatusBarTintApi.KEY_STATUS_BAR_TINT, overridingStatusBar ? KITKAT_TRANSPARENT_COLOR : mSettingsHelper.getDefaultTint(Tint.STATUS_BAR));
		if (!intent.hasExtra(StatusBarTintApi.KEY_STATUS_BAR_ICON_TINT))
			intent.putExtra(StatusBarTintApi.KEY_STATUS_BAR_ICON_TINT, overridingStatusBar ? Color.WHITE : defaultNormal);

		intent.putExtra(StatusBarTintApi.KEY_NAVIGATION_BAR_TINT, overridingNavBar ? KITKAT_TRANSPARENT_COLOR : navigationBarTintColor);
		intent.putExtra(StatusBarTintApi.KEY_NAVIGATION_BAR_ICON_TINT, overridingNavBar ? Color.WHITE : navigationBarIconTintColor);

		intent.putExtra("time", System.currentTimeMillis());
		intent.putExtra("link_panels", mSettingsHelper.shouldLinkPanels(packageName, null));

		if (mSettingsHelper.shouldAlwaysReverseTint() && mSettingsHelper.shouldReverseTintAbColor(packageName)) {
			ActionBar actionBar = activity.getActionBar();
			if (actionBar != null && actionBar.isShowing()) {
				// Reverse tint
				actionBar.setBackgroundDrawable(
						new IgnoredColorDrawable(intent.getIntExtra(
								StatusBarTintApi.KEY_STATUS_BAR_TINT, -1)));
			}
		}

		activity.sendBroadcast(intent);
	}
}
