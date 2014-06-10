package com.mohammadag.colouredstatusbar;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;

public class ScreenColorPickerActivity extends Activity {

	private int touchColor;
	private int generalColor;
	private int iconTintColor;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final SettingsHelper settingsHelper = SettingsHelper.getInstance(getApplicationContext());
		Bundle bundle = getIntent().getExtras();
		final String pkg = bundle.getString("pkg");
		String title = bundle.getString("title");
		final String activity;
		if (title.equals(pkg))
			activity = null;
		else
			activity = title;

		byte[] bitmapByteArray = bundle.getByteArray("bitmap");
		final Bitmap bitmap = BitmapFactory.decodeByteArray(bitmapByteArray, 0, bitmapByteArray.length);

		setTitle(title);
		if (Utils.hasActionBar())
			getActionBar().setDisplayHomeAsUpEnabled(true);

		setContentView(R.layout.activity_screen_color_picker);

		final ImageView bitmapImageView = (ImageView) findViewById(R.id.bitmap);
		final Spinner iconColor = (Spinner) findViewById(R.id.iconColor);
		final ImageView iconColorPreview = (ImageView) findViewById(R.id.iconColorPreview);
		final ImageView touchPreview = (ImageView) findViewById(R.id.touchColor);
		final ImageView generalPreview = (ImageView) findViewById(R.id.generalColor);
		final Button touchApply = (Button) findViewById(R.id.touchApply);
		final Button generalApply = (Button) findViewById(R.id.generalApply);

		bitmapImageView.setImageBitmap(bitmap);
		bitmapImageView.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View view, MotionEvent motionEvent) {
				if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
					int x = (int) motionEvent.getX();
					int y = (int) motionEvent.getY();
					touchPreview.setBackgroundColor(bitmap.getPixel(x, y));
					return true;
				}
				return false;
			}
		});

		touchColor = bitmap.getPixel(1, 1);
		touchPreview.setBackgroundColor(touchColor);
		generalColor = Utils.getMainColorFromActionBarDrawable(new BitmapDrawable(getResources(), bitmap));
		generalPreview.setBackgroundColor(generalColor);

		touchApply.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				settingsHelper.setStatusBarTintColor(pkg, activity, Utils.convertToARGB(touchColor));
				settingsHelper.setIconColors(pkg, activity, Utils.convertToARGB(iconTintColor));
				finish();
			}
		});

		generalApply.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				settingsHelper.setStatusBarTintColor(pkg, activity, Utils.convertToARGB(generalColor));
				settingsHelper.setIconColors(pkg, activity, Utils.convertToARGB(iconTintColor));
				finish();
			}
		});

		int iconTint = settingsHelper.getDefaultTint(SettingsHelper.Tint.ICON);
		int iconInvertedTint = settingsHelper.getDefaultTint(SettingsHelper.Tint.ICON_INVERTED);
		int iconAuto = Utils.getIconColorForColor(generalColor, iconTint, iconInvertedTint, settingsHelper.getHsvMax());
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item,
				new String[]{Utils.convertToARGB(iconTint), Utils.convertToARGB(iconInvertedTint),
						Utils.convertToARGB(iconAuto)}
		);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		iconColor.setAdapter(adapter);
		iconTintColor = iconAuto;
		iconColor.setSelection(2);
		iconColor.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
				String color = (String) parent.getItemAtPosition(pos);
				iconTintColor = Color.parseColor(Utils.addHashIfNeeded(color));
				iconColorPreview.setBackgroundColor(iconTintColor);
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
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

}
