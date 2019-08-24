package com.shinow.qrscan;

import android.widget.LinearLayout;
import android.view.View;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;

class LightSensorEventListener implements SensorEventListener {

    private LinearLayout lightLayout;

    public LightSensorEventListener(LinearLayout lightLayout) {
        this.lightLayout = lightLayout;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // System.out.println("---------------------|||||||||||||---light strength: " + light_strength + "---|||||||||||-------------------------");
        if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
            float light_strength = event.values[0];
            if (lightLayout.getVisibility() == View.VISIBLE && light_strength > 300 && !SecondActivity.isLightOpen) {
                lightLayout.setVisibility(View.INVISIBLE);
            } else if (lightLayout.getVisibility() == View.INVISIBLE && light_strength <= 200) {
                lightLayout.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        System.out.println("---------------------|||||||||||||---onAccuracyChanged---|||||||||||-------------------------");
    }
}