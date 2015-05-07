package com.javray.cordova.plugin;

import android.content.Context;
import android.content.Intent;
import android.content.BroadcastReceiver;

import android.net.ConnectivityManager;
import android.net.NetworkInfo.State;

import android.util.Log;

public class SIPReceiver extends BroadcastReceiver {
  @Override
  public void onReceive(Context context, Intent intent) {

      Log.d("SIP", "Llamada recibida");

      ConnectivityManager conMan = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

      //State mobile = conMan.getNetworkInfo(0).getState();

      State wifi = conMan.getNetworkInfo(1).getState();

      if (wifi == State.CONNECTED || wifi == State.CONNECTING) {
        Log.d("SIP", "WIFI");
      }
  }
}
