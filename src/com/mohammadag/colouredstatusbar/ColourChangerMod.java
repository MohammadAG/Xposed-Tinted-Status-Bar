package com.mohammadag.colouredstatusbar;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.getStaticObjectField;

import java.util.ArrayList;
import java.util.Locale;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.XModuleResources;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.mohammadag.colouredstatusbar.SettingsHelper.Tint;
import com.mohammadag.colouredstatusbar.drawables.OverlayDrawable;
import com.mohammadag.colouredstatusbar.hooks.ActionBarHooks;
import com.mohammadag.colouredstatusbar.hooks.ActivityOnResumeHook;
import com.mohammadag.colouredstatusbar.hooks.BatteryHooks;
import com.mohammadag.colouredstatusbar.hooks.BluetoothControllerHook;
import com.mohammadag.colouredstatusbar.hooks.KitKatBatteryHook;
import com.mohammadag.colouredstatusbar.hooks.NavigationBarHook;
import com.mohammadag.colouredstatusbar.hooks.OnWindowFocusedHook;
import com.mohammadag.colouredstatusbar.hooks.SignalClusterHook;
import com.mohammadag.colouredstatusbar.hooks.StatusBarHook;
import com.mohammadag.colouredstatusbar.hooks.StatusBarLayoutInflationHook;
import com.mohammadag.colouredstatusbar.hooks.StatusBarViewHook;
import com.mohammadag.colouredstatusbar.hooks.TickerHooks;
import com.mohammadag.colouredstatusbar.hooks.WindowDimHooks;
import com.mohammadag.colouredstatusbar.hooks.oemhooks.CustomRomHooks;
import com.mohammadag.colouredstatusbar.hooks.oemhooks.HtcTransparencyHook;
import com.mohammadag.colouredstatusbar.hooks.oemhooks.LGHooks;
import com.mohammadag.colouredstatusbar.hooks.oemhooks.SViewHooks;
import com.mohammadag.colouredstatusbar.hooks.oemhooks.SamsungHook;

