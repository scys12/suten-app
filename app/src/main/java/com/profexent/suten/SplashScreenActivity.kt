package com.profexent.suten

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class SplashScreenActivity : AppCompatActivity() {
    private lateinit var timerThread: Thread
    private lateinit var mainIntent: Intent
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)
        mainIntent = Intent(this, MainActivity::class.java)
        startThreadTimerSplash()

    }
    private fun startThreadTimerSplash() {
        timerThread = object : Thread(){
            override fun run() {
                try {
                    sleep(SPLASH_TIMER.toLong())
                    startActivity(mainIntent)
                    finish()
                }catch (e: InterruptedException){
                    e.printStackTrace()
                }
            }
        }
        timerThread.start()
    }
    companion object{
        var SPLASH_TIMER = 3000
    }
}