package com.harry.bwatch.data;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/** Formats one CSV row. All numbers use {@link Locale#US} so the column separator is stable. */
public final class CsvRow {

  public static final String HEADER =
    "date,time,hr,gyro_x,gyro_y,gyro_z,acc_x,acc_y,acc_z";

  private CsvRow() {}

  public static String format(long epochMillis, SensorSnapshot snapshot) {
    SimpleDateFormat timestamp = new SimpleDateFormat(
      "yyyy-MM-dd,HH:mm:ss.SSS",
      Locale.US
    );
    timestamp.setTimeZone(TimeZone.getDefault());

    return (
      timestamp.format(new Date(epochMillis)) +
      "," +
      snapshot.getHeartRate() +
      "," +
      decimal(snapshot.getGyroX()) +
      "," +
      decimal(snapshot.getGyroY()) +
      "," +
      decimal(snapshot.getGyroZ()) +
      "," +
      decimal(snapshot.getAccX()) +
      "," +
      decimal(snapshot.getAccY()) +
      "," +
      decimal(snapshot.getAccZ())
    );
  }

  private static String decimal(float value) {
    // Non-finite values cannot be represented in the study data format.
    if (!Float.isFinite(value)) {
      return "0.00";
    }
    return String.format(Locale.US, "%.2f", value);
  }
}
