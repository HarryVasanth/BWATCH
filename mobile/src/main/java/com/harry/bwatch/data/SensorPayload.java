package com.harry.bwatch.data;

import java.util.Locale;

/**
 * Message contract shared with the wear module.
 *
 * <p>Payloads are short comma-separated ASCII records:
 *
 * <pre>
 *   HR,72
 *   GYRO,1.250,-4.500,0.000
 *   ACC,0.100,0.200,9.810
 *   STOP
 * </pre>
 *
 * <p>{@link #PATH} must match the path used by the wear module.
 */
public final class SensorPayload {

  /** Message path used by both modules. */
  public static final String PATH = "/bwatch/sensor";

  private static final int MAX_PAYLOAD_CHARS = 128;
  private static final int MAX_HEART_RATE = 300;
  private static final float MAX_ABS_VALUE = 1_000_000f;

  private SensorPayload() {}

  /**
   * Parses a payload received from the watch.
   *
   * @return a validated update, or {@code null} when the payload is malformed
   */
  public static SensorUpdate parse(String payload) {
    if (payload == null) {
      return null;
    }
    String trimmed = payload.trim();
    if (trimmed.isEmpty() || trimmed.length() > MAX_PAYLOAD_CHARS) {
      return null;
    }

    String[] parts = trimmed.split(",", -1);
    String type = parts[0].trim().toUpperCase(Locale.US);
    try {
      switch (type) {
        case "HR":
          if (parts.length != 2) {
            return null;
          }
          return parseHeartRate(parts[1]);
        case "GYRO":
          if (parts.length != 4) {
            return null;
          }
          return SensorUpdate.gyroscope(
            parseCoordinate(parts[1]),
            parseCoordinate(parts[2]),
            parseCoordinate(parts[3])
          );
        case "ACC":
          if (parts.length != 4) {
            return null;
          }
          return SensorUpdate.accelerometer(
            parseCoordinate(parts[1]),
            parseCoordinate(parts[2]),
            parseCoordinate(parts[3])
          );
        case "STOP":
          return parts.length == 1 ? SensorUpdate.stop() : null;
        default:
          return null;
      }
    } catch (NumberFormatException exception) {
      return null;
    }
  }

  private static SensorUpdate parseHeartRate(String raw) throws NumberFormatException {
    int bpm = Integer.parseInt(raw.trim());
    if (bpm < 0 || bpm > MAX_HEART_RATE) {
      return null;
    }
    return SensorUpdate.heartRate(bpm);
  }

  private static float parseCoordinate(String raw) throws NumberFormatException {
    float value = Float.parseFloat(raw.trim());
    if (!Float.isFinite(value) || Math.abs(value) > MAX_ABS_VALUE) {
      throw new NumberFormatException("Sensor value out of range");
    }
    return value;
  }
}
