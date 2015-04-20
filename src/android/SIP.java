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
import android.content.Intent;
import android.content.IntentFilter;

import android.app.PendingIntent;

import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.media.ToneGenerator;

import android.net.sip.SipManager;
import android.net.sip.SipProfile;
import android.net.sip.SipAudioCall;
import android.net.sip.SipException;
import android.net.sip.SipRegistrationListener;

import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import static android.telephony.PhoneStateListener.*;

import android.content.BroadcastReceiver;

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

  public static TelephonyManager telephonyManager = null;

  public class IncomingCallReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        SipAudioCall incomingCall = null;

        Log.d("SIP", "Llamada recibida");
        try {
            SipAudioCall.Listener listener = new SipAudioCall.Listener() {
                @Override
                public void onRinging(SipAudioCall call, SipProfile caller) {
                    try {
                        call.answerCall(30);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            };
            incomingCall = mSipManager.takeAudioCall(intent, listener);
            incomingCall.answerCall(30);
            incomingCall.startAudio();
            incomingCall.setSpeakerMode(true);
            if(incomingCall.isMuted()) {
                incomingCall.toggleMute();
            }
            appView.sendJavascript("cordova.fireWindowEvent('incommingCall', {})");
        } catch (Exception e) {
            if (incomingCall != null) {
                incomingCall.close();
            }
        }
    }
  }

  public IncomingCallReceiver callReceiver = null;

  public SIP() {
  }

  public void initialize(CordovaInterface cordova, CordovaWebView webView) {

    super.initialize(cordova, webView);

    appView = webView;

    telephonyManager = (TelephonyManager) cordova.getActivity().getSystemService(Context.TELEPHONY_SERVICE);

    telephonyManager.listen(phoneStateListener, LISTEN_CALL_STATE);

  }

  private PhoneStateListener phoneStateListener = new PhoneStateListener() {
      @Override
      public void onCallStateChanged (int state, String incomingNumber)
      {

          Log.d("SIP", Integer.toString(state));

          switch (state) {
          case TelephonyManager.CALL_STATE_IDLE:
              if (call != null) {
                try {
                  call.continueCall(0);
                }
                catch (SipException e) {
                  Log.d("SIP", "Cant continue sipcall");
                }
              }
              break;
          case TelephonyManager.CALL_STATE_RINGING:
              Log.d("SIP", "CALL_STATE_RINGING");
              break;
          case TelephonyManager.CALL_STATE_OFFHOOK:
              if (call != null) {
                try {
                  call.holdCall(0);
                }
                catch (SipException e) {
                  Log.d("SIP", "Cant hold sipcall");
                }
              }
              break;
          }
      }
  };

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

  private void listenSIP() {

    IntentFilter filter = new IntentFilter();
    filter.addAction("es.sarenet.INCOMING_CALL");
    callReceiver = new IncomingCallReceiver();
    cordova.getActivity().registerReceiver(callReceiver, filter);

    Intent intent = new Intent(); 
    intent.setAction("es.sarenet.INCOMING_CALL"); 
    PendingIntent pendingIntent = PendingIntent.getBroadcast(cordova.getActivity(), 0, intent, Intent.FILL_IN_DATA); 
    try {
      mSipManager.open(mSipProfile, pendingIntent, null);
    }
    catch (SipException e) {
      Log.d("SIP", "Cant open SIP Manager form incomming calls");
    }
  }

  private void stopListenSIP() {
    if (callReceiver != null) {
      cordova.getActivity().unregisterReceiver(callReceiver);
      callReceiver = null;
    }
  }

  private void disconnectSip(CallbackContext callbackContext) {
    if (call != null) {
        call.close();
    }
    if (mSipManager != null) {
      try {
        if (mSipProfile != null) {
            this.stopListenSIP();
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

          @Override
          public void onCallHeld(SipAudioCall call) {
            av.sendJavascript("cordova.fireWindowEvent('callHold', {})");
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

  private void sendDtmf(int code) {
    Log.d("SIP", "sendDtmf: " + code);
    if (call != null) {
      call.sendDtmf(code);
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
          this.sendDtmf(code);
      }
      else if (action.equals("listen")) {
          this.listenSIP();
      }
      else if (action.equals("stoplisten")) {
          this.stopListenSIP();
      }

      return false;
  }
}

