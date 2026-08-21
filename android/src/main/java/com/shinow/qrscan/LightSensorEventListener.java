package com.shinow.qrscan;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.view.View;

class LightSensorEventListener implements SensorEventListener {

    private final View lightLayout;

    LightSensorEventListener(View lightLayout) {
        this.lightLayout = lightLayout;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_LIGHT || lightLayout == null) {
            return;
        }
        float strength = event.values[0];
        if (lightLayout.getVisibility() == View.VISIBLE && strength > 300 && !SecondActivity.isLightOpen) {
            lightLayout.setVisibility(View.INVISIBLE);
        } else if (lightLayout.getVisibility() == View.INVISIBLE && strength <= 200) {
            lightLayout.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
}
