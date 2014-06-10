package com.mohammadag.colouredstatusbar;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;

public class MainActivity extends Activity implements OnClickListener {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_welcome);

		findViewById(R.id.per_app_button).setOnClickListener(this);
		findViewById(R.id.settings_button).setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		Intent intent = null;
		if (v.getId() == R.id.per_app_button) {
			intent = new Intent(this, PackageListActivity.class);
		} else if (v.getId() == R.id.settings_button) {
			intent = new Intent(this, SettingsActivity.class);
		}

		if (intent != null)
			startActivity(intent);
	}
}
