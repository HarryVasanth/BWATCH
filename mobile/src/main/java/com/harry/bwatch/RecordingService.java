package com.harry.bwatch;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.ServiceCompat;
import com.harry.bwatch.data.SensorRepository;
import com.harry.bwatch.data.SensorSnapshot;
import com.harry.bwatch.data.SessionWriter;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Records the latest sensor snapshot once per second while the phone screen is
 * off.
 *
 * <p>A foreground service and a partial wake lock are used because a plain
 * {@link android.os.Handler} or {@link java.util.Timer} in the activity stops
 * as soon as the study participant turns the screen off.
 */
public final class RecordingService extends Service {

  public static final String ACTION_STOP = "com.harry.bwatch.action.STOP";

  private static final String TAG = "RecordingService";
  private static final String CHANNEL_ID = "recording";
  private static final String DIRECTORY_NAME = "BWATCH_data";
  private static final int NOTIFICATION_ID = 1;
  private static final long SAMPLE_PERIOD_MS = 1_000L;
  private static final long STALE_AFTER_MS = 5_000L;
  private static final long WAKE_LOCK_TIMEOUT_MS = 24L * 60L * 60L * 1_000L;

  private final SensorRepository repository = SensorRepository.getInstance();

  private ScheduledExecutorService scheduler;
  private SessionWriter writer;
  private PowerManager.WakeLock wakeLock;

  public static Intent startIntent(Context context) {
    return new Intent(context, RecordingService.class);
  }

  public static Intent stopIntent(Context context) {
    return new Intent(context, RecordingService.class).setAction(ACTION_STOP);
  }

  @Override
  public void onCreate() {
    super.onCreate();
    createNotificationChannel();
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    if (intent != null && ACTION_STOP.equals(intent.getAction())) {
      stopSelf();
      return START_NOT_STICKY;
    }

    startForegroundCompat();
    acquireWakeLock();
    startSampling();
    return START_NOT_STICKY;
  }

  private void startSampling() {
    if (scheduler != null) {
      return;
    }

    try {
      writer = new SessionWriter(sessionDirectory(), sessionFileName());
    } catch (IOException error) {
      Log.e(TAG, "Unable to create the session file", error);
      stopSelf();
      return;
    }

    updateNotification(writer.file().getName());
    scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
      Thread thread = new Thread(runnable, "bwatch-sampler");
      thread.setDaemon(true);
      return thread;
    });
    scheduler.scheduleWithFixedDelay(
      this::sampleOnce,
      0L,
      SAMPLE_PERIOD_MS,
      TimeUnit.MILLISECONDS
    );
  }

  private void sampleOnce() {
    SessionWriter currentWriter = writer;
    if (currentWriter == null) {
      return;
    }

    long now = System.currentTimeMillis();
    SensorSnapshot snapshot = repository.snapshot();
    if (!snapshot.isFresh(now, STALE_AFTER_MS)) {
      return;
    }

    try {
      currentWriter.append(now, snapshot);
    } catch (IOException error) {
      Log.e(TAG, "Unable to write a sample", error);
      stopSelf();
    }
  }

  private File sessionDirectory() {
    File external = getExternalFilesDir(DIRECTORY_NAME);
    return external != null ? external : new File(getFilesDir(), DIRECTORY_NAME);
  }

  private String sessionFileName() {
    String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(
      new Date()
    );
    return "bWatch_" + timestamp + ".csv";
  }

  private void startForegroundCompat() {
    int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
      ? ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
      : 0;
    ServiceCompat.startForeground(
      this,
      NOTIFICATION_ID,
      buildNotification(null),
      type
    );
  }

  private void updateNotification(String fileName) {
    NotificationManager manager = getSystemService(NotificationManager.class);
    if (manager != null) {
      manager.notify(NOTIFICATION_ID, buildNotification(fileName));
    }
  }

  private Notification buildNotification(@Nullable String fileName) {
    Intent openIntent = new Intent(this, PhoneActivity.class);
    PendingIntent openPendingIntent = PendingIntent.getActivity(
      this,
      0,
      openIntent,
      PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
    );
    PendingIntent stopPendingIntent = PendingIntent.getService(
      this,
      1,
      stopIntent(this),
      PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
    );

    String text = fileName == null
      ? getString(R.string.notification_starting)
      : getString(R.string.notification_recording, fileName);

    return new NotificationCompat.Builder(this, CHANNEL_ID)
      .setSmallIcon(R.drawable.ic_stat_bwatch)
      .setContentTitle(getString(R.string.notification_title))
      .setContentText(text)
      .setContentIntent(openPendingIntent)
      .setOngoing(true)
      .setSilent(true)
      .setCategory(NotificationCompat.CATEGORY_SERVICE)
      .setPriority(NotificationCompat.PRIORITY_LOW)
      .addAction(0, getString(R.string.notification_stop), stopPendingIntent)
      .build();
  }

  private void createNotificationChannel() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
      return;
    }
    NotificationChannel channel = new NotificationChannel(
      CHANNEL_ID,
      getString(R.string.notification_channel),
      NotificationManager.IMPORTANCE_LOW
    );
    channel.setDescription(getString(R.string.notification_channel_description));
    channel.setShowBadge(false);

    NotificationManager manager = getSystemService(NotificationManager.class);
    if (manager != null) {
      manager.createNotificationChannel(channel);
    }
  }

  private void acquireWakeLock() {
    if (wakeLock != null) {
      return;
    }
    PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
    if (powerManager == null) {
      return;
    }
    wakeLock = powerManager.newWakeLock(
      PowerManager.PARTIAL_WAKE_LOCK,
      "bwatch:recording"
    );
    wakeLock.setReferenceCounted(false);
    wakeLock.acquire(WAKE_LOCK_TIMEOUT_MS);
  }

  private void releaseWakeLock() {
    if (wakeLock != null && wakeLock.isHeld()) {
      wakeLock.release();
    }
    wakeLock = null;
  }

  @Override
  public void onDestroy() {
    if (scheduler != null) {
      scheduler.shutdownNow();
      scheduler = null;
    }
    if (writer != null) {
      try {
        writer.close();
      } catch (IOException error) {
        Log.e(TAG, "Unable to close the session file", error);
      }
      writer = null;
    }
    releaseWakeLock();
    super.onDestroy();
  }

  @Nullable
  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }
}
