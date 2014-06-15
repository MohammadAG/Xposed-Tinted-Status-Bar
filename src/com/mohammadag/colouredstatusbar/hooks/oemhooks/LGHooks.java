package com.mohammadag.colouredstatusbar.hooks.oemhooks;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getObjectField;

import java.util.Locale;

import com.mohammadag.colouredstatusbar.StatusBarTintApi;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.view.View;
import android.widget.ImageView;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.XposedHelpers.ClassNotFoundError;

public class LGHooks {
	private static final int IME_ACTIVE = 0x1;
	private static boolean mWasKeyboardUp = false;
	
	public static void doImeHook(){
		if (!isLGKitKatDevice())
			return;
		
		try {
			Class<?> inputMethodMgrClss = findClass("com.android.server.InputMethodManagerService",null);
			findAndHookMethod(inputMethodMgrClss,"setImeWindowStatus",IBinder.class,int.class,int.class, new XC_MethodHook() {
				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					int vis = (Integer) param.args[1];
					Context mContext = (Context) getObjectField(param.thisObject, "mContext"); 
					boolean isKeyboardUp = (vis & IME_ACTIVE) != 0 ;
					if (isKeyboardUp != mWasKeyboardUp && mContext != null) {
						Intent intent = new Intent(StatusBarTintApi.INTENT_CHANGE_COLOR_NAME);
						intent.putExtra("keyboard_up", isKeyboardUp ? 1 : 0);
						intent.putExtra("time", System.currentTimeMillis());
						mContext.sendBroadcast(intent);
					}
					mWasKeyboardUp = isKeyboardUp;
				}
			});
		}catch (ClassNotFoundError e) {
			//DO NOTHING
		}
	}
	
	public static void doHook(ClassLoader classLoader) {
		if (!isLGKitKatDevice())
			return;
		
		hookStatusBar(classLoader);
		hookNavigationBar(classLoader);
	}

	private static void hookStatusBar(ClassLoader classLoader) {
		// REMOVE THE BACKGROUND ONLY
		try {
			Class<?> StatusBarBackGroundClss = findClass("com.lge.systemui.StatusBarBackground", classLoader);
			findAndHookMethod(StatusBarBackGroundClss, "applyMode", int.class, boolean.class, new XC_MethodHook() {
				@SuppressLint("NewApi")
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					int i = (Integer) param.args[0];
					if (i != 1 && i != 2) {
						XposedHelpers.setIntField(param.thisObject, "mMode", i);
						ImageView imageView = (ImageView) param.thisObject;
						imageView.setBackground(null);
						imageView.setVisibility(View.VISIBLE);
						param.setResult(null);
					}
				}
			});
		} catch (ClassNotFoundError e) {
			XposedBridge.log("TintedStatusBar: LG StatusBarBackground has not been found...skipping");
		}
	}

	private static void hookNavigationBar(ClassLoader classLoader) {
		Class<?> NavigationBackGroundClss = null;
		try {
			NavigationBackGroundClss = XposedHelpers.findClass("com.lge.systemui.navigationbar.NavigationBarBackground", classLoader);
		} catch (ClassNotFoundError e) {
			XposedBridge.log("TintedStatusBar: LG NavigationBarBackground has not been found...");
		}
		if (NavigationBackGroundClss == null) {
			try {
				NavigationBackGroundClss = XposedHelpers.findClass("com.lge.navigationbar.NavigationBarBackground", classLoader);
			} catch (ClassNotFoundError e) {
				XposedBridge.log("TintedStatusBar: G3 NavigationBarBackground has not been found...skipping");
			}
		}
		if (NavigationBackGroundClss != null) { 
			findAndHookMethod(NavigationBackGroundClss, "updateThemeResource", new XC_MethodHook() {
				@SuppressLint("NewApi")
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					ImageView imageView = (ImageView) param.thisObject;
					imageView.setBackground(null);
					param.setResult(null);
				}
			});
		}
	} 
	
	private static boolean isLGKitKatDevice(){
		return (android.os.Build.BRAND.toLowerCase(Locale.getDefault()).contains("lge") && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT);
	}
}
