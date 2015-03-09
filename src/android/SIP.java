package com.javray.cordova.plugin;

import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
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

    private CordovaWebView appView = null;


    public SIP() {
    }

    public void initialize(CordovaInterface cordova, CordovaWebView webView) {

      super.initialize(cordova, webView);

      appView = webView;
    }

    private void connectSip(String user, String pass, String domain, CallbackContext callbackContext) {

      mContext = cordova.getActivity();

      mSipManager = SipManager.newInstance(mContext);

      if (mSipManager.isVoipSupported(mContext)) {

        try {

          SipProfile.Builder builder = new SipProfile.Builder(user, domain);

          builder.setPassword(pass);
          builder.setOutboundProxy(domain);
          mSipProfile = builder.build();

          if (mSipManager.isOpened(mSipProfile.getUriString())) {

            callbackContext.success("El perfil SIP ya está abierto");
          }
          else {

            mSipManager.open(mSipProfile);

            callbackContext.success("Perfil configurado");
          }
        }
        catch (Exception e) {
          callbackContext.error("Perfil no configurado" + e.toString());
        }
      }
      else {
        callbackContext.error("SIP no soportado");
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

      final CordovaWebView av = appView;

      if (call == null) {
        try {
          SipAudioCall.Listener listener = new SipAudioCall.Listener() {

            @Override
            public void onCallEstablished(SipAudioCall call) {
                stopRingbackTone();
                call.startAudio();
                av.sendJavascript("cordova.fireWindowEvent('callEstablished', {})");
            }

            @Override
            public void onRingingBack(SipAudioCall call) {
              startRingbackTone();
              av.sendJavascript("cordova.fireWindowEvent('ringingBack', {})");
            }

            @Override
            public void onCallEnded(SipAudioCall call) {
              setSpeakerMode();
              av.sendJavascript("cordova.fireWindowEvent('callEnd', {})");
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
      setSpeakerMode();

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
      am.setMode(AudioManager.MODE_IN_COMMUNICATION);
      Log.d("SIP", "Speaker: " + am.isSpeakerphoneOn());
      if (am.isSpeakerphoneOn()) {
        am.setSpeakerphoneOn(false);
      }
      Log.d("SIP", "Speaker: " + am.isSpeakerphoneOn());
    }

    private void setSpeakerMode() {
      Log.d("SIP", "setSpeakerMode");
      AudioManager am = ((AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE));
      am.setMode(AudioManager.MODE_NORMAL);
      am.setSpeakerphoneOn(true);
    }

    private void muteMicrophone(Boolean state) {
      Log.d("SIP", "muteMicrophone: " + state.toString());
      AudioManager am = ((AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE));
      am.setMicrophoneMute(state);
    }

    private void sendDmtf(int code) {
      Log.d("SIP", "sendDmtf: " + code);
      if (call != null) {
        call.sendDmtf(code);
      }
    }

    private synchronized void startRingbackTone() {
        setInCallMode();
        if (mRingbackTone == null) {
            // The volume relative to other sounds in the stream
            int toneVolume = 80;
            mRingbackTone = new ToneGenerator(
                    AudioManager.STREAM_MUSIC, toneVolume);
        }
        if (mRingbackTone.startTone(ToneGenerator.TONE_SUP_RINGTONE)) {
          Log.d("SIP", "Tono iniciado");
        }
        else {
          Log.d("SIP", "Tono no iniciado");
        }

    }

    private synchronized void stopRingbackTone() {
        if (mRingbackTone != null) {
            mRingbackTone.stopTone();
            mRingbackTone.release();
            mRingbackTone = null;
        }
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

        if (action.equals("connect")) {
            String user = args.getString(0);
            String pass = args.getString(1);
            String domain = args.getString(2);

            this.connectSip(user, pass, domain, callbackContext);
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
        else if (action.equals("mutecall")) {
            String estado = args.getString(0);
            this.muteMicrophone(estado.equals("on"));
        }
        else if (action.equals("speakercall")) {
            String estado = args.getString(0);
            if (estado.equals("on")) {
              this.setSpeakerMode();
            }
            else {
              this.setInCallMode();
            }
        }
        else if (action.equals("dtmfcall")) {
            int code = args.getInt(0);
        }

        return false;
    }
}

