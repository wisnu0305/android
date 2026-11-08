package com.example.projectorremote

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.projectorremote.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHost = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHost.navController
        binding.bottomNavigation.setupWithNavController(navController)

        // Android 15+ memaksa tampilan edge-to-edge: beri jarak dari status bar,
        // dan dari keyboard agar kolom input paling bawah tetap bisa di-scroll terlihat.
        ViewCompat.setOnApplyWindowInsetsListener(binding.navHostFragment) { view, insets ->
            val statusBar = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            val imeOverlap = (ime - binding.bottomNavigation.height).coerceAtLeast(0)
            view.updatePadding(top = statusBar, bottom = imeOverlap)
            insets
        }
    }
}
