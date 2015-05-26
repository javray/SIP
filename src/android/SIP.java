package com.javray.cordova.plugin;

import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.util.Iterator;
import java.util.Set;

import android.os.Bundle;
import android.net.Uri;

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
import android.net.sip.SipSession;
import android.net.sip.SipException;
import android.net.sip.SipRegistrationListener;

import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import static android.telephony.PhoneStateListener.*;

import android.content.BroadcastReceiver;

import android.util.Log;

import com.javray.cordova.plugin.SIPReceiver;

public class SIP extends CordovaPlugin {

  private Context mContext;
  private boolean mRingbackToneEnabled = true;
  private boolean mRingtoneEnabled = true;
  private ToneGenerator mRingbackTone;

  private SipManager mSipManager = null;
  private SipProfile mSipProfile = null;
  private SipAudioCall call = null;

  private CordovaWebView appView = null;

  public static TelephonyManager telephonyManager = null;

  private Intent incommingCallIntent = null;
  private PendingIntent pendingCallIntent = null;
  private Ringtone mRingtone = null;

  /*
  public class IncomingCallReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        //SipAudioCall incomingCall = null;

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
            call = mSipManager.takeAudioCall(intent, listener);
            call.answerCall(30);
            call.startAudio();
            call.setSpeakerMode(true);
            if(incomingCall.isMuted()) {
                incomingCall.toggleMute();
            }
            appView.sendJavascript("cordova.fireWindowEvent('incommingCall', {})");
        } catch (Exception e) {
            if (call != null) {
                call.close();
            }
        }
    }
  }
            */

  public SIPReceiver callReceiver = null;

  public SIP() {
    Log.d("SIP", "constructor");
  }

