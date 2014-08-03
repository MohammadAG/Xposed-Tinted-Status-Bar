package com.mohammadag.colouredstatusbar.activities;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import uk.co.senab.actionbarpulltorefresh.library.ActionBarPullToRefresh;
import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshLayout;
import uk.co.senab.actionbarpulltorefresh.library.listeners.OnRefreshListener;
import android.app.DownloadManager;
import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.mohammadag.colouredstatusbar.DownloadsUtil;
import com.mohammadag.colouredstatusbar.DownloadsUtil.DownloadInfo;
import com.mohammadag.colouredstatusbar.R;
import com.mohammadag.colouredstatusbar.Utils;

public class PluginDownloaderActivity extends ListActivity implements OnRefreshListener {
	private static final String JSON_FILE = "http://mohammadag.xceleo.org/public/Android/Xposed/TintedStatusBarPlugins/.json";
	private ArrayList<Long> mDownloadIds = new ArrayList<Long>();
	private PullToRefreshLayout mPullToRefreshLayout;

	class Plugin {
		String packageName;
		String packageLabel;
		List<String> pluginFor;
		String versionName;
		int versionCode;
		String url;
		String md5sum;
	}

	private int mDefaultListColor = -3;

	private BroadcastReceiver mDownlaodReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			Long dwnId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0);
			if (!mDownloadIds.contains(dwnId))
				return;

			DownloadInfo info = DownloadsUtil.getById(context, dwnId);
			if (info.status == DownloadManager.STATUS_SUCCESSFUL) {
				Intent dlIntent = new Intent(Intent.ACTION_VIEW);
				File file = new File(info.localFilename);
				dlIntent.setDataAndType(Uri.fromFile(file), DownloadsUtil.MIME_TYPE_APK);
				startActivity(dlIntent);
			}
		}
	};
	private Plugin mLongClickPlugin;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		registerForContextMenu(getListView());
		getActionBar().setDisplayHomeAsUpEnabled(true);

		mPullToRefreshLayout = new PullToRefreshLayout(this);

		ViewGroup parent = (ViewGroup) getListView().getParent();

		ActionBarPullToRefresh.from(this)
		.insertLayoutInto(parent)
		.theseChildrenArePullable(android.R.id.list, android.R.id.empty)
		.listener(this)
		.setup(mPullToRefreshLayout);
		mPullToRefreshLayout.setRefreshing(true);

		refresh();
	}

	private void refresh() {
		new GetJsonAsync().execute(JSON_FILE);
	}

	@Override
	protected void onResume() {
		super.onResume();
		registerReceiver(mDownlaodReceiver,
				new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
	}

	@Override
	protected void onPause() {
		unregisterReceiver(mDownlaodReceiver);
		super.onPause();
	}

	private class GetJsonAsync extends AsyncTask<String, Integer, List<Plugin>> {
		@Override
		protected List<Plugin> doInBackground(String... arg0) {
			try {
				HttpClient client = new DefaultHttpClient();
				HttpGet get = new HttpGet(arg0[0]);
				HttpResponse responseGet = client.execute(get);
				HttpEntity resEntityGet = responseGet.getEntity();
				if (resEntityGet != null) {
					String json = EntityUtils.toString(resEntityGet);
					JSONObject obj = new JSONObject(json);
					JSONArray pluginsJson = obj.getJSONArray("plugins");
					ArrayList<Plugin> pluginsList = new ArrayList<Plugin>();

					for (int i = 0; i < pluginsJson.length(); i++) {
						JSONObject pluginJson = pluginsJson.getJSONObject(i);
						Plugin plugin = new Plugin();
						plugin.packageName = pluginJson.getString("packageName");
						plugin.packageLabel = pluginJson.getString("packageLabel");
						String pluginStringPkgs = pluginJson.getString("pluginFor");
						ArrayList<String> pluginPackages = new ArrayList<String>();
						if (pluginStringPkgs.contains("#")) {
							String[] pkgs = pluginStringPkgs.split("#");
							for (String pkg : pkgs) {
								pluginPackages.add(pkg);
							}
						} else {
							pluginPackages.add(pluginStringPkgs);
						}
						plugin.pluginFor = pluginPackages;
						plugin.versionName = pluginJson.getString("versionName");
						plugin.versionCode = pluginJson.getInt("versionCode");
						plugin.url = pluginJson.getString("url");
						plugin.md5sum = pluginJson.getString("md5sum");
						pluginsList.add(plugin);
					}

					return pluginsList;
				}
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			return null;
		}

		@Override
		protected void onPostExecute(List<Plugin> result) {
			super.onPostExecute(result);
			onGotPlugins(result);
		}
	}

	public void onGotPlugins(List<Plugin> result) {
		setListAdapter(new PluginArrayAdapter(this, 0, result));
		mPullToRefreshLayout.setRefreshComplete();
	}

	public class PluginArrayAdapter extends ArrayAdapter<Plugin> {

		public PluginArrayAdapter(Context context, int resource,
				List<Plugin> objects) {
			super(context, resource, objects);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v = convertView;
			if (v == null) {
				v = LayoutInflater.from(getApplicationContext()).inflate(R.layout.plugin_list_item, parent, false);
			}

			Plugin plugin = getItem(position);
			TextView pluginName = (TextView) v.findViewById(R.id.pluginName);
			pluginName.setText(plugin.packageLabel);

			TextView pluginForName = (TextView) v.findViewById(R.id.pluginForName);
			String pluginForText = "";
			for (String pkg : plugin.pluginFor) {
				String label = pkg;
				ApplicationInfo info = Utils.getApplicationInfo(getContext(), pkg);
				if (info != null)
					label = info.loadLabel(getPackageManager()).toString();
				pluginForText += label + "\n";
			}
			pluginForText = pluginForText.substring(0, pluginForText.lastIndexOf("\n"));
			pluginForName.setText(pluginForText);

			TextView versionNumber = (TextView) v.findViewById(R.id.versionNumber);
			if (mDefaultListColor == -3) {
				mDefaultListColor = versionNumber.getCurrentTextColor();
			}

			boolean updateAvailable = false;
			PackageInfo pkgInfo = Utils.getPackageInfo(getContext(), plugin.packageName);
			if (pkgInfo != null) {
				if (pkgInfo.versionCode < plugin.versionCode) {
					versionNumber.setTextColor(Color.GREEN);
					updateAvailable = true;
				} else {
					versionNumber.setTextColor(mDefaultListColor);
				}
			} else {
				versionNumber.setTextColor(mDefaultListColor);
			}
			versionNumber.setText(getString(
					updateAvailable ? R.string.plugin_version_update :R.string.plugin_version,
							plugin.versionName, plugin.versionCode));

			ImageView icon = (ImageView) v.findViewById(R.id.pluginForPackageIcon);
			if (Utils.isPackageInstalled(getContext(), plugin.pluginFor.get(0))) {
				icon.setImageDrawable(Utils.getPackageIcon(getContext(), plugin.pluginFor.get(0)));
			} else {
				icon.setImageResource(R.drawable.ic_launcher);
			}

			return v;
		}
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		Plugin plugin = (Plugin) l.getItemAtPosition(position);

		downloadPlugin(plugin);
	}

	private void downloadPlugin(Plugin plugin) {
		DownloadManager.Request request = new DownloadManager.Request(Uri.parse(plugin.url));
		request.setDescription(getString(R.string.download_description));
		request.setTitle(plugin.packageLabel);
		request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
		request.setVisibleInDownloadsUi(false);
		String fileName = plugin.url.substring(plugin.url.lastIndexOf("/"));
		request.setDestinationInExternalFilesDir(this, Environment.DIRECTORY_DOWNLOADS, fileName);

		DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
		mDownloadIds.add(manager.enqueue(request));
	}

	private void uninstallPlugin(Plugin plugin) {
		if (!Utils.isPackageInstalled(getApplicationContext(), plugin.packageName))
			return;

		startActivity(new Intent(Intent.ACTION_UNINSTALL_PACKAGE, 
				Uri.parse("package:" + plugin.packageName)));
	}

	@Override
	public void onRefreshStarted(View view) {
		refresh();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			onBackPressed();
			return true;
		case R.id.open_xposed:
			Intent intent = new Intent("de.robv.android.xposed.installer.OPEN_SECTION");
			intent.putExtra("section", "modules");
			startActivity(intent);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.plugins, menu);
		MenuItem item = menu.findItem(R.id.open_xposed);
		if (Utils.isPackageInstalled(this, "de.robv.android.xposed.installer")) {
			item.setIcon(Utils.getPackageIcon(this, "de.robv.android.xposed.installer"));
		}
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		AdapterView.AdapterContextMenuInfo info;
		try {
			info = (AdapterView.AdapterContextMenuInfo) menuInfo;
		} catch (ClassCastException e) {
			return;
		}
		Plugin plugin = (Plugin) getListView().getItemAtPosition(info.position);
		if (Utils.isPackageInstalled(this, plugin.packageName)) {
			mLongClickPlugin = plugin;
			menu.add(R.string.uninstall);
		}

		super.onCreateContextMenu(menu, v, menuInfo);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		// This will cause issues if the user has Hodor!
		if (item.getTitle().equals(getString(R.string.uninstall))) {
			uninstallPlugin(mLongClickPlugin);
			mLongClickPlugin = null;
		}
		return super.onContextItemSelected(item);
	}
}
