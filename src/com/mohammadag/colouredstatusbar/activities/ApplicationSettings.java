package com.mohammadag.colouredstatusbar.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.mohammadag.colouredstatusbar.Common;
import com.mohammadag.colouredstatusbar.PackageNames;
import com.mohammadag.colouredstatusbar.R;
import com.mohammadag.colouredstatusbar.SettingsHelper;
import com.mohammadag.colouredstatusbar.SettingsHelper.Tint;
import com.mohammadag.colouredstatusbar.SettingsKeys;
import com.mohammadag.colouredstatusbar.Utils;

public class ApplicationSettings extends Activity {

	// Make these random, why not...
	private static final int STATUS_BAR_TINT_COLOR_REQUEST = R.id.status_bar_tint_button;
	private static final int STATUS_BAR_ICON_TINT_COLOR_REQUEST = R.id.icon_tint_button;
	private static final int NAVIGATION_BAR_TINT_COLOR_REQUEST = R.id.navigation_bar_tint_button;
	private static final int NAVIGATION_BAR_ICON_TINT_COLOR_REQUEST = R.id.navigation_bar_icon_tint_button;

	private Switch mSwitch;
	@SuppressWarnings("unused")
	private boolean mDirty = false;
	private String mPackageName;
	private String mActivityName = null;
	@SuppressWarnings("unused")
	private Intent parentIntent;
	private String mStatusBarTint;
	private String mStatusBarIconTint;

	private Button mStatusBarTintButton;
	private Button mStatusBarIconTintButton;
	private Button mNavigationBarTintButton;
	private Button mNavigationBarIconTintButton;

	private SettingsHelper mSettingsHelper = null;

	private Button mResetToAutoDetectButton;
	private CheckBox mLinkPanelsCheckbox;
	private CheckBox mReactToActionBarCheckbox;