  public void initialize(CordovaInterface cordova, CordovaWebView webView) {

    Log.d("SIP", "initialize");

    super.initialize(cordova, webView);

    appView = webView;

    telephonyManager = (TelephonyManager) cordova.getActivity().getSystemService(Context.TELEPHONY_SERVICE);

    telephonyManager.listen(phoneStateListener, LISTEN_CALL_STATE);

    Intent intent = cordova.getActivity().getIntent();

    dumpIntent(intent);

    if (intent.getAction().equals("com.javray.cordova.plugin.SIP.INCOMING_CALL")) {
      appView.sendJavascript("cordova.fireWindowEvent('incommingCall', {})");
    }

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

  private SipAudioCall.Listener listener = new SipAudioCall.Listener() {

    @Override
    public void onError(SipAudioCall call, int code, String message) {
      appView.sendJavascript("cordova.fireWindowEvent('error', {'code':" + Integer.toString(code) + ",'message': '" + message + "'})");
    }

    @Override
    public void onChanged(SipAudioCall call) {
      appView.sendJavascript("cordova.fireWindowEvent('change', {'state':" + Integer.toString(call.getState()) + "})");
    }

    @Override
    public void onCallEstablished(SipAudioCall call) {
        stopRingbackTone();
        call.startAudio();
        appView.sendJavascript("cordova.fireWindowEvent('callEstablished', {})");
    }

    @Override
    public void onRingingBack(SipAudioCall call) {
      startRingbackTone();
      appView.sendJavascript("cordova.fireWindowEvent('ringingBack', {})");
    }

    @Override
    public void onCallBusy(SipAudioCall call) {
      Log.d("SIP", "onCallBusy - call");
      Log.d("SIP", Integer.toString(call.getState()));
      setSpeakerMode();
      appView.sendJavascript("cordova.fireWindowEvent('callBusy', {})");
    }

    @Override
    public void onCallEnded(SipAudioCall call) {
      Log.d("SIP", "onCallEnded- call");
      setSpeakerMode();
      appView.sendJavascript("cordova.fireWindowEvent('callEnd', {})");
    }

    @Override
    public void onCallHeld(SipAudioCall call) {
      appView.sendJavascript("cordova.fireWindowEvent('callHold', {})");
    }

    @Override
    public void onRinging(SipAudioCall call, SipProfile caller) {
      appView.sendJavascript("cordova.fireWindowEvent('ringing', {})");
    }
  };

  private SipSession.Listener sessionListener = new SipSession.Listener() {

    @Override
    public void onError(SipSession session, int code, String message) {
      appView.sendJavascript("cordova.fireWindowEvent('error', {'code':" + Integer.toString(code) + ",'message': '" + message + " - EO'})");
    }

    @Override
    public void onCallBusy(SipSession session) {
      Log.d("SIP", "onCallBusy - session");
      appView.sendJavascript("cordova.fireWindowEvent('callBusy', {})");
    }

    @Override
    public void onCallEstablished(SipSession session, String description) {
      Log.d("SIP", "onCallEstablished");
        appView.sendJavascript("cordova.fireWindowEvent('callEstablished', {})");
    }

    @Override
    public void onRingingBack(SipSession session) {
      Log.d("SIP", "onRingingBack");
      appView.sendJavascript("cordova.fireWindowEvent('ringingBack', {})");
    }

    @Override
    public void onCallEnded(SipSession session) {
      Log.d("SIP", "onCallEnded - session");
      appView.sendJavascript("cordova.fireWindowEvent('callEnd', {})");
    }

    @Override
    public void onRinging(SipSession session, SipProfile caller, String description) {
      Log.d("SIP", "onRinging");
      appView.sendJavascript("cordova.fireWindowEvent('ringing', {})");
    }

  };

  private void connectSip(final String tel, final String user, final String pass, final String domain, final CallbackContext callbackContext) {

    cordova.getThreadPool().execute(new Runnable() {
      public void run() {
        mContext = cordova.getActivity();

        mSipManager = SipManager.newInstance(mContext);

        if (mSipManager.isVoipSupported(mContext)) {

          try {

            SipProfile.Builder builder = new SipProfile.Builder(user, domain);

            Log.d("SIP", "Tel:" + tel);

            builder.setPassword(pass);
            builder.setOutboundProxy(domain);
            builder.setDisplayName(tel);
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
    });
  }

  private void listenSIP() {

    cordova.getThreadPool().execute(new Runnable() {
      public void run() {
        Intent intent = new Intent(); 
        intent.setAction("com.javray.cordova.plugin.SIP.INCOMING_CALL"); 
        pendingCallIntent = PendingIntent.getBroadcast(cordova.getActivity(), 0, intent, Intent.FILL_IN_DATA); 
        try {
          mSipManager.open(mSipProfile, pendingCallIntent, null);
        }
        catch (SipException e) {
          Log.d("SIP", "Cant open SIP Manager form incomming calls");
        }
      }
    });
  }

  private void stopListenSIP() {
    cordova.getThreadPool().execute(new Runnable() {
      public void run() {
        if (pendingCallIntent != null) {
          pendingCallIntent.cancel();
          pendingCallIntent = null;
        }
      }
    });
  }

  private void disconnectSip(final CallbackContext callbackContext) {
    cordova.getThreadPool().execute(new Runnable() {
      public void run() {
        if (call != null) {
            call.close();
        }
        if (mSipManager != null) {
          try {
            if (mSipProfile != null) {
                stopListenSIP();
                mSipManager.unregister(mSipProfile, null);
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
    });
  }

  private void isConnected(final CallbackContext callbackContext) {
    cordova.getThreadPool().execute(new Runnable() {
      public void run() {
      if (mSipManager != null) {
          callbackContext.success("OK");
        }
        else {
          callbackContext.success("KO");
        }
      }
    });
  }

  private void callSip(final String number, final CallbackContext callbackContext) {

    cordova.getThreadPool().execute(new Runnable() {
      public void run() {
        if (call == null) {
          try {
            SipSession session = mSipManager.createSipSession(mSipProfile, sessionListener);
            SipProfile.Builder builder = new SipProfile.Builder("sip:" + number + "@" + mSipProfile.getSipDomain() + ";user=phone");

            SipProfile peerProfile = builder.build();

            call = new SipAudioCall(mContext, mSipProfile);
            call.setListener(listener);
            call.makeCall(peerProfile, session, 10);
            //call = mSipManager.makeAudioCall(mSipProfile.getUriString(), "sip:" + number + "@" + mSipProfile.getSipDomain() + ";user=phone", listener, 30);
            callbackContext.success("Llamada enviada");
          }
          catch (Exception e) {
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
    });
  }

  private void incommingCallSip(final CallbackContext callbackContext) {

    cordova.getThreadPool().execute(new Runnable() {
      public void run() {

        Log.d("SIP", "incommingCallSip");

        Intent intent;

        if (incommingCallIntent != null) {
          intent = incommingCallIntent;
        }
        else {
          intent = cordova.getActivity().getIntent();
        }

        dumpIntent(intent);

        call = null;

        try {
          call = mSipManager.takeAudioCall(intent, listener);

          SipProfile peer = call.getPeerProfile();

          StartRingtone();
          JSONObject json = new JSONObject();

          json.put("displayName", "Desconocido");
          json.put("user", "Desconocido");

          if (peer != null) {

            Log.d("SIP", peer.getUriString());
            Log.d("SIP", peer.getUserName());

            json.put("displayName", peer.getDisplayName());
            json.put("user", peer.getUserName());
          }

          callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, json));
        }
        catch (Exception e) {
          Log.d("SIP", e.toString());
          callbackContext.error("Error al coger la llamada");
        }
      }
    });
  }

  private void incommingCallAnswerSip(final CallbackContext callbackContext) {
    cordova.getThreadPool().execute(new Runnable() {
      public void run() {

        Log.d("SIP", "incommingCallAnswerSip");

        try {
            call.answerCall(10);
            StopRingtone();
        } catch (Exception e) {
            callbackContext.error("Error al contestar la llamada " + e.toString());
        }
        callbackContext.success("Llamada contestada");
      }
    });
  }

  private void callSipEnd(final CallbackContext callbackContext) {

    cordova.getThreadPool().execute(new Runnable() {

      public void run() {

        stopRingbackTone();
        StopRingtone();
        setSpeakerMode();

        Log.d("SIP", "callSipEnd");

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
    });
  }

  private void setInCallMode() {
    cordova.getThreadPool().execute(new Runnable() {
      public void run() {
        AudioManager am =  ((AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE));
        am.setMode(AudioManager.MODE_IN_COMMUNICATION);
        Log.d("SIP", "Speaker: " + am.isSpeakerphoneOn());
        if (am.isSpeakerphoneOn()) {
          am.setSpeakerphoneOn(false);
        }
        Log.d("SIP", "Speaker: " + am.isSpeakerphoneOn());
      }
    });
  }

  private void setSpeakerMode() {
    cordova.getThreadPool().execute(new Runnable() {
      public void run() {
        Log.d("SIP", "setSpeakerMode");
        AudioManager am = ((AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE));
        am.setMode(AudioManager.MODE_NORMAL);
        am.setSpeakerphoneOn(true);
      }
    });
  }

  private void muteMicrophone(Boolean state) {
    Log.d("SIP", "muteMicrophone: " + state.toString());
    AudioManager am = ((AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE));
    am.setMicrophoneMute(state);
  }

  private void sendDtmf(final int code) {
    cordova.getThreadPool().execute(new Runnable() {
      public void run() {
        Log.d("SIP", "sendDtmf: " + code);
        if (call != null) {
          call.sendDtmf(code);
        }
      }
    });
  }
  private void StartRingtone() {
      Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
      mRingtone = RingtoneManager.getRingtone(mContext, notification);
      mRingtone.play();
  }

  private void StopRingtone() {

      if (mRingtone != null) {
        mRingtone.stop();
        mRingtone = null;
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

  public static void dumpIntent(Intent i){

    Log.d("SIP", i.getAction());
    Log.d("SIP", Integer.toString(i.getFlags()));
    Uri uri = i.getData();
    if (uri != null) {
      Log.d("SIP", uri.toString());
    }
    else {
      Log.d("SIP", "data null");
    }

    Bundle bundle = i.getExtras();
    if (bundle != null) {
        Set<String> keys = bundle.keySet();
        Iterator<String> it = keys.iterator();
        while (it.hasNext()) {
            String key = it.next();
            Log.d("SIP","[" + key + "=" + bundle.get(key)+"]");
        }
    }
  }

  @Override
  public void onNewIntent(final Intent intent) {

    cordova.getThreadPool().execute(new Runnable() {
      public void run() {
        Log.d("SIP", "onNewIntent");

        dumpIntent(intent);

        if (intent.getAction().equals("com.javray.cordova.plugin.SIP.INCOMING_CALL")) {
          incommingCallIntent = intent;
          appView.sendJavascript("cordova.fireWindowEvent('incommingCall', {})");
        }
        else {
          incommingCallIntent = null;
        }
      }
    });
  }

  @Override
  public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

      if (action.equals("connect")) {
          String tel = args.getString(0);
          String user = args.getString(1);
          String pass = args.getString(2);
          String domain = args.getString(3);

          this.connectSip(tel, user, pass, domain, callbackContext);
          return true;
      }
      else if (action.equals("makecall")) {
          String number = args.getString(0);

          this.callSip(number, callbackContext);
          return true;
      }
      else if (action.equals("endcall")) {
          this.callSipEnd(callbackContext);
          return true;
      }
      else if (action.equals("disconnect")) {
          this.disconnectSip(callbackContext);
          return true;
      }
      else if (action.equals("isconnected")) {
          this.isConnected(callbackContext);
          return true;
      }
      else if (action.equals("mutecall")) {
          String estado = args.getString(0);
          this.muteMicrophone(estado.equals("on"));
          return true;
      }
      else if (action.equals("speakercall")) {
          String estado = args.getString(0);
          if (estado.equals("on")) {
            this.setSpeakerMode();
          }
          else {
            this.setInCallMode();
          }
          return true;
      }
      else if (action.equals("dtmfcall")) {
          int code = args.getInt(0);
          this.sendDtmf(code);
          return true;
      }
      else if (action.equals("listen")) {
          this.listenSIP();
          return true;
      }
      else if (action.equals("stoplisten")) {
          this.stopListenSIP();
          return true;
      }
      else if (action.equals("incommingcall")) {
          this.incommingCallSip(callbackContext);
          return true;
      }
      else if (action.equals("answercall")) {
          this.incommingCallAnswerSip(callbackContext);
          return true;
      }

      return false;
  }
}

