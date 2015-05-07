package com.javray.cordova.plugin;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.content.BroadcastReceiver;

import android.net.ConnectivityManager;
import android.net.NetworkInfo.State;

import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;

import android.util.Log;
import java.util.Iterator;

import java.util.Set;

public class SIPReceiver extends BroadcastReceiver {
  @Override
  public void onReceive(Context context, Intent intent) {

      Log.d("SIP", "Llamada recibida");
      dumpIntent(intent);

      ConnectivityManager conMan = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

      //State mobile = conMan.getNetworkInfo(0).getState();

      State wifi = conMan.getNetworkInfo(1).getState();

      if (wifi == State.CONNECTED || wifi == State.CONNECTING) {

        Log.d("SIP", "WIFI");

        /*
        SipAudioCall incomingCall = null;

        try {

          SipAudioCall.Listener = Listener = new SipAudioCall.Listener() {
            @Override
            public void onRinging(SipAudioCall call, SipProfile caller) {
              caller
            }
          };

          call = m
        }
        catch (Exception e) {
        }

        */
        PowerManager pm = (PowerManager)context.getSystemService(Context.POWER_SERVICE);
        WakeLock wakeLock = pm.newWakeLock((PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP), "TAG");
        wakeLock.acquire();

        KeyguardManager keyguardManager = (KeyguardManager)context.getSystemService(Context.KEYGUARD_SERVICE); 
        KeyguardLock keyguardLock =  keyguardManager.newKeyguardLock("TAG");
        keyguardLock.disableKeyguard();

        intent = new Intent();
        intent.setAction("com.javray.cordova.plugin.SIP.INCOMING_CALL");
        intent.setPackage(context.getPackageName());
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
      }
  }

  public static void dumpIntent(Intent i){

    Bundle bundle = i.getExtras();
    if (bundle != null) {
        Set<String> keys = bundle.keySet();
        Iterator<String> it = keys.iterator();
        while (it.hasNext()) {
            String key = it.next();
            Log.e("SIP","[" + key + "=" + bundle.get(key)+"]");
        }
    }
}
}
