package com.harry.bwatch.data;

/**
 * Immutable view of the most recent value received for each sensor.
 *
 * <p>Each channel keeps its own timestamp because the watch sends independent
 * sensor events. A fresh motion stream does not make an old heart-rate reading
 * valid.
 */
public final class SensorSnapshot {

  public static final SensorSnapshot EMPTY =
    new SensorSnapshot(0, 0f, 0f, 0f, 0f, 0f, 0f, 0L, 0L);

  private final int heartRate;
  private final float gyroX;
  private final float gyroY;
  private final float gyroZ;
  private final float accX;
  private final float accY;
  private final float accZ;
  private final long heartRateUpdatedAtMs;
  private final long motionUpdatedAtMs;

  private SensorSnapshot(
    int heartRate,
    float gyroX,
    float gyroY,
    float gyroZ,
    float accX,
    float accY,
    float accZ,
    long heartRateUpdatedAtMs,
    long motionUpdatedAtMs
  ) {
    this.heartRate = heartRate;
    this.gyroX = gyroX;
    this.gyroY = gyroY;
    this.gyroZ = gyroZ;
    this.accX = accX;
    this.accY = accY;
    this.accZ = accZ;
    this.heartRateUpdatedAtMs = heartRateUpdatedAtMs;
    this.motionUpdatedAtMs = motionUpdatedAtMs;
  }

  public SensorSnapshot withHeartRate(int heartRate, long nowMs) {
    return new SensorSnapshot(
      heartRate,
      gyroX,
      gyroY,
      gyroZ,
      accX,
      accY,
      accZ,
      nowMs,
      motionUpdatedAtMs
    );
  }

  public SensorSnapshot withGyroscope(float x, float y, float z, long nowMs) {
    return new SensorSnapshot(
      heartRate,
      x,
      y,
      z,
      accX,
      accY,
      accZ,
      heartRateUpdatedAtMs,
      nowMs
    );
  }

  public SensorSnapshot withAccelerometer(float x, float y, float z, long nowMs) {
    return new SensorSnapshot(
      heartRate,
      gyroX,
      gyroY,
      gyroZ,
      x,
      y,
      z,
      heartRateUpdatedAtMs,
      nowMs
    );
  }

  public int getHeartRate() {
    return heartRate;
  }

  public float getGyroX() {
    return gyroX;
  }

  public float getGyroY() {
    return gyroY;
  }

  public float getGyroZ() {
    return gyroZ;
  }

  public float getAccX() {
    return accX;
  }

  public float getAccY() {
    return accY;
  }

  public float getAccZ() {
    return accZ;
  }

  /** True when any sensor has reported within {@code timeoutMs}. */
  public boolean isFresh(long nowMs, long timeoutMs) {
    return isRecent(Math.max(heartRateUpdatedAtMs, motionUpdatedAtMs), nowMs, timeoutMs);
  }

  /** True when the heart-rate channel itself has reported recently. */
  public boolean hasFreshHeartRate(long nowMs, long timeoutMs) {
    return isRecent(heartRateUpdatedAtMs, nowMs, timeoutMs);
  }

  private static boolean isRecent(long updatedAtMs, long nowMs, long timeoutMs) {
    if (updatedAtMs <= 0L || timeoutMs < 0L) {
      return false;
    }
    long age = nowMs - updatedAtMs;
    return age >= 0L && age <= timeoutMs;
  }
}
