package net.osmand.plus.hawsa;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.os.Build;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import net.osmand.plus.R;

import java.util.Calendar;
import java.util.TimeZone;

public class CompassActivity extends AppCompatActivity implements SensorEventListener, LocationListener {
    private static final String TAG = "HawsaCompass";

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor magnetometer;

    private float[] gravityValues = null;
    private float[] geomagneticValues = null;
    private float[] rotationMatrix = new float[9];
    private float[] orientationValues = new float[3];

    private float currentAzimuth = 0f;
    private float qiblaBearing = 0f;

    private double latitude = 21.4225;
    private double longitude = 39.8262;
    private double altitude = 0;

    private CompassView compassView;
    private TextView tvCompassDegree;
    private TextView tvQiblaDirection;
    private TextView tvFajrTime;
    private TextView tvSunriseTime;
    private TextView tvDhuhrTime;
    private TextView tvAsrTime;
    private TextView tvMaghribTime;
    private TextView tvIshaTime;
    private ImageView btnCompassBack;

    private LocationManager locationManager;
    private boolean hasLocation = false;

    private static final double KAABA_LAT = 21.4225;
    private static final double KAABA_LNG = 39.8262;

    // Hard-coded fallback colors (ARGB)
    private static final int COLOR_GOLD = 0xFFD700;
    private static final int COLOR_WHITE = 0xFFFFFF;
    private static final int COLOR_DEEP = 0xFF0A1628;
    private static final int COLOR_GREEN = 0xFF00FF88;
    private static final int COLOR_SOFT_GRAY = 0xFFD6D6DB;
    private static final int COLOR_RED = 0xFFCF6679;
    private static final int COLOR_CARD_DARK = 0xFF1A2E4A;
    private static final int COLOR_BLUE = 0xFF00BFFF;
    private static final int COLOR_ORANGE = 0xFFFFAA33;

