package com.mohammadag.colouredstatusbar;

public class Common {

	public static final int GOOGLE_SEARCH_VERSION_CODE_WITH_GEL = 300300170;

	public static final String INTENT_CHANGE_COLOR_NAME = "com.mohammadag.colouredstatusbar.ChangeStatusBarColor";
	public static final String INTENT_SAVE_ACTIONBAR_COLOR_NAME = "com.mohammadag.colouredstatusbar.SaveActionBarColor";
	public static final String INTENT_RESET_ACTIONBAR_COLOR_NAME = "com.mohammadag.colouredstatusbar.ResetActionBarColor";
	public static final String INTENT_SETTINGS_UPDATED = "com.mohammadag.colouredstatusbar.SETTINGS_UPDATED";

	public static final String INTENT_SAMSUNG_SVIEW_COVER = "com.samsung.cover.OPEN";
	public static final String KEY_SVIEW_COVER_OPENED = "coverOpen";

	public static final String COLOR_WHITE = "FFFFFF";
	public static final String COLOR_A_SHADE_OF_GREY = "FF080808";
	public static final String COLOR_BLACK = "000000";

	public static final String EXTRA_KEY_ACTIVITY_NAME = "activityName";
	public static final String EXTRA_KEY_PACKAGE_NAME = "packageName";
	public static final String EXTRA_KEY_PACKAGE_FRIENDLY_NAME = "packageUserFriendlyName";

	/* TODO: Use null instead of this. */
	public static final String EXTRA_KEY_VALUE_NONE = "NONE";

	public static final String URL_MY_MODULES = "http://repo.xposed.info/module-overview?combine=MohammadAG&sort_by=title";
	public static final String URL_MY_APPS = "market://search?q=pub:Mohammad Abu-Garbeyyeh";
	public static final String URL_DONATION_PACKAGE = "market://details?id=" + PackageNames.DONATION;
}
