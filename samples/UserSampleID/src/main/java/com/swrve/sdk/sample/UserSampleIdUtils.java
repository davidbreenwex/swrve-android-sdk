package com.swrve.sdk.sample;

import com.swrve.sdk.SwrveHelper;
import com.swrve.sdk.SwrveSDK;

import java.util.HashMap;
import java.util.Map;

/**
 * A utility to send a user_property update to Swrve. The user property should be a number
 * from 1-100 and allows is generated using thier Swrve User Id.
 * This number can then be used to separate users into target groups and compare different
 * marketing strategys.
 * E.g. To target 1/4 of users you would use the filter "user_sample_id between 1 and 25 inclusive"
 */

public class UserSampleIdUtils {

    public static int generateNumberForUser(String user_id) {
        // get md5 hash of the user_id
        String hash = SwrveHelper.md5(user_id);
        hash = hash.substring(0, 8);

        // get a numeric representation of the hash
        double user_number = (double)Long.parseLong(hash, 16);
        final double max_md5 = 4294967295.0;
        // divide by the max_md5 to return a decimal between 0 and 1
        double user_range = user_number / max_md5;
        // From this decimal get an int from 1-100
        int userSampleId = (int) Math.ceil(user_range * 100.0d);

        return userSampleId;
    }
}
