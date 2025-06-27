package com.yonatanh_tald_evem.smartsilence.activities;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.yonatanh_tald_evem.smartsilence.R;

public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DURATION_MS = 3000;
    private static final long ANIMATION_DELAY = 300;

    private CardView logoContainer;
    private LinearLayout teamContainer;
    private CardView dateContainer;
    private ProgressBar progressBar;
    private ImageView logo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        initViews();
        startAnimations();

        // מעבר למסך הבא אחרי זמן המתנה
        new Handler().postDelayed(() -> {
            startActivity(new Intent(this, HomeActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        }, SPLASH_DURATION_MS);
    }

    private void initViews() {
        logoContainer = findViewById(R.id.logoContainer);
        teamContainer = findViewById(R.id.teamContainer);
        dateContainer = findViewById(R.id.dateContainer);
        progressBar = findViewById(R.id.progressBar);
        logo = findViewById(R.id.logo);

        // הגדרת שקיפות התחלתית לאנימציות
        logoContainer.setAlpha(0f);
        teamContainer.setAlpha(0f);
        dateContainer.setAlpha(0f);
        progressBar.setAlpha(0f);

        // הגדרת מיקום התחלתי לאנימציות
        logoContainer.setTranslationY(-200f);
        teamContainer.setTranslationY(100f);
        dateContainer.setTranslationY(100f);
        progressBar.setTranslationY(50f);
    }

    private void startAnimations() {
        // אנימציית הלוגו
        animateLogo();

        // אנימציית מידע הצוות
        new Handler().postDelayed(this::animateTeamInfo, ANIMATION_DELAY);

        // אנימציית התאריך
        new Handler().postDelayed(this::animateDate, ANIMATION_DELAY * 2);

        // אנימציית הפרוגרס בר
        new Handler().postDelayed(this::animateProgressBar, ANIMATION_DELAY * 3);
    }

    private void animateLogo() {
        AnimatorSet logoAnimSet = new AnimatorSet();

        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(logoContainer, "alpha", 0f, 1f);
        ObjectAnimator slideDown = ObjectAnimator.ofFloat(logoContainer, "translationY", -200f, 0f);
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(logoContainer, "scaleX", 0.5f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(logoContainer, "scaleY", 0.5f, 1f);

        logoAnimSet.playTogether(fadeIn, slideDown, scaleX, scaleY);
        logoAnimSet.setDuration(800);
        logoAnimSet.setInterpolator(new AccelerateDecelerateInterpolator());
        logoAnimSet.start();
    }

    private void animateTeamInfo() {
        AnimatorSet teamAnimSet = new AnimatorSet();

        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(teamContainer, "alpha", 0f, 1f);
        ObjectAnimator slideUp = ObjectAnimator.ofFloat(teamContainer, "translationY", 100f, 0f);

        teamAnimSet.playTogether(fadeIn, slideUp);
        teamAnimSet.setDuration(600);
        teamAnimSet.setInterpolator(new AccelerateDecelerateInterpolator());
        teamAnimSet.start();
    }

    private void animateDate() {
        AnimatorSet dateAnimSet = new AnimatorSet();

        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(dateContainer, "alpha", 0f, 1f);
        ObjectAnimator slideUp = ObjectAnimator.ofFloat(dateContainer, "translationY", 100f, 0f);
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(dateContainer, "scaleX", 0.8f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(dateContainer, "scaleY", 0.8f, 1f);

        dateAnimSet.playTogether(fadeIn, slideUp, scaleX, scaleY);
        dateAnimSet.setDuration(500);
        dateAnimSet.setInterpolator(new AccelerateDecelerateInterpolator());
        dateAnimSet.start();
    }

    private void animateProgressBar() {
        AnimatorSet progressAnimSet = new AnimatorSet();

        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(progressBar, "alpha", 0f, 1f);
        ObjectAnimator slideUp = ObjectAnimator.ofFloat(progressBar, "translationY", 50f, 0f);

        progressAnimSet.playTogether(fadeIn, slideUp);
        progressAnimSet.setDuration(400);
        progressAnimSet.setInterpolator(new AccelerateDecelerateInterpolator());
        progressAnimSet.start();
    }
}