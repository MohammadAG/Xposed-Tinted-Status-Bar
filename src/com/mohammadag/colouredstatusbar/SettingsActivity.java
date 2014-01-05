package com.mohammadag.colouredstatusbar;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.view.MenuItem;
import android.widget.Toast;

public class SettingsActivity extends PreferenceActivity {
	private SettingsHelper mSettingsHelper = null;

	@Override
	public SharedPreferences getSharedPreferences(String name, int mode) {
		return Utils.getSharedPreferences(getApplicationContext());
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);	
		if (Utils.hasActionBar())
			getActionBar().setDisplayHomeAsUpEnabled(true);

		if (mSettingsHelper == null)
			mSettingsHelper = new SettingsHelper(this);

	}

	@SuppressWarnings({ "deprecation" })
	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);

		getPreferenceManager().setSharedPreferencesMode(MODE_WORLD_READABLE);
		addPreferencesFromResource(R.xml.pref_general);

		Preference copyrightPreference = findPreference("copyright_key");
		copyrightPreference.setOnPreferenceClickListener(new OnPreferenceClickListener() {

			@Override
			public boolean onPreferenceClick(Preference preference) {
				AlertDialog.Builder builder = new AlertDialog.Builder(SettingsActivity.this);
				builder.setTitle("")
				.setItems(R.array.my_apps, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						Uri uri = null;
						Intent intent = new Intent(Intent.ACTION_VIEW);
						switch (which) {
						case 0:
							uri = Uri.parse(Common.URL_MY_APPS);
							intent.setPackage("com.android.vending");
							break;
						case 1:
							uri = Uri.parse(Common.URL_MY_MODULES);
							break;
						case 2:
							uri = Uri.parse("https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=KGGZ5C3AVC8ZU");
							break;
						}
						startActivity(intent.setData(uri));
					}
				});
				builder.create().show();
				return false;
			}
		});

		Preference donatePreference = findPreference("donate_key");
		donatePreference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference arg0) {
				Intent intent = new Intent(Intent.ACTION_VIEW).setData(Uri.parse(Common.URL_DONATION_PACKAGE));
				intent.setPackage("com.android.vending");
				startActivity(intent);
				return false;
			}
		});

		if (Utils.isDonateVersionInstalled(getApplicationContext())) {
			copyrightPreference.setTitle(R.string.app_name_donate_version);

			donatePreference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference preference) {
					Toast.makeText(SettingsActivity.this, R.string.thank_you, Toast.LENGTH_SHORT).show();
					return false;
				}
			});

			donatePreference.setTitle(R.string.thank_you);
			donatePreference.setSummary(R.string.thank_you_summary);
		}

		String[] colorKeys = {
				Common.SETTINGS_KEY_DEFAULT_STATUS_BAR_TINT,
				Common.SETTINGS_KEY_DEFAULT_STATUS_BAR_ICON_TINT,
				Common.SETTINGS_KEY_DEFAULT_STATUS_BAR_INVERTED_ICON_TINT,
				Common.SETTINGS_KEY_DEFAULT_NAV_BAR_TINT,
				Common.SETTINGS_KEY_DEFAULT_NAV_BAR_ICON_TINT,
				Common.SETTINGS_KEY_DEFAULT_NAV_BAR_IM_TINT,
				Common.SETTINGS_KEY_DEFAULT_NAV_BAR_ICON_IM_TINT
		};

		intializeColorPreferences(colorKeys);
		
		findPreference(Common.SETTINGS_KEY_LINK_PANEL_VIEW_COLORS).setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				sendBroadcast(new Intent(Common.INTENT_SETTINGS_UPDATED));
				return true;
			}
		});
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
			onBackPressed();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@SuppressWarnings("deprecation")
	private ColorPreference findColorPreference(String key) {
		return (ColorPreference) findPreference(key);
	}

	private void intializeColorPreferences(String[] keys) {
		for (String key: keys) {
			findColorPreference(key).setSettingsActivity(this).setSettingsHelper(mSettingsHelper);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode != RESULT_OK)
			return;

		String key = data.getStringExtra("key");
		if (key == null)
			return;

		mSettingsHelper.getSharedPreferences().edit().putString(key, data.getStringExtra("color")).commit();
		findColorPreference(key).refresh();
		super.onActivityResult(requestCode, resultCode, data);
	}
}
