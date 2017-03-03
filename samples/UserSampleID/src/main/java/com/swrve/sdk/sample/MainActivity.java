package com.swrve.sdk.sample;

import android.os.Bundle;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.swrve.sdk.SwrveSDK;

import static com.swrve.sdk.sample.R.id.user_id_text_view;


public class MainActivity extends AppCompatActivity {

    TextView userIdTextView, userSampleTextView;

    String userId;
    int userSampleId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SwrveSDK.onCreate(this);
        userId = SwrveSDK.getUserId();
        // Generate integer from 1-100 from user_id string and
        // send to Swrve as a user property update
        userSampleId = UserSampleIdUtils.generateNumberForUser(userId);

        setContentView(R.layout.activity_main);
        userIdTextView = (TextView) findViewById(user_id_text_view);
        userSampleTextView = (TextView) findViewById(R.id.user_sample_id_text_view);

        // Display the User Id and Sample User Id on the screen
        userIdTextView.setText(userId);
        userSampleTextView.setText(String.valueOf(userSampleId));
    }

    @Override
    protected void onResume() {
        super.onResume();
        SwrveSDK.onResume(this);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        SwrveSDK.onLowMemory();
    }

    @Override
    protected void onPause() {
        super.onPause();
        SwrveSDK.onPause();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        SwrveSDK.onNewIntent(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SwrveSDK.onDestroy(this);
    }
}
