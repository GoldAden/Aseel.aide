package net.osmand.plus.hawsa;

import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import net.osmand.plus.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class TideActivity extends AppCompatActivity {

    private TideChartView chart;
    private TextView tvDate;
    private TextView tvNextTide;

    // Harmonic constants for tide calculation (simplified for major ports)
    private static final double M2_AMPLITUDE = 1.2;
    private static final double S2_AMPLITUDE = 0.45;
    private static final double K1_AMPLITUDE = 0.32;
    private static final double O1_AMPLITUDE = 0.25;
    private static final double M2_SPEED = 28.984104;
    private static final double S2_SPEED = 30.0;
    private static final double K1_SPEED = 15.041069;
    private static final double O1_SPEED = 13.943035;

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
        setContentView(R.layout.activity_tide);

        chart = findViewById(R.id.tide_chart);
        tvDate = findViewById(R.id.tv_tide_date);
        tvNextTide = findViewById(R.id.tv_next_tide);

        calculateAndDisplayTide();

        findViewById(R.id.btn_tide_back).setOnClickListener(v -> finish());
    }

    private void calculateAndDisplayTide() {
        Calendar now = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, dd MMMM yyyy", new Locale("ar"));
        tvDate.setText(dateFormat.format(now.getTime()));

        double hoursSinceEpoch = now.getTimeInMillis() / 3600000.0;
        int currentHour = now.get(Calendar.HOUR_OF_DAY);

        List<Float> hourList = new ArrayList<>();
        List<Float> heightList = new ArrayList<>();

        for (int h = 0; h <= 24; h++) {
            double t = hoursSinceEpoch - currentHour + h;
            double height = calculateTideHeight(t);
            hourList.add((float) h);
            heightList.add((float) height);
        }

        chart.setData(hourList, heightList);

        findNextTideEvent(now);
    }

    private double calculateTideHeight(double t) {
        double m2Phase = Math.toRadians(M2_SPEED * t);
        double s2Phase = Math.toRadians(S2_SPEED * t);
        double k1Phase = Math.toRadians(K1_SPEED * t);
        double o1Phase = Math.toRadians(O1_SPEED * t);

        return M2_AMPLITUDE * Math.cos(m2Phase)
                + S2_AMPLITUDE * Math.cos(s2Phase)
                + K1_AMPLITUDE * Math.cos(k1Phase)
                + O1_AMPLITUDE * Math.cos(o1Phase);
    }

    private void findNextTideEvent(Calendar now) {
        double currentHour = now.get(Calendar.HOUR_OF_DAY) + now.get(Calendar.MINUTE) / 60.0;
        double baseHours = now.getTimeInMillis() / 3600000.0;

        boolean lastRising = false;
        double lastHeight = calculateTideHeight(baseHours - currentHour);

        for (int m = 0; m <= 720; m += 10) {
            double t = baseHours - currentHour + currentHour + (m / 60.0);
            double height = calculateTideHeight(t);
            boolean rising = height > lastHeight;

            if (m > 0 && rising != lastRising) {
                int hour = (int) (currentHour + m / 60.0);
                int min = m % 60;
                String type = rising ? "مد (ارتفاع)" : "جزر (انخفاض)";
                String timeStr = String.format(Locale.getDefault(), "%02d:%02d", hour % 24, min);
                tvNextTide.setText(String.format(Locale.getDefault(),
                        "القادم: %s عند %s - الارتفاع: %.1f م", type, timeStr, height));
                break;
            }

            lastHeight = height;
            lastRising = rising;
        }
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
