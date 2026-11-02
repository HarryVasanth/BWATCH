package com.harry.bwatch.data;

/** A validated sensor reading received from the watch. */
public final class SensorUpdate {

  public enum Type {
    HEART_RATE,
    GYROSCOPE,
    ACCELEROMETER,
    STOP,
  }

  private final Type type;
  private final int heartRate;
  private final float x;
  private final float y;
  private final float z;

  private SensorUpdate(Type type, int heartRate, float x, float y, float z) {
    this.type = type;
    this.heartRate = heartRate;
    this.x = x;
    this.y = y;
    this.z = z;
  }

  public static SensorUpdate heartRate(int bpm) {
    return new SensorUpdate(Type.HEART_RATE, bpm, 0f, 0f, 0f);
  }

  public static SensorUpdate gyroscope(float x, float y, float z) {
    return new SensorUpdate(Type.GYROSCOPE, 0, x, y, z);
  }

  public static SensorUpdate accelerometer(float x, float y, float z) {
    return new SensorUpdate(Type.ACCELEROMETER, 0, x, y, z);
  }

  public static SensorUpdate stop() {
    return new SensorUpdate(Type.STOP, 0, 0f, 0f, 0f);
  }

  public Type getType() {
    return type;
  }

  public int getHeartRate() {
    return heartRate;
  }

  public float getX() {
    return x;
  }

  public float getY() {
    return y;
  }

  public float getZ() {
    return z;
  }
}
