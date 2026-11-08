package com.harry.bwatch.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

public class SensorRepositoryTest {

  private SensorRepository repository;

  @Before
  public void setUp() {
    repository = SensorRepository.getInstance();
    repository.clear();
  }

  @Test
  public void mergesIndependentSensorChannels() {
    repository.apply(SensorUpdate.heartRate(70), 1_000L);
    repository.apply(SensorUpdate.gyroscope(1f, 2f, 3f), 2_000L);
    repository.apply(SensorUpdate.accelerometer(4f, 5f, 6f), 3_000L);

    SensorSnapshot snapshot = repository.snapshot();

    assertEquals(70, snapshot.getHeartRate());
    assertEquals(1f, snapshot.getGyroX(), 0.0001f);
    assertEquals(6f, snapshot.getAccZ(), 0.0001f);
    assertTrue(snapshot.isFresh(3_500L, 1_000L));
    assertTrue(snapshot.hasFreshHeartRate(3_500L, 3_000L));
  }

  @Test
  public void heartRateFreshnessIsIndependentOfMotion() {
    repository.apply(SensorUpdate.heartRate(70), 1_000L);
    repository.apply(SensorUpdate.gyroscope(1f, 2f, 3f), 5_000L);

    SensorSnapshot snapshot = repository.snapshot();

    assertTrue(snapshot.isFresh(5_500L, 1_000L));
    assertFalse(snapshot.hasFreshHeartRate(5_500L, 1_000L));
  }

  @Test
  public void stopClearsEveryChannel() {
    repository.apply(SensorUpdate.heartRate(70), 1_000L);
    repository.apply(SensorUpdate.stop(), 2_000L);

    SensorSnapshot snapshot = repository.snapshot();

    assertEquals(0, snapshot.getHeartRate());
    assertFalse(snapshot.isFresh(2_000L, 5_000L));
  }

  @Test
  public void ignoresNullUpdates() {
    repository.apply(null, 1_000L);

    assertFalse(repository.snapshot().isFresh(1_000L, 5_000L));
  }
}
