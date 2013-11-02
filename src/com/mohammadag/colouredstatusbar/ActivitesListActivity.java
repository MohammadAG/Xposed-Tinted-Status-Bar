package com.mohammadag.colouredstatusbar;

import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.haarman.listviewanimations.ArrayAdapter;
import com.haarman.listviewanimations.swinginadapters.prepared.AlphaInAnimationAdapter;

import com.mohammadag.colouredstatusbar.Common;

public class ActivitesListActivity extends ListActivity {
	private String mPackageName;
	private String mFriendlyPackageName;
	private Switch mSwitch;
	private SettingsHelper mSettingsHelper;
	protected boolean mDirty;

	@SuppressLint("WorldReadableFiles")
	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// TODO: make this is a single instance, or a singleton
		mSettingsHelper = new SettingsHelper(getSharedPreferences(Common.PREFS, Context.MODE_WORLD_READABLE), this);

		getActionBar().setHomeButtonEnabled(true);
		getActionBar().setDisplayHomeAsUpEnabled(true);

		Intent intent = getIntent();
		mPackageName = intent.getStringExtra(Common.EXTRA_KEY_PACKAGE_NAME);
		mFriendlyPackageName = intent.getStringExtra(Common.EXTRA_KEY_PACKAGE_FRIENDLY_NAME);

		loadActivitesForPackage(mPackageName);

		setTitle(mFriendlyPackageName);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activites_list, menu);

		MenuItem actionSwitch = menu.findItem(R.id.switch_button);
		mSwitch = (Switch) actionSwitch.getActionView().findViewById(R.id.color_switch);
		if (mSwitch != null) {
			mSwitch.setChecked(mSettingsHelper.isEnabled(mPackageName, null));

			// Toggle the visibility of the lower panel when changed
			mSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					mDirty = true;
					String keyName = SettingsHelper.getKeyName(mPackageName, null, Common.SETTINGS_KEY_IS_ACTIVE);
					mSettingsHelper.getSharedPreferences().edit().putBoolean(keyName, isChecked).commit();
					getListView().invalidateViews();
				}
			});
		}

		return true;
	}

	@Override
	protected void onListItemClick(ListView list, View view, int position, long id) {
		Intent i = new Intent(getApplicationContext(), ApplicationSettings.class);
		i.putExtra(Common.EXTRA_KEY_PACKAGE_NAME, mPackageName);
		i.putExtra(Common.EXTRA_KEY_PACKAGE_FRIENDLY_NAME, mFriendlyPackageName);
		String activityName = "NONE";
		if (position != 0)
			activityName = Utils.removePackageName(
					((TextView)view.findViewById(android.R.id.text1)).getText().toString(), mPackageName);
		i.putExtra(Common.EXTRA_KEY_ACTIVITY_NAME, activityName);
		startActivityForResult(i, position);
	}

	private void loadActivitesForPackage(String packageName) {
		try {
			PackageManager pm = getPackageManager();
			PackageInfo info = pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
			ActivityInfo[] list = info.activities;
			getActionBar().setIcon(info.applicationInfo.loadIcon(pm));

			List<String> activityNames = new ArrayList<String>(); 
			activityNames.add(getString(R.string.all_the_wonderful_activities));

			if (list == null) {
				Toast.makeText(this, R.string.no_activities_error, Toast.LENGTH_SHORT).show();
				finish();
				return;
			}
			for (int i = 0; i < list.length; i++) {
				activityNames.add(list[i].name);
			}

			ArrayAdapter<String> activityListAdapter = new ArrayAdapter<String>(activityNames) {
				@Override
				public View getView(int position, View convertView, ViewGroup parent) {
					// Load or reuse the view for this row
					View row = convertView;
					if (row == null) {
						row = getLayoutInflater().inflate(android.R.layout.simple_list_item_1, parent, false);
					}

					String activityName = getItem(position);

					TextView textView = (TextView) row.findViewById(android.R.id.text1);
					textView.setText(activityName);
					boolean isEnabled = 
							mSettingsHelper.isEnabled(mPackageName, Utils.removePackageName(activityName, mPackageName));

					textView.setTextColor(isEnabled ? Color.WHITE : Color.RED);

					if (position == 0) {
						textView.setTypeface(null, Typeface.BOLD);
					}

					return row;
				}
			};
			AlphaInAnimationAdapter alphaAdapter = new AlphaInAnimationAdapter(activityListAdapter);
			getListView().setAdapter(alphaAdapter);
			alphaAdapter.setAbsListView(getListView());
		} catch (NameNotFoundException e) {
			e.printStackTrace();
			finish();
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			onBackPressed();
			break;

		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		ListView list = getListView();
		if (requestCode >= list.getFirstVisiblePosition() && requestCode <= list.getLastVisiblePosition()) {
			View v = list.getChildAt(requestCode - list.getFirstVisiblePosition());
			list.getAdapter().getView(requestCode, v, list);
		}
		super.onActivityResult(requestCode, resultCode, data);
	}
}
