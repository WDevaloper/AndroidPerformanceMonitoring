package com.github.crash;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.github.andcrash.jcrash.CrashLogger;
import com.github.andcrash.nativecrash.NativeCrash;

@SuppressLint("MissingInflatedId")
public class CrashMainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_crash_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        findViewById(R.id.btn_native_crash).setOnClickListener(v -> {
            NativeCrash.testNativeCrash();
        });
        findViewById(R.id.btn_java_crash).setOnClickListener(v -> {
            throw new RuntimeException("Test Java Crash");
        });

        findViewById(R.id.btn_java_post_crash).setOnClickListener(v -> {
            try {
                throw new RuntimeException("Test Java Post Crash");
            } catch (RuntimeException e) {
                CrashLogger.getInstance().postCrash(e);
            }
        });

    }
}