package com.saikou.teraplay

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.saikou.teraplay.utils.initActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initActivity(this)
    }
}