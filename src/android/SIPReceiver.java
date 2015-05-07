package com.javray.cordova.plugin;

import android.content.Context;
import android.content.Intent;
import android.content.BroadcastReceiver;

public class SIPReceiver extends BroadcastReceiver {
  @Override
  public void onReceive(Context context, Intent intent) {
      Log.d("SIP", "Llamada recibida");
  }
}
