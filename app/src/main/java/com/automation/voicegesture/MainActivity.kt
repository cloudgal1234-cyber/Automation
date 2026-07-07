package com.automation.voicegesture

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.automation.voicegesture.data.AppDatabase
import com.automation.voicegesture.data.Automation
import com.automation.voicegesture.databinding.ActivityMainBinding
import com.automation.voicegesture.service.GestureAccessibilityService
import com.automation.voicegesture.service.VoiceListenerService
import com.automation.voicegesture.ui.AutomationAdapter
import com.automation.voicegesture.util.Prefs
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val dao by lazy { AppDatabase.getInstance(this).automationDao() }
    private val adapter = AutomationAdapter(onDeleteClick = ::onDeleteAutomation)

    private val requestPermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grantResults ->
        if (grantResults[Manifest.permission.RECORD_AUDIO] == true) {
            enableListening()
        } else {
            Toast.makeText(this, R.string.mic_permission_required, Toast.LENGTH_LONG).show()
            binding.listeningSwitch.isChecked = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.automationsList.layoutManager = LinearLayoutManager(this)
        binding.automationsList.adapter = adapter

        dao.observeAll().observe(this) { automations ->
            adapter.submitList(automations)
            binding.emptyState.visibility = if (automations.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
        }

        binding.addAutomationFab.setOnClickListener {
            startActivity(Intent(this, AddAutomationActivity::class.java))
        }

        binding.accessibilityButton.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        binding.listeningSwitch.isChecked = Prefs.isListeningEnabled(this)
        binding.listeningSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) requestMicPermissionThenEnable() else disableListening()
        }
    }

    override fun onResume() {
        super.onResume()
        binding.accessibilityStatusText.setText(
            if (GestureAccessibilityService.isEnabled) R.string.accessibility_enabled
            else R.string.accessibility_disabled
        )
    }

    private fun requestMicPermissionThenEnable() {
        val permissionsToRequest = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        requestPermissions.launch(permissionsToRequest.toTypedArray())
    }

    private fun enableListening() {
        Prefs.setListeningEnabled(this, true)
        VoiceListenerService.start(this)
    }

    private fun disableListening() {
        Prefs.setListeningEnabled(this, false)
        VoiceListenerService.stop(this)
    }

    private fun onDeleteAutomation(automation: Automation) {
        lifecycleScope.launch { dao.delete(automation) }
    }
}
