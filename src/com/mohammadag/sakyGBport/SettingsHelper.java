package com.mohammadag.sakyGBport;

import android.app.AndroidAppHelper;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;

public class SettingsHelper {
	private XSharedPreferences mXPreferences = null;
	private SharedPreferences mPreferences = null;
	private Context mContext = null;

	@SuppressWarnings("unused")
	private static final boolean DEBUG = true;

	/* TODO: Rework this class to use this enum for more consistent code */
	public enum Tint { STATUS_BAR, ICON, NAV_BAR };

	// To be used from within module class.
	public SettingsHelper(XSharedPreferences prefs) {
		mXPreferences = prefs;
	}

	// For activitiy classes.
	public SettingsHelper(SharedPreferences prefs, Context context) {
		mPreferences = prefs;
		mContext = context;
	}

	public SettingsHelper(Context context) {
		mPreferences = Utils.getSharedPreferences(context);
		mContext = context;
	}

	public SharedPreferences getSharedPreferences() {
		return mPreferences;
	}

	// This returns whether the activity is enabled in our settings
	// not in Android's package manager.
	public boolean isEnabled(String packageName, String activityName) {
		if (activityName == null) {
			String keyName = getKeyName(packageName, null, Common.SETTINGS_KEY_IS_ACTIVE);
			return getBoolean(keyName, true);
		} else {
			String keyName = getKeyName(packageName, activityName, Common.SETTINGS_KEY_IS_ACTIVE);
			return getBoolean(keyName, isEnabled(packageName, null));
		}
	}

	public String getTintColor(String packageName, String activityName, boolean withHash) {
		String keyName = getKeyName(packageName, activityName, Common.SETTINGS_KEY_STATUS_BAR_TINT);
		String defaultValue = getDefaultTintColor(packageName, activityName);
		String hexColor = getString(keyName, defaultValue);
		if (withHash && hexColor != null)
			hexColor = "#" + hexColor;
		if (mXPreferences != null)
			XposedBridge.log("getTintColorForPackage: " + packageName+"Color "+hexColor);
		return hexColor;
	}

	public String getIconColors(String packageName, String activityName, boolean withHash) {
		String keyName = getKeyName(packageName, activityName,
				Common.SETTINGS_KEY_STATUS_BAR_ICON_TINT);

		String defaultValue;

		if (activityName == null) {
			defaultValue = getDefaultIconTintColorForPackage(packageName);
		} else {
			/* If we're getting the colour for an activity, we have to check if the user
			 * is using the main override, if they are, we shouldn't provide our own default
			 * value since that collides with what the user expects.
			 */
			String overrideKey = getKeyName(packageName, null, Common.SETTINGS_KEY_STATUS_BAR_ICON_TINT);
			String overrideValue = getString(overrideKey, null);
			if (overrideValue == null) {
				defaultValue = getDefaultIconTintColorForActivity(packageName, activityName);
			} else {
				defaultValue = overrideValue;
			}
		}

		String hexColor = getString(keyName, defaultValue);
		if (withHash && hexColor != null)
			hexColor = "#" + hexColor;

		if (mXPreferences != null)
			XposedBridge.log("getIconColors: " + packageName + " " + activityName + " : " + hexColor);

		return hexColor;
	}

	/* Setters */
	public void setTintColor(String packageName, String activityName, String color) {
		if (mPreferences == null) {
			return;
		}

		mPreferences.edit().putString(getKeyName(packageName, activityName,
				Common.SETTINGS_KEY_STATUS_BAR_TINT), color).commit();

		mContext.sendBroadcast(new Intent(Common.INTENT_SETTINGS_UPDATED));
	}

	public void setIconColors(String packageName, String activityName, String color) {
		if (mPreferences == null)
			return;

		mPreferences.edit().putString(getKeyName(packageName, activityName,
				Common.SETTINGS_KEY_STATUS_BAR_ICON_TINT), color).commit();

		mContext.sendBroadcast(new Intent(Common.INTENT_SETTINGS_UPDATED));
	}

