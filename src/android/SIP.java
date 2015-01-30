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

public class SIP extends CordovaPlugin {

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

      mSipManager = SipManager.newInstance(cordova.getActivity());

      try {

        SipProfile.Builder builder = new SipProfile.Builder(user, domain);

        builder.setPassword(pass);
        builder.setOutboundProxy(domain);
        mSipProfile = builder.build();

        mSipManager.open(mSipProfile);

        callbackContext.success("Connected");
      }
      catch (Exception e) {
        callbackContext.error("Not Connected " + e.toString());
      }
    }

    private void callSip(String number, CallbackContext callbackContext) {

      final CallbackContext cc = callbackContext;

      try {
        SipAudioCall.Listener listener = new SipAudioCall.Listener() {

          @Override
          public void onCallEstablished(SipAudioCall call) {
              call.startAudio();
              stopRingbackTone();
              cc.success("Llamada establecida");
          }

          @Override
          public void onRingingBack(SipAudioCall call) {
            startRingbackTone();
          }

          @Override
          public void onCallEnded(SipAudioCall call) {
          }
        };

        call = mSipManager.makeAudioCall(mSipProfile.getUriString(), "sip:" + number + "@" + mSipProfile.getSipDomain() + ";user=phone", listener, 30);
      }
      catch (SipException e) {
        callbackContext.error("error " + e.toString());
        if (call != null) {
          call.close();
        }
      }
    }

    private void callSipEnd(CallbackContext callbackContext) {

      if(call != null) {
          try {
            call.endCall();
          } catch (SipException se) {
            callbackContext.error("Error al finalizar la llamada " + se.toString());
          }
          call.close();
      }
      else {
        callbackContext.error("No hay niguna llamada en curso");
      }
    }

    private void setInCallMode() {
        ((AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE))
                .setMode(AudioManager.MODE_IN_CALL);
    }

    private void setSpeakerMode() {
        ((AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE))
                .setMode(AudioManager.MODE_NORMAL);
    }

    private synchronized void startRingbackTone() {
        if (mRingbackTone == null) {
            // The volume relative to other sounds in the stream
            int toneVolume = 80;
            mRingbackTone = new ToneGenerator(
                    AudioManager.STREAM_MUSIC, toneVolume);
        }
        setInCallMode();
        mRingbackTone.startTone(ToneGenerator.TONE_CDMA_LOW_PBX_L);
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

        return false;
    }
}

