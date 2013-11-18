package com.mohammadag.sakyGBport;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SectionIndexer;
import android.widget.TextView;

public class MainActivity extends Activity {

	private SharedPreferences prefs;
	private SettingsHelper mSettingsHelper;

	private ListView mListView = null;
	private ArrayList<ApplicationInfo> appList = new ArrayList<ApplicationInfo>();
	private ArrayList<ApplicationInfo> filteredAppList = new ArrayList<ApplicationInfo>();

	static class ViewHolder {
		TextView app_name;
		TextView app_package;
		ImageView app_icon;
		int position;
		ApplicationInfo app_info;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		prefs = Utils.getSharedPreferences(this);
		mSettingsHelper = new SettingsHelper(prefs, this);

		mListView = (ListView) findViewById(R.id.listView);
		mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {


				String pkgName = ((TextView) view.findViewById(R.id.app_package)).getText().toString();
				String friendlyName = ((TextView) view.findViewById(R.id.app_name)).getText().toString();
				Intent i = new Intent(getApplicationContext(), ActivitesListActivity.class);
				i.putExtra("packageName", pkgName);
				i.putExtra("packageUserFriendlyName", friendlyName);
				startActivityForResult(i, position);
			}
		});

        ProgressDialog dialog;



            dialog = new ProgressDialog(((ListView) findViewById(R.id.listView)).getContext());
            dialog.setMessage(getString(R.string.loading_title));
            dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            dialog.setCancelable(false);
            dialog.show();




            if (appList.size() == 0) {
                loadApps(dialog);
            }




            AppListAdaptor appListAdaptor = new AppListAdaptor(MainActivity.this, appList);
            mListView.setAdapter(appListAdaptor);
            mListView.setFastScrollEnabled(true);
            //mListView.setFastScrollAlwaysVisible(true);

            try {
                dialog.dismiss();
            } catch (Exception e) {

            }
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			onBackPressed();
			return true;
		case R.id.action_settings:
			startActivity(new Intent(this, SettingsActivity.class));
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (mListView == null)
			return;

		if (requestCode >= mListView.getFirstVisiblePosition() && requestCode <= mListView.getLastVisiblePosition()) {
			View v = mListView.getChildAt(requestCode - mListView.getFirstVisiblePosition());
			mListView.getAdapter().getView(requestCode, v, mListView);
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	@SuppressLint("DefaultLocale")
	class AppListAdaptor extends ArrayAdapter<ApplicationInfo> implements SectionIndexer {

		private Map<String, Integer> alphaIndexer;
		private String[] sections;

		@SuppressLint("DefaultLocale")
		public AppListAdaptor(Context context, List<ApplicationInfo> items) {
			super(context, R.layout.app_list_item, new ArrayList<ApplicationInfo>(items));

			filteredAppList.addAll(items);
			alphaIndexer = new HashMap<String, Integer>();
			for(int i = filteredAppList.size() - 1; i >= 0; i--)
			{
				ApplicationInfo app = filteredAppList.get(i);
				String appName = app.name;
				String firstChar;
				if (appName == null || appName.length() < 1) {
					firstChar = "@";
				} else {
					firstChar = appName.substring(0, 1).toUpperCase();
					if(firstChar.charAt(0) > 'Z' || firstChar.charAt(0) < 'A')
						firstChar = "@";
				}

				alphaIndexer.put(firstChar, i);
			}

			Set<String> sectionLetters = alphaIndexer.keySet();

			// create a list from the set to sort
			List<String> sectionList = new ArrayList<String>(sectionLetters); 

			Collections.sort(sectionList);

			sections = new String[sectionList.size()];
			sectionList.toArray(sections);
		}

		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {
			// Load or reuse the view for this row
			View row = convertView;
			if (row == null) {
				row = getLayoutInflater().inflate(R.layout.app_list_item, parent, false);
			}

			ApplicationInfo app = filteredAppList.get(position);

			if (row.getTag() == null) {
				ViewHolder holder = new ViewHolder();
				holder.app_icon = (ImageView) row.findViewById(R.id.app_icon);	
				holder.app_name = (TextView) row.findViewById(R.id.app_name);
				holder.app_package = (TextView) row.findViewById(R.id.app_package);
				holder.position = position;
				holder.app_info = app;
				row.setTag(holder);
			}

			ViewHolder holder = (ViewHolder) row.getTag();

			holder.app_name.setText(app.name == null ? "" : app.name);
			holder.app_package.setTextColor(mSettingsHelper.isEnabled(app.packageName, null)
					? Color.parseColor("#0099CC") : Color.RED);
			holder.app_package.setText(app.packageName);
			holder.app_icon.setTag(app.packageName);
			holder.app_icon.setVisibility(View.INVISIBLE);

			ImageLoader il = new ImageLoader(holder.app_icon, app.packageName);
            il.onPostExecute(il.doInBackground(app));

			return row;
		}

		@Override
		public int getPositionForSection(int section) {
			if (section >= sections.length)
				return filteredAppList.size() - 1;

			return alphaIndexer.get(sections[section]);
		}

		@Override
		public int getSectionForPosition(int position) {

			// Iterate over the sections to find the closest index
			// that is not greater than the position
			int closestIndex = 0;
			int latestDelta = Integer.MAX_VALUE;

			for (int i = 0; i < sections.length; i++) {
				int current = alphaIndexer.get(sections[i]);
				if (current == position) {
					// If position matches an index, return it immediately
					return i;
				} else if (current < position) {
					// Check if this is closer than the last index we inspected
					int delta = position - current;
					if (delta < latestDelta) {
						closestIndex = i;
						latestDelta = delta;
					}
				}
			}

			return closestIndex;
		}

		@Override
		public Object[] getSections() {
			return sections;
		}
	}

	@SuppressLint("DefaultLocale")
	private void loadApps(ProgressDialog dialog) {
		appList.clear();

		PackageManager pm = getPackageManager();
		List<ApplicationInfo> apps = getPackageManager().getInstalledApplications(0);
		dialog.setMax(apps.size());
		int i = 1;
		for (ApplicationInfo appInfo : apps) {
			dialog.setProgress(i++);

			if (appInfo == null)
				continue;

			appInfo.name = appInfo.loadLabel(pm).toString();
			appList.add(appInfo);
		}

		Collections.sort(appList, new Comparator<ApplicationInfo>() {
			@SuppressLint("DefaultLocale")
			@Override
			public int compare(ApplicationInfo lhs, ApplicationInfo rhs) {
				if (lhs.name == null) {
					return -1;
				} else if (rhs.name == null) {
					return 1;
				} else {
					return lhs.name.toUpperCase().compareTo(rhs.name.toUpperCase());
				}
			}
		});
	}

	class ImageLoader extends AsyncTask<Object, Void, Drawable> {
		private ImageView imageView;
		private String mPackageName;

		public ImageLoader(ImageView view, String packageName) {
			mPackageName = packageName;
			imageView = view;
		}

		@Override
		protected Drawable doInBackground(Object... params) {
			ApplicationInfo info = (ApplicationInfo) params[0];
			return getPackageManager().getApplicationIcon(info);
		}

		@Override
		protected void onPostExecute(Drawable result) {
			super.onPostExecute(result);
			if (imageView.getTag().toString().equals(mPackageName)) {
				imageView.setImageDrawable(result);
				imageView.setVisibility(View.VISIBLE);
			}
		}
	}
}
