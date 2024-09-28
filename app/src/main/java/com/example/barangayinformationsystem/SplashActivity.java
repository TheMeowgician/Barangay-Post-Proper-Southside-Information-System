package com.example.barangayinformationsystem;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ProgressBar;

import java.util.Timer;
import java.util.TimerTask;

public class SplashActivity extends AppCompatActivity {

    ProgressBar progressBar;
    int counter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        initializeComponents();
        startProgressBar();
    }

    private void startMainActivity() {//The code to move to the next activity named "ChooseActivity"

        Intent intent = new Intent(SplashActivity.this, ChooseActivity.class);
        startActivity(intent);
        finish();

    }

    private void startProgressBar() {//The code to start the timer and progressbar

        final Timer timer = new Timer();
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                counter = counter + 2;
                progressBar.setProgress(counter);
                if(counter == 100) {
                    timer.cancel();
                    startMainActivity();
                }
            }
        };

        timer.schedule(timerTask, 1000, 100);

    }

    private void initializeComponents() {//The Code to initialize components

        progressBar = findViewById(R.id.loadingProgressBar);
        counter = 0;

    }

}