package com.example.lab3mtm;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class WaveformView extends View {
    private byte[] mBytes;
    private float[] mPoints;
    private Paint mPaint = new Paint();

    public WaveformView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mBytes = null;
        mPaint.setStrokeWidth(5f);
        mPaint.setAntiAlias(true);
        mPaint.setColor(Color.MAGENTA);
    }

    // Metoda do przekazywania danych z Visualizera
    public void updateVisualizer(byte[] bytes) {
        mBytes = bytes;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mBytes == null) {
            return;
        }

        if (mPoints == null || mPoints.length < mBytes.length * 4) {
            mPoints = new float[mBytes.length * 4];
        }

        // Obliczanie współrzędnych punktów do narysowania linii
        for (int i = 0; i < mBytes.length - 1; i++) {
            mPoints[i * 4] = canvas.getWidth() * i / (float) (mBytes.length - 1);
            mPoints[i * 4 + 1] = canvas.getHeight() / 2f + ((byte) (mBytes[i] + 128)) * (canvas.getHeight() / 2f) / 128;
            mPoints[i * 4 + 2] = canvas.getWidth() * (i + 1) / (float) (mBytes.length - 1);
            mPoints[i * 4 + 3] = canvas.getHeight() / 2f + ((byte) (mBytes[i + 1] + 128)) * (canvas.getHeight() / 2f) / 128;
        }

        canvas.drawLines(mPoints, mPaint);
    }
}