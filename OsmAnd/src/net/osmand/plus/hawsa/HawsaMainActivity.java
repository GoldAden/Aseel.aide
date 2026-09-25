package net.osmand.plus.hawsa;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;

public class HawsaMainActivity extends AppCompatActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Full screen mode
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
			getWindow().setDecorFitsSystemWindows(false);
		} else {
			getWindow().setFlags(
					WindowManager.LayoutParams.FLAG_FULLSCREEN,
					WindowManager.LayoutParams.FLAG_FULLSCREEN
			);
		}

		// Portrait orientation
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

		setContentView(R.layout.activity_hawsa_main);

		// Set title
		TextView tvTitle = findViewById(R.id.tv_app_title);
		tvTitle.setText(R.string.hawsa_app_title);

		// Set designer text
		TextView tvDesigner = findViewById(R.id.tv_designer);
		tvDesigner.setText(R.string.hawsa_designer_credit);

		// Map button
		CardView cardMap = findViewById(R.id.card_map);
		cardMap.setOnClickListener(v -> {
			Intent intent = new Intent(HawsaMainActivity.this, MapActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
			startActivity(intent);
		});

		// Tide button
		CardView cardTide = findViewById(R.id.card_tide);
		cardTide.setOnClickListener(v -> {
			Intent intent = new Intent(HawsaMainActivity.this, TideActivity.class);
			startActivity(intent);
		});

		// Moon button
		CardView cardMoon = findViewById(R.id.card_moon);
		cardMoon.setOnClickListener(v -> {
			Intent intent = new Intent(HawsaMainActivity.this, MoonActivity.class);
			startActivity(intent);
		});

		// Compass button
		CardView cardCompass = findViewById(R.id.card_compass);
		cardCompass.setOnClickListener(v -> {
			Intent intent = new Intent(HawsaMainActivity.this, CompassActivity.class);
			startActivity(intent);
		});
	}

	@Override
	protected void onResume() {
		super.onResume();
		View decorView = getWindow().getDecorView();
		int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
				| View.SYSTEM_UI_FLAG_FULLSCREEN
				| View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
		decorView.setSystemUiVisibility(uiOptions);
	}
}
