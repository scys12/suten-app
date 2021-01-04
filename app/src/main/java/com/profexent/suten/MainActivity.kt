package com.profexent.suten

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast

class MainActivity : AppCompatActivity(), View.OnClickListener {
    private lateinit var tebakButton : LinearLayout
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        tebakButton  = findViewById(R.id.buttonTebak)
        tebakButton.setOnClickListener(this)
    }

    override fun onClick(view: View) {
        when(view.id) {
            R.id.buttonTebak -> {
                val tebakIntent = Intent(this@MainActivity, TebakActivity::class.java)
                startActivity(tebakIntent)
            }
        }
    }

}