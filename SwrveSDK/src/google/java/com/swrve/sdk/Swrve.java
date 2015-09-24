package com.swrve.sdk;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.iid.InstanceID;
import com.swrve.sdk.config.SwrveConfig;
import com.swrve.sdk.gcm.ISwrvePushNotificationListener;
import com.swrve.sdk.gcm.SwrveGcmNotification;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;

/**
 * Main implementation of the Google Swrve SDK.
 */
public class Swrve extends SwrveBase<ISwrve, SwrveConfig> implements ISwrve {
    protected static final String REGISTRATION_ID_CATEGORY = "RegistrationId";
    protected static final String SWRVE_GCM_TOKEN = "swrve.gcm_token";

    protected String registrationId;
    protected ISwrvePushNotificationListener pushNotificationListener;

    protected Swrve(Context context, int appId, String apiKey, SwrveConfig config) {
        super();
        if (context instanceof Activity) {
            this.context = new WeakReference<Context>(context.getApplicationContext());
            this.activityContext = new WeakReference<Activity>((Activity) context);
        } else {
            this.context = new WeakReference<Context>(context);
        }
        this.appId = appId;
        this.apiKey = apiKey;
        this.config = config;
    }

    public void onTokenRefreshed() {
        registerInBackground(getContext());
    }

    @Override
    protected void beforeSendDeviceInfo(Context context) {
        if (config.isPushEnabled()) {
            try {
                // Push notification configured for this app
                // Check device for Play Services APK.
                if (checkPlayServices()) {
                    String newRegistrationId = getRegistrationId();
                    if (SwrveHelper.isNullOrEmpty(newRegistrationId)) {
                        registerInBackground(getContext());
                    } else {
                        registrationId = newRegistrationId;
                    }
                }
            } catch (Throwable exp) {
                // Don't trust GCM and all the moving parts to work as expected
                Log.e(LOG_TAG, "Couldn't obtain the registration id for the device", exp);
            }
        }
    }

    @Override
    protected void afterInit() {
    }

    @Override
    protected void afterBind() {
        // Process intent that opened the app
        Activity ctx = getActivityContext();
        if (config.isPushEnabled() && ctx != null) {
            processIntent(ctx.getIntent());
        }
    }

    @Override
    protected void extraDeviceInfo(JSONObject deviceInfo) throws JSONException {
        if (!SwrveHelper.isNullOrEmpty(registrationId)) {
            deviceInfo.put(SWRVE_GCM_TOKEN, registrationId);
        }
    }

