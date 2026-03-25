package com.example.aicamera.data.network.copywriter

import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.aicamera.databinding.ActivityCopywriterTestBinding
import java.util.UUID

/**
 * 简单的 /ai/write 接口测试页。
 */
class CopywriterTestActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCopywriterTestBinding

    private var selectedUris: List<Uri> = emptyList()

    private val pickMultipleMedia = registerForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(10)
    ) { uris ->
        selectedUris = uris
        binding.tvSelectedCount.text = "已选择：${uris.size} 张"
        binding.tvSelectedUris.text = uris.joinToString(separator = "\n") { it.toString() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCopywriterTestBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.etSessionId.setText(UUID.randomUUID().toString())

        binding.btnPickImages.setOnClickListener {
            pickMultipleMedia.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }

        binding.btnGenerate.setOnClickListener {
            binding.tvResult.text = ""
            binding.progress.visibility = View.VISIBLE

            val sessionId = binding.etSessionId.text?.toString()?.trim().orEmpty()
            val req = CopywriterRequirement(
                type = binding.etType.text?.toString()?.trim().takeUnless { it.isNullOrBlank() },
                emotion = binding.etEmotion.text?.toString()?.trim().takeUnless { it.isNullOrBlank() },
                theme = binding.etTheme.text?.toString()?.trim().takeUnless { it.isNullOrBlank() },
                style = binding.etStyle.text?.toString()?.trim().takeUnless { it.isNullOrBlank() },
                length = binding.etLength.text?.toString()?.trim().takeUnless { it.isNullOrBlank() },
                special = binding.etSpecial.text?.toString()?.trim().takeUnless { it.isNullOrBlank() },
                custom = binding.etCustom.text?.toString()?.trim().takeUnless { it.isNullOrBlank() },
            )

            AiWriteManager.getInstance(this).writeWithUris(
                sessionId = sessionId,
                imageUris = selectedUris,
                requirement = req,
                callback = object : AiWriteManager.AiWriteCallback {
                    override fun onResult(result: AiWriteResult) {
                        runOnUiThread {
                            binding.progress.visibility = View.GONE
                            binding.tvResult.text = if (result.success) {
                                result.content ?: ""
                            } else {
                                "失败：${result.errorMessage}"
                            }
                        }
                    }
                }
            )
        }
    }
}
