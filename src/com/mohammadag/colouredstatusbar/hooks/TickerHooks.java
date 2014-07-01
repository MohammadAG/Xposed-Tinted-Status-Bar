package com.mohammadag.colouredstatusbar.hooks;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.TextSwitcher;
import android.widget.TextView;

import com.mohammadag.colouredstatusbar.ColourChangerMod;
import com.mohammadag.colouredstatusbar.Common;
import com.mohammadag.colouredstatusbar.MiuiV5Support;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.XposedHelpers.ClassNotFoundError;

public class TickerHooks {
	private ColourChangerMod mInstance;

	public TickerHooks(ColourChangerMod instance, ClassLoader classLoader) {
		mInstance = instance;

		String className = "com.android.systemui.statusbar.phone.Ticker";
		/* API > 17 hint from GravityBox, thanks! */
		String notificationClassName = android.os.Build.VERSION.SDK_INT > 17 ?
				"android.service.notification.StatusBarNotification" :
					"com.android.internal.statusbar.StatusBarNotification";
		if (MiuiV5Support.IS_MIUIV5) 
			notificationClassName = "com.android.systemui.statusbar.ExpandedNotification";
		String addMethod = "addEntry";
		try {
			Class<?> Ticker = findClass(className, classLoader);
			Class<?> StatusBarNotification = findClass(notificationClassName, classLoader);

			XposedBridge.hookAllConstructors(Ticker, new XC_MethodHook() {
				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					setColorToAllTextSwitcherChildren(
							(TextSwitcher) XposedHelpers.getObjectField(param.thisObject, "mTextSwitcher"),
							mInstance.getColorForStatusIcons());
					setColorToAllImageSwitcherChildren(
							(ImageSwitcher) XposedHelpers.getObjectField(param.thisObject, "mIconSwitcher"),
							mInstance.getColorForStatusIcons());
				}
			});

			try {
				findAndHookMethod(Ticker, addMethod, StatusBarNotification, new XC_MethodHook() {
					@Override
					protected void afterHookedMethod(MethodHookParam param) throws Throwable {
						setColorToAllTextSwitcherChildren(
								(TextSwitcher) getObjectField(param.thisObject, "mTextSwitcher"),
								mInstance.getColorForStatusIcons());
						setColorToAllImageSwitcherChildren(
								(ImageSwitcher) XposedHelpers.getObjectField(param.thisObject, "mIconSwitcher"),
								mInstance.getColorForStatusIcons());
					};
				});
			} catch (NoSuchMethodError e) {
				mInstance.log("Not hooking method " + className + "." + addMethod);
			}
		} catch (ClassNotFoundError e) {
			mInstance.log("Not hooking class: " + className);
		}
	}

	private void setColorToAllTextSwitcherChildren(TextSwitcher switcher, int color) {
		if (color != 0) {
			for (int i = 0; i < switcher.getChildCount(); i++) {
				TextView view = (TextView) switcher.getChildAt(i);
				view.setTextColor(color);
				mInstance.addTextLabel(view);
			}
		}	
	}

	private void setColorToAllImageSwitcherChildren(ImageSwitcher switcher, int color) {
		if (color != 0) {
			for (int i = 0; i < switcher.getChildCount(); i++) {
				ImageView view = (ImageView) switcher.getChildAt(i);
				view.setColorFilter(color, mInstance.getSettingsHelper().getNotificationIconCfType());
				mInstance.addNotificationIconView(view);
			}
		}	
	}
}
