package com.harry.bwatch;

import android.content.Context;
import android.util.Log;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Sends protocol payloads to every connected node.
 *
 * <p>Node lookup is cached and retried at most once every five seconds; sensor
 * events can arrive faster than a node query completes.
 */
final class MessageSender {

  private static final String TAG = "MessageSender";
  private static final long REFRESH_INTERVAL_MS = 5_000L;

  private final Context context;
  private final AtomicBoolean refreshing = new AtomicBoolean(false);
  private volatile List<Node> nodes = Collections.emptyList();
  private volatile long lastRefreshMs = 0L;

  MessageSender(Context context) {
    this.context = context.getApplicationContext();
  }

  void refresh() {
    long now = System.currentTimeMillis();
    if (now - lastRefreshMs < REFRESH_INTERVAL_MS) {
      return;
    }
    if (!refreshing.compareAndSet(false, true)) {
      return;
    }
    lastRefreshMs = now;

    Wearable.getNodeClient(context)
      .getConnectedNodes()
      .addOnSuccessListener(result -> {
        List<Node> connected = new ArrayList<>(result);
        nodes = Collections.unmodifiableList(connected);
        if (connected.isEmpty()) {
          Log.w(TAG, "No connected nodes; waiting for the phone");
        }
      })
      .addOnFailureListener(error ->
        Log.w(TAG, "Unable to list connected nodes", error)
      )
      .addOnCompleteListener(task -> refreshing.set(false));
  }

  void send(String payload) {
    List<Node> targets = nodes;
    if (targets.isEmpty()) {
      refresh();
      return;
    }

    byte[] bytes = payload.getBytes(StandardCharsets.UTF_8);
    for (Node node : targets) {
      Wearable.getMessageClient(context)
        .sendMessage(node.getId(), WearProtocol.PATH, bytes)
        .addOnFailureListener(error -> {
          Log.w(TAG, "Message delivery failed; re-resolving nodes", error);
          nodes = Collections.emptyList();
          lastRefreshMs = 0L;
        });
    }
  }
}
