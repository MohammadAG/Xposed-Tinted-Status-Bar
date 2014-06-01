package com.mohammadag.colouredstatusbar;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

public class ScreenColorPickerActivity extends Activity {

    private int touchColor;
    private int generalColor;

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
                    touchPreview.setImageDrawable(new ColorDrawable(bitmap.getPixel(x, y)));
                    return true;
                }
                return false;
            }
        });

        touchColor = bitmap.getPixel(1, 1);
        touchPreview.setImageDrawable(new ColorDrawable(touchColor));
        generalColor = Utils.getMainColorFromActionBarDrawable(new BitmapDrawable(getResources(), bitmap));
        generalPreview.setImageDrawable(new ColorDrawable(generalColor));

        touchApply.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String color = String.format("%08X", touchColor);
                settingsHelper.setStatusBarTintColor(pkg, activity, color);
                finish();
            }
        });

        generalApply.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String color = String.format("%08X", generalColor);
                settingsHelper.setStatusBarTintColor(pkg, activity, color);
                finish();
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
