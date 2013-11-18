package com.mohammadag.sakyGBport;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getObjectField;

import java.lang.reflect.Method;
import java.util.ArrayList;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextSwitcher;
import android.widget.TextView;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.XposedHelpers.ClassNotFoundError;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class ColourChangerMod implements IXposedHookLoadPackage, IXposedHookZygoteInit {
	private static FrameLayout mStatusBarView;
    private final String logTag = "TintedStatusBarGBport";
	private ArrayList<ImageView> mIconViews = new ArrayList<ImageView>();
	private ArrayList<TextView> mTextLabels = new ArrayList<TextView>();
	private static int mColorForStatusIcons = 0;
	private static SettingsHelper mSettingsHelper;

	private static int mLastTint = Color.parseColor("#" + Common.COLOR_A_SHADE_OF_GREY);
	private static int mLastIconTint = Color.WHITE;

	private static LinearLayout mStatusIcons = null;

	private int mLastSetColor;
	private static final boolean mClockAnimation = false;
	private static final boolean mAnimateStatusBarTintChange = true;

	/* Wokraround for Samsung UX */
	private static boolean mIsStatusBarNowTransparent = false;

	private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (Common.INTENT_CHANGE_COLOR_NAME.equals(intent.getAction())) {
				if (intent.hasExtra(StatusBarTintApi.KEY_STATUS_BAR_TINT)) {
					mLastTint = intent.getIntExtra(StatusBarTintApi.KEY_STATUS_BAR_TINT, -1);
					setStatusBarTint(mLastTint);
				}

				if (intent.hasExtra(StatusBarTintApi.KEY_STATUS_BAR_ICON_TINT)) {
					mLastIconTint = intent.getIntExtra(StatusBarTintApi.KEY_STATUS_BAR_ICON_TINT, -1);
					setStatusBarIconsTint(mLastIconTint);
				}
			} else if (Common.INTENT_SETTINGS_UPDATED.equals(intent.getAction())) {
				Log.d("Xposed", "ColouredStatusBar settings updated, reloading...");
				mSettingsHelper.reload();
			}
		}
	};

	@Override
	public void initZygote(StartupParam startupParam) throws Throwable {

        Log.d(logTag,"initZygote()");
        mStatusBarView = null;
		mSettingsHelper = new SettingsHelper(new XSharedPreferences(Common.PACKAGE_NAME, Common.PREFS));

		Class<?> ActivityClass = XposedHelpers.findClass("android.app.Activity", null);

		findAndHookMethod(ActivityClass, "performResume", new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {

				Activity activity = (Activity) param.thisObject;
				String packageName = activity.getPackageName();
				String activityName = activity.getLocalClassName();

				mSettingsHelper.reload();

				if (!mSettingsHelper.isEnabled(packageName, activityName))
					return;

				if (mSettingsHelper.getBoolean(Common.SETTINGS_KEY_ALLOW_API_CHANGES, true)) {
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

				Drawable backgroundDrawable = null;
				int color = 0;

                //TODO ActionBarImpl.
				/*ActionBar actionBar = activity.getActionBar();
				boolean colorHandled = false;
				if (actionBar != null) {
					// If it's not showing, we shouldn't detect it.
					if (actionBar.isShowing()) {
						Object container = XposedHelpers.getObjectField(actionBar, "mContainerView");
						if (container != null) {
							backgroundDrawable = (Drawable) XposedHelpers.getObjectField(container, "mBackground");
							if (backgroundDrawable != null) {
								try {
									color = Utils.getMainColorFromActionBarDrawable(backgroundDrawable);
									colorHandled = true;
								} catch (IllegalArgumentException e) {}
							}
						}
					}
				}*/

				int statusBarTintColor = color;
				int iconTintColor;

				if (statusBarTint != null) {
					try {
						statusBarTintColor = Color.parseColor(statusBarTint);
					} catch (IllegalArgumentException e) {
						e.printStackTrace();
					}
				}

				if (iconTint == null) {
					iconTintColor = Utils.getIconColorForColor(statusBarTintColor, mSettingsHelper.getHsvMax());
				} else {
					iconTintColor = Color.parseColor(iconTint);
				}

				Intent intent = new Intent(Common.INTENT_CHANGE_COLOR_NAME);

				if (statusBarTint != null)
					intent.putExtra(StatusBarTintApi.KEY_STATUS_BAR_TINT, statusBarTintColor);

				if (iconTint != null)
					intent.putExtra(StatusBarTintApi.KEY_STATUS_BAR_ICON_TINT, iconTintColor);

                //TODO ActionBarImpl.
                /*
				if (colorHandled == true) {
					if (!intent.hasExtra(StatusBarTintApi.KEY_STATUS_BAR_TINT))
						intent.putExtra(StatusBarTintApi.KEY_STATUS_BAR_TINT, color);
					if (!intent.hasExtra(StatusBarTintApi.KEY_STATUS_BAR_ICON_TINT))
						intent.putExtra(StatusBarTintApi.KEY_STATUS_BAR_ICON_TINT, iconTintColor);
				}
                */

				/* We failed to get a colour, fall back to the defaults */
				if (!intent.hasExtra(StatusBarTintApi.KEY_STATUS_BAR_TINT))
					intent.putExtra(StatusBarTintApi.KEY_STATUS_BAR_TINT, mSettingsHelper.getDefaultTint(SettingsHelper.Tint.STATUS_BAR));
				if (!intent.hasExtra(StatusBarTintApi.KEY_STATUS_BAR_ICON_TINT))
					intent.putExtra(StatusBarTintApi.KEY_STATUS_BAR_ICON_TINT, mSettingsHelper.getDefaultTint(SettingsHelper.Tint.ICON));

				activity.sendOrderedBroadcast(intent, null);
			}
		});

        //TODO ActionBarImpl.
        /*
		try {
			Class<?> ActionBarImpl = findClass("com.android.internal.app.ActionBarImpl", null);
			findAndHookMethod(ActionBarImpl, "setBackgroundDrawable", Drawable.class, new XC_MethodHook() {
				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					ActionBar actionBar = (ActionBar) param.thisObject;
					int color = Utils.getMainColorFromActionBarDrawable((Drawable) param.args[0]);
					sendColorChangeIntent(color, Utils.getIconColorForColor(color, mSettingsHelper.getHsvMax()), actionBar.getThemedContext());
				}
			});
		} catch (ClassNotFoundError e) {

		} catch (NoSuchMethodError e) {

		}*/
	}

	private static void sendColorChangeIntent(int statusBarTint, int iconColorTint, Context context) {
		Intent intent = new Intent(Common.INTENT_CHANGE_COLOR_NAME);
		intent.putExtra(StatusBarTintApi.KEY_STATUS_BAR_TINT, statusBarTint);
		intent.putExtra(StatusBarTintApi.KEY_STATUS_BAR_ICON_TINT, iconColorTint);

		Utils.sendOrderedBroadcast(context, intent);
	}

	@Override
	public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
		if (lpparam.packageName.equals("android")) {
			doSViewHooks(lpparam.classLoader);	
		}

		if (!lpparam.packageName.equals("com.android.systemui"))
			return;

		try {
			Class<?> PhoneStatusBar = findClass("com.android.systemui.statusbar.StatusBarService",
					lpparam.classLoader);
			try {

                XC_MethodHook xc_methodHook = new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        mStatusIcons = (LinearLayout) getObjectField(param.thisObject, "mStatusIcons");
                        mStatusBarView = (FrameLayout) getObjectField(param.thisObject, "mStatusBarView");
                        super.afterHookedMethod(param);
                    }
                };
                Method m = XposedHelpers.findMethodBestMatch(PhoneStatusBar,"makeStatusBarView",Context.class);
                XposedBridge.hookMethod(m,xc_methodHook);

			} catch (NoSuchMethodError e) {
                Log.d(logTag,"Could Not Locate makeStatusBarView");
			}

			try {
				Method swapViews = XposedHelpers.findMethodBestMatch(PhoneStatusBar, "swapViews", View.class, int.class,
						int.class, long.class);

				XperiaTransparencyHook xperiaHook = new XperiaTransparencyHook();
				XposedBridge.hookMethod(swapViews, xperiaHook);
			} catch (NoSuchMethodError e) {
				// Not an Xperia
			} catch (NoSuchFieldError e) {
				// Whaaaa?
			}

			try {
				findAndHookMethod(PhoneStatusBar, "transparentizeStatusBar", int.class, new XC_MethodHook() {
					@Override
					protected void afterHookedMethod(MethodHookParam param) throws Throwable {
						// This is casted internally from a boolean to an int, not sure why,
						// could be a Handler thing.
						int isTransparent = (Integer)param.args[0];

						if (isTransparent == 0) {
							mIsStatusBarNowTransparent = false;

							View statusBarView = (View) getObjectField(param.thisObject, "mStatusBarView");
							statusBarView.setBackgroundColor(mLastTint);
						} else if (isTransparent == 1) {
							mIsStatusBarNowTransparent = true;
						}
					}
				});
			} catch (NoSuchMethodError e) {
				// Not an S4
			}

			doStatusBarIconsHooks(PhoneStatusBar, lpparam.classLoader);
		} catch (ClassNotFoundError e) {
            Log.d(logTag,"Could not locate com.android.systemui.statusbar.StatusBarService");
		}



		try {
			Class<?> PhoneStatusBarView = findClass("com.android.systemui.statusbar.StatusBarView",
					lpparam.classLoader);

			XposedBridge.hookAllConstructors(PhoneStatusBarView, new XC_MethodHook() {
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					Context context = (Context) param.args[0];
					IntentFilter iF = new IntentFilter();
					iF.addAction(Common.INTENT_CHANGE_COLOR_NAME);
					iF.addAction(Common.INTENT_SAMSUNG_SVIEW_COVER);
					iF.addAction(Common.INTENT_SETTINGS_UPDATED);
                    Log.d(logTag,"Registering Receiver");
					context.registerReceiver(mBroadcastReceiver, iF);
				}

				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					mStatusBarView = (FrameLayout) param.thisObject;
				}
			});
		} catch (ClassNotFoundError e) {
            Log.d(logTag,"Could not locate com.android.systemui.statusbar.StatusBarView");
		}

		doBatteryHooks(lpparam.classLoader);
		doSignalHooks(lpparam.classLoader);
		doBluetoothHooks(lpparam.classLoader);
		doClockHooks(lpparam.classLoader);
		doTickerHooks(lpparam.classLoader);
	}

	private void doStatusBarIconsHooks(Class<?> PhoneStatusBar, ClassLoader classLoader) {
		Class<?> StatusBarIcon = XposedHelpers.findClass("com.android.internal.statusbar.StatusBarIcon", null);
		findAndHookMethod(PhoneStatusBar, "addIcon", String.class, int.class, int.class, StatusBarIcon,
				new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				XposedBridge.log("Hooking Icons");
                mStatusIcons = (LinearLayout) getObjectField(param.thisObject, "mStatusIcons");
				setColorForLayout(mStatusIcons, mColorForStatusIcons);
			}
		});

		findAndHookMethod(PhoneStatusBar, "removeIcon", String.class, int.class, int.class,
				new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				mStatusIcons = (LinearLayout) getObjectField(param.thisObject, "mStatusIcons");
			}
		});

		Class<?> StatusBarIconView = XposedHelpers.findClass("com.android.systemui.statusbar.StatusBarIconView", classLoader);
		XposedBridge.hookAllConstructors(StatusBarIconView, new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				ImageView view = (ImageView) param.thisObject;
				view.setColorFilter(mColorForStatusIcons, Mode.MULTIPLY);
				addStatusBarImageView(view);
			}
		});
	}

	private static void setColorForLayout(LinearLayout statusIcons, int color) {
		if (color == 0)
			return;

		if (statusIcons == null)
			return;

		for (int i = 0; i < statusIcons.getChildCount(); i++) {
			try {
				ImageView view = (ImageView) statusIcons.getChildAt(i);
				if (view != null)
					view.setColorFilter(color, Mode.MULTIPLY);
			} catch (ClassCastException e) {

			}
		}
	}

	private void setColorToAllTextSwitcherChildren(TextSwitcher switcher, int color) {
		if (color != 0) {
			for (int i = 0; i < switcher.getChildCount(); i++) {
				TextView view = (TextView) switcher.getChildAt(i);
				view.setTextColor(color);
				mTextLabels.add(view);
			}
		}	
	}

	private void setColorToAllImageSwitcherChildren(ImageSwitcher switcher, int color) {
		if (color != 0) {
			for (int i = 0; i < switcher.getChildCount(); i++) {
				ImageView view = (ImageView) switcher.getChildAt(i);
				view.setColorFilter(color, Mode.MULTIPLY);
				addStatusBarImageView(view);
			}
		}	
	}

	private void setStatusBarTint(final int tintColor) {
		Log.d(logTag,"Called setStatusBarTint :) (y)");
        if (mStatusBarView == null)
        {
            Log.d(logTag,"mStatusBarView == null :( what is this");
            return;
        }

		if (mLastSetColor == tintColor)
			return;

		mLastSetColor = tintColor;

        XposedBridge.log("Changing Status bar bg to "+tintColor);
		mStatusBarView.setBackgroundColor(tintColor);
	}

	private void setStatusBarIconsTint(int iconTint) {
		mColorForStatusIcons = iconTint;
		try {
			if (mIconViews != null) {
				for (ImageView view : mIconViews) {
					if (view != null) {
						view.setColorFilter(iconTint, Mode.MULTIPLY);
					} else {
						mIconViews.remove(view);
					}
				}
			}

			if (mTextLabels != null) {
				for (TextView view : mTextLabels) {
					if (view != null) {
						view.setTextColor(iconTint);
					} else {
						mTextLabels.remove(view);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		setColorForLayout(mStatusIcons, iconTint);
	}

	private void addStatusBarImageView(ImageView imageView) {
		if (!mIconViews.contains(imageView))
			mIconViews.add(imageView);
	}

	private void doBatteryHooks(ClassLoader classLoader) {
		String className = "com.android.systemui.statusbar.policy.BatteryController";
		String addIconMethodName = "addIconView";
		String addLabelMethodName = "addLabelView";
		try {
			Class<?> BatteryController = XposedHelpers.findClass(className, classLoader);

			try {
				XposedHelpers.findAndHookMethod(BatteryController, addIconMethodName, ImageView.class,
						new XC_MethodHook() {
					@Override
					protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
						addStatusBarImageView((ImageView) param.args[0]);
					}
				});
			} catch (NoSuchMethodError e) {
				XposedBridge.log("Not hooking method " + className + "." + addIconMethodName);
			}

			try {
				XposedHelpers.findAndHookMethod(BatteryController, addLabelMethodName, TextView.class,
						new XC_MethodHook() {
					@Override
					protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
						mTextLabels.add((TextView) param.args[0]);
					}
				});
			} catch (NoSuchMethodError e) {
				XposedBridge.log("Not hooking method " + className + "." + addLabelMethodName);
			}

		} catch (ClassNotFoundError e) {
			// Really shouldn't happen, but we can't afford a crash here.
			XposedBridge.log("Not hooking class: " + className);
		}
	}

	private void doSignalHooks(ClassLoader classLoader) {
		final String className = "com.android.systemui.statusbar.SignalClusterView";
		String methodName = "onAttachedToWindow";
		try {
			Class<?> SignalClusterView = XposedHelpers.findClass(className, classLoader);

			findAndHookMethod(SignalClusterView, methodName, new XC_MethodHook() {
				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					for (String name : Common.SIGNAL_CLUSTER_ICON_NAMES) {
						try {
							ImageView view = (ImageView) XposedHelpers.getObjectField(param.thisObject, name);
							addStatusBarImageView(view);
						} catch (NoSuchFieldError e) {
							XposedBridge.log("Couldn't find field " + name + "in class " + className);
						}
					}
				}
			});
		} catch (ClassNotFoundError e) {
			// Really shouldn't happen, but we can't afford a crash here.
			XposedBridge.log("Not hooking class: " + className);
		} catch (NoSuchMethodError e) {
			XposedBridge.log("Not hooking method " + className + "." + methodName);
		}
	}

	private void doBluetoothHooks(ClassLoader classLoader) {
		String className = "com.android.systemui.statusbar.policy.BluetoothController";
		String methodName = "addIconView";
		try {
			Class<?> BluetoothController = XposedHelpers.findClass(className, classLoader);

			XposedHelpers.findAndHookMethod(BluetoothController, methodName, ImageView.class, new XC_MethodHook() {
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					addStatusBarImageView((ImageView) param.args[0]);
				}
			});
		} catch (ClassNotFoundError e) {
			XposedBridge.log("Not hooking class: " + className);
		} catch (NoSuchMethodError e) {
			XposedBridge.log("Not hooking method " + className + "." + methodName);
		}

	}

	private void doClockHooks(ClassLoader classLoader) {
		String className = "com.android.systemui.statusbar.Clock";
		try {
			Class<?> Clock = XposedHelpers.findClass(className, classLoader);

			XposedBridge.hookAllConstructors(Clock, new XC_MethodHook() {
				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					TextView clock = (TextView) param.thisObject;
					// TODO: TouchWiz specific, replace this with more generic code.
					try {
						boolean mExpandedHeader = XposedHelpers.getBooleanField(param.thisObject,
								"mExpandedHeader");
						if (!mExpandedHeader)
							mTextLabels.add(clock);
					} catch (NoSuchFieldError e) {
						/* TODO: Check if this is Sony specific :/ */
						if (clock.getId() != clock.getResources().getIdentifier("clock_expanded",
								"id", "com.android.systemui"))
							mTextLabels.add(clock);
					}
				}
			});

			findAndHookMethod(Clock, "updateClock", new XC_MethodHook() {
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					if (mClockAnimation) {
						TextView clock = (TextView) param.thisObject;
						Animation fadeOutAnimation = AnimationUtils.loadAnimation(clock.getContext(),
								android.R.anim.fade_out);
						clock.setAnimation(fadeOutAnimation);
					}
				}

				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					TextView clock = (TextView) param.thisObject;

					if (mColorForStatusIcons != 0) {
						try {
							boolean mExpandedHeader = XposedHelpers.getBooleanField(param.thisObject,
									"mExpandedHeader");
							if (!mExpandedHeader)
								clock.setTextColor(mColorForStatusIcons);
						} catch (NoSuchFieldError e) {
							clock.setTextColor(mColorForStatusIcons);
						}	
					}

					if (mClockAnimation) {
						Animation fadeInAnimation = AnimationUtils.loadAnimation(clock.getContext(),
								android.R.anim.fade_in);
						clock.setAnimation(fadeInAnimation);
					}
				}
			});
		} catch (ClassNotFoundError e) {
			XposedBridge.log("Not hooking class: " + className);
		}
	}

	private void doTickerHooks(ClassLoader classLoader) {
		String className = "com.android.systemui.statusbar.Ticker";
		String notificationClassName = "com.android.internal.statusbar.StatusBarNotification";
		String addMethod = "addEntry";
		try {
			Class<?> Ticker = findClass(className, classLoader);
			Class<?> StatusBarNotification = findClass(notificationClassName, classLoader);

			XposedBridge.hookAllConstructors(Ticker, new XC_MethodHook() {
				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					setColorToAllTextSwitcherChildren(
							(TextSwitcher) XposedHelpers.getObjectField(param.thisObject, "mTextSwitcher"),
							mColorForStatusIcons);
					setColorToAllImageSwitcherChildren(
							(ImageSwitcher) XposedHelpers.getObjectField(param.thisObject, "mIconSwitcher"),
							mColorForStatusIcons);
				}
			});

			try {
				findAndHookMethod(Ticker, addMethod, StatusBarNotification, new XC_MethodHook() {
					@Override
					protected void afterHookedMethod(MethodHookParam param) throws Throwable {
						setColorToAllTextSwitcherChildren(
								(TextSwitcher) getObjectField(param.thisObject, "mTextSwitcher"),
								mColorForStatusIcons);
						setColorToAllImageSwitcherChildren(
								(ImageSwitcher) XposedHelpers.getObjectField(param.thisObject, "mIconSwitcher"),
								mColorForStatusIcons);
					};
				});
			} catch (NoSuchMethodError e) {
				XposedBridge.log("Not hooking method " + className + "." + addMethod);
			}
		} catch (ClassNotFoundError e) {
			XposedBridge.log("Not hooking class: " + className);
		}
	}

	private void doSViewHooks(ClassLoader classLoader) {
		try {
			Class<?> SViewCoverManager = findClass("com.android.internal.policy.impl.sviewcover.SViewCoverManager", classLoader);

			findAndHookMethod(SViewCoverManager, "handleShow", new XC_MethodHook() {
				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					boolean isUiShowing = XposedHelpers.getBooleanField(param.thisObject, "mShowingCoverUI");
					if (!isUiShowing)
						return;		

					if (mStatusBarView == null) {
						Context context = (Context) getObjectField(param.thisObject, "mContext");
						Intent intent = new Intent(Common.INTENT_CHANGE_COLOR_NAME);
						intent.putExtra(StatusBarTintApi.KEY_STATUS_BAR_TINT, Color.BLACK);
						intent.putExtra(StatusBarTintApi.KEY_STATUS_BAR_ICON_TINT, Color.WHITE);
						Utils.sendOrderedBroadcast(context, intent);
					} else {
						setStatusBarTint(Color.BLACK);
						setStatusBarIconsTint(Color.WHITE);
					}
				}
			});

			findAndHookMethod(SViewCoverManager, "handleHide", new XC_MethodHook() {
				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					boolean isUiShowing = XposedHelpers.getBooleanField(param.thisObject, "mShowingCoverUI");
					if (isUiShowing)
						return;

					if (mStatusBarView == null) {
						Context context = (Context) getObjectField(param.thisObject, "mContext");
						Intent intent = new Intent(Common.INTENT_CHANGE_COLOR_NAME);
						intent.putExtra(StatusBarTintApi.KEY_STATUS_BAR_TINT, mLastTint);
						intent.putExtra(StatusBarTintApi.KEY_STATUS_BAR_ICON_TINT, mLastIconTint);
						Utils.sendOrderedBroadcast(context, intent);
					} else {
						setStatusBarTint(mLastTint);
						setStatusBarIconsTint(mLastIconTint);
					}
				}
			});
		} catch (ClassNotFoundError e) {

		}
	}
}
