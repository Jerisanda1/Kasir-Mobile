package com.example.program_kasir;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        LinearLayout llContent = findViewById(R.id.llSplashContent);
        SessionManager sessionManager = new SessionManager(this);

        // Load dan jalankan animasi
        Animation splashAnim = AnimationUtils.loadAnimation(this, R.anim.splash_animation);
        llContent.startAnimation(splashAnim);

        // Tunggu animasi selesai baru pindah screen (delay sedikit lebih lama dari durasi anim)
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent intent;
            if (sessionManager.isLoggedIn()) {
                intent = new Intent(SplashActivity.this, MainActivity.class);
            } else {
                intent = new Intent(SplashActivity.this, LoginActivity.class);
            }
            startActivity(intent);
            finish();
            // Transisi fade out saat splash ditutup
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }, 2200);
    }
}
