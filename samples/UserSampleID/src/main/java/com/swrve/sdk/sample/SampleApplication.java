package com.swrve.sdk.sample;

import android.app.Application;
import android.util.Log;

import com.swrve.sdk.SwrveHelper;
import com.swrve.sdk.SwrveSDK;
import com.swrve.sdk.config.SwrveConfig;

import static com.swrve.sdk.sample.UserSampleIdUtils.generateNumberForUser;

public class SampleApplication extends Application {

    private static final String LOG_TAG = "SwrveSample";
    //  FIXME Add your own App ID and Api Key here
    private static final int YOUR_APP_ID = 0;
    private static final String YOUR_API_KEY = "<add_your_api_key_here";

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            SwrveConfig config = new SwrveConfig();

//            Uncomment these lines to test the correct values are being returned for user_id
//            config.setUserId("01a94aec-d8ba-4292-8ab3-04edef4d1d71"); // returns 1
//            config.setUserId("018bb009-db2c-4feb-9c61-ec5871cd1cea"); // returns 50
//            config.setUserId("01b2d2e6-3fa5-4abb-aad8-d0aed0ece9b8");   // returns 100

            SwrveSDK.createInstance(this, YOUR_APP_ID, YOUR_API_KEY, config);
        } catch (Exception exp) {
            Log.e(LOG_TAG, "Could not initialize the Swrve SDK", exp);
        }
    }
}
