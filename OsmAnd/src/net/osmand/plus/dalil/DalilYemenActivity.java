package net.osmand.plus.dalil;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;

public class DalilYemenActivity extends AppCompatActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
			getWindow().setDecorFitsSystemWindows(true);
		} else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		}

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.dalil_status_bar_color));
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
				View decor = getWindow().getDecorView();
				int flags = decor.getSystemUiVisibility();
				flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
				decor.setSystemUiVisibility(flags);
			}
		}

		getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

		setContentView(R.layout.activity_dalil_yemen);

		initClickHandlers();
	}

	private void initClickHandlers() {
		// Search bar
		LinearLayout searchBar = findViewById(R.id.dalil_search_bar);
		searchBar.setOnClickListener(v -> {
			EditText searchInput = findViewById(R.id.dalil_search_input);
			searchInput.requestFocus();
		});

		// Near Me button
		LinearLayout nearMeBtn = findViewById(R.id.dalil_near_me_btn);
		nearMeBtn.setOnClickListener(v -> {
			Intent intent = new Intent(DalilYemenActivity.this, MapActivity.class);
			intent.putExtra("dalil_category", "near_me");
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
		});

		// Category cards
		findViewById(R.id.dalil_cat_restaurant).setOnClickListener(v -> openMapForCategory("restaurant"));
		findViewById(R.id.dalil_cat_pharmacy).setOnClickListener(v -> openMapForCategory("pharmacy"));
		findViewById(R.id.dalil_cat_hospital).setOnClickListener(v -> openMapForCategory("hospital"));
		findViewById(R.id.dalil_cat_fuel).setOnClickListener(v -> openMapForCategory("fuel"));
		findViewById(R.id.dalil_cat_workshop).setOnClickListener(v -> openMapForCategory("workshop"));
		findViewById(R.id.dalil_cat_store).setOnClickListener(v -> openMapForCategory("store"));

		// Popular place cards
		findViewById(R.id.dalil_place_card_1).setOnClickListener(v ->
				Toast.makeText(this, "مطعم السعيدة - صنعاء", Toast.LENGTH_SHORT).show());
		findViewById(R.id.dalil_place_card_2).setOnClickListener(v ->
				Toast.makeText(this, "صيدلية النور - عدن", Toast.LENGTH_SHORT).show());
		findViewById(R.id.dalil_place_card_3).setOnClickListener(v ->
				Toast.makeText(this, "مستشفى الأمل - تعز", Toast.LENGTH_SHORT).show());

		// Footer
		LinearLayout footer = findViewById(R.id.dalil_footer);
		footer.setOnClickListener(v ->
				Toast.makeText(this, R.string.dalil_footer_credit, Toast.LENGTH_SHORT).show());
	}

	private void openMapForCategory(String category) {
		Intent intent = new Intent(DalilYemenActivity.this, MapActivity.class);
		intent.putExtra("dalil_category", category);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);
	}
}