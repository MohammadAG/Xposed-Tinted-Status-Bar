package com.mohammadag.colouredstatusbar.hooks;

import static de.robv.android.xposed.XposedHelpers.findClass;
import android.annotation.SuppressLint;
import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.view.View;

import com.mohammadag.colouredstatusbar.ColourChangerMod;
import com.mohammadag.colouredstatusbar.Common;
import com.mohammadag.colouredstatusbar.SettingsHelper.Tint;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers.ClassNotFoundError;

public class StatusBarViewHook {
	private ColourChangerMod mInstance;
	public StatusBarViewHook(ColourChangerMod instance, ClassLoader classLoader) {
		mInstance = instance;
		try {
			Class<?> PhoneStatusBarView = findClass("com.android.systemui.statusbar.phone.PhoneStatusBarView", classLoader);

			XposedBridge.hookAllConstructors(PhoneStatusBarView, new XC_MethodHook() {
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					Context context = (Context) param.args[0];
					IntentFilter iF = new IntentFilter();
					iF.addAction(Common.INTENT_CHANGE_COLOR_NAME);
					iF.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY);
					context.registerReceiver(mInstance.getBroadcastReceiver(), iF);
					
					IntentFilter iF2 = new IntentFilter();
					iF2.addAction(Common.INTENT_SAMSUNG_SVIEW_COVER);
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
							KeyguardManager kgm = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
							boolean keyguardLocked;
							
							if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
								keyguardLocked = kgm.isKeyguardLocked();
							} else {
								keyguardLocked = kgm.inKeyguardRestrictedInputMode();
							}
							
							if (keyguardLocked) {

								String statusBarUserTint = mInstance.getSettingsHelper().getTintColor(
										Common.PACKAGE_NAME_LOCKSCREEN_STUB, null, true);

								String statusBarUserIconTint = mInstance.getSettingsHelper().getIconColors(
										Common.PACKAGE_NAME_LOCKSCREEN_STUB, null, true);

								int statusBarTint;
								int statusBarIconTint;

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

								mInstance.setStatusBarTint(statusBarTint);
								mInstance.setStatusBarIconsTint(statusBarIconTint);
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
