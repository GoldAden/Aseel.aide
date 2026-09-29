package net.osmand.plus.hawsa;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;

public class HawsaMainActivity extends AppCompatActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Show status bar with dark navy color
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
			getWindow().setDecorFitsSystemWindows(true);
		} else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			// Clear fullscreen flag to show status bar
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		}

		// Set status bar color to match app theme
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			getWindow().setStatusBarColor(ContextCompat.getColor(this, net.osmand.plus.R.color.hawsa_status_bar_color));
			// Light icons on dark status bar
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
				View decor = getWindow().getDecorView();
				int systemUiFlags = decor.getSystemUiVisibility();
				systemUiFlags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
				decor.setSystemUiVisibility(systemUiFlags);
			}
		}

		// Portrait orientation
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

		setContentView(R.layout.activity_hawsa_main);

		// Set title
		TextView tvTitle = findViewById(R.id.tv_app_title);
		if (tvTitle != null) {
			tvTitle.setText(R.string.hawsa_app_title);
		}

		// Set designer text
		TextView tvDesigner = findViewById(R.id.tv_designer);
		if (tvDesigner != null) {
			tvDesigner.setText(R.string.hawsa_designer_credit);
		}

		// Map button
		LinearLayout cardMap = findViewById(R.id.card_map);
		cardMap.setOnClickListener(v -> {
			Intent intent = new Intent(HawsaMainActivity.this, MapActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
			startActivity(intent);
		});

		// Tide button
		LinearLayout cardTides = findViewById(R.id.card_tides);
		cardTides.setOnClickListener(v -> {
			Intent intent = new Intent(HawsaMainActivity.this, TideActivity.class);
			startActivity(intent);
		});

		// Moon button
		LinearLayout cardMoon = findViewById(R.id.card_moon);
		cardMoon.setOnClickListener(v -> {
			Intent intent = new Intent(HawsaMainActivity.this, MoonActivity.class);
			startActivity(intent);
		});

		// Compass button
		LinearLayout cardCompass = findViewById(R.id.card_compass);
		cardCompass.setOnClickListener(v -> {
			Intent intent = new Intent(HawsaMainActivity.this, CompassActivity.class);
			startActivity(intent);
		});
	}
}
