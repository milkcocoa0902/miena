package com.milkcocoa.info.miena_sample

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

/**
 * LaunchActivity
 * @author keita
 * @since 2023/12/17 16:17
 */

/**
 *
 */
class LaunchActivity : AppCompatActivity(){
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_launch)
        findViewById<Button>(R.id.auth).setOnClickListener {
            startActivity(Intent(this, JpkiAuthActivity::class.java))
        }
        findViewById<Button>(R.id.sign).setOnClickListener {
            startActivity(Intent(this, JpkiSignActivity::class.java))
        }


    }
}