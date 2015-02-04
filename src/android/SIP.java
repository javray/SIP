package com.javray.cordova.plugin;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;

import android.content.Context;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.media.ToneGenerator;

import android.net.sip.SipManager;
import android.net.sip.SipProfile;
import android.net.sip.SipAudioCall;
import android.net.sip.SipException;
import android.net.sip.SipRegistrationListener;

import android.util.Log;

public class SIP extends CordovaPlugin {

    private Context mContext;
    private boolean mRingbackToneEnabled = true;
    private boolean mRingtoneEnabled = true;
    private Ringtone mRingtone;
    private ToneGenerator mRingbackTone;

    private SipManager mSipManager = null;
    private SipProfile mSipProfile = null;
    private SipAudioCall call = null;


    public SIP() {
    }

    private void connectSip(String user, String pass, String domain, CallbackContext callbackContext) {

      mContext = cordova.getActivity();

      mSipManager = SipManager.newInstance(mContext);

      PluginResult pluginResult = null;

      if (mSipManager.isVoipSupported(mContext)) {

        try {

          SipProfile.Builder builder = new SipProfile.Builder(user, domain);

          builder.setPassword(pass);
          builder.setOutboundProxy(domain);
          mSipProfile = builder.build();

          if (mSipManager.isOpened(mSipProfile.getUriString())) {

            //callbackContext.success("El perfil SIP ya está abierto");
            pluginResult = new PluginResult(PluginResult.Status.OK, "El perfil SIP ya estaba abierto");
            pluginResult.setKeepCallback(true);
            callbackContext.sendPluginResult(pluginResult);
          }
          else {

            mSipManager.open(mSipProfile);

            //callbackContext.success("Perfil configurado");
            pluginResult = new PluginResult(PluginResult.Status.OK, "Perfil configurado");
            pluginResult.setKeepCallback(true);
            callbackContext.sendPluginResult(pluginResult);
          }
        }
        catch (Exception e) {
          //callbackContext.error("Perfil no configurado" + e.toString());
          pluginResult = new PluginResult(PluginResult.Status.Error, "Perfil no configurado" + e.toString());
          pluginResult.setKeepCallback(true);
          callbackContext.sendPluginResult(pluginResult);
        }
      }
      else {
        //callbackContext.error("SIP no soportado");
        pluginResult = new PluginResult(PluginResult.Status.Error, "SIP no soportado" + e.toString());
        pluginResult.setKeepCallback(true);
        callbackContext.sendPluginResult(pluginResult);
      }
    }

    private void disconnectSip(CallbackContext callbackContext) {
      if (call != null) {
          call.close();
      }
      if (mSipManager != null) {
        try {
          if (mSipProfile != null) {
              mSipManager.close(mSipProfile.getUriString());
              mSipProfile = null;
          }
          mSipManager = null;
          callbackContext.success("Perfil cerrado");
        } catch (Exception e) {
          callbackContext.error("Perfil no cerrado " + e.toString());
        }
      }
    }

    private void callSip(String number, CallbackContext callbackContext) {

      if (call == null) {
        try {
          SipAudioCall.Listener listener = new SipAudioCall.Listener() {

            @Override
            public void onCallEstablished(SipAudioCall call) {
                stopRingbackTone();
                call.startAudio();
            }

            @Override
            public void onRingingBack(SipAudioCall call) {
              Log.d("SIP", "onRingingBack");
              startRingbackTone();
            }

            @Override
            public void onCallEnded(SipAudioCall call) {
            }
          };

          call = mSipManager.makeAudioCall(mSipProfile.getUriString(), "sip:" + number + "@" + mSipProfile.getSipDomain() + ";user=phone", listener, 30);

          callbackContext.success("Llamada enviada");
        }
        catch (SipException e) {
          callbackContext.error("error " + e.toString());
          if (call != null) {
            call.close();
          }
        }
      }
      else {
        callbackContext.error("Hay una llamada en curso");
      }
    }

    private void callSipEnd(CallbackContext callbackContext) {

      stopRingbackTone();

      if(call != null) {
          try {
            call.endCall();
          } catch (SipException se) {
            callbackContext.error("Error al finalizar la llamada " + se.toString());
          }
          call.close();
          call = null;
          callbackContext.success("Llamada finalizada");
      }
      else {
        callbackContext.error("No hay niguna llamada en curso");
      }
    }

    private void setInCallMode() {
      AudioManager am =  ((AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE));
      am.setMode(AudioManager.MODE_IN_CALL);
      am.setSpeakerphoneOn(false);
    }

    private void setSpeakerMode() {
      AudioManager am = ((AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE));
      am.setMode(AudioManager.MODE_NORMAL);
      am.setSpeakerphoneOn(true);
    }

    private synchronized void startRingbackTone() {
        if (mRingbackTone == null) {
            // The volume relative to other sounds in the stream
            int toneVolume = 80;
            mRingbackTone = new ToneGenerator(
                    AudioManager.STREAM_MUSIC, toneVolume);
        }
        setInCallMode();
        mRingbackTone.startTone(ToneGenerator.TONE_SUP_RINGTONE);

    }

    private synchronized void stopRingbackTone() {
        if (mRingbackTone != null) {
            mRingbackTone.stopTone();
            setSpeakerMode();
            mRingbackTone.release();
            mRingbackTone = null;
        }
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

        if (action.equals("connect")) {

            final String user = args.getString(0);
            final String pass = args.getString(1);
            final String domain = args.getString(2);
            final CallbackContext cc = callbackContext;

            cordova.getThreadPool().execute(new Runnable() {
              public void run() {
                connectSip(user, pass, domain, cc);
              }
            });
        }
        else if (action.equals("makecall")) {
            String number = args.getString(0);

            this.callSip(number, callbackContext);
        }
        else if (action.equals("endcall")) {
            this.callSipEnd(callbackContext);
        }
        else if (action.equals("disconnect")) {
            this.disconnectSip(callbackContext);
        }

        return false;
    }
}

