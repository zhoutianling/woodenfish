package com.zero.woodenfish

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updateLayoutParams
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.navOptions
import com.zero.woodenfish.databinding.ActivityMainBinding
import com.zero.woodenfish.feedback.HapticFeedbackPlayer
import com.zero.woodenfish.media.TapSoundPlayer
import com.zero.woodenfish.model.WoodenFishTab
import com.zero.woodenfish.model.WoodenFishUiEvent
import com.zero.woodenfish.ui.extension.disableAutomaticSystemInsets

class MainActivity : AppCompatActivity() {
    private val viewModel: WoodenFishViewModel by viewModels()
    private lateinit var binding: ActivityMainBinding
    private lateinit var soundPlayer: TapSoundPlayer
    private lateinit var hapticFeedbackPlayer: HapticFeedbackPlayer
    private var lastHandledEventId = 0L
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.onNotificationPermissionGranted()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        soundPlayer = TapSoundPlayer.getInstance(this)
        hapticFeedbackPlayer = HapticFeedbackPlayer(this)
        configureInsets()
        configureNavigation()
        observeViewModel()
        requestNotificationPermissionIfNeeded()
    }

    override fun onResume() {
        super.onResume()
        viewModel.onVisible()
    }

    override fun onPause() {
        viewModel.onHidden()
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    private fun configureInsets() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding.bottomNavigation.disableAutomaticSystemInsets()

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.navHostFragment.setPadding(0, bars.top, 0, 0)
            binding.systemNavigationBarSpacer.updateLayoutParams<ConstraintLayout.LayoutParams> {
                height = bars.bottom
            }
            insets
        }
        ViewCompat.requestApplyInsets(binding.root)
    }

    private fun configureNavigation() {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            navController.navigateToBottomNavigationDestination(item.itemId)
            true
        }
        binding.bottomNavigation.setOnItemReselectedListener { }
        navController.addOnDestinationChangedListener { _, destination, _ ->
            binding.bottomNavigation.menu.findItem(destination.id)?.isChecked = true
            destination.id.toWoodenFishTab()?.let(viewModel::onTabSelected)
        }
    }

    private fun observeViewModel() {
        viewModel.state.observe(this) { state ->
            applyImmersiveMode(state.immersiveEnabled)
        }
        viewModel.events.observe(this) { event ->
            handleUiEvent(event)
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (hasNotificationPermission()) {
            viewModel.onNotificationPermissionGranted()
            return
        }
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun hasNotificationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun handleUiEvent(event: WoodenFishUiEvent) {
        if (event.id <= lastHandledEventId) return
        lastHandledEventId = event.id
        when (event) {
            is WoodenFishUiEvent.TapFeedback -> {
                if (event.soundEnabled) soundPlayer.play()
                if (event.hapticEnabled) hapticFeedbackPlayer.playLightTap()
            }
        }
    }

    private fun applyImmersiveMode(enabled: Boolean) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            if (enabled) {
                hide(WindowInsetsCompat.Type.statusBars())
            } else {
                show(WindowInsetsCompat.Type.statusBars())
            }
        }
    }

    private fun Int.toWoodenFishTab(): WoodenFishTab? {
        return when (this) {
            R.id.tap_fragment -> WoodenFishTab.TAP
            R.id.record_fragment -> WoodenFishTab.RECORD
            R.id.settings_fragment -> WoodenFishTab.SETTINGS
            else -> null
        }
    }

    private fun NavController.navigateToBottomNavigationDestination(destinationId: Int) {
        if (currentDestination?.id == destinationId) return
        navigate(
            resId = destinationId,
            args = null,
            navOptions = navOptions {
                popUpTo(graph.startDestinationId) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        )
    }
}
