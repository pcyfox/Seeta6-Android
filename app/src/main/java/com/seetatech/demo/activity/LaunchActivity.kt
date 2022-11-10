package com.seetatech.demo.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.df.seeta6.R

class LaunchActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launch)
    }

    fun onClick(view: View?) {
        startActivity(Intent(this, MainActivity::class.java))
    }

    fun onTestClick(view: View?) {
        startActivity(Intent(this, TestActivity::class.java))
    }
}