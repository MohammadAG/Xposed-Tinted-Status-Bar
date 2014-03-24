package com.mohammadag.colouredstatusbar;

public class Common {
	public static final String PACKAGE_NAME = "com.mohammadag.colouredstatusbar";
	public static final String PACKAGE_NAME_DONATE = "com.mohammadag.tinedstatusbardonate";
	public static final String PACKAGE_NAME_GOOGLE_SEARCH = "com.google.android.googlequicksearchbox";
	public static final String PACKAGE_NAME_GEL_STUB = "com.google.android.launcher";
	public static final String GEL_ACTIVITY_NAME = "com.google.android.launcher.GEL";
	public static final int GOOGLE_SEARCH_VERSION_CODE_WITH_GEL = 300300170;
	public static final String PACKAGE_NAME_LOCKSCREEN_STUB = "com.mohammadag.tintedstatusbarlockscreenstub";

	public static final String INTENT_CHANGE_COLOR_NAME = "com.mohammadag.colouredstatusbar.ChangeStatusBarColor";
	public static final String INTENT_SETTINGS_UPDATED = "com.mohammadag.colouredstatusbar.SETTINGS_UPDATED";

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

	public static final String SETTINGS_KEY_DEFAULT_STATUS_BAR_TINT = "pref_default_status_bar_tint";
	public static final String SETTINGS_KEY_DEFAULT_STATUS_BAR_ICON_TINT = "pref_default_status_bar_icon_tint";
	public static final String SETTINGS_KEY_DEFAULT_STATUS_BAR_INVERTED_ICON_TINT = "pref_default_status_bar_inverted_icon_tint";
	public static final String SETTINGS_KEY_DEFAULT_NAV_BAR_TINT = "pref_default_nav_bar_tint";
	public static final String SETTINGS_KEY_DEFAULT_NAV_BAR_ICON_TINT = "pref_default_nav_bar_icon_tint";
	public static final String SETTINGS_KEY_DEFAULT_NAV_BAR_IM_TINT = "pref_default_nav_bar_im_tint";
	public static final String SETTINGS_KEY_DEFAULT_NAV_BAR_ICON_IM_TINT = "pref_default_nav_bar_icon_im_tint";
	public static final String SETTINGS_KEY_IS_ACTIVE = "active";

	public static final String SETTINGS_KEY_ALLOW_API_CHANGES = "pref_allow_api_usage";
	public static final String SETTINGS_KEY_HSV_VALUE = "pref_max_hsv_value_before_using_black_icons";

	public static final String SETTINGS_KEY_TOAST_ACTIVITY_NAMES = "pref_toast_activity_names";
	public static final String SETTINGS_KEY_REACT_TO_ACTION_BAR_VISIBILITY = "pref_react_action_bar_visibility";
	public static final String SETTINGS_KEY_ANIMATE_TINT_CHANGE = "pref_animate_tint";
	public static final String SETTINGS_KEY_SYSTEM_ICON_CF_MODE = "pref_system_icon_colorfilter_mode";
	public static final String SETTINGS_KEY_NOTIFICATION_ICON_CF_MODE = "pref_notification_icon_colorfilter_mode";
	public static final String SETTINGS_KEY_LINK_PANEL_VIEW_COLORS = "pref_link_panel_view_colors";
	public static final String SETTINGS_KEY_DEBUG_MODE = "pref_debug_mode";
	public static final String SETTINGS_KEY_REACT_LIGHTS_OUT = "pref_react_lights_out";
	public static final String SETTINGS_KEY_RESPECT_KITKAT_API = "pref_respect_kitkat_api";

	public static final String EXTRA_KEY_ACTIVITY_NAME = "activityName";
	public static final String EXTRA_KEY_PACKAGE_NAME = "packageName";
	public static final String EXTRA_KEY_PACKAGE_FRIENDLY_NAME = "packageUserFriendlyName";

	/* TODO: Use null instead of this. */
	public static final String EXTRA_KEY_VALUE_NONE = "NONE";

	public static final String URL_MY_MODULES = "http://repo.xposed.info/module-overview?combine=MohammadAG&sort_by=title";
	public static final String URL_MY_APPS = "market://search?q=pub:Mohammad Abu-Garbeyyeh";
	public static final String URL_DONATION_PACKAGE = "market://details?id=" + PACKAGE_NAME_DONATE;
}
