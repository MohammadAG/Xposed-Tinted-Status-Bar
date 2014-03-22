package com.mohammadag.colouredstatusbar;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

public class ColorPreference extends Preference implements Preference.OnPreferenceClickListener {
	private SettingsHelper mSettingsHelper;
	private SettingsActivity mSettingsActivity;
	private ImageView mImageView;

	public ColorPreference(Context context) {
		super(context);
		init(context, null);
	}

	public ColorPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context, attrs);
	}

	public ColorPreference(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context, attrs);
	}

	private void init(Context context, AttributeSet attrs) {
		setWidgetLayoutResource(R.layout.color_preference);
		setOnPreferenceClickListener(this);
	}

	@Override
	protected void onBindView(View view) {
		super.onBindView(view);
		mImageView = (ImageView) view.findViewById(R.id.color_image);
		int color;
		try {
			color = Color.parseColor(Utils.addHashIfNeeded(getCurrentColor()));
		} catch (IllegalArgumentException e) {
			color = Color.RED;
			setSummary(R.string.invalid_color);
			Toast.makeText(view.getContext(), R.string.invalid_color, Toast.LENGTH_SHORT).show();
		}
		ColorDrawable background = new ColorDrawable(color);
		Utils.setImageViewBackground(mImageView, background);
	}

	public ColorPreference setSettingsHelper(SettingsHelper settingsHelper) {
		mSettingsHelper = settingsHelper;
		return this;
	}

	public ColorPreference setSettingsActivity(SettingsActivity settingsActivity) {
		mSettingsActivity = settingsActivity;
		return this;
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		if (mSettingsHelper == null || mSettingsActivity == null)
			throw new RuntimeException("You have to set an instance of SettingsHelper and/or SettingsActivity");

		Intent colorIntent = new Intent(mSettingsActivity, ColorPickerActivity.class);
		Bundle bundle = new Bundle();
		bundle.putString("title", getTitle().toString());
		bundle.putString("key", getKey());

		bundle.putString("color", getCurrentColor());
		colorIntent.putExtras(bundle);
		mSettingsActivity.startActivityForResult(colorIntent, 1);
		return false;
	}

	private String getCurrentColor() {
		SettingsHelper.Tint tintType = null;
		if (getKey().equals(Common.SETTINGS_KEY_DEFAULT_NAV_BAR_TINT))
			tintType = SettingsHelper.Tint.NAV_BAR;
		else if (getKey().equals(Common.SETTINGS_KEY_DEFAULT_NAV_BAR_ICON_TINT))
			tintType = SettingsHelper.Tint.NAV_BAR_ICON;
		else if (getKey().equals(Common.SETTINGS_KEY_DEFAULT_STATUS_BAR_ICON_TINT))
			tintType = SettingsHelper.Tint.ICON;
		else if (getKey().equals(Common.SETTINGS_KEY_DEFAULT_STATUS_BAR_TINT))
			tintType = SettingsHelper.Tint.STATUS_BAR;
		else if (getKey().equals(Common.SETTINGS_KEY_DEFAULT_STATUS_BAR_INVERTED_ICON_TINT))
			tintType = SettingsHelper.Tint.ICON_INVERTED;
		else if (getKey().equals(Common.SETTINGS_KEY_DEFAULT_NAV_BAR_IM_TINT))
			tintType = SettingsHelper.Tint.NAV_BAR_IM;
		else if (getKey().equals(Common.SETTINGS_KEY_DEFAULT_NAV_BAR_ICON_IM_TINT))
			tintType = SettingsHelper.Tint.NAV_BAR_ICON_IM;
		else
			throw new RuntimeException("You forgot to add the key to ColorPreference!!");

		Context context = getContext();
		if (mSettingsActivity != null)
			context = mSettingsActivity;

		if (mSettingsHelper == null)
			mSettingsHelper = SettingsHelper.getInstance(context);

		return mSettingsHelper.getDefaultTint(tintType, false);
	}

	public void refresh() {
		ColorDrawable drawable = new ColorDrawable(Color.parseColor(Utils.addHashIfNeeded(getCurrentColor())));
		Utils.setImageViewBackground(mImageView, drawable);
	}
}
