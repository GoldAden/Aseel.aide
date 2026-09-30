package net.osmand.plus.hawsa;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
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

		// === Show status bar with dark navy color ===
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
			getWindow().setDecorFitsSystemWindows(true);
		} else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		}

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.hawsa_status_bar_color));
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
				View decor = getWindow().getDecorView();
				int flags = decor.getSystemUiVisibility();
				flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
				decor.setSystemUiVisibility(flags);
			}
		}

		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

		setContentView(R.layout.activity_hawsa_main);

		// Title
		TextView tvTitle = findViewById(R.id.tv_app_title);
		if (tvTitle != null) tvTitle.setText(R.string.hawsa_app_title);

		// Designer credit
		TextView tvDesigner = findViewById(R.id.tv_designer);
		if (tvDesigner != null) tvDesigner.setText(R.string.hawsa_designer_credit);

		// === Navigation buttons ===
		findViewById(R.id.card_map).setOnClickListener(v -> {
			try {
				Intent intent = new Intent(HawsaMainActivity.this, MapActivity.class);
				intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
				startActivity(intent);
			} catch (Exception e) {
				// Map activity not available
			}
		});

		findViewById(R.id.card_tides).setOnClickListener(v -> {
			try {
				Intent intent = new Intent(HawsaMainActivity.this, TideActivity.class);
				startActivity(intent);
			} catch (Exception e) {
				// Tide activity not available
			}
		});

		findViewById(R.id.card_moon).setOnClickListener(v -> {
			try {
				Intent intent = new Intent(HawsaMainActivity.this, MoonActivity.class);
				startActivity(intent);
			} catch (Exception e) {
				// Moon activity not available
			}
		});

		findViewById(R.id.card_compass).setOnClickListener(v -> {
			try {
				Intent intent = new Intent(HawsaMainActivity.this, CompassActivity.class);
				startActivity(intent);
			} catch (Exception e) {
				// Compass activity not available
			}
		});
	}
}
