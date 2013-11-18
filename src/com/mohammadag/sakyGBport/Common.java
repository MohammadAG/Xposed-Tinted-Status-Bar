package com.mohammadag.sakyGBport;

public class Common {
	public static final String PACKAGE_NAME = "com.mohammadag.sakyGBport";

	public static final String INTENT_CHANGE_COLOR_NAME = "com.mohammadag.sakyGBport.ChangeStatusBarColor";
	public static final String INTENT_SETTINGS_UPDATED = "com.mohammadag.sakyGBport.SETTINGS_UPDATED";

	public static final String[] SIGNAL_CLUSTER_ICON_NAMES = {
		"mMobile", "mMobileActivity", "mMobileType",
		"mMobileRoaming", "mWifi", "mWifiActivity",
		"mEthernet", "mEthernetActivity", "mAirplane"	
	};

	public static final String INTENT_SAMSUNG_SVIEW_COVER = "com.samsung.cover.OPEN";
	public static final String KEY_SVIEW_COVER_OPENED = "coverOpen";

	public static final String COLOR_WHITE = "FFFFFF";
	public static final String COLOR_A_SHADE_OF_GREY = "FF080808";
	public static final String COLOR_BLACK = "000000";

	public static final String PREFS = "preferences";

	public static final String SETTINGS_KEY_STATUS_BAR_TINT = "status_bar_color";
	public static final String SETTINGS_KEY_STATUS_BAR_ICON_TINT = "status_bar_icons_color";
	public static final String SETTINGS_KEY_NAVIGATION_BAR_TINT = "navigation_bar_color";
	public static final String SETTINGS_KEY_NAVIGATION_BAR_ICON_TINT = "navigation_bar_icon_tint";

	public static final String SETTINGS_KEY_IS_ACTIVE = "active";
	public static final String SETTINGS_KEY_DEFAULT_STATUS_BAR_TINT = "pref_default_status_bar_tint";
	public static final String SETTINGS_KEY_DEFAULT_STATUS_BAR_ICON_TINT = "pref_default_status_bar_icon_tint";
	public static final String SETTINGS_KEY_DEFAULT_NAV_BAR_TINT = "pref_default_nav_bar_tint";

	public static final String SETTINGS_KEY_ALLOW_API_CHANGES = "pref_allow_api_usage";
	public static final String SETTINGS_KEY_HSV_VALUE = "pref_max_hsv_value_before_using_black_icons";

	public static final String EXTRA_KEY_ACTIVITY_NAME = "activityName";
	public static final String EXTRA_KEY_PACKAGE_NAME = "packageName";
	public static final String EXTRA_KEY_PACKAGE_FRIENDLY_NAME = "packageUserFriendlyName";

	/* TODO: Use null instead of this. */
	public static final String EXTRA_KEY_VALUE_NONE = "NONE";

	public static final String URL_MY_MODULES = "http://repo.xposed.info/module-overview?combine=MohammadAG&sort_by=title";
	public static final String URL_MY_APPS = "market://search?q=pub:Mohammad Abu-Garbeyyeh";
}
