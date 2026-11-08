package com.harry.bwatch.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class SessionWriterTest {

  @Rule
  public final TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test
  public void writesHeaderOnceAndAppendsRows() throws IOException {
    File directory = temporaryFolder.newFolder("nested");
    SensorSnapshot snapshot = SensorSnapshot.EMPTY.withHeartRate(72, 0L);

    try (SessionWriter writer = new SessionWriter(directory, "session.csv")) {
      writer.append(0L, snapshot);
      writer.append(1_000L, snapshot);
    }

    List<String> lines = Files.readAllLines(
      new File(directory, "session.csv").toPath()
    );
    assertEquals(3, lines.size());
    assertEquals(CsvRow.HEADER, lines.get(0));
    assertTrue(lines.get(1).startsWith("1970-01-01,"));
  }

  @Test
  public void reopeningAnExistingFileDoesNotDuplicateTheHeader()
    throws IOException {
    File directory = temporaryFolder.newFolder("reopen");

    try (SessionWriter writer = new SessionWriter(directory, "session.csv")) {
      writer.append(0L, SensorSnapshot.EMPTY);
    }
    try (SessionWriter writer = new SessionWriter(directory, "session.csv")) {
      writer.append(1_000L, SensorSnapshot.EMPTY);
    }

    List<String> lines = Files.readAllLines(
      new File(directory, "session.csv").toPath()
    );
    assertEquals(3, lines.size());
    assertEquals(CsvRow.HEADER, lines.get(0));
    assertEquals(1, lines.stream().filter(CsvRow.HEADER::equals).count());
  }

  @Test
  public void rejectsMissingArguments() {
    try {
      new SessionWriter(null, "session.csv");
      fail("Expected IOException for a null directory");
    } catch (IOException expected) {
      // expected
    }

    try {
      new SessionWriter(temporaryFolder.getRoot(), "");
      fail("Expected IOException for an empty file name");
    } catch (IOException expected) {
      // expected
    }
  }
}
