package com.harry.bwatch.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class SensorPayloadTest {

  @Test
  public void parsesHeartRate() {
    SensorUpdate update = SensorPayload.parse("HR,72");

    assertNotNull(update);
    assertEquals(SensorUpdate.Type.HEART_RATE, update.getType());
    assertEquals(72, update.getHeartRate());
  }

  @Test
  public void parsesThreeAxisSensors() {
    SensorUpdate gyro = SensorPayload.parse("GYRO,1.25,-4.5,0");
    SensorUpdate acc = SensorPayload.parse("ACC,0.1,0.2,9.81");

    assertNotNull(gyro);
    assertEquals(SensorUpdate.Type.GYROSCOPE, gyro.getType());
    assertEquals(1.25f, gyro.getX(), 0.0001f);
    assertEquals(-4.5f, gyro.getY(), 0.0001f);
    assertEquals(0f, gyro.getZ(), 0.0001f);

    assertNotNull(acc);
    assertEquals(SensorUpdate.Type.ACCELEROMETER, acc.getType());
    assertEquals(9.81f, acc.getZ(), 0.0001f);
  }

  @Test
  public void parsesStop() {
    SensorUpdate update = SensorPayload.parse("STOP");

    assertNotNull(update);
    assertEquals(SensorUpdate.Type.STOP, update.getType());
  }

  @Test
  public void acceptsWhitespaceAndLowerCase() {
    SensorUpdate update = SensorPayload.parse("  hr , 72 ");

    assertNotNull(update);
    assertEquals(72, update.getHeartRate());
  }

  @Test
  public void acceptsZeroHeartRateAsNoContact() {
    SensorUpdate update = SensorPayload.parse("HR,0");

    assertNotNull(update);
    assertEquals(0, update.getHeartRate());
  }

  @Test
  public void rejectsMalformedPayloads() {
    String tooLong = "HR," + "7".repeat(200);
    String[] payloads = {
      null,
      "",
      "   ",
      "HR",
      "HR,72,1",
      "HR,abc",
      "HR,-1",
      "HR,301",
      "HR,72.5",
      "GYRO,1,2",
      "GYRO,1,2,3,4",
      "GYRO,NaN,1,2",
      "GYRO,Infinity,1,2",
      "ACC,1,2,1000001",
      "STOP,1",
      "UNKNOWN,1",
      tooLong,
    };

    for (String payload : payloads) {
      assertNull("Expected rejection for: " + payload, SensorPayload.parse(payload));
    }
  }
}
