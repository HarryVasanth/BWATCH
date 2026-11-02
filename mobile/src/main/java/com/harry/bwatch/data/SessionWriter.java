package com.harry.bwatch.data;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Appends samples to a session CSV.
 *
 * <p>The writer is kept open for the lifetime of a recording, flushed after
 * every row, and closed by the owning service. Flushing costs little at the
 * one-sample-per-second rate and keeps data safe if the process is killed.
 */
public final class SessionWriter implements Closeable {

  private final File file;
  private final BufferedWriter writer;

  public SessionWriter(File directory, String fileName) throws IOException {
    if (directory == null || fileName == null || fileName.isEmpty()) {
      throw new IOException("Session directory and file name are required");
    }
    if (!directory.exists() && !directory.mkdirs() && !directory.exists()) {
      throw new IOException("Unable to create session directory: " + directory);
    }

    file = new File(directory, fileName);
    boolean needsHeader = !file.exists() || file.length() == 0L;
    writer = new BufferedWriter(new FileWriter(file, true), 8192);
    if (needsHeader) {
      writer.write(CsvRow.HEADER);
      writer.newLine();
      writer.flush();
    }
  }

  public File file() {
    return file;
  }

  public synchronized void append(long epochMillis, SensorSnapshot snapshot)
    throws IOException {
    writer.write(CsvRow.format(epochMillis, snapshot));
    writer.newLine();
    writer.flush();
  }

  @Override
  public synchronized void close() throws IOException {
    writer.flush();
    writer.close();
  }
}
