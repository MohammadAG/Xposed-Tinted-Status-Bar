package com.mohammadag.sakyGBport;

 /* Status Bar Tinting API v1
  * (C) 2013 Mohammad Abu-Garbeyyeh
  * Feel free to copy this class into your project as is, just change the package declaration above.
  */

import android.content.Context;
import android.content.Intent;

public class StatusBarTintApi {
	/* I was planning to have two different keys but that got annoying during development,
	 * this was done to speed up time. The following keys should be removed eventually.
	 * TODO: What he said ^
	 */
	public static final String KEY_STATUS_BAR_TINT = "status_bar_color";
	public static final String KEY_STATUS_BAR_ICON_TINT = "status_bar_icons_color";
	public static final String KEY_NAVIGATION_BAR_TINT = "navigation_bar_color";
	public static final String KEY_NAVIGATION_BAR_ICON_TINT = "navigation_bar_icon_tint";

	/* You can use this meta-data value to override auto detection of colours. 
	 * <meta-data android:name="override_tinted_status_bar_defaults" android:value="true" />
	 * 
	 * You should implement colour changes by sending an intent from the onResume() method of
	 * each Activity.
	 * 
	 * Here's an example on how to do that (helper method below)
	 *     int color = Color.parseColor("#33b5e5");
	 *     Intent intent = new Intent("com.mohammadag.sakyGBport.ChangeStatusBarColor");
	 *     intent.putExtra("status_bar_color", color);
	 *     intent.putExtra("status_bar_icons_color", Color.WHITE);
	 *     // Please note that these are not yet implemented!!!
	 *     // You're free to include them in your code so that when they 
	 *     // are implemented, your app will work out of the box.
	 *     intent.putExtra("navigation_bar_color", Color.BLACK);
	 *     intent.putExtra("navigation_bar_icon_color", Color.WHITE);
	 *     context.sendOrderedBroadcast(intent, null);
	 */
	protected static final String METADATA_OVERRIDE_COLORS = "override_tinted_status_bar_defaults";

	/* Helper method, pass -1 for a colour you don't want to change */
	public static void sendColorChangeIntent(int statusBarTint, int iconColorTint,
			int navBarTint, int navBarIconTint, Context context) {
		Intent intent = new Intent(Common.INTENT_CHANGE_COLOR_NAME);
		if (statusBarTint != -1)
			intent.putExtra(KEY_STATUS_BAR_TINT, statusBarTint);
		if (iconColorTint != -1)
			intent.putExtra(KEY_STATUS_BAR_ICON_TINT, iconColorTint);
		if (navBarTint != -1)
			intent.putExtra(KEY_NAVIGATION_BAR_TINT, navBarTint);
		if (navBarIconTint != -1)
			intent.putExtra(KEY_NAVIGATION_BAR_ICON_TINT, navBarIconTint);

		context.sendOrderedBroadcast(intent, null);
	}
}
