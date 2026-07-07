package com.automation.voicegesture

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.automation.voicegesture.data.AppDatabase
import com.automation.voicegesture.data.Automation
import com.automation.voicegesture.data.GestureSerializer
import com.automation.voicegesture.databinding.ActivityAddAutomationBinding
import kotlinx.coroutines.launch

/** Screen for recording a new trigger word + gesture pair. */
class AddAutomationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddAutomationBinding
    private val dao by lazy { AppDatabase.getInstance(this).automationDao() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddAutomationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.clearGestureButton.setOnClickListener {
            binding.gestureDrawingView.clear()
        }

        binding.gestureDrawingView.setOnGestureChangedListener {
            binding.gestureHintText.visibility =
                if (binding.gestureDrawingView.hasRecordedGesture()) android.view.View.GONE
                else android.view.View.VISIBLE
        }

        binding.saveButton.setOnClickListener { save() }
    }

    private fun save() {
        val word = binding.triggerWordInput.text?.toString()?.trim().orEmpty()
        if (word.isEmpty()) {
            binding.triggerWordInput.error = getString(R.string.error_word_required)
            return
        }
        if (!binding.gestureDrawingView.hasRecordedGesture()) {
            Toast.makeText(this, R.string.error_gesture_required, Toast.LENGTH_LONG).show()
            return
        }

        val strokes = binding.gestureDrawingView.getRecordedStrokes()
        val automation = Automation(
            triggerWord = word,
            strokesJson = GestureSerializer.toJson(strokes)
        )

        lifecycleScope.launch {
            dao.insert(automation)
            Toast.makeText(this@AddAutomationActivity, R.string.automation_saved, Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
