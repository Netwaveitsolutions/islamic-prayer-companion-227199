package org.example.app

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import org.example.app.ui.MainActivity as RealMainActivity

/**
 * Compatibility shim: the app's real entrypoint is [org.example.app.ui.MainActivity].
 *
 * This class stays to avoid breaking any legacy references, but it simply forwards to the real
 * activity without relying on class inheritance (Kotlin classes are final by default).
 */
class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(Intent(this, RealMainActivity::class.java))
        finish()
    }
}
