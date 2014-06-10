package com.mohammadag.colouredstatusbar.hooks;

import static de.robv.android.xposed.XposedHelpers.findClass;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.view.View;

import com.mohammadag.colouredstatusbar.ColourChangerMod;
import com.mohammadag.colouredstatusbar.Common;
import com.mohammadag.colouredstatusbar.PackageNames;
import com.mohammadag.colouredstatusbar.StatusBarTintApi;
import com.mohammadag.colouredstatusbar.SettingsHelper.Tint;
import com.mohammadag.colouredstatusbar.Utils;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers.ClassNotFoundError;

public class StatusBarViewHook {
	private ColourChangerMod mInstance;

	public static final String INTENT_SAMSUNG_SVIEW_COVER = "com.samsung.cover.OPEN";
	public static final String KEY_SVIEW_COVER_OPENED = "coverOpen";

	public StatusBarViewHook(ColourChangerMod instance, ClassLoader classLoader) {
		mInstance = instance;
		try {
			Class<?> PhoneStatusBarView = findClass("com.android.systemui.statusbar.phone.PhoneStatusBarView", classLoader);

			XposedBridge.hookAllConstructors(PhoneStatusBarView, new XC_MethodHook() {
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					Context context = (Context) param.args[0];
					IntentFilter iF = new IntentFilter();
					iF.addAction(StatusBarTintApi.INTENT_CHANGE_COLOR_NAME);
					iF.addAction(Common.INTENT_RESET_ACTIONBAR_COLOR_NAME);
					iF.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY);
					context.registerReceiver(mInstance.getBroadcastReceiver(), iF);
					
					IntentFilter iF2 = new IntentFilter();
					iF2.addAction(INTENT_SAMSUNG_SVIEW_COVER);
					iF2.addAction(Common.INTENT_SETTINGS_UPDATED);
					context.registerReceiver(mInstance.getBroadcastReceiver(), iF2);
					
					IntentFilter lockscreenFilter = new IntentFilter();
					lockscreenFilter.addAction(Intent.ACTION_SCREEN_ON);
					lockscreenFilter.addAction(Intent.ACTION_SCREEN_OFF);
					context.registerReceiver(new BroadcastReceiver() {
						@SuppressLint("NewApi")
						@Override
						public void onReceive(Context context, Intent intent) {
							mInstance.getSettingsHelper().reload();

							if (Utils.isKeyguardLocked(context)) {
								String statusBarUserTint = mInstance.getSettingsHelper().getTintColor(
										PackageNames.LOCKSCREEN_STUB, null, true);

								String statusBarUserIconTint = mInstance.getSettingsHelper().getIconColors(
										PackageNames.LOCKSCREEN_STUB, null, true);

								String navBarUserTint = mInstance.getSettingsHelper().getNavigationBarTint(
										PackageNames.LOCKSCREEN_STUB, null, true);

								String navBarIconUserTint = mInstance.getSettingsHelper().getNavigationBarIconTint(
										PackageNames.LOCKSCREEN_STUB, null, true);

								int statusBarTint, statusBarIconTint, navBarTint, navBarIconTint;

								if (statusBarUserTint != null) {
									try {
										statusBarTint = Color.parseColor(statusBarUserTint);
									} catch (Throwable t) {
										statusBarTint = mInstance.getSettingsHelper().getDefaultTint(Tint.STATUS_BAR);
									}
								} else {
									statusBarTint = mInstance.getSettingsHelper().getDefaultTint(Tint.STATUS_BAR);
								}

								if (statusBarUserIconTint != null) {
									try {
										statusBarIconTint = Color.parseColor(statusBarUserIconTint);
									} catch (Throwable t) {
										statusBarIconTint = mInstance.getSettingsHelper().getDefaultTint(Tint.ICON);
									}
								} else {
									statusBarIconTint = mInstance.getSettingsHelper().getDefaultTint(Tint.ICON);
								}

								if (navBarUserTint != null) {
									try {
										navBarTint = Color.parseColor(navBarUserTint);
									} catch (Throwable t) {
										navBarTint = mInstance.getSettingsHelper().getDefaultTint(Tint.NAV_BAR);
									}
								} else {
									navBarTint = mInstance.getSettingsHelper().getDefaultTint(Tint.NAV_BAR);
								}

								if (navBarIconUserTint != null) {
									try {
										navBarIconTint = Color.parseColor(navBarIconUserTint);
									} catch (Throwable t) {
										navBarIconTint = mInstance.getSettingsHelper().getDefaultTint(Tint.NAV_BAR_ICON);
									}
								} else {
									navBarIconTint = mInstance.getSettingsHelper().getDefaultTint(Tint.NAV_BAR_ICON);
								}

								mInstance.setStatusBarTint(statusBarTint);
								mInstance.setStatusBarIconsTint(statusBarIconTint);
								mInstance.setNavigationBarTint(navBarTint);
								mInstance.setNavigationBarIconTint(navBarIconTint);
							}
						}

					}, lockscreenFilter);
				}

				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					mInstance.setStatusBarView((View) param.thisObject);
				}
			});
		} catch (ClassNotFoundError e) {

		}
	}
}
