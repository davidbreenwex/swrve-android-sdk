package com.swrve.sdk;

import android.content.Intent;
import android.os.Bundle;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.swrve.sdk.config.SwrveConfig;
import com.swrve.sdk.gcm.SwrveGcmConstants;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.manifest.BroadcastReceiverData;
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.shadows.ShadowApplication;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class SwrvePushEngageReceiverTest extends SwrveBaseTest {

    private ShadowApplication shadowApplication;
    private ShadowActivity shadowActivity;
    private Swrve swrveSpy;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        SwrveTestUtils.removeSwrveSDKSingletonInstance();
        shadowApplication = Shadows.shadowOf(RuntimeEnvironment.application);
        shadowActivity = Shadows.shadowOf(mActivity);
        SwrveConfig config = new SwrveConfig();
        config.setSenderId("12345");
        Swrve swrveReal = (Swrve) SwrveSDK.createInstance(mActivity, 1, "apiKey", config);
        swrveSpy = Mockito.spy(swrveReal);

    }

    @After
    public void tearDown() throws Exception {
        swrveSpy.shutdown();
        SwrveTestUtils.removeSwrveSDKSingletonInstance();
    }

    @Test
    public void testReceiverInManifest() throws Exception {
        List<BroadcastReceiverData> receiverDataList = shadowApplication.getAppManifest().getBroadcastReceivers();
        boolean inManifest = false;
        for (BroadcastReceiverData receiverData : receiverDataList) {
            if (receiverData.getClassName().equals("com.swrve.sdk.SwrvePushEngageReceiver")) {
                inManifest = true;
                break;
            }
        }
        assertTrue(inManifest);
    }

    @Test
    public void testReceiverOpenActivity() throws Exception {

        Intent intent = new Intent();
        Bundle extras = new Bundle();
        extras.putString("text", "validBundle");
        extras.putString(SwrveGcmConstants.SWRVE_TRACKING_KEY, "1234");
        intent.putExtra(SwrveGcmConstants.GCM_BUNDLE, extras);

        SwrvePushEngageReceiver pushEngageReceiver = new SwrvePushEngageReceiver();
        pushEngageReceiver.onReceive(RuntimeEnvironment.application.getApplicationContext(), intent);

        Intent nextIntent = shadowApplication.peekNextStartedActivity();
        assertNotNull(nextIntent);
        assertEquals("com.swrve.sdk.test.MainActivity", nextIntent.getComponent().getClassName());

        List<String> events = assertEventCount(1);
        assertEngagedEvent(events.get(0), "Swrve.Messages.Push-1234.engaged");
    }

    @Test
    public void testReceiverOpenDeeplink() throws Exception {

        Intent intent = new Intent();
        Bundle extras = new Bundle();
        extras.putString("customdata", "customdata_value");
        extras.putString(SwrveGcmConstants.DEEPLINK_KEY, "swrve://deeplink/campaigns");
        extras.putString(SwrveGcmConstants.SWRVE_TRACKING_KEY, "4567");
        intent.putExtra(SwrveGcmConstants.GCM_BUNDLE, extras);

        SwrvePushEngageReceiver pushEngageReceiver = new SwrvePushEngageReceiver();
        pushEngageReceiver.onReceive(RuntimeEnvironment.application.getApplicationContext(), intent);

        Intent nextIntent = shadowApplication.peekNextStartedActivity();
        assertNotNull(nextIntent);
        assertEquals(Intent.ACTION_VIEW, nextIntent.getAction());
        assertEquals("swrve://deeplink/campaigns", nextIntent.getData().toString());
        assertTrue(nextIntent.hasExtra("customdata"));
        assertEquals("customdata_value", nextIntent.getStringExtra("customdata"));

        List<String> events = assertEventCount(1);
        assertEngagedEvent(events.get(0), "Swrve.Messages.Push-4567.engaged");
    }

    private List<String> assertEventCount(int count) {
        List<String> events = null;
        List<Intent> broadcastIntents = shadowActivity.getBroadcastIntents();
        if (count == 0) {
            assertEquals(0, broadcastIntents.size());
        } else {
            assertEquals(1, broadcastIntents.size());
            assertEquals("com.swrve.sdk.SwrveWakefulReceiver", broadcastIntents.get(0).getComponent().getShortClassName());
            events = (List) broadcastIntents.get(0).getExtras().get(SwrveWakefulService.EXTRA_EVENTS);
            assertNotNull(events);
            assertEquals(count, events.size());
        }
        return events;
    }

    private void assertEngagedEvent(String eventJson, String eventName) {
        Gson gson = new Gson(); // eg: {"type":"event","time":1466519995192,"name":"Swrve.Messages.Push-1.engaged"}
        Type type = new TypeToken<Map<String, String>>() {
        }.getType();
        Map<String, String> event = gson.fromJson(eventJson, type);
        assertEquals(3, event.size());
        assertTrue(event.containsKey("type"));
        assertEquals("event", event.get("type"));
        assertTrue(event.containsKey("name"));
        assertEquals(eventName, event.get("name"));
        assertTrue(event.containsKey("time"));
    }

}
