package com.mohammadag.colouredstatusbar.activities;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

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
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.mohammadag.colouredstatusbar.Common;
import com.mohammadag.colouredstatusbar.PackageNames;
import com.mohammadag.colouredstatusbar.R;
import com.mohammadag.colouredstatusbar.SettingsHelper;
import com.mohammadag.colouredstatusbar.SettingsKeys;
import com.mohammadag.colouredstatusbar.Utils;

public class ActivitesListActivity extends ListActivity {
	private String mPackageName;
	private String mFriendlyPackageName;
	private Switch mSwitch;
	private SettingsHelper mSettingsHelper;
	protected boolean mDirty;

	private ArrayList<String> mActivityList = new ArrayList<String>();
	private ArrayList<String> mFilteredActivityList = new ArrayList<String>();

	private MenuItem mSearchItem;
	private ActivityListAdapter mActivityListAdapter;
	private String mNameFilter;

	private static final int GOOGLE_SEARCH_VERSION_CODE_WITH_GEL = 300300170;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mSettingsHelper = SettingsHelper.getInstance(getApplicationContext());

		if (Utils.hasActionBar()) {
			getActionBar().setHomeButtonEnabled(true);
			getActionBar().setDisplayHomeAsUpEnabled(true);
		}

		Intent intent = getIntent();
		mPackageName = intent.getStringExtra(Common.EXTRA_KEY_PACKAGE_NAME);
		mFriendlyPackageName = intent.getStringExtra(Common.EXTRA_KEY_PACKAGE_FRIENDLY_NAME);

		loadActivitesForPackage(mPackageName);

		getListView().setScrollBarStyle(View.SCROLLBARS_OUTSIDE_OVERLAY);

