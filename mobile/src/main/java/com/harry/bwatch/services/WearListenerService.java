package com.harry.bwatch.services;

import android.util.Log;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;
import com.harry.bwatch.data.SensorPayload;
import com.harry.bwatch.data.SensorRepository;
import com.harry.bwatch.data.SensorUpdate;
import java.nio.charset.StandardCharsets;

/**
 * Receives sensor messages from the watch and republishes them in the process
 * wide {@link SensorRepository}.
 */
public class WearListenerService extends WearableListenerService {

  private static final String TAG = "WearListenerService";

  @Override
  public void onMessageReceived(MessageEvent messageEvent) {
    if (messageEvent == null || !SensorPayload.PATH.equals(messageEvent.getPath())) {
      return;
    }

    byte[] data = messageEvent.getData();
    String payload = data == null
      ? null
      : new String(data, StandardCharsets.UTF_8);
    SensorUpdate update = SensorPayload.parse(payload);
    if (update == null) {
      Log.w(TAG, "Ignoring malformed sensor payload");
      return;
    }

    SensorRepository.getInstance().apply(update, System.currentTimeMillis());
  }
}
