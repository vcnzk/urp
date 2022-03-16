package com.example.mc04_urp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText

class activity_settings_ln : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_ln)

        val etLnRes = findViewById<EditText>(R.id.etLnResist)
        val btnApply = findViewById<Button>(R.id.btnApply)
        val btnCancel = findViewById<Button>(R.id.btnCancel)
        val btnDefault = findViewById<Button>(R.id.btnDefault)

        var i = intent

        val lnRes = i.getStringExtra("lnRESIST")

        etLnRes.setText(i.getStringExtra("lnRESIST").toString())

        btnApply.setOnClickListener {
            i = Intent()
            i.putExtra("lnRESIST", etLnRes.text.toString())
            setResult(RESULT_OK, i)
            finish()
        }

        btnCancel.setOnClickListener {
            i = Intent()
            i.putExtra("lnRESIST", lnRes.toString())
            setResult(RESULT_OK, i)
            finish()
        }
    }
}