package net.osmand.plus.hawsa;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import net.osmand.plus.R;

public class HawsaMainActivity extends AppCompatActivity {

    // =====================================================================
    //  تكوين تطبيق Organic Maps (Al-Haswa-Magellan-)
    //  قم بتغيير هذا حسب package name الفعلي لتطبيقك المبنى من المستودع
    // =====================================================================
    private static final String ORGANIC_MAPS_PACKAGE = "app.organicmaps";
    // الإحداثيات الافتراضية (الحسوة)
    private static final double DEFAULT_LAT = 19.4431;
    private static final double DEFAULT_LON = 40.5167;
    private static final int DEFAULT_ZOOM = 10;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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

        TextView tvTitle = findViewById(R.id.tv_app_title);
        if (tvTitle != null) tvTitle.setText(R.string.hawsa_app_title);

        TextView tvDesigner = findViewById(R.id.tv_designer);
        if (tvDesigner != null) tvDesigner.setText(R.string.hawsa_designer_credit);

        // =============================================================
        //  بطاقة الخريطة — تفتح Organic Maps بدلاً من OsmAnd
        // =============================================================
        findViewById(R.id.card_map).setOnClickListener(v -> openOrganicMaps());

        findViewById(R.id.card_tides).setOnClickListener(v -> {
            try {
                startActivity(new Intent(this, TideActivity.class));
            } catch (Exception e) {
                Toast.makeText(this, "خطأ في فتح المد والجزر", Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.card_moon).setOnClickListener(v -> {
            try {
                startActivity(new Intent(this, MoonActivity.class));
            } catch (Exception e) {
                Toast.makeText(this, "خطأ في فتح القمر", Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.card_compass).setOnClickListener(v -> {
            try {
                startActivity(new Intent(this, CompassActivity.class));
            } catch (Exception e) {
                Toast.makeText(this, "خطأ في فتح البوصلة", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // =================================================================
    //  دالة فتح Organic Maps (Al-Haswa-Magellan-)
    //  تستخدم geo:// intent الذي يدعمه Organic Maps
    //  مع fallback لفتح التطبيق مباشرة إذا لم يعمل geo://
    // =================================================================
    private void openOrganicMaps() {
        if (isOrganicMapsInstalled()) {
            // محاولة 1: geo:// intent (الطريقة القياسية)
            Intent geoIntent = new Intent(Intent.ACTION_VIEW,
                Uri.parse("geo:" + DEFAULT_LAT + "," + DEFAULT_LON + "?z=" + DEFAULT_ZOOM));
            geoIntent.setPackage(ORGANIC_MAPS_PACKAGE);
            geoIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            if (tryStartActivity(geoIntent)) return;

            // محاولة 2: om:// deep link (خاصة بـ Organic Maps)
            Intent omIntent = new Intent(Intent.ACTION_VIEW,
                Uri.parse("om://map?z=" + DEFAULT_ZOOM + "/" + DEFAULT_LAT + "/" + DEFAULT_LON));
            omIntent.setPackage(ORGANIC_MAPS_PACKAGE);
            omIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            if (tryStartActivity(omIntent)) return;

            // محاولة 3: فتح التطبيق مباشرة عبر Launcher
            Intent launchIntent = getPackageManager().getLaunchIntentForPackage(ORGANIC_MAPS_PACKAGE);
            if (launchIntent != null && tryStartActivity(launchIntent)) return;

            Toast.makeText(this, "تعذر فتح خريطة الحسوة ماجلان", Toast.LENGTH_SHORT).show();
        } else {
            showOrganicMapsNotInstalledDialog();
        }
    }

    /**
     * التحقق مما إذا كان تطبيق Organic Maps مثبتاً على الجهاز
     */
    private boolean isOrganicMapsInstalled() {
        try {
            getPackageManager().getPackageInfo(ORGANIC_MAPS_PACKAGE, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    /**
     * محاولة تشغيل Intent مع معالجة الاستثناءات
     */
    private boolean tryStartActivity(Intent intent) {
        try {
            startActivity(intent);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * عرض رسالة عندما يكون تطبيق Organic Maps غير مثبت
     */
    private void showOrganicMapsNotInstalledDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("خريطة الحسوة ماجلان")
            .setMessage("تطبيق خريطة الحسوة ماجلان غير مثبت على جهازك.\nيرجى تثبيت التطبيق أولاً.")
            .setPositiveButton("حسناً", null)
            .show();
    }
}