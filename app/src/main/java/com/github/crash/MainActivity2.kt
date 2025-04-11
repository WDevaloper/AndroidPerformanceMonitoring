package com.github.crash

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.github.andcrash.jcrash.CrashLogger
import com.github.andcrash.jcrash.TestCrash
import kotlin.concurrent.thread

class MainActivity2 : AppCompatActivity() {
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main2)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        findViewById<View>(R.id.btn_main).setOnClickListener {
            com.github.andcrash.jcrash.TestCrash.testCrash()
        }

        findViewById<View>(R.id.btn_work).setOnClickListener {
            thread { com.github.andcrash.jcrash.TestCrash.testCrash() }
        }

        findViewById<View>(R.id.btn_post).setOnClickListener {
            try {
                com.github.andcrash.jcrash.TestCrash.testCrash()
            } catch (e: Exception) {
                com.github.andcrash.jcrash.CrashLogger.getInstance().postCrash(e)
            }
        }
    }
}