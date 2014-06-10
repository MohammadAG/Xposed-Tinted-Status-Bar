package com.mohammadag.colouredstatusbar.hooks;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;

import com.mohammadag.colouredstatusbar.ColourChangerMod;
import com.mohammadag.colouredstatusbar.StatusBarTintApi;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.XposedHelpers.ClassNotFoundError;

public class SViewHooks {
	private ColourChangerMod mInstance;

	public SViewHooks(ColourChangerMod instance, ClassLoader classLoader) {
		mInstance = instance;
		try{
			Class<?> SViewCoverManager = findClass("com.android.internal.policy.impl.sviewcover.SViewCoverManager", classLoader);

			findAndHookMethod(SViewCoverManager, "handleShow", new XC_MethodHook() {
				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					boolean isUiShowing = XposedHelpers.getBooleanField(param.thisObject, "mShowingCoverUI");
					if (!isUiShowing)
						return;		

					Context context = (Context) getObjectField(param.thisObject, "mContext");
					Intent intent = new Intent(StatusBarTintApi.INTENT_CHANGE_COLOR_NAME);
					intent.putExtra(StatusBarTintApi.KEY_STATUS_BAR_TINT, Color.BLACK);
					intent.putExtra(StatusBarTintApi.KEY_STATUS_BAR_ICON_TINT, Color.WHITE);
					intent.putExtra("time", System.currentTimeMillis());

					context.sendBroadcast(intent);
				}
			});

			findAndHookMethod(SViewCoverManager, "handleHide", new XC_MethodHook() {
				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					boolean isUiShowing = XposedHelpers.getBooleanField(param.thisObject, "mShowingCoverUI");
					if (isUiShowing)
						return;

					Context context = (Context) getObjectField(param.thisObject, "mContext");
					Intent intent = new Intent(StatusBarTintApi.INTENT_CHANGE_COLOR_NAME);
					intent.putExtra(StatusBarTintApi.KEY_STATUS_BAR_TINT, mInstance.getLastStatusBarTint());
					intent.putExtra(StatusBarTintApi.KEY_STATUS_BAR_ICON_TINT, mInstance.getLastIconTint());
					intent.putExtra("time", System.currentTimeMillis());

					context.sendBroadcast(intent);
				}
			});
		} catch (ClassNotFoundError e) {

		}
	}
}
