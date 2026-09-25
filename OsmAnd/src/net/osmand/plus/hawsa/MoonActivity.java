package net.osmand.plus.hawsa;

import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import net.osmand.plus.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class MoonActivity extends AppCompatActivity {

    private TextView tvMoonPhase;
    private TextView tvMoonAge;
    private TextView tvIllumination;
    private ImageView ivMoonIcon;
    private TextView tvHijriDate;
    private TextView tvGregorianDate;
    private ImageView[] dayMoons = new ImageView[7];
    private TextView[] dayLabels = new TextView[7];

    // Arabic Hijri month names
    private static final String[] HIJRI_MONTHS = {
            "محرم", "صفر", "ربيع الأول", "ربيع الثاني",
            "جمادى الأولى", "جمادى الثانية", "رجب", "شعبان",
            "رمضان", "شوال", "ذو القعدة", "ذو الحجة"
    };

    // Moon phase names in Arabic
    private static final String[] PHASE_NAMES = {
            "محاق (قمر جديد)",
            "هلال متزايد",
            "تربيع أول",
            "أحدب متزايد",
            "بدر (قمر مكتمل)",
            "أحدب متناقص",
            "تربيع أخير",
            "هلال متناقص"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
        } else {
            getWindow().setFlags(
                    WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN
            );
        }

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_moon);

        tvMoonPhase = findViewById(R.id.tv_moon_phase);
        tvMoonAge = findViewById(R.id.tv_moon_age);
        tvIllumination = findViewById(R.id.tv_illumination);
        ivMoonIcon = findViewById(R.id.iv_moon_icon);
        tvHijriDate = findViewById(R.id.tv_hijri_date);
        tvGregorianDate = findViewById(R.id.tv_gregorian_date);

        // 7-day forecast
        for (int i = 0; i < 7; i++) {
            int labelId = getResources().getIdentifier("tv_day_" + i, "id", getPackageName());
            int moonId = getResources().getIdentifier("iv_day_moon_" + i, "id", getPackageName());
            dayLabels[i] = findViewById(labelId);
            dayMoons[i] = findViewById(moonId);
        }

        calculateAndDisplayMoonInfo();

        findViewById(R.id.btn_moon_back).setOnClickListener(v -> finish());
    }

    private void calculateAndDisplayMoonInfo() {
        Calendar now = Calendar.getInstance();
        double moonAge = calculateMoonAge(now);
        double illumination = calculateIllumination(moonAge);
        int phaseIndex = getPhaseIndex(moonAge);

        // Current moon info
        tvMoonPhase.setText(PHASE_NAMES[phaseIndex]);
        tvMoonAge.setText(String.format(Locale.getDefault(), "عمر القمر: %.1f يوم", moonAge));
        tvIllumination.setText(String.format(Locale.getDefault(), "الإضاءة: %.0f%%", illumination * 100));

        // Moon phase icon
        ivMoonIcon.setImageResource(getMoonPhaseDrawable(phaseIndex));

        // Hijri date
        int[] hijri = gregorianToHijri(now.get(Calendar.YEAR),
                now.get(Calendar.MONTH) + 1,
                now.get(Calendar.DAY_OF_MONTH));

        tvHijriDate.setText(String.format(Locale.getDefault(), "%d %s %d هـ",
                hijri[0], HIJRI_MONTHS[hijri[1] - 1], hijri[2]));

        SimpleDateFormat gregFormat = new SimpleDateFormat("dd MMMM yyyy", new Locale("ar"));
        tvGregorianDate.setText(gregFormat.format(now.getTime()));

        // 7-day forecast
        for (int i = 0; i < 7; i++) {
            Calendar future = (Calendar) now.clone();
            future.add(Calendar.DAY_OF_MONTH, i);

            double futureAge = calculateMoonAge(future);
            int futurePhase = getPhaseIndex(futureAge);

            SimpleDateFormat dayFormat = new SimpleDateFormat("EEE", new Locale("ar"));
            dayLabels[i].setText(dayFormat.format(future.getTime()));
            dayMoons[i].setImageResource(getMoonPhaseDrawable(futurePhase));
        }
    }

    // Approximate moon age (synodic month = 29.53059 days)
    private double calculateMoonAge(Calendar date) {
        // Reference: New moon Jan 6, 2000 18:14 UTC
        Calendar ref = Calendar.getInstance();
        ref.set(2000, Calendar.JANUARY, 6, 18, 14, 0);
        double diffMs = date.getTimeInMillis() - ref.getTimeInMillis();
        double diffDays = diffMs / (1000.0 * 60 * 60 * 24);
        double synodicMonth = 29.53059;
        double age = diffDays % synodicMonth;
        if (age < 0) age += synodicMonth;
        return age;
    }

    private double calculateIllumination(double age) {
        return 0.5 * (1.0 - Math.cos(2.0 * Math.PI * age / 29.53059));
    }

    private int getPhaseIndex(double age) {
        int phase = (int) (age / 3.6913);
        if (phase > 7) phase = 7;
        return phase;
    }

    private int getMoonPhaseDrawable(int phaseIndex) {
        switch (phaseIndex) {
            case 0: return R.drawable.moon_new;
            case 1: return R.drawable.moon_waxing_crescent;
            case 2: return R.drawable.moon_first_quarter;
            case 3: return R.drawable.moon_waxing_gibbous;
            case 4: return R.drawable.moon_full;
            case 5: return R.drawable.moon_waning_gibbous;
            case 6: return R.drawable.moon_last_quarter;
            case 7: return R.drawable.moon_waning_crescent;
            default: return R.drawable.moon_new;
        }
    }

    // Approximate Gregorian to Hijri conversion
    private int[] gregorianToHijri(int year, int month, int day) {
        double jd = gregorianToJD(year, month, day);
        jd = Math.floor(jd) + 0.5;

        double l = Math.floor(jd - 1948439.5) + 10632.0;
        double n = Math.floor((l - 0.13375) / 10631.0);
        l = l - Math.floor(n * 10631.0) + 354.0;
        double j = Math.floor((10985.0 - l) / 5316.0) * Math.floor(50.0 - l / 17719.0)
                + Math.floor(l / 5670.0) * Math.floor(43.0 - l / 15269.0);
        l = l - Math.floor(Math.floor(30.0 - j / 15.0) * Math.floor(17719.0 - j / 15.0) * 0.002)
                + Math.floor(j / 15.0) * 0.5 + 28.5;

        int hMonth = (int) Math.floor((l - 1.0) / 29.5) + 1;
        int hDay = (int) l - 29 * (hMonth - 1);
        int hYear = (int) (30.0 * n + j - 30.0);

        if (hMonth > 12) hMonth = 12;

        return new int[]{hDay, hMonth, hYear};
    }

    private double gregorianToJD(int year, int month, int day) {
        if (month <= 2) {
            year -= 1;
            month += 12;
        }
        double A = Math.floor(year / 100.0);
        double B = 2 - A + Math.floor(A / 4.0);
        return Math.floor(365.25 * (year + 4716)) + Math.floor(30.6001 * (month + 1)) + day + B - 1524.5;
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