	public void setEnabled(String packageName, String activityName, boolean shouldEnable) {
		String keyName = getKeyName(packageName, activityName, Common.SETTINGS_KEY_IS_ACTIVE);
		mPreferences.edit().putBoolean(keyName, shouldEnable).commit();
	}

	/* Getters */
	private String getDefaultTintColor(String packageName, String activityName) {
		if (activityName == null)
			return getDefaultTintColorForPackage(packageName);
		else
			return getDefaultTintColorForActivity(packageName, activityName);
	}

	// Hand-picked defaults :)
	private String getDefaultTintColorForActivity(String packageName, String activityName) {
		if ("bbc.mobile.news.ww".equals(packageName)) {
			if ("bbc.mobile.news.video.VideoActivity".equals(activityName))
				return Common.COLOR_BLACK;
		}

		if ("com.paypal.android.p2pmobile".equals(packageName)) {
			if ("activity.LoginActivity".equals(activityName))
				return "55a0cc";
			else if ("com.paypal.android.choreographer.flows.firsttimeuse.FirstTimeUseCarouselActivity".equals(activityName))
				return "ffffff";
		}

		if ("com.google.android.apps.plus".equals(packageName)) {
			if ("phone.LocationPickerActivity".equals(activityName))
				return "292929";
		}

		return getTintColor(packageName, null, false);
	}

	private static String getDefaultTintColorForPackage(String packageName) {
		/*if ("com.android.vending".equals(packageName))
			return "96aa39";
		else if ("com.google.android.talk".equals(packageName))
			return "e5e5e5";
		else if ("com.viber.voip".equals(packageName))
			return "774f99";
		else if ("com.whatsapp".equals(packageName))
			return "2f4444";
		 */
		if ("com.skype.raider".equals(packageName))
			return "01aef0";
		else if ("com.dropbox.android".equals(packageName))
			return "007de3";
		else if ("com.google.android.gm".equals(packageName))
			return "dddddd";
		else if ("com.chrome.beta".equals(packageName) || "com.android.chrome".equals(packageName))
			return "e1e1e1";
		else if ("bbc.mobile.news.ww".equals(packageName))
			return "990000";
		else if ("com.paypal.android.p2pmobile".equals(packageName))
			return "50443d";
		else if ("com.google.android.apps.plus".equals(packageName))
			return "dddddd";
		else if ("com.evernote".equals(packageName))
			return "57a330";

		return null;
		//return Common.COLOR_A_SHADE_OF_GREY;
	}

	private static String getDefaultIconTintColorForActivity(String packageName, String activityName) {
		if ("com.google.android.apps.plus".equals(packageName)) {
			if ("phone.LocationPickerActivity".equals(activityName))
				return Common.COLOR_WHITE;
		}

		if ("com.paypal.android.p2pmobile".equals(packageName)) {
			if ("activity.LoginActivity".equals(activityName))
				return Common.COLOR_WHITE;
			else if ("com.paypal.android.choreographer.flows.firsttimeuse.FirstTimeUseCarouselActivity".equals(activityName))
				return Common.COLOR_BLACK;
		}

		return getDefaultIconTintColorForPackage(packageName);
	}

	private static String getDefaultIconTintColorForPackage(String packageName) {
		if ("com.android.vending".equals(packageName))
			return Common.COLOR_WHITE;
		else if ("com.google.android.talk".equals(packageName))
			return "FF080808";
		else if ("com.viber.voip".equals(packageName))
			return Common.COLOR_WHITE;
		else if ("com.skype.raider".equals(packageName))
			return Common.COLOR_WHITE;
		else if ("com.whatsapp".equals(packageName))
			return Common.COLOR_WHITE;
		else if ("com.dropbox.android".equals(packageName))
			return Common.COLOR_WHITE;
		else if ("com.google.android.gm".equals(packageName))
			return "000000";
		else if ("com.chrome.beta".equals(packageName) || "com.android.chrome".equals(packageName))
			return "090909";
		else if ("bbc.mobile.news.ww".equals(packageName))
			return Common.COLOR_WHITE;	
		else if ("com.paypal.android.p2pmobile".equals(packageName))
			return Common.COLOR_WHITE;
		else if ("com.google.android.apps.plus".equals(packageName))
			return Common.COLOR_BLACK;
		else if ("com.evernote".equals(packageName))
			return Common.COLOR_WHITE;

		return null;
	}

