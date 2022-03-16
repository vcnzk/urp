package com.example.mc04_urp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText

class activity_settings_rp : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_rp)

        val etVOLT = findViewById<EditText>(R.id.etVolt)
        val etPWR = findViewById<EditText>(R.id.etRpPwr)
        val btnApply = findViewById<Button>(R.id.btnApply)
        val btnCancel = findViewById<Button>(R.id.btnCancel)
        val btnDefault = findViewById<Button>(R.id.btnDefault)

        var i = intent

        val volt = i.getStringExtra("URP")
        val pwr = i.getStringExtra("PRP")

        etVOLT.setText(i.getStringExtra("URP").toString())
        etPWR.setText(i.getStringExtra("PRP").toString())

        btnApply.setOnClickListener {
            i = Intent()
            i.putExtra("URP", etVOLT.text.toString())
            i.putExtra("PRP", etPWR.text.toString())
            setResult(RESULT_OK, i)
            finish()
        }

        btnCancel.setOnClickListener {
            i = Intent()
            i.putExtra("URP", volt.toString())
            i.putExtra("PRP", pwr.toString())
            setResult(RESULT_OK, i)
            finish()
        }
    }
}