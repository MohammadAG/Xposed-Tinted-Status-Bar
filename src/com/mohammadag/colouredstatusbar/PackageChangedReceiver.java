package com.mohammadag.colouredstatusbar;

import java.util.HashSet;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

public class PackageChangedReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent.getAction().equals(Intent.ACTION_PACKAGE_REMOVED)
				&& intent.getBooleanExtra(Intent.EXTRA_REPLACING, false))
			// Ignore existing packages being removed in order to be updated
			// Thanks rovo89
			return;

		String packageName = getPackageName(intent);
		if (packageName == null)
			return;

		if (intent.getAction().equals(Intent.ACTION_PACKAGE_CHANGED)) {
			// make sure that the change is for the complete package, not only a component
			String[] components = intent.getStringArrayExtra(Intent.EXTRA_CHANGED_COMPONENT_NAME_LIST);
			if (components != null) {
				boolean isForPackage = false;
				for (String component : components) {
					if (packageName.equals(component)) {
						isForPackage = true;
						break;
					}
				}
				if (!isForPackage)
					return;
			}
		}

		String pluginString = null;
		PackageManager pm = context.getPackageManager();
		ApplicationInfo info;
		try {
			info = pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA);
		} catch (NameNotFoundException e) {
			return;
		}
		if (info.metaData != null) {
			Bundle metadata = info.metaData;
			if (!metadata.containsKey(StatusBarTintApi.METADATA_PLUGIN)) {
				return;
			}

			pluginString = metadata.getString(StatusBarTintApi.METADATA_PLUGIN);
		}

		HashSet<String> packages = new HashSet<String>();

		if (pluginString.contains("#")) {
			String[] pkgNames = pluginString.split("#");
			for (String pkg : pkgNames) {
				packages.add(pkg);
			}
		} else {
			packages.add(pluginString);
		}

		SettingsHelper helper = SettingsHelper.getInstance(context);
		if (Intent.ACTION_PACKAGE_REMOVED.equals(intent.getAction())) {
			for (String pkg : packages) {
				Log.d("TintedStatusBar", "Plugin removed for package: " + pkg);
				helper.setEnabled(pkg, null, true);
			}
			return;
		}

		for (String pkg : packages) {
			helper.setEnabled(pkg, null, false);
			Log.d("TintedStatusBar", "Plugin installed for package: " + pkg);
		}
	}

	private static String getPackageName(Intent intent) {
		Uri uri = intent.getData();
		return (uri != null) ? uri.getSchemeSpecificPart() : null;
	}
}
