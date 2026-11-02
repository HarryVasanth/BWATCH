package com.harry.bwatch.data;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Process-wide cache of the latest sensor values.
 *
 * <p>The listener service writes from a binder thread while the UI and the
 * recorder read from their own threads, so the snapshot is swapped atomically
 * instead of mutating shared fields.
 */
public final class SensorRepository {

  private static final SensorRepository INSTANCE = new SensorRepository();

  private final AtomicReference<SensorSnapshot> snapshot = new AtomicReference<>(
    SensorSnapshot.EMPTY
  );

  private SensorRepository() {}

  public static SensorRepository getInstance() {
    return INSTANCE;
  }

  public SensorSnapshot snapshot() {
    return snapshot.get();
  }

  public void apply(SensorUpdate update, long nowMs) {
    if (update == null) {
      return;
    }
    // compareAndSet keeps API 23 support; AtomicReference.updateAndGet is API 24.
    while (true) {
      SensorSnapshot current = snapshot.get();
      SensorSnapshot next = applyTo(current, update, nowMs);
      if (snapshot.compareAndSet(current, next)) {
        return;
      }
    }
  }

  private static SensorSnapshot applyTo(
    SensorSnapshot current,
    SensorUpdate update,
    long nowMs
  ) {
    switch (update.getType()) {
      case HEART_RATE:
        return current.withHeartRate(update.getHeartRate(), nowMs);
      case GYROSCOPE:
        return current.withGyroscope(
          update.getX(),
          update.getY(),
          update.getZ(),
          nowMs
        );
      case ACCELEROMETER:
        return current.withAccelerometer(
          update.getX(),
          update.getY(),
          update.getZ(),
          nowMs
        );
      case STOP:
        return SensorSnapshot.EMPTY;
      default:
        return current;
    }
  }

  public void clear() {
    snapshot.set(SensorSnapshot.EMPTY);
  }
}
