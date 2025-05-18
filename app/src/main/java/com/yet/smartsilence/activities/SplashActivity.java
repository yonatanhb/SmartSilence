package com.yet.smartsilence.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;

import com.yet.smartsilence.R;

public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DURATION_MS = 2000; // 2 שניות

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // מעבר אוטומטי ל-HomeActivity אחרי 2 שניות
        new Handler().postDelayed(() -> {
            startActivity(new Intent(this, HomeActivity.class));
            finish();
        }, SPLASH_DURATION_MS);
    }
}
