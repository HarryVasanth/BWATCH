package com.harry.bwatch.data;

import static org.junit.Assert.assertEquals;

import java.time.Instant;
import java.util.Locale;
import java.util.TimeZone;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class CsvRowTest {

  private Locale previousLocale;
  private TimeZone previousTimeZone;

  @Before
  public void setUp() {
    previousLocale = Locale.getDefault();
    previousTimeZone = TimeZone.getDefault();
    Locale.setDefault(Locale.GERMANY);
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
  }

  @After
  public void tearDown() {
    Locale.setDefault(previousLocale);
    TimeZone.setDefault(previousTimeZone);
  }

  @Test
  public void formatsExpectedColumns() {
    SensorSnapshot snapshot = SensorSnapshot.EMPTY
      .withHeartRate(72, 0L)
      .withGyroscope(1.25f, -4.5f, 0f, 0L)
      .withAccelerometer(0.1f, 0.2f, 9.81f, 0L);
    long epochMillis = Instant.parse("2026-11-20T00:00:00Z").toEpochMilli();

    String row = CsvRow.format(epochMillis, snapshot);

    assertEquals(
      "2026-11-20,00:00:00.000,72,1.25,-4.50,0.00,0.10,0.20,9.81",
      row
    );
    assertEquals(9, row.split(",", -1).length);
  }

  @Test
  public void neverEmitsLocaleDependentDecimalSeparators() {
    SensorSnapshot snapshot = SensorSnapshot.EMPTY.withGyroscope(
      1.5f,
      2.5f,
      3.5f,
      0L
    );

    String row = CsvRow.format(0L, snapshot);

    assertEquals(true, row.contains("1.50,2.50,3.50"));
  }

  @Test
  public void replacesNonFiniteValues() {
    SensorSnapshot snapshot = SensorSnapshot.EMPTY.withGyroscope(
      Float.NaN,
      Float.POSITIVE_INFINITY,
      0f,
      0L
    );

    String row = CsvRow.format(0L, snapshot);

    assertEquals(true, row.endsWith(",0.00,0.00,0.00,0.00,0.00,0.00"));
  }
}
