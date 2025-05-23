package com.github.crash;

import android.annotation.SuppressLint;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.github.andcrash.jcrash.AndCrash;
import com.github.andcrash.nativecrash.NativeCrash;

import java.util.ArrayList;
import java.util.List;

@SuppressLint("MissingInflatedId")
public class CrashMainActivity extends AppCompatActivity {
    private List<byte[]> bytes = new ArrayList<>();

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
                AndCrash.getInstance().postCrash(e);
            }
        });
        findViewById(R.id.btn_java_post_crash_custom_handler).setOnClickListener(v -> {
            try {
                CustomException.test();
            } catch (CustomException e) {
                AndCrash.getInstance().postCustomCrash(e);
            }
        });


        findViewById(R.id.btn_java_oom_crash).setOnClickListener(v -> {

            for (int i = 0; i < 1000000000; i++) {
                bytes.add(new byte[1024 * 1024]);
            }
        });


    }
}