import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_InitPackageResources.InitPackageResourcesParam;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class ColourChangerMod implements IXposedHookLoadPackage, IXposedHookZygoteInit, IXposedHookInitPackageResources {
	private static View mStatusBarView;
	private static View mNavigationBarView;
	private static KitKatBattery mKitKatBattery;
	private static ArrayList<ImageView> mSystemIconViews = new ArrayList<ImageView>();
	private static ArrayList<ImageView> mNotificationIconViews = new ArrayList<ImageView>();
	private static ArrayList<TextView> mTextLabels = new ArrayList<TextView>();
	private static int mColorForStatusIcons = 0;
	private static SettingsHelper mSettingsHelper;

	private static int mLastTint = Color.BLACK;
	private static int mLastIconTint = Color.WHITE;
	private static int mNavigationBarTint = Color.BLACK;
	private static int mNavigationBarIconTint = Color.WHITE;

	/* Notification icons */
	private static LinearLayout mStatusIcons = null;

	private int mLastSetColor;
	// This holds the action bar's background color when we explicitly call sendActionBarColorSaveIntent,
	// so that we can later get it back. Used when changing the status bar's color for ActionModes,
	// since we can't access the ActionBar directly from within ActionBarContextView.
	private int mActionBarColor;
	private int mLastSetNavBarTint;
	private long mLastReceivedTime;
	private static final String KK_TRANSPARENT_COLOR_STRING = "#66000000";
	private static final int KITKAT_TRANSPARENT_COLOR = Color.parseColor(KK_TRANSPARENT_COLOR_STRING);

	private static XModuleResources mResources;
	private OverlayDrawable mGradientDrawable;
	private OverlayDrawable mNavGradientDrawable;

	/* Fall back to old method to get the clock when no clock is found */
	private static ClassLoader mSystemUiClassLoader = null;
	private static boolean mFoundClock = false;
	private static boolean mHookClockOnSystemUiInit = false;
	static {
		if (MiuiV5Support.IS_MIUIV5)
			mHookClockOnSystemUiInit = true;
	}

	/* 
	 * Workaround for race condition when an app is closed with the keyboard open.
	 * The broadcast and SystemUI will conflict, usually the broadcast would set the
	 * color back for icons and nav bar, and in between, SystemUI resets the color
	 * back to the old keyboard down one.
	 */
	private boolean mKeyboardUp = false;

	/* LG BUTTON IDs */
	private static int qmemoButtonRESID = 0;
	private static int notificationButtonRESID = 0;

	public void log(String text) {
		if (mSettingsHelper.isDebugMode())
			XposedBridge.log("TintedStatusBar: " + text);
	}

	private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (StatusBarTintApi.INTENT_CHANGE_COLOR_NAME.equals(intent.getAction())) {
				boolean link = intent.getBooleanExtra("link_panels", false);

				if (intent.hasExtra("time")) {
					long time = intent.getLongExtra("time", -1);
					if (time != -1) {
						if (mLastReceivedTime == -1) {
							mLastReceivedTime = time;
						} else {
							if (time < mLastReceivedTime) {
								log("Ignoring change request because of earlier request");
								log("mLastReceivedTime: " + mLastReceivedTime + " time: " + time);
								return;
							} else {
								mLastReceivedTime = time;
							}
						}
					}
				}

				if (intent.hasExtra(Common.INTENT_SAVE_ACTIONBAR_COLOR_NAME))
					mActionBarColor = mLastTint;

				if (intent.hasExtra(StatusBarTintApi.KEY_STATUS_BAR_TINT)) {
					mLastTint = intent.getIntExtra(StatusBarTintApi.KEY_STATUS_BAR_TINT, -1);
					setStatusBarTint(mLastTint);
				}

				if (intent.hasExtra(StatusBarTintApi.KEY_STATUS_BAR_ICON_TINT)) {
					mLastIconTint = intent.getIntExtra(StatusBarTintApi.KEY_STATUS_BAR_ICON_TINT, -1);
					setStatusBarIconsTint(mLastIconTint);
				}

				if (intent.hasExtra(StatusBarTintApi.KEY_NAVIGATION_BAR_TINT) && !link) {
					mNavigationBarTint = intent.getIntExtra(StatusBarTintApi.KEY_NAVIGATION_BAR_TINT, -1);
					setNavigationBarTint(mNavigationBarTint, true);
				} else if (link) {
					mNavigationBarTint = intent.getIntExtra(StatusBarTintApi.KEY_STATUS_BAR_TINT, -1);
					setNavigationBarTint(mNavigationBarTint);
				}

				if (intent.hasExtra(StatusBarTintApi.KEY_NAVIGATION_BAR_ICON_TINT) && !link) {
					mNavigationBarIconTint = intent.getIntExtra(StatusBarTintApi.KEY_NAVIGATION_BAR_ICON_TINT, -1);
					setNavigationBarIconTint(mNavigationBarIconTint, true);
				} else if (link) {
					mNavigationBarIconTint = intent.getIntExtra(StatusBarTintApi.KEY_STATUS_BAR_ICON_TINT, -1);
					setNavigationBarIconTint(mNavigationBarIconTint);
				}
			} else if (Common.INTENT_RESET_ACTIONBAR_COLOR_NAME.equals(intent.getAction())) {
				mLastTint = mActionBarColor;
				setStatusBarTint(mActionBarColor);
			} else if (Common.INTENT_SETTINGS_UPDATED.equals(intent.getAction())) {
				Log.d("Xposed", "TintedStatusBar settings updated, reloading...");
				mSettingsHelper.reload();
				mSettingsHelper.reloadOverlayMode();
			} else if (Common.INTENT_KEYBOARD_VISIBLITY_CHANGED.equals(intent.getAction())) {
				if (intent.hasExtra(Common.EXTRA_KEY_KEYBOARD_UP))
					onKeyboardVisible(intent.getBooleanExtra(Common.EXTRA_KEY_KEYBOARD_UP, false));
			} else if (WindowDimHooks.INTENT_DIM_CHANGED.equals(intent.getAction())) {
				if (intent.hasExtra(WindowDimHooks.KEY_DIM_AMOUNT))
					onDimLayerChanged(intent.getFloatExtra(WindowDimHooks.KEY_DIM_AMOUNT, -1));
			}
		}
	};

	@Override
	public void initZygote(StartupParam startupParam) throws Throwable {
		mStatusBarView = null;
		mSettingsHelper = new SettingsHelper(new XSharedPreferences(PackageNames.OURS, SettingsHelper.PREFS));

		mResources = XModuleResources.createInstance(startupParam.modulePath, null);

		findAndHookMethod(Activity.class, "onWindowFocusChanged", boolean.class,
				new OnWindowFocusedHook(mSettingsHelper, mResources));
		findAndHookMethod(Activity.class, "performResume",
				new ActivityOnResumeHook(mSettingsHelper, mResources));
		WindowDimHooks.doHook();

		if (Utils.hasActionBar())
			new ActionBarHooks(mSettingsHelper);

		// TODO: Fix this
		// WindowManagerServiceHooks.doHook(null);
		LGHooks.doImeHook();
	}

	public static void sendColorChangeIntent(int statusBarTint, int iconColorTint, Context context) {
		Intent intent = new Intent(StatusBarTintApi.INTENT_CHANGE_COLOR_NAME);
		intent.putExtra(StatusBarTintApi.KEY_STATUS_BAR_TINT, statusBarTint);
		intent.putExtra(StatusBarTintApi.KEY_STATUS_BAR_ICON_TINT, iconColorTint);

		intent.putExtra("time", System.currentTimeMillis());
		intent.putExtra("link_panels", mSettingsHelper.shouldLinkPanels(context.getPackageName(), null));

		context.sendBroadcast(intent);
	}

	public static void sendColorSaveAndChangeIntent(int statusBarTint, int iconColorTint, Context context) {
		Intent intent = new Intent(StatusBarTintApi.INTENT_CHANGE_COLOR_NAME);
		intent.putExtra(StatusBarTintApi.KEY_STATUS_BAR_TINT, statusBarTint);
		intent.putExtra(StatusBarTintApi.KEY_STATUS_BAR_ICON_TINT, iconColorTint);
		intent.putExtra(Common.INTENT_SAVE_ACTIONBAR_COLOR_NAME, 0);

		intent.putExtra("time", System.currentTimeMillis());
		intent.putExtra("link_panels", mSettingsHelper.shouldLinkPanels(context.getPackageName(), null));

		context.sendBroadcast(intent);
	}

	public static void sendResetActionBarColorsIntent(Context context) {
		Intent intent = new Intent(Common.INTENT_RESET_ACTIONBAR_COLOR_NAME);
		context.sendBroadcast(intent);
	}

	@Override
	public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
		if (lpparam.packageName.equals("android")) {
			if (android.os.Build.MANUFACTURER.toLowerCase(Locale.getDefault()).contains("samsung"))
				new SViewHooks(this, lpparam.classLoader);
		}

		if (lpparam.packageName.equals("com.htc.launcher")) {
			HtcTransparencyHook.doBlinkFeedHooks(lpparam.classLoader);
		}

		if (lpparam.packageName.equals("com.htc.lockscreen")) {
			HtcTransparencyHook.doLockscreenHooks(lpparam.classLoader);
		}

		if (!lpparam.packageName.equals("com.android.systemui"))
			return;

		mGradientDrawable = new OverlayDrawable(mResources, Color.TRANSPARENT,
				R.drawable.status_background);
		mNavGradientDrawable = new OverlayDrawable(mResources, Color.TRANSPARENT,
				R.drawable.nav_background);

		if (mHookClockOnSystemUiInit)
			doClockHooks(lpparam.classLoader);

		mSystemUiClassLoader = lpparam.classLoader;

		new StatusBarHook(this, lpparam.classLoader);
		new StatusBarViewHook(this, lpparam.classLoader);
		new NavigationBarHook(this, lpparam.classLoader);
		new BatteryHooks(this, lpparam.classLoader);
		new SignalClusterHook(this, lpparam.classLoader);
		new BluetoothControllerHook(this, lpparam.classLoader);
		new TickerHooks(this, lpparam.classLoader);
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
			new KitKatBatteryHook(this, lpparam.classLoader);
		}

		CustomRomHooks.doHook(lpparam.classLoader, this);
		HtcTransparencyHook.doHook(lpparam.classLoader);
		LGHooks.doHook(lpparam.classLoader);
		SamsungHook.doHook(this, lpparam.classLoader);
	}

	private void setKitKatBatteryColor(int iconColor) {
		if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.KITKAT)
			return;

		if (mKitKatBattery == null)
			return;

		mKitKatBattery.updateBattery(iconColor);
	}

	private static void setColorForLayout(LinearLayout statusIcons, int color, PorterDuff.Mode mode) {
		if (color == 0)
			return;

		if (statusIcons == null)
			return;

		for (int i = 0; i < statusIcons.getChildCount(); i++) {
			View childView = statusIcons.getChildAt(i);
			if (childView instanceof ImageView) {
				ImageView view = (ImageView) childView;
				if (view != null) {
					view.setColorFilter(color, mode);
				}
			}
		}
	}

	@SuppressLint("NewApi")
	public void setStatusBarTint(final int tintColor) {
		if (mStatusBarView == null)
			return;

		log("Setting statusbar color to " + tintColor);

		if (mSettingsHelper.animateStatusBarTintChange()) {
			int animateFrom = mLastSetColor == KITKAT_TRANSPARENT_COLOR ? Color.TRANSPARENT : mLastSetColor;
			int animateTo = tintColor == KITKAT_TRANSPARENT_COLOR ? Color.TRANSPARENT : tintColor;
			ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), animateFrom, animateTo);
			colorAnimation.addUpdateListener(new AnimatorUpdateListener() {
				@Override
				public void onAnimationUpdate(ValueAnimator animator) {
					mGradientDrawable.setColor((Integer)animator.getAnimatedValue());
				}
			});
			Utils.setViewBackground(mStatusBarView, mGradientDrawable);
			colorAnimation.start();
		} else {
			mStatusBarView.setAlpha(1f);
			if (tintColor == KITKAT_TRANSPARENT_COLOR) {
				Utils.setViewBackground(mStatusBarView, mGradientDrawable);
				mGradientDrawable.setColor(Color.TRANSPARENT);
			} else {
				Utils.setViewBackground(mStatusBarView, mGradientDrawable);
				mGradientDrawable.setColor(tintColor);
			}
		}
		mGradientDrawable.setMode(mSettingsHelper.getOverlayMode(), mSettingsHelper.getSemiTransparentOverlayOpacity());
		mGradientDrawable.setIsTransparentCauseOfKitKatApi(tintColor == KITKAT_TRANSPARENT_COLOR
				&& mSettingsHelper.isLegacyGradientMode());

		mLastSetColor = tintColor;

		if (mSettingsHelper.shouldLinkStatusBarAndNavBar() && !mKeyboardUp) {
			mNavigationBarTint = tintColor;
			setNavigationBarTint(tintColor, true);
		}
	}

	public void setStatusBarIconsTint(int iconTint) {
		if (mSettingsHelper.shouldLinkStatusBarAndNavBar() && !mKeyboardUp) {
			mNavigationBarIconTint = iconTint;
			setNavigationBarIconTint(iconTint, true);
		}

		if (mSettingsHelper.shouldForceWhiteTintWithOverlay()) {
			iconTint = Color.parseColor("#ccffffff");
		}

		mColorForStatusIcons = iconTint;
		try {
			if (mSystemIconViews != null) {
				for (ImageView view : mSystemIconViews) {
					if (view != null) {
						view.setColorFilter(iconTint, mSettingsHelper.getSystemIconCfType());
					} else {
						mSystemIconViews.remove(view);
					}
				}
			}

			if (mNotificationIconViews != null) {
				for (ImageView view : mNotificationIconViews) {
					if (view != null) {
						view.setColorFilter(iconTint, mSettingsHelper.getNotificationIconCfType());
					} else {
						mNotificationIconViews.remove(view);
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

			if (mStatusBarView != null) {
				Intent intent = new Intent("gravitybox.intent.action.STATUSBAR_COLOR_CHANGED");
				intent.putExtra("iconColorEnable", true);
				intent.putExtra("iconColor", iconTint);
				mStatusBarView.getContext().sendBroadcast(intent);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		setColorForLayout(mStatusIcons, iconTint, mSettingsHelper.getNotificationIconCfType());
		setKitKatBatteryColor(iconTint);
	}

	@SuppressLint("NewApi")
	private void setNavigationBarTint(final int tintColor, boolean force) {
		if (mNavigationBarView == null)
			return;

		if (mSettingsHelper.shouldLinkStatusBarAndNavBar() && !force) {
			log("Ignoring manual navigation bar color change cause we're linked");
			return;
		}

		log("Setting navigation bar color to " + tintColor);

		if (mSettingsHelper.animateStatusBarTintChange()) {
			int animateFrom = mLastSetColor == KITKAT_TRANSPARENT_COLOR ? Color.TRANSPARENT : mLastSetNavBarTint;
			int animateTo = tintColor == KITKAT_TRANSPARENT_COLOR ? Color.TRANSPARENT : tintColor;
			ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), animateFrom, animateTo);
			colorAnimation.addUpdateListener(new AnimatorUpdateListener() {
				@Override
				public void onAnimationUpdate(ValueAnimator animator) {
					mNavGradientDrawable.setColor((Integer)animator.getAnimatedValue());
				}
			});
			Utils.setViewBackground(mNavigationBarView, mNavGradientDrawable);
			colorAnimation.start();
		} else {
			mNavigationBarView.setAlpha(1f);
			if (tintColor == KITKAT_TRANSPARENT_COLOR) {
				Utils.setViewBackground(mNavigationBarView, mNavGradientDrawable);
				mNavGradientDrawable.setColor(Color.TRANSPARENT);
			} else {
				Utils.setViewBackground(mNavigationBarView, mNavGradientDrawable);
				mNavGradientDrawable.setColor(tintColor);
			}
		}
		mNavGradientDrawable.setMode(mSettingsHelper.getOverlayMode(), mSettingsHelper.getSemiTransparentOverlayOpacity());
		mNavGradientDrawable.setIsTransparentCauseOfKitKatApi(tintColor == KITKAT_TRANSPARENT_COLOR
				&& mSettingsHelper.isLegacyGradientMode());

		if (mNavigationBarView != null && tintColor != KITKAT_TRANSPARENT_COLOR) {
			Intent intent = new Intent("gravitybox.intent.action.ACTION_NAVBAR_CHANGED");
			intent.putExtra("navbarBgColor", tintColor);
			intent.putExtra("navbarColorEnable", true);
			mNavigationBarView.getContext().sendBroadcast(intent);
		}

		mLastSetNavBarTint = tintColor;
	}

	public void setNavigationBarTint(final int tintColor) {
		setNavigationBarTint(tintColor, false);
	}

	private void setNavigationBarIconTint(int tintColor, boolean force) {
		if (mNavigationBarView == null)
			return;

		if (mSettingsHelper.shouldLinkStatusBarAndNavBar() && !force) {
			return;
		}

		if (mSettingsHelper.shouldForceWhiteTintWithOverlay()) {
			tintColor = Color.parseColor("#ccffffff");
		}

		ImageView recentsButton = null;
		ImageView menuButton = null;
		ImageView backButton = null;
		ImageView homeButton = null;

		/* LG BUTTONS */ 
		ImageView qmemoButton = null;
		ImageView notificationButton = null;

		Class<?> NavbarEditor = null;

		try {
			NavbarEditor = getObjectField(mNavigationBarView, "mEditBar").getClass();
		} catch (NoSuchFieldError e) { }

		try {
			recentsButton = (ImageView) XposedHelpers.callMethod(mNavigationBarView, "getRecentsButton");
		} catch (NoSuchMethodError e) {
			try {
				if (NavbarEditor != null) {
					recentsButton =
							(ImageView) mNavigationBarView.findViewWithTag(
									getStaticObjectField(NavbarEditor, "NAVBAR_RECENT"));
				}
			} catch (NoSuchFieldError e1) {
				e1.printStackTrace();
			}
		}

		try {
			menuButton = (ImageView) XposedHelpers.callMethod(mNavigationBarView, "getMenuButton");
		} catch (NoSuchMethodError e) {
			try {
				if (NavbarEditor != null) {
					menuButton =
							(ImageView) mNavigationBarView.findViewWithTag(
									getStaticObjectField(NavbarEditor, "NAVBAR_ALWAYS_MENU"));
				}
			} catch (NoSuchFieldError e1) {
				e1.printStackTrace();
			}
		}

		try {
			backButton = (ImageView) XposedHelpers.callMethod(mNavigationBarView, "getBackButton");
		} catch (NoSuchMethodError e) {
			try {
				backButton =
						(ImageView) mNavigationBarView.findViewWithTag(
								getStaticObjectField(NavbarEditor, "NAVBAR_BACK"));
			} catch (NoSuchFieldError e1) {
				e1.printStackTrace();
			}
		}

		try {
			homeButton = (ImageView) XposedHelpers.callMethod(mNavigationBarView, "getHomeButton");
		} catch (NoSuchMethodError e) {
			try {
				homeButton = (ImageView) mNavigationBarView.findViewWithTag(
						getStaticObjectField(NavbarEditor, "NAVBAR_HOME"));
			} catch (NoSuchFieldError e1) {
				e1.printStackTrace();
			}
		}

		/* LG BUTTONS*/
		if (qmemoButtonRESID > 0)
			qmemoButton = (ImageView) mNavigationBarView.findViewById(qmemoButtonRESID);

		if (notificationButtonRESID > 0)
			notificationButton = (ImageView) mNavigationBarView.findViewById(notificationButtonRESID);

		if (recentsButton != null)
			recentsButton.setColorFilter(tintColor);
		if (menuButton != null)
			menuButton.setColorFilter(tintColor);
		if (backButton != null)
			backButton.setColorFilter(tintColor);
		if (homeButton != null)
			homeButton.setColorFilter(tintColor);
		if (qmemoButton != null)
			qmemoButton.setColorFilter(tintColor);
		if (notificationButton != null)
			notificationButton.setColorFilter(tintColor);

		if (mNavigationBarView != null) {
			Intent intent = new Intent("gravitybox.intent.action.ACTION_NAVBAR_CHANGED");
			intent.putExtra("navbarKeyColor", tintColor);
			intent.putExtra("navbarColorEnable", true);
			mNavigationBarView.getContext().sendBroadcast(intent);
		}
	}

	public void setNavigationBarIconTint(final int tintColor) {
		setNavigationBarIconTint(tintColor, false);
	}

	public void addSystemIconView(ImageView imageView) {
		addSystemIconView(imageView, false);
	}

	public void addSystemIconView(ImageView imageView, boolean applyColor) {
		if (!mSystemIconViews.contains(imageView))
			mSystemIconViews.add(imageView);

		if (applyColor) {
			imageView.setColorFilter(mColorForStatusIcons, mSettingsHelper.getSystemIconCfType());
		}
	}

	public void addNotificationIconView(ImageView imageView) {
		if (!mNotificationIconViews.contains(imageView))
			mNotificationIconViews.add(imageView);
	}

	@Override
	public void handleInitPackageResources(InitPackageResourcesParam resparam) throws Throwable {
		if (!resparam.packageName.equals("com.android.systemui"))
			return;

		try {
			// Before anything else, let's make sure we're not dealing with a Lenovo device
			// Lenovo is known for doing some deep customizations into UI, so let's just check
			// if is possible to hook a specific layout and work with it in that case
			String layout = "lenovo_gemini_super_status_bar";
			try {
				int resourceId = resparam.res.getIdentifier(layout, "layout", "com.android.systemui");
				if (resourceId == 0)
					layout = Utils.hasGeminiSupport() ? "gemini_super_status_bar" : "super_status_bar";
			} catch (Throwable t) {
				layout = Utils.hasGeminiSupport() ? "gemini_super_status_bar" : "super_status_bar";
			}

			StatusBarLayoutInflationHook hook = new StatusBarLayoutInflationHook(this);

			try {
				int resourceId = resparam.res.getIdentifier("msim_super_status_bar", "layout", "com.android.systemui");
				if (resourceId != 0)
					resparam.res.hookLayout("com.android.systemui", "layout", "msim_super_status_bar", hook);
			} catch (Throwable t) {
			}

			resparam.res.hookLayout("com.android.systemui", "layout", layout, hook);
		} catch (Throwable t) {
			log(t.getMessage());
		}

		qmemoButtonRESID = resparam.res.getIdentifier("navigation_button_qmemo", "id", "com.android.systemui");
		notificationButtonRESID = resparam.res.getIdentifier("navigation_button_notification", "id", "com.android.systemui");
	}

	public int getLastStatusBarTint() {
		return mLastTint;
	}

	public void addTextLabel(TextView textView) {
		mTextLabels.add(textView);
	}

	public void setStatusIcons(LinearLayout statusIcons) {
		mStatusIcons = statusIcons;
	}

	public void setNavigationBarView(View navBarView) {
		mNavigationBarView = navBarView;
	}

	public void refreshStatusIconColors() {
		if (mStatusIcons != null)
			setColorForLayout(mStatusIcons, mColorForStatusIcons, mSettingsHelper.getNotificationIconCfType());
	}

	public int getLastIconTint() {
		return mLastIconTint;
	}

	public BroadcastReceiver getBroadcastReceiver() {
		return mBroadcastReceiver;
	}

	public void setStatusBarView(View view) {
		mStatusBarView = view;
		Utils.setViewBackground(mStatusBarView, mGradientDrawable);
	}

	public int getColorForStatusIcons() {
		return mColorForStatusIcons;
	}

	public SettingsHelper getSettingsHelper() {
		return mSettingsHelper;
	}

	public void setKitKatBatteryView(KitKatBattery kitkatBattery) {
		mKitKatBattery = kitkatBattery;
		setKitKatBatteryColor(mLastIconTint);
	}

	public void setNoClockFound() {
		if (mFoundClock)
			return;

		if (mSystemUiClassLoader != null)
			doClockHooks(mSystemUiClassLoader);
		else
			mHookClockOnSystemUiInit = true;
	}

	public void setClockFound() {
		mFoundClock = true;
	}

	private void doClockHooks(ClassLoader loader) {
		Class<?> Clock = XposedHelpers.findClass("com.android.systemui.statusbar.policy.Clock", loader);
		XposedBridge.hookAllConstructors(Clock, new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				addTextLabel((TextView) param.thisObject);
				mFoundClock = true;
			}
		});

		findAndHookMethod(Clock, "updateClock", new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				TextView textView = (TextView) param.thisObject;
				textView.setTextColor(mColorForStatusIcons);
			}
		});
	}

	public void onKeyboardVisible(boolean keyboardUp) {
		log("Keyboard visibility changed, isUp? " + keyboardUp);

		mKeyboardUp = keyboardUp;

		if (keyboardUp) {
			mNavGradientDrawable.setOverrideColor(mSettingsHelper.getDefaultTint(Tint.NAV_BAR_IM));
			setNavigationBarIconTint(mSettingsHelper.getDefaultTint(Tint.NAV_BAR_ICON_IM), true);
		} else {
			mNavGradientDrawable.setOverrideColor(-3);
			setNavigationBarIconTint(mNavigationBarIconTint, true);
		}
	}

	public void onLightsOutChanged(boolean lightsOut) {
		if (!mSettingsHelper.shouldReactToLightsOut())
			return;

		log("Lights out changed, isOn? " + lightsOut);

		int transparent = Color.parseColor("#c3121212");
		if (lightsOut) {
			setStatusBarTint(transparent);
			setStatusBarIconsTint(Color.WHITE);

			mNavGradientDrawable.setOverrideColor(transparent);
			setNavigationBarIconTint(Color.WHITE, true);
		} else {
			setStatusBarTint(mLastSetColor);
			setStatusBarIconsTint(mLastIconTint);

			mNavGradientDrawable.setOverrideColor(-3);
			setNavigationBarIconTint(mNavigationBarIconTint, true);
		}
	}

	public void onImmersiveModeChanged(boolean immersiveMode) {
		if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.KITKAT)
			return;

		log("Immersive mode changed, isImmersive? " + immersiveMode);

		if (immersiveMode) {
			setStatusBarTint(Color.parseColor("#65000000"));
			setStatusBarIconsTint(Color.WHITE);
			mNavGradientDrawable.setOverrideColor(Color.parseColor("#65000000"));
			setNavigationBarIconTint(Color.WHITE, true);
		} else {
			setStatusBarTint(mLastTint);
			setStatusBarIconsTint(mLastIconTint);
			setNavigationBarTint(mNavigationBarTint, true);
			setNavigationBarIconTint(mNavigationBarIconTint, true);
		}
	}

	private void onDimLayerChanged(float alpha) {
		log("Dim changed");

		mGradientDrawable.setDimAmount(alpha);
		mNavGradientDrawable.setDimAmount(alpha);
	}
}
