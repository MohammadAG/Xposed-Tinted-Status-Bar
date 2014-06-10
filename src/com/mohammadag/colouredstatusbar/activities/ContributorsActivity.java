package com.mohammadag.colouredstatusbar.activities;

import java.lang.reflect.Field;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.view.MenuItem;

import com.mohammadag.colouredstatusbar.R;
import com.mohammadag.colouredstatusbar.Utils;

public class ContributorsActivity extends PreferenceActivity {
	@Override
	public SharedPreferences getSharedPreferences(String name, int mode) {
		return super.getSharedPreferences("contirubters", mode);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (Utils.hasActionBar())
			getActionBar().setDisplayHomeAsUpEnabled(true);
	}

	@SuppressWarnings("deprecation")
	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.contributors_prefs);

		PreferenceCategory coders = (PreferenceCategory) findPreference("coders");
		PreferenceCategory designers = (PreferenceCategory) findPreference("designers");
		PreferenceCategory libraries = (PreferenceCategory) findPreference("libraries");
		PreferenceCategory translators = (PreferenceCategory) findPreference("translators");

		Field[] fields = R.array.class.getFields();

		Resources res = getResources();
		for (Field f : fields) {
			String fName = f.getName();
			if (fName.startsWith("contributor_")
					|| fName.startsWith("translator_")
					|| fName.startsWith("library_")
					|| fName.startsWith("designer_")) {
				try {
					int arrayId = (Integer) f.get(null);
					String[] contributor = res.getStringArray(arrayId);
					if (contributor.length != 3)
						continue;

					String name = contributor[0];
					String type = contributor[1];
					String link = contributor[2];

					Preference preference = new Preference(this);
					preference.setTitle(name);
					preference.setSummary(getSummaryHelpfulText(type, link));

					Intent intent = new Intent(Intent.ACTION_VIEW);
					intent.setData(Uri.parse(link));
					preference.setIntent(intent);

					if ("CODER".equals(type)) {
						coders.addPreference(preference);
					} else if ("TRANSLATOR".equals(type)) {
						translators.addPreference(preference);
					} else if ("LIBRARY".equals(type)) {
						libraries.addPreference(preference);
					} else if ("DESIGNER".equals(type)) {
						designers.addPreference(preference);
					} else if ("BITCHING".equals(preference)) {
						throw new RuntimeException("GermainZ asked for this");
					}
				} catch (IndexOutOfBoundsException e) {
					// Doesn't deserve to be added
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	private int getSummaryHelpfulText(String type, String link) {
		if (link.contains("xda-developers.com")) {
			return R.string.link_xda;
		} else if (link.contains("github.com")) {
			if ("LIBRARY".equals(type)) {
				return R.string.link_github_library;
			} else {
				return R.string.link_github;
			}
		} else if (link.contains("facebook.com")) {
			return R.string.link_facebook;
		} else if (link.contains("plus.google.com")) {
			return R.string.link_google_plus;
		} else if (link.contains("twitter.com")) {
			return R.string.link_twitter;
		} else if (link.contains("linkedin.com")) {
			return R.string.link_linkedin;
		} else if (link.contains("youtube")) {
			return R.string.link_youtube;
		}

		if ("LIBRARY".equals(type)) {
			return R.string.link_generic_library;
		} else {
			return R.string.link_generic;
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
			onBackPressed();
		}
		return super.onOptionsItemSelected(item);
	}
}