		setTitle(mFriendlyPackageName);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activites_list, menu);

		mSearchItem = menu.findItem(R.id.action_search);
		final SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
		searchView.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);

		mSearchItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
			@Override
			public boolean onMenuItemActionExpand(MenuItem menuItem) {
				searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

					@Override
					public boolean onQueryTextSubmit(String query) {
						mNameFilter = query;
						mActivityListAdapter.getFilter().filter(mNameFilter);
						findViewById(R.id.action_search).clearFocus();
						return false;
					}

					@Override
					public boolean onQueryTextChange(String newText) {
						mNameFilter = newText;
						mActivityListAdapter.getFilter().filter(mNameFilter);
						return false;
					}

				});
				return true;
			}

			@Override
			public boolean onMenuItemActionCollapse(MenuItem menuItem) {
				mActivityListAdapter.getFilter().filter("");
				return true;
			}
		});

		MenuItem actionSwitch = menu.findItem(R.id.switch_button);
		mSwitch = (Switch) actionSwitch.getActionView().findViewById(R.id.color_switch);
		if (mSwitch != null) {
			mSwitch.setChecked(mSettingsHelper.isEnabled(mPackageName, null));

			// Toggle the visibility of the lower panel when changed
			mSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					mDirty = true;
					String keyName = SettingsHelper.getKeyName(mPackageName, null, SettingsKeys.IS_ACTIVE);
					mSettingsHelper.getSharedPreferences().edit().putBoolean(keyName, isChecked).commit();
					getListView().invalidateViews();
				}
			});
		}

		return true;
	}

	@Override
	public boolean onSearchRequested() {
		mSearchItem.expandActionView();
		return super.onSearchRequested();
	}

	@Override
	protected void onListItemClick(ListView list, View view, int position, long id) {
		Intent i = new Intent(getApplicationContext(), ApplicationSettings.class);
		i.putExtra(Common.EXTRA_KEY_PACKAGE_NAME, mPackageName);
		i.putExtra(Common.EXTRA_KEY_PACKAGE_FRIENDLY_NAME, mFriendlyPackageName);
		String activityName = "NONE";
		String activityNameTemp = ((TextView)view.findViewById(android.R.id.text1)).getText().toString();
		if (activityNameTemp != getString(R.string.all_the_wonderful_activities))
			activityName = Utils.removePackageName(activityNameTemp, mPackageName);
		i.putExtra(Common.EXTRA_KEY_ACTIVITY_NAME, activityName);
		startActivityForResult(i, position);
	}

	private void loadActivitesForPackage(String packageName) {
		try {
			mActivityList.clear();

			PackageManager pm = getPackageManager();
			PackageInfo info = pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
			ActivityInfo[] list = info.activities;
			getActionBar().setIcon(info.applicationInfo.loadIcon(pm));

			mActivityList.add(getString(R.string.all_the_wonderful_activities));

			if (list == null) {
				Toast.makeText(this, R.string.no_activities_error, Toast.LENGTH_SHORT).show();
				finish();
				return;
			}

			/* Workaround for GEL not showing in the activity list */
			if (packageName.equals(PackageNames.GOOGLE_SEARCH)) {
				/* Version code from APK on the intenret */
				if (info.versionCode >= GOOGLE_SEARCH_VERSION_CODE_WITH_GEL) {
					if (!mActivityList.contains(PackageNames.GEL_ACTIVITY_NAME)) {
						mActivityList.add(PackageNames.GEL_ACTIVITY_NAME);
					}
				}
			}

			for (int i = 0; i < list.length; i++) {
				mActivityList.add(list[i].name);
			}

			if (packageName.equals("com.google.android.launcher")) {
				Toast.makeText(this, R.string.gel_stub_toast, Toast.LENGTH_LONG).show();
			}

			mActivityListAdapter = new ActivityListAdapter(this, mActivityList);
			setListAdapter(mActivityListAdapter);
		} catch (NameNotFoundException e) {
			e.printStackTrace();
			finish();
		}
	}

	class ActivityListAdapter extends ArrayAdapter<String> implements Filterable {

		private Filter filter;

		public ActivityListAdapter(Context context, List<String> items) {
			super(context, 0, new ArrayList<String>(items));
			mFilteredActivityList.addAll(items);
			filter = new ActivityListFilter(this);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			// Load or reuse the view for this row
			View row = convertView;
			if (row == null) {
				row = getLayoutInflater().inflate(android.R.layout.simple_list_item_1, parent, false);
			}

			String activityName = mFilteredActivityList.get(position);

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

		@Override
		public Filter getFilter() {
			return filter;
		}

	}

	private class ActivityListFilter extends Filter {

		private ActivityListAdapter adaptor;

		ActivityListFilter(ActivityListAdapter adaptor) {
			super();
			this.adaptor = adaptor;
		}

		@SuppressLint("WorldReadableFiles")
		@Override
		protected FilterResults performFiltering(CharSequence constraint) {
			// NOTE: this function is *always* called from a background thread, and
			// not the UI thread.

			ArrayList<String> items = new ArrayList<String>();
			synchronized (this) {
				items.addAll(mActivityList);
			}

			FilterResults result = new FilterResults();
			if (constraint != null && constraint.length() > 0) {
				Pattern regexp = Pattern.compile(constraint.toString(), Pattern.LITERAL | Pattern.CASE_INSENSITIVE);
				for (Iterator<String> i = items.iterator(); i.hasNext(); ) {
					String name = i.next();
					if (!regexp.matcher(name == null ? "" : name).find()) {
						i.remove();
					}
				}
			}

			result.values = items;
			result.count = items.size();

			return result;
		}

		@SuppressWarnings("unchecked")
		@Override
		protected void publishResults(CharSequence constraint, Filter.FilterResults results) {
			// NOTE: this function is *always* called from the UI thread.
			mFilteredActivityList = (ArrayList<String>) results.values;
			adaptor.clear();
			adaptor.addAll(mFilteredActivityList);
			adaptor.notifyDataSetInvalidated();
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
			getListAdapter().getView(requestCode, v, list);
		}
		super.onActivityResult(requestCode, resultCode, data);
	}
}