    public void setPushNotificationListener(ISwrvePushNotificationListener pushNotificationListener) {
        this.pushNotificationListener = pushNotificationListener;
    }

    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    protected boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(context.get());
        return resultCode == ConnectionResult.SUCCESS;
    }

    /**
     * Registers the application with GCM servers asynchronously.
     *
     * Stores the registration ID and app version
     */
    protected void registerInBackground(final Context context) {
        new AsyncTask<Void, Integer, Void>() {
            private void setRegistrationId(String regId) {
                try {
                    registrationId = regId;
                    if (qaUser != null) {
                        qaUser.updateDeviceInfo();
                    }

                    // Store registration id and app version
                    cachedLocalStorage.setAndFlushSharedEntry(REGISTRATION_ID_CATEGORY, registrationId);
                    cachedLocalStorage.setAndFlushSharedEntry(APP_VERSION_CATEGORY, appVersion);
                    // Re-send data now
                    queueDeviceInfoNow(true);
                } catch (Exception ex) {
                    Log.e(LOG_TAG, "Couldn't save the GCM registration id for the device", ex);
                }
            }

            @Override
            protected Void doInBackground(Void... params) {
                // Try to obtain the GCM registration id from Google Play
                try {
                    InstanceID instanceID = InstanceID.getInstance(context);
                    String gcmRegistrationId = instanceID.getToken(config.getSenderId(), null);
                    if (!SwrveHelper.isNullOrEmpty(gcmRegistrationId)) {
                        setRegistrationId(gcmRegistrationId);
                    }
                } catch (Exception ex) {
                    Log.e(LOG_TAG, "Couldn't obtain the GCM registration id for the device", ex);
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void v) {
            }
        }.execute(null, null, null);
    }

    /**
     * Gets the current registration ID for application on GCM service.
     * If result is empty, the app needs to register.
     *
     * @return registration ID, or empty string if there is no existing
     * registration ID.
     */
    protected String getRegistrationId() {
        // Try to get registration id from storage
        String registrationIdRaw = cachedLocalStorage.getSharedCacheEntry(REGISTRATION_ID_CATEGORY);
        if (SwrveHelper.isNullOrEmpty(registrationIdRaw)) {
            return "";
        }
        // Check if app was updated; if so, it must clear the registration ID
        // since the existing regID is not guaranteed to work with the new
        // app version.
        String registeredVersion = cachedLocalStorage.getSharedCacheEntry(APP_VERSION_CATEGORY);
        if (!SwrveHelper.isNullOrEmpty(registeredVersion) && !registeredVersion.equals(appVersion)) {
            return "";
        }
        return registrationIdRaw;
    }

    public void iapPlay(String productId, double productPrice, String currency, String purchaseData, String dataSignature) {
        SwrveIAPRewards rewards = new SwrveIAPRewards();
        this.iapPlay(productId, productPrice, currency, rewards, purchaseData, dataSignature);
    }

    public void iapPlay(String productId, double productPrice, String currency, SwrveIAPRewards rewards, String purchaseData, String dataSignature) {
        this._iapPlay(productId, productPrice, currency, rewards, purchaseData, dataSignature);
    }

    protected void _iapPlay(String productId, double productPrice, String currency, SwrveIAPRewards rewards, String purchaseData, String dataSignature) {
        try {
            if (checkPlayStoreSpecificArguments(purchaseData, dataSignature)) {
                this._iap(1, productId, productPrice, currency, rewards, purchaseData, dataSignature, "Google");
            }
        } catch (Exception exp) {
            Log.e(LOG_TAG, "IAP Play event failed", exp);
        }
    }

    protected boolean checkPlayStoreSpecificArguments(String purchaseData, String receiptSignature) throws IllegalArgumentException {
        if (SwrveHelper.isNullOrEmpty(purchaseData)) {
            Log.e(LOG_TAG, "IAP event illegal argument: receipt cannot be empty for Google Play store event");
            return false;
        }
        if (SwrveHelper.isNullOrEmpty(receiptSignature)) {
            Log.e(LOG_TAG, "IAP event illegal argument: receiptSignature cannot be empty for Google Play store event");
            return false;
        }
        return true;
    }

    public void onResume(Activity ctx) {
        super.onResume(ctx);
        try {
            if (config.isPushEnabled()) {
                if (activityContext != null) {
                    Activity relatedActivity = activityContext.get();
                    if (relatedActivity != null) {
                        processIntent(relatedActivity.getIntent());
                    }
                }
                if (qaUser != null) {
                    qaUser.bindToServices();
                }
            }
        } catch (Exception exp) {
            Log.e(LOG_TAG, "onResume failed", exp);
        }
    }

    public void processIntent(Intent intent) {
        if (intent != null) {
            Bundle extras = intent.getExtras();
            if (extras != null && !extras.isEmpty()) {
                Bundle msg = extras.getBundle(SwrveGcmNotification.GCM_BUNDLE);
                if (msg != null && config.isPushEnabled()) {
                    // Obtain push id
                    Object rawId = msg.get("_p");
                    String msgId = (rawId != null) ? rawId.toString() : null;
                    // Only process once the message if possible
                    if (!SwrveHelper.isNullOrEmpty(msgId) && (lastProcessedMessage == null || !lastProcessedMessage.equals(msgId))) {
                        lastProcessedMessage = msgId;
                        event("Swrve.Messages.Push-" + msgId + ".engaged", null);
                        // Call custom listener
                        if (pushNotificationListener != null) {
                            pushNotificationListener.onPushNotification(msg);
                        }
                    }
                }
            }
        }
    }
}
