package com.mohammadag.colouredstatusbar;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

import com.larswerkman.colorpicker.ColorPicker;
import com.larswerkman.colorpicker.ColorPicker.OnColorChangedListener;
import com.larswerkman.colorpicker.OpacityBar;
import com.larswerkman.colorpicker.SaturationBar;
import com.larswerkman.colorpicker.ValueBar;

public class ColorPickerActivity extends Activity implements OnColorChangedListener {

	private EditText editText;
	private String prefColor;
	private String prefKey;
	private Switch enabledSwitch;
	private boolean prefEnabled;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Bundle bundle = getIntent().getExtras();
		String prefTitle = bundle.getString("title");
		prefKey = bundle.getString("key");
		prefColor = bundle.getString("color");
		prefEnabled = bundle.getBoolean("enabled");

		setTitle(prefTitle);

		getActionBar().setDisplayHomeAsUpEnabled(true);

		setContentView(R.layout.activity_color_picker);

		final ColorPicker picker = (ColorPicker) findViewById(R.id.picker);
		OpacityBar opacityBar = (OpacityBar) findViewById(R.id.opacitybar);
		SaturationBar saturationBar = (SaturationBar) findViewById(R.id.saturationbar);
		ValueBar valueBar = (ValueBar) findViewById(R.id.valuebar);

		picker.addOpacityBar(opacityBar);
		picker.addSaturationBar(saturationBar);
		picker.addValueBar(valueBar);

		editText = (EditText) findViewById(R.id.edittext);

		picker.setOldCenterColor(Color.parseColor("#ff33b5e5"));
		picker.setOnColorChangedListener(this);
		picker.setColor(Color.parseColor("#" + prefColor));

		Button bPreview = (Button) findViewById(R.id.bPreviewColor);
		bPreview.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				try {
					String textEditString = editText.getText().toString();
					int colourHex;
					if (isFullyTransparent(textEditString)) {
						colourHex = Color.parseColor("#00000000");
					} else {
						colourHex = Color.parseColor("#" + textEditString);
						picker.setColor(colourHex);
					}
					
					ColorDrawable previewDrawable = new ColorDrawable(colourHex);
					
					if (Common.SETTINGS_KEY_STATUS_BAR_TINT.equals(prefKey)) {
						getActionBar().setBackgroundDrawable(previewDrawable);
						
						/* Workaround, there's no invalidate() method that would redraw the
						 * action bar, and setting the drawable at runtime simply does nothing.
						 */
						getActionBar().setDisplayShowTitleEnabled(false);
						getActionBar().setDisplayShowTitleEnabled(true);
					}
					
					Intent intent = new Intent(Common.INTENT_CHANGE_COLOR_NAME);
					intent.putExtra(prefKey, colourHex);
					sendOrderedBroadcast(intent, null);
				} catch (IllegalArgumentException e) {
					Toast.makeText(getApplicationContext(), "Invalid colour", Toast.LENGTH_SHORT).show();
				}
			}
		});
		Button bApply = (Button) findViewById(R.id.bApplyColor);
		bApply.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				returnResults();
			}
		});
	}
	
	private boolean isFullyTransparent(String colorCode) {
		return "0".equals(colorCode) || colorCode.startsWith("00");
	}
	
	private void returnResults() {
		String text = editText.getText().toString();
		
		Intent intent = new Intent();
		intent.putExtra("key", prefKey);
		if (isFullyTransparent(text)) {
			text = "00000000";
		}
		intent.putExtra("color", text);
		//intent.putExtra("enabled", enabledSwitch.isChecked());
		setResult(Activity.RESULT_OK, intent);
		finish();
	}

	private void updateEdittext(String color) {
		if (isFullyTransparent(color)) {
			editText.setText("00000000");
			return;
		}
		editText.setText(color);
	}

	@Override
	public void onColorChanged(int color) {
		updateEdittext(Integer.toHexString(color));
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.color_picker, menu);
		enabledSwitch = (Switch) menu.findItem(R.id.action_color_enable)
				.getActionView().findViewById(R.id.color_switch);
		enabledSwitch.setChecked(prefEnabled);
		enabledSwitch.setVisibility(View.GONE);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
			onBackPressed();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	public void onBackPressed() {
		setResult(Activity.RESULT_CANCELED);
		super.onBackPressed();
	}
}