	@SuppressWarnings("deprecation")
	public void reload() {
		if (mPreferences != null)
			AndroidAppHelper.reloadSharedPreferencesIfNeeded(mPreferences);
		if (mXPreferences != null)
			mXPreferences.reload();
	}

	public String getDefaultTint(Tint tintType, boolean withHash) {
		switch (tintType) {
		case STATUS_BAR:
			return getColorForKey(Common.SETTINGS_KEY_DEFAULT_STATUS_BAR_TINT, Common.COLOR_BLACK, withHash);
		case NAV_BAR:
			return getColorForKey(Common.SETTINGS_KEY_DEFAULT_NAV_BAR_TINT, Common.COLOR_BLACK, withHash);
		case ICON:
			return getColorForKey(Common.SETTINGS_KEY_DEFAULT_STATUS_BAR_ICON_TINT, Common.COLOR_WHITE, withHash);
		}

		return Common.COLOR_BLACK;
	}

	public int getDefaultTint(Tint tintType) {
		switch (tintType) {
		case STATUS_BAR:
			return getColorForKey(Common.SETTINGS_KEY_DEFAULT_STATUS_BAR_TINT, Color.BLACK);
		case NAV_BAR:
			return getColorForKey(Common.SETTINGS_KEY_DEFAULT_NAV_BAR_TINT, Color.BLACK);
		case ICON:
			return getColorForKey(Common.SETTINGS_KEY_DEFAULT_STATUS_BAR_ICON_TINT, Color.WHITE);
		}

		return Color.BLACK;
	}

	private int getColorForKey(String key, int defaultColor) {
		String colorFromPreferences = getString(key, null);
		if (colorFromPreferences == null) {
			return defaultColor;
		} else {
			int color;
			try {
				color = Color.parseColor("#" + colorFromPreferences);
				return color;
			} catch (IllegalArgumentException e) {
				return defaultColor;
			}
		}
	}

	@Deprecated
	private String getColorForKey(String key, String defaultColor, boolean withHash) {
		String colorFromPreferences = getString(key, null);
		if (colorFromPreferences == null) {
			return defaultColor;
		} else {
			return colorFromPreferences;
		}
	}

	/* Helper methods */
	public String getString(String key, String defaultValue) {
		String returnResult = defaultValue;
		if (mPreferences != null) {
			returnResult = mPreferences.getString(key, defaultValue);
		} else if (mXPreferences != null) {
			returnResult = mXPreferences.getString(key, defaultValue);
		}

		return returnResult;
	}

	public boolean getBoolean(String key, boolean defaultValue) {
		boolean returnResult = defaultValue;
		if (mPreferences != null) {
			returnResult = mPreferences.getBoolean(key, defaultValue);
		} else if (mXPreferences != null) {
			returnResult = mXPreferences.getBoolean(key, defaultValue);
		}
		return returnResult;
	}

	private float getFloat(String key, float defaultValue) {
		float returnResult = defaultValue;
		if (mPreferences != null) {
			returnResult = mPreferences.getFloat(key, defaultValue);
		} else if (mXPreferences != null) {
			returnResult = mXPreferences.getFloat(key, defaultValue);
		}
		return returnResult;
	}

	public static String getKeyName(String packageName, String activityName, String keyName) {
		if (activityName == null) {
			return packageName + "/" + keyName;
		} else {
			String processedActivityName = Utils.removePackageName(activityName, packageName);
			return packageName + "." + processedActivityName + "/" + keyName;
		}
	}

	public float getHsvMax() {
		return getFloat(Common.SETTINGS_KEY_HSV_VALUE, 0.7f);
	}
}
