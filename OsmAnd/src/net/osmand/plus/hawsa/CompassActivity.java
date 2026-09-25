package net.osmand.plus.hawsa;

import android.Manifest;
import android.app.Activity;
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
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;

import net.osmand.plus.R;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.SimpleTimeZone;
import java.util.TimeZone;

public class CompassActivity extends Activity implements SensorEventListener, LocationListener {

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor magnetometer;

    private float[] gravityValues = null;
    private float[] geomagneticValues = null;
    private float[] rotationMatrix = new float[9];
    private float[] orientationValues = new float[3];

    private float currentAzimuth = 0f;
    private float qiblaBearing = 0f;

    private double latitude = 21.4225;  // default: Makkah
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

    // Kaaba coordinates
    private static final double KAABA_LAT = 21.4225;
    private static final double KAABA_LNG = 39.8262;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compass);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Views
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

        btnCompassBack.setOnClickListener(v -> finish());

        // Sensors
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        // Location
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        requestLocation();

        // Calculate Qibla for default location
        calculateQibla();

        // Calculate prayer times
        calculatePrayerTimes();
    }

    private void requestLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }
        try {
            Criteria criteria = new Criteria();
            criteria.setAccuracy(Criteria.ACCURACY_FINE);
            String provider = locationManager.getBestProvider(criteria, true);
            if (provider != null) {
                locationManager.requestSingleUpdate(provider, this, null);
                Location lastKnown = locationManager.getLastKnownLocation(provider);
                if (lastKnown != null) {
                    updateLocation(lastKnown);
                }
            }
        } catch (Exception e) {
            // Use default Makkah location
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
    public void onStatusChanged(String provider, int status, Bundle extras) {}

    @Override
    public void onProviderEnabled(String provider) {}

    @Override
    public void onProviderDisabled(String provider) {}

    private void calculateQibla() {
        double latRad = Math.toRadians(latitude);
        double kaabaLatRad = Math.toRadians(KAABA_LAT);
        double dLng = Math.toRadians(KAABA_LNG - longitude);

        double x = Math.sin(dLng);
        double y = Math.cos(latRad) * Math.tan(kaabaLatRad) - Math.sin(latRad) * Math.cos(dLng);

        qiblaBearing = (float) Math.toDegrees(Math.atan2(x, y));
        if (qiblaBearing < 0) qiblaBearing += 360;

        // Declination correction
        GeomagneticField geoField = new GeomagneticField(
                (float) latitude, (float) longitude, (float) altitude,
                System.currentTimeMillis());
        qiblaBearing -= geoField.getDeclination();

        compassView.setQiblaBearing(qiblaBearing);

        String qiblaText = String.format("اتجاه القبلة: %.1f°", qiblaBearing);
        tvQiblaDirection.setText(qiblaText);
    }

    // --- Prayer Times Calculation (Simplified Solar Position) ---
    private void calculatePrayerTimes() {
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

        double noon = q - Math.toDegrees(longitude) - timezoneOffset * 15;
        while (noon < 0) noon += 360;
        while (noon >= 360) noon -= 360;

        double sunNoon = 12 + (noon - RA) / 15.0;
        if (sunNoon < 0) sunNoon += 24;

        double latRad = Math.toRadians(latitude);
        double decRad = Dec;

        // Equation of time correction (simplified)
        double eqTime = (q - RA) * 4; // in minutes (approximate)
        double solarNoon = 12 - longitude / 15.0 + timezoneOffset - eqTime / 60.0;

        // Hour angle for sun angle
        double cosH = (Math.sin(Math.toRadians(-0.833)) - Math.sin(latRad) * Math.sin(decRad))
                / (Math.cos(latRad) * Math.cos(decRad));
        if (cosH > 1) cosH = 1;
        if (cosH < -1) cosH = -1;
        double H = Math.toDegrees(Math.acos(cosH));

        double sunriseTime = solarNoon - H / 15.0;
        double sunsetTime = solarNoon + H / 15.0;

        // Fajr: sun at -18° (Isha), Fajr at -15° — using standard -15 for Fajr
        double cosHFajr = (Math.sin(Math.toRadians(-15)) - Math.sin(latRad) * Math.sin(decRad))
                / (Math.cos(latRad) * Math.cos(decRad));
        if (cosHFajr > 1) cosHFajr = 1;
        if (cosHFajr < -1) cosHFajr = -1;
        double HFajr = Math.toDegrees(Math.acos(cosHFajr));
        double fajrTime = solarNoon - HFajr / 15.0;

        // Isha: sun at -17°
        double cosHIsha = (Math.sin(Math.toRadians(-17)) - Math.sin(latRad) * Math.sin(decRad))
                / (Math.cos(latRad) * Math.cos(decRad));
        if (cosHIsha > 1) cosHIsha = 1;
        if (cosHIsha < -1) cosHIsha = -1;
        double HIsha = Math.toDegrees(Math.acos(cosHIsha));
        double ishaTime = solarNoon + HIsha / 15.0;

        double dhuhrTime = solarNoon;
        double asrTime = solarNoon + (H * 0.5) / 15.0; // Shafi'i: shadow = object + shadow at noon
        double maghribTime = sunsetTime;

        tvFajrTime.setText(formatTime(fajrTime));
        tvSunriseTime.setText(formatTime(sunriseTime));
        tvDhuhrTime.setText(formatTime(dhuhrTime));
        tvAsrTime.setText(formatTime(asrTime));
        tvMaghribTime.setText(formatTime(maghribTime));
        tvIshaTime.setText(formatTime(ishaTime));
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
        return String.format("%02d:%02d", hour, minute);
    }

    // --- Sensor Handling ---
    @Override
    protected void onResume() {
        super.onResume();
        if (accelerometer != null)
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        if (magnetometer != null)
            sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
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

                // Declination correction
                GeomagneticField geoField = new GeomagneticField(
                        (float) latitude, (float) longitude, (float) altitude,
                        System.currentTimeMillis());
                azimuth += geoField.getDeclination();

                currentAzimuth = azimuth;
                compassView.setAzimuth(azimuth);
                compassView.invalidate();

                tvCompassDegree.setText(String.format("%.0f°", azimuth));
            }
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

    // --- Custom Compass View ---
    public static class CompassView extends View {

        private float azimuth = 0f;
        private float qiblaBearing = 0f;
        private Paint circlePaint;
        private Paint tickPaint;
        private Paint tickMinorPaint;
        private Paint textPaint;
        private Paint needlePaint;
        private Paint needleSouthPaint;
        private Paint qiblaPaint;
        private Paint qiblaArrowPaint;
        private Paint centerPaint;

        public CompassView(Context context) {
            super(context);
            init();
        }

        private void init() {
            circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            circlePaint.setStyle(Paint.Style.STROKE);
            circlePaint.setColor(Color.parseColor("#FFD700"));
            circlePaint.setStrokeWidth(3f);

            tickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            tickPaint.setStyle(Paint.Style.STROKE);
            tickPaint.setColor(Color.parseColor("#FFD700"));
            tickPaint.setStrokeWidth(3f);

            tickMinorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            tickMinorPaint.setStyle(Paint.Style.STROKE);
            tickMinorPaint.setColor(Color.parseColor("#AAAAAA"));
            tickMinorPaint.setStrokeWidth(1.5f);

            textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            textPaint.setColor(Color.parseColor("#FFFFFF"));
            textPaint.setTextSize(36f);
            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setFakeBoldText(true);

            needlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            needlePaint.setColor(Color.parseColor("#FF4444"));
            needlePaint.setStyle(Paint.Style.FILL);

            needleSouthPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            needleSouthPaint.setColor(Color.parseColor("#FFFFFF"));
            needleSouthPaint.setStyle(Paint.Style.FILL);

            qiblaPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            qiblaPaint.setColor(Color.parseColor("#00FF88"));
            qiblaPaint.setStyle(Paint.Style.FILL);

            qiblaArrowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            qiblaArrowPaint.setColor(Color.parseColor("#00FF88"));
            qiblaArrowPaint.setStyle(Paint.Style.STROKE);
            qiblaArrowPaint.setStrokeWidth(3f);

            centerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            centerPaint.setColor(Color.parseColor("#FFD700"));
            centerPaint.setStyle(Paint.Style.FILL);
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

            int w = getWidth();
            int h = getHeight();
            int cx = w / 2;
            int cy = h / 2;
            int radius = Math.min(cx, cy) - 16;

            // Draw background circle
            Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            bgPaint.setColor(Color.parseColor("#0D1B2A"));
            bgPaint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(cx, cy, radius, bgPaint);

            // Draw outer ring
            canvas.drawCircle(cx, cy, radius, circlePaint);

            // Draw ticks and labels (rotated by -azimuth so N stays at top)
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

            // Cardinal labels
            float labelR = radius - 48;
            String[] cardinals = {"N", "E", "S", "W"};
            int[] angles = {0, 90, 180, 270};
            for (int i = 0; i < 4; i++) {
                float rad = (float) Math.toRadians(angles[i]);
                float tx = cx + labelR * (float) Math.sin(rad);
                float ty = cy - labelR * (float) Math.cos(rad);
                Paint dirPaint = new Paint(textPaint);
                if (cardinals[i].equals("N")) {
                    dirPaint.setColor(Color.parseColor("#FF4444"));
                    dirPaint.setTextSize(42f);
                }
                canvas.drawText(cardinals[i], tx, ty + dirPaint.getTextSize() / 3, dirPaint);
            }

            // Qibla indicator arrow (rotates with compass)
            float qRad = (float) Math.toRadians(qiblaBearing);
            float qStartR = radius - 55;
            float qEndR = radius - 16;

            float qx1 = cx + qStartR * (float) Math.sin(qRad);
            float qy1 = cy - qStartR * (float) Math.cos(qRad);
            float qx2 = cx + qEndR * (float) Math.sin(qRad);
            float qy2 = cy - qEndR * (float) Math.cos(qRad);

            // Qibla triangle
            Path qiblaPath = new Path();
            float qTipR = radius - 10;
            float qBaseR = radius - 30;
            float qTipX = cx + qTipR * (float) Math.sin(qRad);
            float qTipY = cy - qTipR * (float) Math.cos(qRad);
            float perpRad = qRad + (float) Math.PI / 2;
            float qBaseX = cx + qBaseR * (float) Math.sin(qRad);
            float qBaseY = cy - qBaseR * (float) Math.cos(qRad);
            float spread = 8f;
            float bx1 = qBaseX + spread * (float) Math.sin(perpRad);
            float by1 = qBaseY - spread * (float) Math.cos(perpRad);
            float bx2 = qBaseX - spread * (float) Math.sin(perpRad);
            float by2 = qBaseY + spread * (float) Math.cos(perpRad);

            qiblaPath.moveTo(qTipX, qTipY);
            qiblaPath.lineTo(bx1, by1);
            qiblaPath.lineTo(bx2, by2);
            qiblaPath.close();
            canvas.drawPath(qiblaPath, qiblaPaint);

            // Kaaba label
            float kaabaR = radius - 62;
            float kaabaX = cx + kaabaR * (float) Math.sin(qRad);
            float kaabaY = cy - kaabaR * (float) Math.cos(qRad);
            Paint kaabaPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            kaabaPaint.setColor(Color.parseColor("#00FF88"));
            kaabaPaint.setTextSize(22f);
            kaabaPaint.setTextAlign(Paint.Align.CENTER);
            kaabaPaint.setFakeBoldText(true);
            canvas.drawText("🕋", kaabaX, kaabaY + 8, kaabaPaint);

            canvas.restore();

            // North needle (fixed at top)
            Path northNeedle = new Path();
            northNeedle.moveTo(cx, cy - radius + 32);
            northNeedle.lineTo(cx - 12, cy + 4);
            northNeedle.lineTo(cx + 12, cy + 4);
            northNeedle.close();
            canvas.drawPath(northNeedle, needlePaint);

            // South needle
            Path southNeedle = new Path();
            southNeedle.moveTo(cx, cy + radius - 32);
            southNeedle.lineTo(cx - 12, cy - 4);
            southNeedle.lineTo(cx + 12, cy - 4);
            southNeedle.close();
            canvas.drawPath(southNeedle, needleSouthPaint);

            // Center dot
            canvas.drawCircle(cx, cy, 8, centerPaint);
        }
    }
}
