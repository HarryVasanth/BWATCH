package com.harry.bwatch;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.widget.TextView;
import java.util.Locale;

/**
 * Streams heart rate, gyroscope and accelerometer samples to the phone.
 *
 * <p>All payloads are formatted with {@link Locale#US}; a locale with a comma
 * decimal separator would otherwise corrupt the CSV protocol.
 */
public class WearActivity extends Activity implements SensorEventListener {

  private static final int REQUEST_BODY_SENSORS = 1;
  private static final float GRAVITY_ALPHA = 0.8f;
  // The phone stores one sample per second; forwarding at sensor rate would
  // spend battery and Bluetooth bandwidth without improving the CSV.
  private static final long MIN_TRANSMIT_INTERVAL_MS = 200L;

  private final float[] gravity = new float[3];
  private long lastGyroSentMs;
  private long lastAccelSentMs;

  private SensorManager sensorManager;
  private Sensor heartRateSensor;
  private Sensor gyroscopeSensor;
  private Sensor accelerometerSensor;
  private TextView statusView;
  private MessageSender sender;
  private boolean sensorsRegistered;
  private boolean permissionRequested;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_wear);
    statusView = findViewById(R.id.textHR);
    sender = new MessageSender(this);
  }

  @Override
  protected void onResume() {
    super.onResume();
    sender.refresh();

    if (
      checkSelfPermission(Manifest.permission.BODY_SENSORS) ==
      PackageManager.PERMISSION_GRANTED
    ) {
      startSensors();
    } else if (!permissionRequested) {
      permissionRequested = true;
      requestPermissions(
        new String[] { Manifest.permission.BODY_SENSORS },
        REQUEST_BODY_SENSORS
      );
    } else {
      statusView.setText(R.string.sensor_permission_required);
    }
  }

  @Override
  public void onRequestPermissionsResult(
    int requestCode,
    String[] permissions,
    int[] grantResults
  ) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    if (requestCode != REQUEST_BODY_SENSORS) {
      return;
    }
    if (
      grantResults.length > 0 &&
      grantResults[0] == PackageManager.PERMISSION_GRANTED
    ) {
      startSensors();
    } else {
      statusView.setText(R.string.sensor_permission_required);
    }
  }

  private void startSensors() {
    if (sensorsRegistered) {
      return;
    }
    sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
    if (sensorManager == null) {
      statusView.setText(R.string.sensors_unavailable);
      return;
    }

    heartRateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);
    gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
    accelerometerSensor =
      sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

    boolean registered = false;
    if (heartRateSensor != null) {
      registered =
        sensorManager.registerListener(
          this,
          heartRateSensor,
          SensorManager.SENSOR_DELAY_NORMAL
        ) ||
        registered;
    } else {
      statusView.setText(R.string.heart_rate_unavailable);
    }
    if (gyroscopeSensor != null) {
      registered =
        sensorManager.registerListener(
          this,
          gyroscopeSensor,
          SensorManager.SENSOR_DELAY_GAME
        ) ||
        registered;
    }
    if (accelerometerSensor != null) {
      registered =
        sensorManager.registerListener(
          this,
          accelerometerSensor,
          SensorManager.SENSOR_DELAY_GAME
        ) ||
        registered;
    }

    if (!registered) {
      statusView.setText(R.string.sensors_unavailable);
      return;
    }
    sensorsRegistered = true;
  }

  private void stopSensors() {
    if (sensorManager != null) {
      sensorManager.unregisterListener(this);
    }
    sensorsRegistered = false;
  }

  @Override
  public void onSensorChanged(SensorEvent event) {
    if (event == null || event.values == null || event.values.length < 1) {
      return;
    }

    long now = SystemClock.elapsedRealtime();
    switch (event.sensor.getType()) {
      case Sensor.TYPE_HEART_RATE:
        int bpm = Math.round(event.values[0]);
        if (bpm <= 0 || bpm > 300) {
          return;
        }
        statusView.setText(getString(R.string.heart_rate_value, bpm));
        sender.send(String.format(Locale.US, "HR,%d", bpm));
        break;
      case Sensor.TYPE_GYROSCOPE:
        if (event.values.length < 3) {
          return;
        }
        if (now - lastGyroSentMs < MIN_TRANSMIT_INTERVAL_MS) {
          return;
        }
        lastGyroSentMs = now;
        sender.send(
          String.format(
            Locale.US,
            "GYRO,%.3f,%.3f,%.3f",
            Math.toDegrees(event.values[0]),
            Math.toDegrees(event.values[1]),
            Math.toDegrees(event.values[2])
          )
        );
        break;
      case Sensor.TYPE_ACCELEROMETER:
        if (event.values.length < 3) {
          return;
        }
        for (int axis = 0; axis < 3; axis++) {
          gravity[axis] =
            GRAVITY_ALPHA * gravity[axis] +
            (1f - GRAVITY_ALPHA) * event.values[axis];
        }
        if (now - lastAccelSentMs < MIN_TRANSMIT_INTERVAL_MS) {
          return;
        }
        lastAccelSentMs = now;
        sender.send(
          String.format(
            Locale.US,
            "ACC,%.3f,%.3f,%.3f",
            event.values[0] - gravity[0],
            event.values[1] - gravity[1],
            event.values[2] - gravity[2]
          )
        );
        break;
      default:
        break;
    }
  }

  @Override
  public void onAccuracyChanged(Sensor sensor, int accuracy) {}

  @Override
  protected void onPause() {
    super.onPause();
    stopSensors();
  }

  @Override
  protected void onDestroy() {
    stopSensors();
    super.onDestroy();
  }

  public void stopRecording(View view) {
    sender.send("STOP");
    stopSensors();
    finish();
  }
}
