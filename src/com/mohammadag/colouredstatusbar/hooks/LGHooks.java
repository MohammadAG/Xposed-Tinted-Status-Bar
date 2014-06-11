package com.mohammadag.colouredstatusbar.hooks;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.XposedHelpers.ClassNotFoundError;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;

public class LGHooks {
	
	public static void doHook(ClassLoader classLoader) {
		if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT)
		{
			HookStatusBar(classLoader);
			HookNavigationBar(classLoader);
		}
	}
	
	private static void HookStatusBar(ClassLoader classLoader){
		//REMOVE THE BACKGROUND ONLY
		try{
			Class<?> statusBarBackGroundClss = findClass("com.lge.systemui.StatusBarBackground",classLoader);
			findAndHookMethod(statusBarBackGroundClss,"applyMode", int.class,boolean.class,new XC_MethodHook() {
				@Override
				protected void beforeHookedMethod(MethodHookParam param)throws Throwable {
					int i = (Integer) param.args[0];
					if(i != 1 && i != 2 ){
						XposedHelpers.setIntField(param.thisObject, "mMode", i);
					    XposedHelpers.callMethod(param.thisObject,"setBackground",(Object)null);
					    XposedHelpers.callMethod(param.thisObject,"setVisibility", 0);
						param.setResult(null);
					}
				}
			});
		}catch(ClassNotFoundError e){
			XposedBridge.log("LG StatusBarBackground has not been found...skipping");
		}
		
	}
	
	private static void HookNavigationBar(ClassLoader classLoader){
		try{
			Class<?> navigationBackGroundClss = XposedHelpers.findClass("com.lge.systemui.navigationbar.NavigationBarBackground",classLoader);
			findAndHookMethod(navigationBackGroundClss, "updateThemeResource", new XC_MethodHook() {
				@Override
				protected void beforeHookedMethod(MethodHookParam param)throws Throwable {
					XposedHelpers.callMethod(param.thisObject,"setBackground", (Object)null);
					param.setResult(null);
				}
			});

		}catch(ClassNotFoundError e){
			XposedBridge.log("LG NavigationBarBackground has not been found...skipping");
			return;
		}
	} 
	
}
