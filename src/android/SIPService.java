package com.javray.cordova.plugin;

import android.content.Context;

import org.doubango.ngn.NgnApplication;
import org.doubango.ngn.NgnEngine;
import org.doubango.ngn.services.INgnConfigurationService;
import org.doubango.ngn.utils.NgnConfigurationEntry;

public class SIPService extends NgnApplication {

  private NgnEngine mEngine = null;
  private Context mContext = null;

  public SIPService(Context ctx) {
    mContext = ctx;
  }

  @Override
  public static Context getContext() {
    return mContext;
  }

  public void connectSip(String user, String pass, String domain) {

    mEngine = NgnEngine.getInstance();

    INgnConfigurationService mConfigurationService = mEngine.getConfigurationService();

    mConfigurationService.putString(NgnConfigurationEntry.IDENTITY_IMPI, user);
    mConfigurationService.putString(NgnConfigurationEntry.IDENTITY_IMPU, String.format("sip:%s@%s", user, domain));
    mConfigurationService.putString(NgnConfigurationEntry.IDENTITY_PASSWORD, pass);
    mConfigurationService.putString(NgnConfigurationEntry.NETWORK_PCSCF_HOST, domain);
    mConfigurationService.putInt(NgnConfigurationEntry.NETWORK_PCSCF_PORT, NgnConfigurationEntry.DEFAULT_NETWORK_PCSCF_PORT);
    mConfigurationService.putString(NgnConfigurationEntry.NETWORK_REALM, domain);
    mConfigurationService.putBoolean(NgnConfigurationEntry.NETWORK_USE_3G, true);
    mConfigurationService.putInt(NgnConfigurationEntry.NETWORK_REGISTRATION_TIMEOUT, 3600);

    mConfigurationService.commit();
  }
}
