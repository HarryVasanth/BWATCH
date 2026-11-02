package com.harry.bwatch;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.harry.bwatch.data.SensorRepository;
import com.harry.bwatch.data.SensorSnapshot;

/** Phone-side dashboard. Recording itself lives in {@link RecordingService}. */
public class PhoneActivity extends AppCompatActivity {

  private static final int REQUEST_NOTIFICATIONS = 1;
  private static final long UI_REFRESH_MS = 250L;
  private static final long STALE_AFTER_MS = 5_000L;

  private final Handler handler = new Handler(Looper.getMainLooper());
  private final SensorRepository repository = SensorRepository.getInstance();

  private TextView txtHRate;
  private TextView txtGyro;
  private TextView txtAcc;
  private TextView txtWearStatus;
  private ImageView imgWear;
  private boolean verbose;

  private final Runnable refresh = new Runnable() {
    @Override
    public void run() {
      render(repository.snapshot());
      handler.postDelayed(this, UI_REFRESH_MS);
    }
  };

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_phone);

    txtWearStatus = findViewById(R.id.txtWearStatus);
    imgWear = findViewById(R.id.imgWear);
    txtHRate = findViewById(R.id.txtHRate);
    txtGyro = findViewById(R.id.txtGyro);
    txtAcc = findViewById(R.id.txtAcc);

    requestNotificationPermission();
    ContextCompat.startForegroundService(this, RecordingService.startIntent(this));
  }

  @Override
  protected void onResume() {
    super.onResume();
    handler.removeCallbacks(refresh);
    handler.post(refresh);
  }

  @Override
  protected void onPause() {
    super.onPause();
    handler.removeCallbacks(refresh);
  }

  @Override
  protected void onDestroy() {
    handler.removeCallbacks(refresh);
    super.onDestroy();
  }

  private void render(SensorSnapshot snapshot) {
    long now = System.currentTimeMillis();
    if (!snapshot.isFresh(now, STALE_AFTER_MS)) {
      imgWear.setImageResource(R.drawable.hr_off);
      txtWearStatus.setText(R.string.status_disconnected);
      txtWearStatus.setTextColor(
        ContextCompat.getColor(this, R.color.status_offline)
      );
    } else if (
      snapshot.getHeartRate() <= 0 ||
      !snapshot.hasFreshHeartRate(now, STALE_AFTER_MS)
    ) {
      imgWear.setImageResource(R.drawable.hr_off);
      txtWearStatus.setText(R.string.status_no_heart_rate);
      txtWearStatus.setTextColor(
        ContextCompat.getColor(this, R.color.status_warning)
      );
    } else {
      imgWear.setImageResource(R.drawable.hr_on);
      txtWearStatus.setText(R.string.status_connected);
      txtWearStatus.setTextColor(
        ContextCompat.getColor(this, R.color.status_online)
      );
    }

    txtHRate.setText(getString(R.string.value_hr, snapshot.getHeartRate()));
    txtGyro.setText(
      getString(
        R.string.value_gyro,
        snapshot.getGyroX(),
        snapshot.getGyroY(),
        snapshot.getGyroZ()
      )
    );
    txtAcc.setText(
      getString(
        R.string.value_acc,
        snapshot.getAccX(),
        snapshot.getAccY(),
        snapshot.getAccZ()
      )
    );
  }

  public void btnExit(View view) {
    stopService(RecordingService.stopIntent(this));
    finish();
  }

  public void btnInfo(View view) {
    Toast
      .makeText(getApplicationContext(), R.string.info_text, Toast.LENGTH_LONG)
      .show();
  }

  public void btnVerbose(View view) {
    verbose = !verbose;
    int visibility = verbose ? View.VISIBLE : View.INVISIBLE;
    txtHRate.setVisibility(visibility);
    txtGyro.setVisibility(visibility);
    txtAcc.setVisibility(visibility);
  }

  public void btnAudio(View view) {
    String packageName = getString(R.string.audio_logger_package);
    Intent launchIntent = getPackageManager().getLaunchIntentForPackage(packageName);
    if (launchIntent == null) {
      Toast.makeText(this, R.string.audio_logger_missing, Toast.LENGTH_SHORT).show();
      return;
    }
    try {
      startActivity(launchIntent);
    } catch (ActivityNotFoundException exception) {
      Toast.makeText(this, R.string.audio_logger_missing, Toast.LENGTH_SHORT).show();
    }
  }

  private void requestNotificationPermission() {
    if (
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
      ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.POST_NOTIFICATIONS
      ) !=
      PackageManager.PERMISSION_GRANTED
    ) {
      ActivityCompat.requestPermissions(
        this,
        new String[] { Manifest.permission.POST_NOTIFICATIONS },
        REQUEST_NOTIFICATIONS
      );
    }
  }
}
