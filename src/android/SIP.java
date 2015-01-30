package com.javray.cordova.plugin;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;

import android.net.sip.SipManager;
import android.net.sip.SipProfile;
import android.net.sip.SipAudioCall;
import android.net.sip.SipException;
import android.net.sip.SipRegistrationListener;

public class SIP extends CordovaPlugin {

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
              cc.success("Llamada establecida");
          }

          @Override
          public void onCallEnded(SipAudioCall call) {
          }
        };

        call = mSipManager.makeAudioCall(mSipProfile.getUriString(), "sip:" + number + mSipProfile.getSipDomain() + ";user=phone", listener, 30);
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

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

        if (action.equals("connect")) {
            String user = args.getString(0);
            String pass = args.getString(1);
            String domain = args.getString(2);

            this.connectSip(user, pass, domain, callbackContext);
        }
        else if (action.equals("call")) {
            String number = args.getString(0);

            this.callSip(number, callbackContext);
        }
        else if (action.equals("callend")) {
            this.callSipEnd(callbackContext);
        }
        /*
        else if (action.equals("disconnect")) {
          if (this.aConn.isConnected()) {
            try {
              this.aConn.disconnect();
            }
            catch (IOException ex) {
              callbackContext.error("Can't Disconnect: " + ex.toString());
              return false;
            }
            callbackContext.success("Done");
          }
          else {
            callbackContext.error("Not Connected");
          }
        }
        else if (action.equals("login")) {
          if (this.aConn.isConnected()) {
            String name = args.getString(0);
            char[] passwod = args.getString(1).toCharArray();
            String res = this.aConn.login(name, passwod);
            callbackContext.success(res);
          }
          else {
            callbackContext.error("Not Connected");
          }
        }
        else if (action.equals("command")) {

          if (this.aConn.isConnected()) {

            String command = args.getString(0);
            String res = this.aConn.sendCommand(command);
            String result = "";

            if (res.equals("Sent successfully")) {
              res = "";
              while (true) {
                  try {
                      res = this.aConn.getData();
                      if (res != null) {
                          result += res;
                          if (res.contains("!done")) {
                            break;
                          }
                      }
                  } catch (InterruptedException ex) {
                    callbackContext.error("No Data: " + ex.toString());
                  }
              }
              callbackContext.success(result);
            }
            else {
              callbackContext.error(res);
            }
          }
          else {
            callbackContext.error("Not Connected");
          }
        }
        */

        return false;
    }
}

