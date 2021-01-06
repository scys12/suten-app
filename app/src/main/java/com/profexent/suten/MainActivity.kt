package com.profexent.suten

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.android.synthetic.main.bottom_sheet_layout.*

class MainActivity : AppCompatActivity(), View.OnClickListener {
    private lateinit var tebakButton : LinearLayout
    private lateinit var captureButton : LinearLayout
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        tebakButton  = findViewById(R.id.buttonTebak)
        tebakButton.setOnClickListener(this)

        captureButton = findViewById(R.id.buttonCapture)
        captureButton.setOnClickListener(this)
//        val linearLayout = findViewById<LinearLayout>(R.id.bottomSheet)
//        bottomSheetBehavior = BottomSheetBehavior.from(linearLayout)


    }

    override fun onClick(view: View) {
        when(view.id) {
            R.id.buttonTebak -> {
                val tebakIntent = Intent(this@MainActivity, TebakActivity::class.java)
                startActivity(tebakIntent)
            }
            R.id.buttonCapture -> {
                val bottomSheetDialog : BottomSheetDialog = BottomSheetDialog(
                    this, R.style.BottomSheetDialogTheme
                );
                val bottomSheetView : View = LayoutInflater.from(applicationContext)
                    .inflate(R.layout.bottom_sheet_layout,
                        findViewById<LinearLayout>(R.id.bottomSheet)
                    )
                bottomSheetDialog.setContentView(bottomSheetView)
                bottomSheetDialog.show()
            }
        }
    }

}