	private String mNavigationBarTint;
	private String mNavigationBarIconTint;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_application_settings);

		mSettingsHelper = SettingsHelper.getInstance(getApplicationContext());

		Intent i = getIntent();
		parentIntent = i;

		setTitle(i.getStringExtra(Common.EXTRA_KEY_PACKAGE_FRIENDLY_NAME));

		String activityName = i.getStringExtra(Common.EXTRA_KEY_ACTIVITY_NAME);
		TextView currentActivity = (TextView) findViewById(R.id.currentActivity);
		if (!Common.EXTRA_KEY_VALUE_NONE.equals(activityName)) {
			currentActivity.setText(activityName);
			mActivityName = activityName;
		} else {
			currentActivity.setText(R.string.all_activities_title);
		}

		try {
			String packageName = i.getStringExtra(Common.EXTRA_KEY_PACKAGE_NAME);
			if (packageName.equals(PackageNames.LOCKSCREEN_STUB)) {
				mPackageName = packageName;
				if (Utils.hasActionBar())
					getActionBar().setIcon(getResources().getDrawable(R.drawable.ic_lock));
			} else {
				PackageManager pm = getPackageManager();
				ApplicationInfo app = pm.getApplicationInfo(packageName, 0);
				if (Utils.hasActionBar())
					getActionBar().setIcon(app.loadIcon(pm));
				mPackageName = app.packageName;
			}
		} catch (NameNotFoundException e) {
			// Close the dialog gracefully, package might have been uninstalled
			finish();
			return;
		}

		if (Utils.hasActionBar()) {
			getActionBar().setDisplayShowCustomEnabled(true);
			getActionBar().setDisplayHomeAsUpEnabled(true);
		}

		mStatusBarTint = mSettingsHelper.getTintColor(mPackageName, mActivityName, false);
		if (mStatusBarTint == null) mStatusBarTint = mSettingsHelper.getDefaultTint(Tint.STATUS_BAR, false);
		mStatusBarTintButton = (Button) findViewById(R.id.status_bar_tint_button);
		mStatusBarTintButton.setBackgroundColor(Color.parseColor(Utils.addHashIfNeeded(mStatusBarTint)));
		mStatusBarTintButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				onStatusBarTintColorButtonClicked();
			}
		});
		mStatusBarIconTint = mSettingsHelper.getIconColors(mPackageName, mActivityName, false);
		if (mStatusBarIconTint == null) mStatusBarIconTint = mSettingsHelper.getDefaultTint(Tint.ICON, false);

		mStatusBarIconTintButton = (Button) findViewById(R.id.icon_tint_button);
		mStatusBarIconTintButton.setBackgroundColor(
				Color.parseColor(Utils.addHashIfNeeded(mStatusBarIconTint)));
		mStatusBarIconTintButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				onStatusBarIconTintColorButtonClicked();
			}
		});
		mNavigationBarTint = mSettingsHelper.getNavigationBarTint(mPackageName, mActivityName, false);
		mNavigationBarIconTint = mSettingsHelper.getNavigationBarIconTint(mPackageName, activityName, false);

		mNavigationBarTintButton = (Button) findViewById(R.id.navigation_bar_tint_button);
		mNavigationBarIconTintButton = (Button) findViewById(R.id.navigation_bar_icon_tint_button);

		mNavigationBarTintButton.setBackgroundColor(
				Color.parseColor(Utils.addHashIfNeeded(mNavigationBarTint)));
		mNavigationBarTintButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				onNavigationBarTintButtonClicked();
			}
		});

		mNavigationBarIconTintButton.setBackgroundColor(
				Color.parseColor(Utils.addHashIfNeeded(mNavigationBarIconTint)));
		mNavigationBarIconTintButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				onNavigationBarIconTintButtonClicked();
			}
		});

		mResetToAutoDetectButton = (Button) findViewById(R.id.reset_to_auto_detect_button);
		mResetToAutoDetectButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				resetToAutoDetect();
			}
		});

		mLinkPanelsCheckbox = (CheckBox) findViewById(R.id.link_panels_checkbox);
		mLinkPanelsCheckbox.setChecked(mSettingsHelper.shouldLinkPanels(mPackageName, mActivityName));
		mLinkPanelsCheckbox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				mSettingsHelper.setShouldLinkPanels(mPackageName, mActivityName, isChecked);
			}
		});

		mReactToActionBarCheckbox = (CheckBox) findViewById(R.id.react_actionbar_checkbox);
		mReactToActionBarCheckbox.setChecked(mSettingsHelper.shouldReactToActionBar(mPackageName, mActivityName));
		mReactToActionBarCheckbox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				mSettingsHelper.setShouldReactToActionBar(mPackageName, mActivityName, isChecked);
			}
		});

		if (mActivityName != null)
			findViewById(R.id.package_specifc_options).setVisibility(View.GONE);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.applications_settings, menu);

		MenuItem actionSwitch = menu.findItem(R.id.switch_button);
		mSwitch = (Switch) actionSwitch.getActionView().findViewById(R.id.color_switch);
		if (mSwitch != null) {
			mSwitch.setChecked(mSettingsHelper.isEnabled(mPackageName, mActivityName));

			// Toggle the visibility of the lower panel when changed
			mSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					mDirty = true;
					Editor editor = mSettingsHelper.getSharedPreferences().edit();
					String keyName = SettingsHelper.getKeyName(mPackageName, mActivityName, SettingsKeys.IS_ACTIVE);

					editor.putBoolean(keyName, isChecked);
					editor.commit();
				}
			});
		}

		updateMenuEntries(getApplicationContext(), menu, mPackageName);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			onBackPressed();
			break;
		case R.id.menu_app_launch:
			Intent intent = getPackageManager().getLaunchIntentForPackage(mPackageName);
			if (intent != null) {
				startActivity(intent);
			}
			break;
		case R.id.menu_app_store:
			break;

		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	public static void updateMenuEntries(Context context, Menu menu, String pkgName) {
		if (context.getPackageManager().getLaunchIntentForPackage(pkgName) == null) {
			menu.findItem(R.id.menu_app_launch).setEnabled(false);
			Drawable icon = menu.findItem(R.id.menu_app_launch).getIcon().mutate();
			icon.setColorFilter(Color.GRAY, Mode.SRC_IN);
			menu.findItem(R.id.menu_app_launch).setIcon(icon);
		}

		boolean hasMarketLink = false;
		try {
			PackageManager pm = context.getPackageManager();
			String installer = pm.getInstallerPackageName(pkgName);
			if (installer != null)
				hasMarketLink = installer.equals("com.android.vending") || installer.contains("google");
		} catch (Exception e) {
		}

		menu.findItem(R.id.menu_app_store).setEnabled(hasMarketLink);
		try {
			Resources res = context.createPackageContext("com.android.vending", 0).getResources();
			int id = res.getIdentifier("ic_launcher_play_store", "mipmap", "com.android.vending");
			Drawable icon = res.getDrawable(id);
			if (!hasMarketLink) {
				icon = icon.mutate();
				icon.setColorFilter(Color.GRAY, Mode.SRC_IN);
			}
			menu.findItem(R.id.menu_app_store).setIcon(icon);
		} catch (Exception e) {
		}
	}

	private void onStatusBarTintColorButtonClicked() {
		Intent colorIntent = new Intent(this, ColorPickerActivity.class);
		Bundle bundle = new Bundle();
		bundle.putString("title", getString(R.string.status_bar_tint_text));
		bundle.putString("key", SettingsKeys.STATUS_BAR_TINT);
		String mOldColor = mSettingsHelper.getTintColor(mPackageName, mActivityName, false);
		if (mOldColor == null) mOldColor = mSettingsHelper.getDefaultTint(Tint.STATUS_BAR, false);
		bundle.putString("color", mOldColor);
		colorIntent.putExtras(bundle);
		startActivityForResult(colorIntent, STATUS_BAR_TINT_COLOR_REQUEST);
	}

	private void onStatusBarIconTintColorButtonClicked() {
		Intent colorIntent = new Intent(this, ColorPickerActivity.class);
		Bundle bundle = new Bundle();
		bundle.putString("title", getString(R.string.status_bar_icon_tint_text));
		bundle.putString("key", SettingsKeys.STATUS_BAR_ICON_TINT);
		String mOldColor = mSettingsHelper.getIconColors(mPackageName, mActivityName, false);
		if (mOldColor == null) mOldColor = mSettingsHelper.getDefaultTint(Tint.STATUS_BAR, false);
		boolean isEnabled = mSettingsHelper.isEnabled(mPackageName, mActivityName);
		bundle.putString("color", mOldColor);
		bundle.putBoolean("enabled", isEnabled);
		colorIntent.putExtras(bundle);
		startActivityForResult(colorIntent, STATUS_BAR_ICON_TINT_COLOR_REQUEST);
	}

	private void onNavigationBarTintButtonClicked() {
		Intent colorIntent = new Intent(this, ColorPickerActivity.class);
		Bundle bundle = new Bundle();
		bundle.putString("title", getString(R.string.navigation_bar_tint_text));
		bundle.putString("key", SettingsKeys.NAVIGATION_BAR_TINT);
		String mOldColor = mSettingsHelper.getNavigationBarTint(mPackageName, mActivityName, false);
		bundle.putString("color", mOldColor);
		colorIntent.putExtras(bundle);
		startActivityForResult(colorIntent, NAVIGATION_BAR_TINT_COLOR_REQUEST);
	}

	private void onNavigationBarIconTintButtonClicked() {
		Intent colorIntent = new Intent(this, ColorPickerActivity.class);
		Bundle bundle = new Bundle();
		bundle.putString("title", getString(R.string.navigation_bar_icon_tint_text));
		bundle.putString("key", SettingsKeys.NAVIGATION_BAR_ICON_TINT);
		String mOldColor = mSettingsHelper.getNavigationBarTint(mPackageName, mActivityName, false);
		bundle.putString("color", mOldColor);
		colorIntent.putExtras(bundle);
		startActivityForResult(colorIntent, NAVIGATION_BAR_ICON_TINT_COLOR_REQUEST);
	}

	private void resetToAutoDetect() {
		Editor editor = mSettingsHelper.getSharedPreferences().edit();

		editor.remove(SettingsHelper.getKeyName(mPackageName, mActivityName, SettingsKeys.STATUS_BAR_TINT));
		editor.remove(SettingsHelper.getKeyName(mPackageName, mActivityName, SettingsKeys.STATUS_BAR_ICON_TINT));
		editor.remove(SettingsHelper.getKeyName(mPackageName, mActivityName, SettingsKeys.NAVIGATION_BAR_TINT));
		editor.remove(SettingsHelper.getKeyName(mPackageName, mActivityName, SettingsKeys.NAVIGATION_BAR_ICON_TINT));
		editor.commit();

		mStatusBarTint = mSettingsHelper.getDefaultTint(Tint.STATUS_BAR, false);
		mStatusBarIconTint = mSettingsHelper.getDefaultTint(Tint.ICON, false);

		mStatusBarTintButton.setBackgroundColor(Color.parseColor(Utils.addHashIfNeeded(mStatusBarTint)));
		mStatusBarIconTintButton.setBackgroundColor(Color.parseColor(Utils.addHashIfNeeded(mStatusBarIconTint)));
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (resultCode == Activity.RESULT_CANCELED)
			return;

		if (requestCode == STATUS_BAR_TINT_COLOR_REQUEST) {
			String newColor = Common.COLOR_A_SHADE_OF_GREY;
			if (data != null) {
				if (data.hasExtra("color"))
					newColor = data.getStringExtra("color");
			}
			if ("#0".equals(newColor)) {
				newColor = "#00000000";
			}
			mStatusBarTint = newColor;
			try {
				int color = Color.parseColor(Utils.addHashIfNeeded(newColor));
				mStatusBarTintButton.setBackgroundColor(color);
				mSettingsHelper.setStatusBarTintColor(mPackageName, mActivityName, newColor);
			} catch (IllegalArgumentException e) {
				Toast.makeText(getApplicationContext(), R.string.invalid_color, Toast.LENGTH_SHORT).show();
			}
		} else if (requestCode == STATUS_BAR_ICON_TINT_COLOR_REQUEST) {
			String newColor = Common.COLOR_WHITE;
			if (data != null) {
				if (data.hasExtra("color"))
					newColor = data.getStringExtra("color");
			}
			if ("#0".equals(newColor)) {
				newColor = "#00ffffff";
			}
			mStatusBarIconTint = newColor;
			try {
				int color = Color.parseColor(Utils.addHashIfNeeded(newColor));
				mStatusBarIconTintButton.setBackgroundColor(color);
				mSettingsHelper.setIconColors(mPackageName, mActivityName, newColor);
			} catch (IllegalArgumentException e) {
				Toast.makeText(getApplicationContext(), R.string.invalid_color, Toast.LENGTH_SHORT).show();
			}
		} else if (requestCode == NAVIGATION_BAR_TINT_COLOR_REQUEST) {
			String newColor = Common.COLOR_BLACK;
			if (data != null) {
				if (data.hasExtra("color"))
					newColor = data.getStringExtra("color");
			}
			if ("#0".equals(newColor)) {
				newColor = "#00000000";
			}
			mStatusBarTint = newColor;
			try {
				int color = Color.parseColor(Utils.addHashIfNeeded(newColor));
				mNavigationBarTintButton.setBackgroundColor(color);
				mSettingsHelper.setTintColor(Tint.NAV_BAR, mPackageName, mActivityName, newColor);
			} catch (IllegalArgumentException e) {
				Toast.makeText(getApplicationContext(), R.string.invalid_color, Toast.LENGTH_SHORT).show();
			}
		} else if (requestCode == NAVIGATION_BAR_ICON_TINT_COLOR_REQUEST) {
			String newColor = Common.COLOR_WHITE;
			if (data != null) {
				if (data.hasExtra("color"))
					newColor = data.getStringExtra("color");
			}
			if ("#0".equals(newColor)) {
				newColor = "#00000000";
			}
			mStatusBarTint = newColor;
			try {
				int color = Color.parseColor(Utils.addHashIfNeeded(newColor));
				mNavigationBarIconTintButton.setBackgroundColor(color);
				mSettingsHelper.setTintColor(Tint.NAV_BAR_ICON, mPackageName, mActivityName, newColor);
			} catch (IllegalArgumentException e) {
				Toast.makeText(getApplicationContext(), R.string.invalid_color, Toast.LENGTH_SHORT).show();
			}
		}
	}

}
