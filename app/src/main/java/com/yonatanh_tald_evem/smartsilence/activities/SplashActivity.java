package com.yonatanh_tald_evem.smartsilence.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;

import com.yonatanh_tald_evem.smartsilence.R;

public class SplashActivity extends AppCompatActivity {
    private static final long SPLASH_DURATION_MS = 3000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set the layout for the splash screen
        setContentView(R.layout.activity_splash);

        // Delay execution of the next activity (HomeActivity) by SPLASH_DURATION_MS
        new Handler().postDelayed(() -> {
            startActivity(new Intent(this, HomeActivity.class));
            // Finish the splash activity so user can't return to it by pressing "Back"
            finish();
        }, SPLASH_DURATION_MS);
    }
}
