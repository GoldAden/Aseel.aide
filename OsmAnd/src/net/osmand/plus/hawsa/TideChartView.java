package net.osmand.plus.hawsa;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class TideChartView extends View {

    private List<Float> hours = new ArrayList<>();
    private List<Float> heights = new ArrayList<>();
    private float minHeight = -1.5f;
    private float maxHeight = 2.5f;

    private Paint bgPaint;
    private Paint gridPaint;
    private Paint axisTextPaint;
    private Paint curvePaint;
    private Paint fillPaint;
    private Paint highTidePaint;
    private Paint lowTidePaint;
    private Paint currentLinePaint;

    private float paddingStart = 48f;
    private float paddingEnd = 16f;
    private float paddingTop = 24f;
    private float paddingBottom = 40f;

    public TideChartView(Context context) {
        super(context);
        init();
    }

    public TideChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public TideChartView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setColor(Color.parseColor("#0A1628"));
        bgPaint.setStyle(Paint.Style.FILL);

        gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gridPaint.setColor(Color.parseColor("#1A2E4A"));
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(1f);

        axisTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        axisTextPaint.setColor(Color.parseColor("#AAAAAA"));
        axisTextPaint.setTextSize(24f);
        axisTextPaint.setTextAlign(Paint.Align.CENTER);

        curvePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        curvePaint.setColor(Color.parseColor("#00BFFF"));
        curvePaint.setStyle(Paint.Style.STROKE);
        curvePaint.setStrokeWidth(3f);

        fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fillPaint.setColor(Color.parseColor("#004080"));
        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setAlpha(80);

        highTidePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        highTidePaint.setColor(Color.parseColor("#00FF88"));
        highTidePaint.setStyle(Paint.Style.FILL);
        highTidePaint.setTextSize(20f);

        lowTidePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        lowTidePaint.setColor(Color.parseColor("#FFAA33"));
        lowTidePaint.setStyle(Paint.Style.FILL);
        lowTidePaint.setTextSize(20f);

        currentLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        currentLinePaint.setColor(Color.parseColor("#FFD700"));
        currentLinePaint.setStyle(Paint.Style.STROKE);
        currentLinePaint.setStrokeWidth(1.5f);
    }

    public void setData(List<Float> h, List<Float> ht) {
        hours.clear();
        heights.clear();
        hours.addAll(h);
        heights.addAll(ht);

        // Auto range
        float min = Float.MAX_VALUE;
        float max = Float.MIN_VALUE;
        for (float v : heights) {
            if (v < min) min = v;
            if (v > max) max = v;
        }
        minHeight = (float) Math.floor(min - 0.5);
        maxHeight = (float) Math.ceil(max + 0.5);

        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int w = getWidth();
        int h = getHeight();

        // Background
        canvas.drawRect(0, 0, w, h, bgPaint);

        float chartW = w - paddingStart - paddingEnd;
        float chartH = h - paddingTop - paddingBottom;

        // Horizontal grid lines (height)
        int numHLines = (int) (maxHeight - minHeight) + 1;
        for (int i = 0; i <= numHLines; i++) {
            float val = minHeight + i;
            float y = paddingTop + chartH * (1 - (val - minHeight) / (maxHeight - minHeight));
            canvas.drawLine(paddingStart, y, w - paddingEnd, y, gridPaint);

            String label = String.format("%.0f", val);
            axisTextPaint.setTextAlign(Paint.Align.RIGHT);
            canvas.drawText(label, paddingStart - 8, y + 6, axisTextPaint);
        }

        // Vertical grid lines (hours)
        for (int hr = 0; hr <= 24; hr += 3) {
            float x = paddingStart + chartW * (hr / 24f);
            canvas.drawLine(x, paddingTop, x, paddingTop + chartH, gridPaint);

            axisTextPaint.setTextAlign(Paint.Align.CENTER);
            String label = String.format("%02d", hr);
            canvas.drawText(label, x, h - paddingBottom + 28, axisTextPaint);
        }

        // Draw zero line
        float zeroY = paddingTop + chartH * (1 - (0 - minHeight) / (maxHeight - minHeight));
        Paint zeroPaint = new Paint(gridPaint);
        zeroPaint.setColor(Color.parseColor("#AAAAAA"));
        zeroPaint.setStrokeWidth(1.5f);
        canvas.drawLine(paddingStart, zeroY, w - paddingEnd, zeroY, zeroPaint);

        // Draw filled area + curve
        if (heights.size() > 1) {
            Path fillPath = new Path();
            Path curvePath = new Path();

            float x0 = paddingStart + chartW * (hours.get(0) / 24f);
            float y0 = paddingTop + chartH * (1 - (heights.get(0) - minHeight) / (maxHeight - minHeight));

            fillPath.moveTo(x0, paddingTop + chartH);
            fillPath.lineTo(x0, y0);
            curvePath.moveTo(x0, y0);

            for (int i = 1; i < heights.size(); i++) {
                float x = paddingStart + chartW * (hours.get(i) / 24f);
                float y = paddingTop + chartH * (1 - (heights.get(i) - minHeight) / (maxHeight - minHeight));

                // Smooth cubic bezier
                float prevX = paddingStart + chartW * (hours.get(i - 1) / 24f);
                float prevY = paddingTop + chartH * (1 - (heights.get(i - 1) - minHeight) / (maxHeight - minHeight));
                float midX = (prevX + x) / 2;

                curvePath.cubicTo(midX, prevY, midX, y, x, y);
                fillPath.cubicTo(midX, prevY, midX, y, x, y);
            }

            float lastX = paddingStart + chartW * (hours.get(hours.size() - 1) / 24f);
            fillPath.lineTo(lastX, paddingTop + chartH);
            fillPath.close();

            canvas.drawPath(fillPath, fillPaint);
            canvas.drawPath(curvePath, curvePaint);

            // Mark high and low tide points
            for (int i = 1; i < heights.size() - 1; i++) {
                float prev = heights.get(i - 1);
                float curr = heights.get(i);
                float next = heights.get(i + 1);

                if (curr > prev && curr > next) {
                    // High tide
                    float x = paddingStart + chartW * (hours.get(i) / 24f);
                    float y = paddingTop + chartH * (1 - (curr - minHeight) / (maxHeight - minHeight));
                    canvas.drawCircle(x, y, 6, highTidePaint);
                    String label = String.format("%.1f", curr);
                    canvas.drawText(label, x, y - 14, highTidePaint);
                } else if (curr < prev && curr < next) {
                    // Low tide
                    float x = paddingStart + chartW * (hours.get(i) / 24f);
                    float y = paddingTop + chartH * (1 - (curr - minHeight) / (maxHeight - minHeight));
                    canvas.drawCircle(x, y, 6, lowTidePaint);
                    String label = String.format("%.1f", curr);
                    canvas.drawText(label, x, y + 22, lowTidePaint);
                }
            }
        }

        // "م" label for meters
        axisTextPaint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText("م", paddingStart - 8, paddingTop + 10, axisTextPaint);
    }
}