    // Resolved colors
    private int colorGold = COLOR_GOLD;
    private int colorWhite = COLOR_WHITE;
    private int colorDeep = COLOR_DEEP;
    private int colorGreen = COLOR_GREEN;
    private int colorSoftGray = COLOR_SOFT_GRAY;
    private int colorRed = COLOR_RED;
    private int colorCardDark = COLOR_CARD_DARK;
    private int colorBlue = COLOR_BLUE;
    private int colorOrange = COLOR_ORANGE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(true);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.hawsa_status_bar_color));
            } catch (Exception e) {
                getWindow().setStatusBarColor(COLOR_DEEP);
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                View decor = getWindow().getDecorView();
                int flags = decor.getSystemUiVisibility();
                flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                decor.setSystemUiVisibility(flags);
            }
        }

        resolveColors();

        try {
            setContentView(R.layout.activity_compass);
        } catch (Exception e) {
            Log.e(TAG, "Failed to load compass layout", e);
            Toast.makeText(this, "خطأ في تحميل شاشة البوصلة: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        compassView = findViewById(R.id.compass_view);
        tvCompassDegree = findViewById(R.id.tv_compass_degree);
        tvQiblaDirection = findViewById(R.id.tv_qibla_direction);
        tvFajrTime = findViewById(R.id.tv_fajr_time);
        tvSunriseTime = findViewById(R.id.tv_sunrise_time);
        tvDhuhrTime = findViewById(R.id.tv_dhuhr_time);
        tvAsrTime = findViewById(R.id.tv_asr_time);
        tvMaghribTime = findViewById(R.id.tv_maghrib_time);
        tvIshaTime = findViewById(R.id.tv_isha_time);
        btnCompassBack = findViewById(R.id.btn_compass_back);

        if (btnCompassBack != null) {
            btnCompassBack.setOnClickListener(v -> finish());
        }

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        }

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        requestLocation();
        calculateQibla();
        calculatePrayerTimes();
    }

    private void resolveColors() {
        try { colorGold = ContextCompat.getColor(this, R.color.hawsa_gold); } catch (Exception e) { colorGold = COLOR_GOLD; }
        try { colorWhite = ContextCompat.getColor(this, R.color.palette_neutral_100); } catch (Exception e) { colorWhite = COLOR_WHITE; }
        try { colorDeep = ContextCompat.getColor(this, R.color.hawsa_bg_dark); } catch (Exception e) { colorDeep = COLOR_DEEP; }
        try { colorGreen = ContextCompat.getColor(this, R.color.hawsa_green); } catch (Exception e) { colorGreen = COLOR_GREEN; }
        try { colorSoftGray = ContextCompat.getColor(this, R.color.palette_neutral_85); } catch (Exception e) { colorSoftGray = COLOR_SOFT_GRAY; }
        try { colorRed = ContextCompat.getColor(this, R.color.palette_red_50); } catch (Exception e) { colorRed = COLOR_RED; }
        try { colorCardDark = ContextCompat.getColor(this, R.color.hawsa_card_dark); } catch (Exception e) { colorCardDark = COLOR_CARD_DARK; }
        try { colorBlue = ContextCompat.getColor(this, R.color.hawsa_blue); } catch (Exception e) { colorBlue = COLOR_BLUE; }
        try { colorOrange = ContextCompat.getColor(this, R.color.hawsa_orange); } catch (Exception e) { colorOrange = COLOR_ORANGE; }
    }

    private void requestLocation() {
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
                return;
            }
            Criteria criteria = new Criteria();
            criteria.setAccuracy(Criteria.ACCURACY_FINE);
            String provider = locationManager.getBestProvider(criteria, true);
            if (provider != null) {
                try {
                    locationManager.requestSingleUpdate(provider, this, null);
                } catch (Exception e) {
                    Log.w(TAG, "Location request failed", e);
                }
                Location lastKnown = null;
                try {
                    lastKnown = locationManager.getLastKnownLocation(provider);
                } catch (Exception e) {
                    Log.w(TAG, "Last known location failed", e);
                }
                if (lastKnown != null) {
                    updateLocation(lastKnown);
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "requestLocation error", e);
        }
    }

    private void updateLocation(Location loc) {
        latitude = loc.getLatitude();
        longitude = loc.getLongitude();
        altitude = loc.getAltitude();
        hasLocation = true;
        calculateQibla();
        calculatePrayerTimes();
    }

    @Override
    public void onLocationChanged(Location location) {
        updateLocation(location);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onStatusChanged(String provider, int status, Bundle extras) {}

    @Override
    public void onProviderEnabled(String provider) {}

    @Override
    public void onProviderDisabled(String provider) {}

    private void calculateQibla() {
        try {
            double latRad = Math.toRadians(latitude);
            double kaabaLatRad = Math.toRadians(KAABA_LAT);
            double dLng = Math.toRadians(KAABA_LNG - longitude);

            double x = Math.sin(dLng);
            double y = Math.cos(latRad) * Math.tan(kaabaLatRad) - Math.sin(latRad) * Math.cos(dLng);

            qiblaBearing = (float) Math.toDegrees(Math.atan2(x, y));
            if (qiblaBearing < 0) qiblaBearing += 360;

            try {
                GeomagneticField geoField = new GeomagneticField(
                        (float) latitude, (float) longitude, (float) altitude,
                        System.currentTimeMillis());
                qiblaBearing -= geoField.getDeclination();
            } catch (Exception e) {
                Log.w(TAG, "GeomagneticField error", e);
            }

            if (compassView != null) {
                compassView.setQiblaBearing(qiblaBearing);
            }

            if (tvQiblaDirection != null) {
                String qiblaText = String.format(java.util.Locale.US, "\u0627\u062a\u062c\u0627\u0647 \u0627\u0644\u0642\u0628\u0644\u0629: %.1f\u00b0", qiblaBearing);
                tvQiblaDirection.setText(qiblaText);
            }
        } catch (Exception e) {
            Log.e(TAG, "Qibla calc error", e);
            if (tvQiblaDirection != null) {
                tvQiblaDirection.setText("\u0627\u062a\u062c\u0627\u0647 \u0627\u0644\u0642\u0628\u0644\u0629: --");
            }
        }
    }

    private void calculatePrayerTimes() {
        try {
            Calendar now = Calendar.getInstance();
            int year = now.get(Calendar.YEAR);
            int month = now.get(Calendar.MONTH) + 1;
            int day = now.get(Calendar.DAY_OF_MONTH);

            TimeZone tz = TimeZone.getDefault();
            double timezoneOffset = tz.getOffset(now.getTimeInMillis()) / 3600000.0;

            double julianDate = julianDate(year, month, day);

            double D = julianDate - 2451545.0;
            double g = 357.529 + 0.98560028 * D;
            double q = 280.459 + 0.98564736 * D;
            double L = q + 1.915 * Math.sin(Math.toRadians(g)) + 0.020 * Math.sin(Math.toRadians(2 * g));
            double e = 23.439 - 0.00000036 * D;
            double RA = Math.toDegrees(Math.atan2(Math.cos(Math.toRadians(e)) * Math.sin(Math.toRadians(L)), Math.cos(Math.toRadians(L))));
            double Dec = Math.asin(Math.sin(Math.toRadians(e)) * Math.sin(Math.toRadians(L)));

            double latRad = Math.toRadians(latitude);
            double decRad = Dec;

            double eqTime = (q - RA) * 4;
            double solarNoon = 12 - longitude / 15.0 + timezoneOffset - eqTime / 60.0;

            double cosH = (Math.sin(Math.toRadians(-0.833)) - Math.sin(latRad) * Math.sin(decRad))
                    / (Math.cos(latRad) * Math.cos(decRad));
            if (cosH > 1) cosH = 1;
            if (cosH < -1) cosH = -1;
            double H = Math.toDegrees(Math.acos(cosH));

            double sunriseTime = solarNoon - H / 15.0;
            double sunsetTime = solarNoon + H / 15.0;

            double cosHFajr = (Math.sin(Math.toRadians(-15)) - Math.sin(latRad) * Math.sin(decRad))
                    / (Math.cos(latRad) * Math.cos(decRad));
            if (cosHFajr > 1) cosHFajr = 1;
            if (cosHFajr < -1) cosHFajr = -1;
            double HFajr = Math.toDegrees(Math.acos(cosHFajr));
            double fajrTime = solarNoon - HFajr / 15.0;

            double cosHIsha = (Math.sin(Math.toRadians(-17)) - Math.sin(latRad) * Math.sin(decRad))
                    / (Math.cos(latRad) * Math.cos(decRad));
            if (cosHIsha > 1) cosHIsha = 1;
            if (cosHIsha < -1) cosHIsha = -1;
            double HIsha = Math.toDegrees(Math.acos(cosHIsha));
            double ishaTime = solarNoon + HIsha / 15.0;

            double dhuhrTime = solarNoon;
            double asrTime = solarNoon + (H * 0.5) / 15.0;
            double maghribTime = sunsetTime;

            if (tvFajrTime != null) tvFajrTime.setText(formatTime(fajrTime));
            if (tvSunriseTime != null) tvSunriseTime.setText(formatTime(sunriseTime));
            if (tvDhuhrTime != null) tvDhuhrTime.setText(formatTime(dhuhrTime));
            if (tvAsrTime != null) tvAsrTime.setText(formatTime(asrTime));
            if (tvMaghribTime != null) tvMaghribTime.setText(formatTime(maghribTime));
            if (tvIshaTime != null) tvIshaTime.setText(formatTime(ishaTime));
        } catch (Exception e) {
            Log.e(TAG, "Prayer times calc error", e);
            if (tvFajrTime != null) tvFajrTime.setText("--:--");
            if (tvSunriseTime != null) tvSunriseTime.setText("--:--");
            if (tvDhuhrTime != null) tvDhuhrTime.setText("--:--");
            if (tvAsrTime != null) tvAsrTime.setText("--:--");
            if (tvMaghribTime != null) tvMaghribTime.setText("--:--");
            if (tvIshaTime != null) tvIshaTime.setText("--:--");
        }
    }

    private double julianDate(int year, int month, int day) {
        if (month <= 2) { year--; month += 12; }
        int A = year / 100;
        int B = 2 - A + A / 4;
        return Math.floor(365.25 * (year + 4716)) + Math.floor(30.6001 * (month + 1))
                + day + B - 1524.5;
    }

    private String formatTime(double time) {
        time = time % 24;
        if (time < 0) time += 24;
        int hour = (int) time;
        int minute = (int) Math.round((time - hour) * 60);
        if (minute >= 60) { minute -= 60; hour++; }
        if (minute < 0) { minute += 60; hour--; }
        hour = hour % 24;
        return String.format(java.util.Locale.US, "%02d:%02d", hour, minute);
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            if (sensorManager != null) {
                if (accelerometer != null)
                    sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
                if (magnetometer != null)
                    sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI);
            }
        } catch (Exception e) {
            Log.w(TAG, "Sensor registration error", e);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            if (sensorManager != null) {
                sensorManager.unregisterListener(this);
            }
        } catch (Exception e) {
            Log.w(TAG, "Sensor unregistration error", e);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        try {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                gravityValues = lowPass(event.values.clone(), gravityValues);
            } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                geomagneticValues = lowPass(event.values.clone(), geomagneticValues);
            }

            if (gravityValues != null && geomagneticValues != null) {
                boolean success = SensorManager.getRotationMatrix(rotationMatrix, null, gravityValues, geomagneticValues);
                if (success) {
                    SensorManager.getOrientation(rotationMatrix, orientationValues);
                    float azimuth = (float) Math.toDegrees(orientationValues[0]);
                    if (azimuth < 0) azimuth += 360;

                    try {
                        GeomagneticField geoField = new GeomagneticField(
                                (float) latitude, (float) longitude, (float) altitude,
                                System.currentTimeMillis());
                        azimuth += geoField.getDeclination();
                    } catch (Exception e) {
                        Log.w(TAG, "Declination error", e);
                    }

                    currentAzimuth = azimuth;
                    if (compassView != null) {
                        compassView.setAzimuth(azimuth);
                        compassView.invalidate();
                    }
                    if (tvCompassDegree != null) {
                        tvCompassDegree.setText(String.format(java.util.Locale.US, "%.0f\u00b0", azimuth));
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Sensor error", e);
        }
    }

    private float[] lowPass(float[] input, float[] output) {
        if (output == null) return input;
        float alpha = 0.25f;
        for (int i = 0; i < input.length; i++) {
            output[i] = output[i] + alpha * (input[i] - output[i]);
        }
        return output;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    public class CompassView extends View {

        private float azimuth = 0f;
        private float qiblaBearing = 0f;
        private Paint circlePaint;
        private Paint tickPaint;
        private Paint tickMinorPaint;
        private Paint textPaint;
        private Paint needlePaint;
        private Paint needleSouthPaint;
        private Paint qiblaPaint;
        private Paint centerPaint;
        private Paint bgPaint;
        private Paint kaabaPaint;
        private boolean initialized = false;

        public CompassView(Context context) {
            super(context);
            init();
        }

        public CompassView(Context context, AttributeSet attrs) {
            super(context, attrs);
            init();
        }

        public CompassView(Context context, AttributeSet attrs, int defStyleAttr) {
            super(context, attrs, defStyleAttr);
            init();
        }

        private void init() {
            circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            circlePaint.setStyle(Paint.Style.STROKE);
            circlePaint.setColor(colorGold);
            circlePaint.setStrokeWidth(3f);

            tickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            tickPaint.setStyle(Paint.Style.STROKE);
            tickPaint.setColor(colorGold);
            tickPaint.setStrokeWidth(3f);

            tickMinorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            tickMinorPaint.setStyle(Paint.Style.STROKE);
            tickMinorPaint.setColor(colorSoftGray);
            tickMinorPaint.setStrokeWidth(1.5f);

            textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            textPaint.setColor(colorWhite);
            textPaint.setTextSize(36f);
            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setFakeBoldText(true);

            needlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            needlePaint.setColor(colorRed);
            needlePaint.setStyle(Paint.Style.FILL);

            needleSouthPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            needleSouthPaint.setColor(colorWhite);
            needleSouthPaint.setStyle(Paint.Style.FILL);

            qiblaPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            qiblaPaint.setColor(colorGreen);
            qiblaPaint.setStyle(Paint.Style.FILL);

            centerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            centerPaint.setColor(colorGold);
            centerPaint.setStyle(Paint.Style.FILL);

            bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            bgPaint.setColor(colorDeep);
            bgPaint.setStyle(Paint.Style.FILL);

            kaabaPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            kaabaPaint.setColor(colorGreen);
            kaabaPaint.setTextSize(22f);
            kaabaPaint.setTextAlign(Paint.Align.CENTER);
            kaabaPaint.setFakeBoldText(true);

            initialized = true;
        }

        public void setAzimuth(float az) {
            this.azimuth = az;
        }

        public void setQiblaBearing(float qb) {
            this.qiblaBearing = qb;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            if (!initialized) return;

            int w = getWidth();
            int h = getHeight();
            int cx = w / 2;
            int cy = h / 2;
            int radius = Math.min(cx, cy) - 16;

            canvas.drawCircle(cx, cy, radius, bgPaint);
            canvas.drawCircle(cx, cy, radius, circlePaint);

            canvas.save();
            canvas.rotate(-azimuth, cx, cy);

            for (int i = 0; i < 360; i += 10) {
                float startX = cx;
                float startY = cy - radius + 8;
                float endY;
                if (i % 30 == 0) {
                    endY = cy - radius + 28;
                    canvas.drawLine(startX, startY, startX, endY, tickPaint);
                } else {
                    endY = cy - radius + 18;
                    canvas.drawLine(startX, startY, startX, endY, tickMinorPaint);
                }
                canvas.rotate(10, cx, cy);
            }

            float labelR = radius - 48;
            String[] cardinals = {"N", "E", "S", "W"};
            int[] angles = {0, 90, 180, 270};
            for (int i = 0; i < 4; i++) {
                float rad = (float) Math.toRadians(angles[i]);
                float tx = cx + labelR * (float) Math.sin(rad);
                float ty = cy - labelR * (float) Math.cos(rad);
                Paint dirPaint = new Paint(textPaint);
                if (cardinals[i].equals("N")) {
                    dirPaint.setColor(colorRed);
                    dirPaint.setTextSize(42f);
                }
                canvas.drawText(cardinals[i], tx, ty + dirPaint.getTextSize() / 3, dirPaint);
            }

            float qRad = (float) Math.toRadians(qiblaBearing);
            float qTipR = radius - 10;
            float qBaseR = radius - 30;
            float qTipX = cx + qTipR * (float) Math.sin(qRad);
            float qTipY = cy - qTipR * (float) Math.cos(qRad);
            float qBaseX = cx + qBaseR * (float) Math.sin(qRad);
            float qBaseY = cy - qBaseR * (float) Math.cos(qRad);
            float perpRad = qRad + (float) Math.PI / 2;
            float spread = 8f;
            float bx1 = qBaseX + spread * (float) Math.sin(perpRad);
            float by1 = qBaseY - spread * (float) Math.cos(perpRad);
            float bx2 = qBaseX - spread * (float) Math.sin(perpRad);
            float by2 = qBaseY + spread * (float) Math.cos(perpRad);

            Path qiblaPath = new Path();
            qiblaPath.moveTo(qTipX, qTipY);
            qiblaPath.lineTo(bx1, by1);
            qiblaPath.lineTo(bx2, by2);
            qiblaPath.close();
            canvas.drawPath(qiblaPath, qiblaPaint);

            canvas.restore();

            Path northNeedle = new Path();
            northNeedle.moveTo(cx, cy - radius + 32);
            northNeedle.lineTo(cx - 12, cy + 4);
            northNeedle.lineTo(cx + 12, cy + 4);
            northNeedle.close();
            canvas.drawPath(northNeedle, needlePaint);

            Path southNeedle = new Path();
            southNeedle.moveTo(cx, cy + radius - 32);
            southNeedle.lineTo(cx - 12, cy - 4);
            southNeedle.lineTo(cx + 12, cy - 4);
            southNeedle.close();
            canvas.drawPath(southNeedle, needleSouthPaint);

            canvas.drawCircle(cx, cy, 8, centerPaint);
        }
    }
}