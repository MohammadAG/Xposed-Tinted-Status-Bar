package com.mohammadag.sakyGBport;

import com.mohammadag.sakyGBport.SettingsHelper.Tint;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class ApplicationSettings extends Activity {

	// Make these random, why not...
	private static final int STATUS_BAR_TINT_COLOR_REQUEST = R.id.status_bar_tint_button;
	private static final int STATUS_BAR_ICON_TINT_COLOR_REQUEST = R.id.icon_tint_button;

	private ToggleButton mSwitch;
	@SuppressWarnings("unused")
	private boolean mDirty = false;
	private SharedPreferences prefs;
	private String mPackageName;
	private String mActivityName = null;
	@SuppressWarnings("unused")
	private Intent parentIntent;
	private String mStatusBarTint;
	private String mStatusBarIconTint;
	private Button mStatusBarTintButton;

	private SettingsHelper mSettingsHelper = null;
	private Button mStatusBarIconTintButton;
	private Button mResetToAutoDetectButton;

	@SuppressLint("WorldReadableFiles")
	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_application_settings);

		prefs = getSharedPreferences(Common.PREFS, Context.MODE_WORLD_READABLE); 
		mSettingsHelper = new SettingsHelper(prefs, this);

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
			PackageManager pm = getPackageManager();
			ApplicationInfo app = pm.getApplicationInfo(i.getStringExtra(Common.EXTRA_KEY_PACKAGE_NAME), 0);
			mPackageName = app.packageName;
		} catch (NameNotFoundException e) {
			// Close the dialog gracefully, package might have been uninstalled
			finish();
			return;
		}




		mStatusBarTint = mSettingsHelper.getTintColor(mPackageName, mActivityName, false);
		if (mStatusBarTint == null) mStatusBarTint = mSettingsHelper.getDefaultTint(Tint.STATUS_BAR, false);
		mStatusBarTintButton = (Button) findViewById(R.id.status_bar_tint_button);
		mStatusBarTintButton.setBackgroundColor(Color.parseColor("#" + mStatusBarTint));
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
				Color.parseColor("#" + mStatusBarIconTint));
		mStatusBarIconTintButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				onStatusBarIconTintColorButtonClicked();
			}
		});

		mResetToAutoDetectButton = (Button) findViewById(R.id.reset_to_auto_detect_button);
		mResetToAutoDetectButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				resetToAutoDetect();
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.applications_settings, menu);
        if(mSettingsHelper.isEnabled(mPackageName, null))
            menu.findItem(R.id.switch_button).setTitle("Disable!");
        else
            menu.findItem(R.id.switch_button).setTitle("Enable!");
		updateMenuEntries(getApplicationContext(), menu, mPackageName);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			onBackPressed();
			break;

        case R.id.switch_button:
            boolean state = !(mSettingsHelper.isEnabled(mPackageName, mActivityName));
            mDirty = true;
            Editor editor = prefs.edit();
            String keyName = SettingsHelper.getKeyName(mPackageName, mActivityName, Common.SETTINGS_KEY_IS_ACTIVE);
            editor.putBoolean(keyName,state);
            editor.commit();
            if(state)
                item.setTitle("Disable!");
            else
                item.setTitle("Enable!");
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
		bundle.putString("key", Common.SETTINGS_KEY_STATUS_BAR_TINT);
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
		bundle.putString("key", Common.SETTINGS_KEY_STATUS_BAR_ICON_TINT);
		String mOldColor = mSettingsHelper.getIconColors(mPackageName, mActivityName, false);
		if (mOldColor == null) mOldColor = mSettingsHelper.getDefaultTint(Tint.STATUS_BAR, false);
		boolean isEnabled = mSettingsHelper.isEnabled(mPackageName, mActivityName);
		bundle.putString("color", mOldColor);
		bundle.putBoolean("enabled", isEnabled);
		colorIntent.putExtras(bundle);
		startActivityForResult(colorIntent, STATUS_BAR_ICON_TINT_COLOR_REQUEST);

	}

	private void resetToAutoDetect() {
		Editor editor = mSettingsHelper.getSharedPreferences().edit();

		editor.remove(SettingsHelper.getKeyName(mPackageName, mActivityName, Common.SETTINGS_KEY_STATUS_BAR_TINT));
		editor.remove(SettingsHelper.getKeyName(mPackageName, mActivityName, Common.SETTINGS_KEY_STATUS_BAR_ICON_TINT));
		editor.remove(SettingsHelper.getKeyName(mPackageName, mActivityName, Common.SETTINGS_KEY_DEFAULT_NAV_BAR_TINT));
		editor.commit();

		mStatusBarTint = mSettingsHelper.getDefaultTint(Tint.STATUS_BAR, false);
		mStatusBarIconTint = mSettingsHelper.getDefaultTint(Tint.ICON, false);

		mStatusBarTintButton.setBackgroundColor(Color.parseColor("#" + mStatusBarTint));
		mStatusBarIconTintButton.setBackgroundColor(Color.parseColor("#" + mStatusBarIconTint));
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
				int color = Color.parseColor("#" + newColor);
				mStatusBarTintButton.setBackgroundColor(color);
				mSettingsHelper.setTintColor(mPackageName, mActivityName, newColor);
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
				int color = Color.parseColor("#" + newColor);
				mStatusBarIconTintButton.setBackgroundColor(color);
				mSettingsHelper.setIconColors(mPackageName, mActivityName, newColor);
			} catch (IllegalArgumentException e) {
				Toast.makeText(getApplicationContext(), R.string.invalid_color, Toast.LENGTH_SHORT).show();
			}
		}
	}

}
