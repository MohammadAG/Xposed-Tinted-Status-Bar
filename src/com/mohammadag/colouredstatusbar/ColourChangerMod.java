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
import android.app.ActionBar;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.XModuleResources;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.mohammadag.colouredstatusbar.SettingsHelper.Tint;
import com.mohammadag.colouredstatusbar.hooks.ActionBarHooks;
import com.mohammadag.colouredstatusbar.hooks.BatteryHooks;
import com.mohammadag.colouredstatusbar.hooks.BluetoothControllerHook;
import com.mohammadag.colouredstatusbar.hooks.HtcTransparencyHook;
import com.mohammadag.colouredstatusbar.hooks.KitKatBatteryHook;
import com.mohammadag.colouredstatusbar.hooks.NavigationBarHook;
import com.mohammadag.colouredstatusbar.hooks.SViewHooks;
import com.mohammadag.colouredstatusbar.hooks.SignalClusterHook;
import com.mohammadag.colouredstatusbar.hooks.StatusBarHook;
import com.mohammadag.colouredstatusbar.hooks.StatusBarLayoutInflationHook;
import com.mohammadag.colouredstatusbar.hooks.StatusBarViewHook;
import com.mohammadag.colouredstatusbar.hooks.TickerHooks;

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
	private static View mKitKatBatteryView;
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

	/* Wokraround for Samsung UX */
	@SuppressWarnings("unused")
	private static boolean mIsStatusBarNowTransparent = false;

	private static XModuleResources mResources;

	/* Fall back to old method to get the clock when no clock is found */
	private static ClassLoader mSystemUiClassLoader = null;
	private static boolean mFoundClock = false;
	private static boolean mHookClockOnSystemUiInit = false;
	
	/* Floating Window Intent ID */
	public static final int FLAG_FLOATING_WINDOW = 0x00002000;

	public void log(String text) {
		if (mSettingsHelper.isDebugMode())
			XposedBridge.log("TintedStatusBar: " + text);
	}

	private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (Common.INTENT_CHANGE_COLOR_NAME.equals(intent.getAction())) {
				boolean link = intent.getBooleanExtra("link_panels", false);

				if (intent.hasExtra("time")) {
					long time = intent.getLongExtra("time", -1);
					if (time != -1) {
						if (mLastReceivedTime == -1) {
							mLastReceivedTime = -1;
						} else {
							if (time < mLastReceivedTime) {
								return;
							} else {
								mLastReceivedTime = time;
							}
						}
					}
				}

				if( intent.hasExtra(Common.INTENT_SAVE_ACTIONBAR_COLOR_NAME))
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
					setNavigationBarTint(mNavigationBarTint);
				} else if (link) {
					mNavigationBarTint = intent.getIntExtra(StatusBarTintApi.KEY_STATUS_BAR_TINT, -1);setNavigationBarTint(mNavigationBarTint);
					setNavigationBarTint(mNavigationBarTint);
				}

				if (intent.hasExtra(StatusBarTintApi.KEY_NAVIGATION_BAR_ICON_TINT) && !link) {
					mNavigationBarIconTint = intent.getIntExtra(StatusBarTintApi.KEY_NAVIGATION_BAR_ICON_TINT, -1);
					setNavigationBarIconTint(mNavigationBarIconTint);
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
			}
		}
	};

	@Override
	public void initZygote(StartupParam startupParam) throws Throwable {
		mStatusBarView = null;
		mSettingsHelper = new SettingsHelper(new XSharedPreferences(Common.PACKAGE_NAME, Common.PREFS));

		mResources = XModuleResources.createInstance(startupParam.modulePath, null);

		Class<?> ActivityClass = XposedHelpers.findClass("android.app.Activity", null);
		findAndHookMethod(ActivityClass, "performResume", new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				Activity activity = (Activity) param.thisObject;
				String packageName = activity.getPackageName();
				String activityName = activity.getLocalClassName();
				Intent activityIntent = activity.getIntent();

				mSettingsHelper.reload();

				if (mSettingsHelper.getBoolean(Common.SETTINGS_KEY_TOAST_ACTIVITY_NAMES, false)) {
					String tosatText = mResources.getString(R.string.toast_text_package_name, packageName);
					tosatText += "\n";
					tosatText += mResources.getString(R.string.toast_text_activity_name, activityName);
					Toast.makeText(activity, tosatText, Toast.LENGTH_SHORT).show();
				}

				if (!mSettingsHelper.isEnabled(packageName, activityName))
					return;
					
				if ((activityIntent.getFlags() & FLAG_FLOATING_WINDOW) == FLAG_FLOATING_WINDOW)
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
				} catch (Throwable t) { }

				try {
					navigationBarIconTintColor = Color.parseColor(Utils.addHashIfNeeded(navBarIconTint));
				} catch (Throwable t) { }

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
									} catch (IllegalArgumentException e) {}
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

				Intent intent = new Intent(Common.INTENT_CHANGE_COLOR_NAME);

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
				activity.sendBroadcast(intent);
			}
		});

		if (Utils.hasActionBar())
			new ActionBarHooks(mSettingsHelper);
	}

	public static void sendColorChangeIntent(int statusBarTint, int iconColorTint, Context context) {
		Intent intent = new Intent(Common.INTENT_CHANGE_COLOR_NAME);
		intent.putExtra(StatusBarTintApi.KEY_STATUS_BAR_TINT, statusBarTint);
		intent.putExtra(StatusBarTintApi.KEY_STATUS_BAR_ICON_TINT, iconColorTint);

		intent.putExtra("time", System.currentTimeMillis());
		intent.putExtra("link_panels", mSettingsHelper.shouldLinkPanels(context.getPackageName(), null));

		context.sendBroadcast(intent);
	}

	public static void sendColorSaveAndChangeIntent(int statusBarTint, int iconColorTint, Context context) {
		Intent intent = new Intent(Common.INTENT_CHANGE_COLOR_NAME);
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

		HtcTransparencyHook.doHook(lpparam.classLoader);
	}

	private void setKitKatBatteryColor(int iconColor) {
		if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.KITKAT)
			return;

		if (mKitKatBatteryView == null)
			return;

		boolean debug = mSettingsHelper.isDebugMode();

		try {
			final int[] colors = (int[]) XposedHelpers.getObjectField(mKitKatBatteryView, "mColors");
			colors[colors.length-1] = iconColor;
			XposedHelpers.setObjectField(mKitKatBatteryView, "mColors", colors);
		} catch (NoSuchFieldError e) {
			if (debug) e.printStackTrace();
		}

		try {
			final Paint framePaint = (Paint) XposedHelpers.getObjectField(mKitKatBatteryView, "mFramePaint");
			framePaint.setColor(iconColor);
			framePaint.setAlpha(100);
		} catch (NoSuchFieldError e) {
			if (debug) e.printStackTrace();
		}

		try {
			final Paint boltPaint = (Paint) XposedHelpers.getObjectField(mKitKatBatteryView, "mBoltPaint");
			boltPaint.setColor(Utils.getIconColorForColor(iconColor, Color.BLACK, Color.WHITE, 0.7f));
			boltPaint.setAlpha(100);
		} catch (NoSuchFieldError e) {
			if (debug) e.printStackTrace();
		}

		try {
			XposedHelpers.setIntField(mKitKatBatteryView, "mChargeColor", iconColor);
		} catch (NoSuchFieldError e) {
			/* Beanstalk, not sure why the ROM changed this */
			try {
				XposedHelpers.setIntField(mKitKatBatteryView, "mBatteryColor", iconColor);
			} catch (NoSuchFieldError e1) {}
			if (debug) e.printStackTrace();
		}

		mKitKatBatteryView.invalidate();
	}

	private static void setColorForLayout(LinearLayout statusIcons, int color, PorterDuff.Mode mode) {
		if (color == 0)
			return;

		if (statusIcons == null)
			return;

		for (int i = 0; i < statusIcons.getChildCount(); i++) {
			try {
				ImageView view = (ImageView) statusIcons.getChildAt(i);
				if (view != null) {
					view.setColorFilter(color, mode);
				}
			} catch (ClassCastException e) {

			}
		}
	}
	@SuppressLint("NewApi")
	public void setStatusBarTint(final int tintColor) {
		if (mStatusBarView == null)
			return;

		if (mLastSetColor == tintColor)
			return;

		log("Setting statusbar color to " + tintColor);

		if (mSettingsHelper.animateStatusBarTintChange()) {			
			if (tintColor != KITKAT_TRANSPARENT_COLOR) {
				ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), mLastSetColor, tintColor);
				colorAnimation.addUpdateListener(new AnimatorUpdateListener() {
				    @Override
				    public void onAnimationUpdate(ValueAnimator animator) {
				        mStatusBarView.setBackgroundColor((Integer)animator.getAnimatedValue());
				    }
				});
				colorAnimation.start();
			} else {
				Utils.setViewBackground(mStatusBarView, new BarBackgroundDrawable(mStatusBarView.getContext(),
						mResources, R.drawable.status_background));
			}
		} else {
			mStatusBarView.setAlpha(1f);
			if (tintColor == KITKAT_TRANSPARENT_COLOR) {
				mStatusBarView.setBackgroundColor(KITKAT_TRANSPARENT_COLOR);
				Utils.setViewBackground(mStatusBarView, new BarBackgroundDrawable(mStatusBarView.getContext(),
						mResources, R.drawable.status_background));
			} else {
				mStatusBarView.setBackgroundColor(tintColor);
			}
		}

		mLastSetColor = tintColor;

		if (mSettingsHelper.shouldLinkStatusBarAndNavBar()) {
			mNavigationBarTint = tintColor;
			setNavigationBarTint(tintColor, true);
		}
	}

	public void setStatusBarIconsTint(int iconTint) {
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
		if (mSettingsHelper.shouldLinkStatusBarAndNavBar()) {
			mNavigationBarIconTint = iconTint;
			setNavigationBarIconTint(iconTint, true);
		}
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
			if (tintColor != KITKAT_TRANSPARENT_COLOR) {
				ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), mLastSetNavBarTint, tintColor);
				colorAnimation.addUpdateListener(new AnimatorUpdateListener() {
				    @Override
				    public void onAnimationUpdate(ValueAnimator animator) {
				        mNavigationBarView.setBackgroundColor((Integer)animator.getAnimatedValue());
				    }
				});
				colorAnimation.start();
			} else {
				mNavigationBarView.setBackgroundColor(KITKAT_TRANSPARENT_COLOR);
				Utils.setViewBackground(mNavigationBarView, new BarBackgroundDrawable(mStatusBarView.getContext(),
						mResources, R.drawable.nav_background));
			}
		} else {
			if (tintColor == KITKAT_TRANSPARENT_COLOR) {
				mNavigationBarView.setBackgroundColor(KITKAT_TRANSPARENT_COLOR);
				Utils.setViewBackground(mNavigationBarView, new BarBackgroundDrawable(mNavigationBarView.getContext(),
						mResources, R.drawable.nav_background));
			} else {
				mNavigationBarView.setBackgroundColor(tintColor);
			}
		}
		
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

	private void setNavigationBarIconTint(final int tintColor, boolean force) {
		if (mNavigationBarView == null)
			return;

		if (mSettingsHelper.shouldLinkStatusBarAndNavBar() && !force) {
			return;
		}
		
		ImageView recentsButton = null;
		ImageView menuButton = null;
		ImageView backButton = null;
		ImageView homeButton = null;
		
		Class<?> NavbarEditor = null;

		try {
			recentsButton = (ImageView) XposedHelpers.callMethod(mNavigationBarView, "getRecentsButton");
		} catch (NoSuchMethodError e) {
			try {
				NavbarEditor = getObjectField(mNavigationBarView, "mEditBar").getClass();
				recentsButton =
						(ImageView) XposedHelpers.callMethod(mNavigationBarView,
								"findViewWithTag", getStaticObjectField(NavbarEditor, "NAVBAR_RECENT"));
			} catch (NoSuchMethodError e1) {
				e1.printStackTrace();
			} catch (NoSuchFieldError e2) {
				e2.printStackTrace();
			}
		}
		
		try {
			menuButton = (ImageView) XposedHelpers.callMethod(mNavigationBarView, "getMenuButton");
		} catch (NoSuchMethodError e) {
			try {
				if (NavbarEditor != null) {
					menuButton =
							(ImageView) XposedHelpers.callMethod(mNavigationBarView,
									"findViewWithTag", getStaticObjectField(NavbarEditor, "NAVBAR_ALWAYS_MENU"));
				}
			} catch (NoSuchMethodError e1) {
				e1.printStackTrace();
			}
		}
		
		try {
			backButton = (ImageView) XposedHelpers.callMethod(mNavigationBarView, "getBackButton");
		} catch (NoSuchMethodError e) {
			try {
				backButton =
						(ImageView) XposedHelpers.callMethod(mNavigationBarView,
								"findViewWithTag", getStaticObjectField(NavbarEditor, "NAVBAR_BACK"));
			} catch (NoSuchMethodError e1) {
				e1.printStackTrace();
			}
		}
		
		try {
			homeButton = (ImageView) XposedHelpers.callMethod(mNavigationBarView, "getHomeButton");
		} catch (NoSuchMethodError e) {
			try {
				homeButton =
						(ImageView) XposedHelpers.callMethod(mNavigationBarView,
								"findViewWithTag", getStaticObjectField(NavbarEditor, "NAVBAR_HOME"));
			} catch (NoSuchMethodError e1) {
				e1.printStackTrace();
			}
		}

		if (recentsButton != null)
			recentsButton.setColorFilter(tintColor);
		if (menuButton != null)
			menuButton.setColorFilter(tintColor);
		if (backButton != null)
			backButton.setColorFilter(tintColor);
		if (homeButton != null)
			homeButton.setColorFilter(tintColor);
		
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
			} catch (Throwable t) { }

			resparam.res.hookLayout("com.android.systemui", "layout", layout, hook);
		} catch (Throwable t) {
			log(t.getMessage());
		}
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
	}

	public int getColorForStatusIcons() {
		return mColorForStatusIcons;
	}

	public SettingsHelper getSettingsHelper() {
		return mSettingsHelper;
	}

	public void setKitKatBatteryView(View batteryView) {
		mKitKatBatteryView = batteryView;
		setKitKatBatteryColor(mLastIconTint);
	}

	public void setTouchWizTransparentStatusBar(boolean transparent) {
		mIsStatusBarNowTransparent = transparent;
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
				textView.setTextColor(mLastIconTint);
			}
		});
	}

	public void onKeyboardVisible(boolean keyboardUp) {
		log("Keyboard visibility changed, isUp? " + keyboardUp);

		if (keyboardUp) {
			setNavigationBarTint(mSettingsHelper.getDefaultTint(Tint.NAV_BAR_IM), true);
			setNavigationBarIconTint(mSettingsHelper.getDefaultTint(Tint.NAV_BAR_ICON_IM), true);
		} else {
			setNavigationBarTint(mNavigationBarTint, true);
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
			
			setNavigationBarTint(transparent, true);
			setNavigationBarIconTint(Color.WHITE, true);
		} else {
			setStatusBarTint(mLastSetColor);
			setStatusBarIconsTint(mLastIconTint);
			
			setNavigationBarTint(mNavigationBarTint, true);
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
			setNavigationBarTint(Color.parseColor("#65000000"), true);
			setNavigationBarIconTint(Color.WHITE, true);
		} else {
			setStatusBarTint(mLastTint);
			setStatusBarIconsTint(mLastIconTint);
			setNavigationBarTint(mNavigationBarTint, true);
			setNavigationBarIconTint(mNavigationBarIconTint, true);
		}
	}
}
