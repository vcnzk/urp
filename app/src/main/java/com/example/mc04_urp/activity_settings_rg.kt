package com.example.mc04_urp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText

class activity_settings_rg : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_rg)

        val etRgRes = findViewById<EditText>(R.id.etRgResist)
        val etRgPwr = findViewById<EditText>(R.id.etRgPwr)
        val btnApply = findViewById<Button>(R.id.btnApply)
        val btnCancel = findViewById<Button>(R.id.btnCancel)
        val btnDefault = findViewById<Button>(R.id.btnDefault)

        var i = intent

        val rgRes = i.getStringExtra("rgRESIST")
        val rgPwr = i.getStringExtra("rgPWR")

        etRgRes.setText(i.getStringExtra("rgRESIST").toString())
        etRgPwr.setText(i.getStringExtra("rgPWR").toString())

        btnApply.setOnClickListener {
            i = Intent()
            i.putExtra("rgRESIST", etRgRes.text.toString())
            i.putExtra("rgPWR", etRgPwr.text.toString())
            setResult(RESULT_OK, i)
            finish()
        }

        btnCancel.setOnClickListener {
            i = Intent()
            i.putExtra("rgRESIST", rgRes.toString())
            i.putExtra("rgPWR", rgPwr.toString())
            setResult(RESULT_OK, i)
            finish()
        }
    }
}