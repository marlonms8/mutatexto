package com.mutatexto.crypto

import android.app.Activity
import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.text.method.ScrollingMovementMethod
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private enum class CryptoMode { MODERN, LEGACY }

    private lateinit var passwordInput: EditText
    private lateinit var inputText: EditText
    private lateinit var resultText: TextView
    private lateinit var inputCounter: TextView
    private lateinit var statusText: TextView
    private lateinit var passwordToggle: ImageButton
    private lateinit var modeModernButton: Button
    private lateinit var modeLegacyButton: Button
    private lateinit var modeHintText: TextView
    private lateinit var loadingOverlay: View
    private val mainHandler = Handler(Looper.getMainLooper())

    private var passwordVisible = false
    private var mode = CryptoMode.MODERN

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        passwordInput = findViewById(R.id.passwordInput)
        inputText = findViewById(R.id.inputText)
        resultText = findViewById(R.id.resultText)
        resultText.movementMethod = ScrollingMovementMethod()
        inputCounter = findViewById(R.id.inputCounter)
        statusText = findViewById(R.id.statusText)
        passwordToggle = findViewById(R.id.passwordToggle)
        modeModernButton = findViewById(R.id.modeModernButton)
        modeLegacyButton = findViewById(R.id.modeLegacyButton)
        modeHintText = findViewById(R.id.modeHintText)
        loadingOverlay = findViewById(R.id.loadingOverlay)

        modeModernButton.setOnClickListener { setMode(CryptoMode.MODERN) }
        modeLegacyButton.setOnClickListener { setMode(CryptoMode.LEGACY) }
        setMode(CryptoMode.MODERN)

        findViewById<Button>(R.id.encryptButton).setOnClickListener { encrypt() }
        findViewById<Button>(R.id.decryptButton).setOnClickListener { decrypt() }
        findViewById<Button>(R.id.copyButton).setOnClickListener { copyResult() }
        findViewById<Button>(R.id.clearButton).setOnClickListener { clearAll() }
        passwordToggle.setOnClickListener { togglePasswordVisibility() }
        findViewById<ImageButton>(R.id.expandInputButton).setOnClickListener {
            showFullscreenEditor(inputText, getString(R.string.input_label))
        }
        findViewById<ImageButton>(R.id.expandResultButton).setOnClickListener {
            showFullscreenResult(resultText.text.toString(), getString(R.string.result_label))
        }
        findViewById<TextView>(R.id.privacyPolicyLink).setOnClickListener {
            openPrivacyPolicy()
        }

        attachCounter(inputText, inputCounter)
    }

    private fun setMode(newMode: CryptoMode) {
        mode = newMode
        when (newMode) {
            CryptoMode.MODERN -> {
                modeModernButton.background = getDrawable(R.drawable.bg_mode_selected)
                modeModernButton.setTextColor(getColor(R.color.text_primary))
                modeLegacyButton.background = getDrawable(R.drawable.bg_mode_unselected)
                modeLegacyButton.setTextColor(getColor(R.color.text_secondary))
                modeHintText.text = getString(R.string.mode_modern_hint)
            }
            CryptoMode.LEGACY -> {
                modeLegacyButton.background = getDrawable(R.drawable.bg_mode_selected)
                modeLegacyButton.setTextColor(getColor(R.color.text_primary))
                modeModernButton.background = getDrawable(R.drawable.bg_mode_unselected)
                modeModernButton.setTextColor(getColor(R.color.text_secondary))
                modeHintText.text = getString(R.string.mode_legacy_hint)
            }
        }
    }

    private fun encrypt() {
        val password = passwordInput.text.toString()
        val text = inputText.text.toString()

        if (password.isEmpty()) {
            showError(getString(R.string.error_password_required))
            passwordInput.requestFocus()
            return
        }
        if (text.isEmpty()) {
            showError(getString(R.string.error_text_required))
            inputText.requestFocus()
            return
        }

        runCryptoOperation(
            operation = {
                when (mode) {
                    CryptoMode.MODERN -> ModernCrypto.encrypt(text, password)
                    CryptoMode.LEGACY -> ColluraCrypto.encrypt(text, password)
                }
            },
            onSuccess = {
                resultText.setText(it)
                showSuccess(getString(R.string.status_encrypted))
            }
        )
    }

    private fun decrypt() {
        val password = passwordInput.text.toString()
        val text = inputText.text.toString()

        if (password.isEmpty()) {
            showError(getString(R.string.error_password_required))
            passwordInput.requestFocus()
            return
        }
        if (text.isBlank()) {
            showError(getString(R.string.error_cipher_required))
            inputText.requestFocus()
            return
        }

        runCryptoOperation(
            operation = {
                when (mode) {
                    CryptoMode.MODERN -> ModernCrypto.decrypt(text, password)
                    CryptoMode.LEGACY -> ColluraCrypto.decrypt(text, password)
                }
            },
            onSuccess = {
                resultText.setText(it)
                showSuccess(getString(R.string.status_decrypted))
            }
        )
    }

    /**
     * Executa a operação de criptografia/descriptografia em uma thread
     * separada, exibindo um spinner sobre a tela enquanto isso acontece.
     * Necessário mesmo sendo uma operação local: o Modo Moderno faz 310.000
     * iterações de PBKDF2, o que pode levar alguns instantes perceptíveis
     * em aparelhos mais fracos — sem isso, a tela travaria sem animação.
     */
    private fun runCryptoOperation(operation: () -> String, onSuccess: (String) -> Unit) {
        loadingOverlay.visibility = View.VISIBLE
        Thread {
            val result = runCatching(operation)
            mainHandler.post {
                loadingOverlay.visibility = View.GONE
                result
                    .onSuccess(onSuccess)
                    .onFailure { showError(it.message ?: getString(R.string.error_generic)) }
            }
        }.start()
    }

    private fun openPrivacyPolicy() {
        val url = getString(R.string.privacy_policy_url)
        runCatching {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }.onFailure {
            showError(getString(R.string.error_generic))
        }
    }

    private fun clearAll() {
        passwordInput.text.clear()
        inputText.text.clear()
        resultText.text = ""
        statusText.visibility = View.GONE
        passwordInput.requestFocus()
    }

    private fun copyResult() {
        val value = resultText.text.toString()
        if (value.isEmpty()) {
            showError(getString(R.string.error_nothing_to_copy))
            return
        }

        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(getString(R.string.result_label), value))
        Toast.makeText(this, R.string.status_copied, Toast.LENGTH_SHORT).show()
        showSuccess(getString(R.string.status_copied))
    }


    private fun togglePasswordVisibility() {
        passwordVisible = !passwordVisible
        passwordInput.transformationMethod = if (passwordVisible) {
            HideReturnsTransformationMethod.getInstance()
        } else {
            PasswordTransformationMethod.getInstance()
        }
        passwordInput.setSelection(passwordInput.text.length)
        passwordToggle.setImageResource(
            if (passwordVisible) R.drawable.ic_visibility_off else R.drawable.ic_visibility
        )
        passwordToggle.contentDescription = getString(
            if (passwordVisible) R.string.hide_password else R.string.show_password
        )
    }

    private fun attachCounter(editText: EditText, counter: TextView) {
        counter.text = getString(R.string.character_count, editText.text.length)
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                counter.text = getString(R.string.character_count, s?.length ?: 0)
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })
    }

    private fun showFullscreenEditor(target: EditText, title: String) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_fullscreen_text)

        dialog.window?.apply {
            setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
            setBackgroundDrawableResource(android.R.color.transparent)
            statusBarColor = getColor(R.color.window_background)
            navigationBarColor = getColor(R.color.window_background)
            setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }

        val titleView = dialog.findViewById<TextView>(R.id.fullscreenTitle)
        val editor = dialog.findViewById<EditText>(R.id.fullscreenEditor)
        val counter = dialog.findViewById<TextView>(R.id.fullscreenCounter)
        val closeButton = dialog.findViewById<ImageButton>(R.id.fullscreenCloseButton)
        val doneButton = dialog.findViewById<Button>(R.id.fullscreenDoneButton)

        titleView.text = title
        editor.setText(target.text)
        editor.setSelection(editor.text.length)
        counter.text = getString(R.string.character_count, editor.text.length)

        editor.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val value = s?.toString().orEmpty()
                counter.text = getString(R.string.character_count, value.length)
                // Sincronização em tempo real: voltar pelo botão do Android não perde o texto.
                target.setText(value)
                target.setSelection(target.text.length)
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })

        closeButton.setOnClickListener { dialog.dismiss() }
        doneButton.setOnClickListener { dialog.dismiss() }

        dialog.setOnShowListener {
            dialog.window?.setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT
            )
            editor.requestFocus()
        }

        dialog.show()
    }

    private fun showSuccess(message: String) {
        statusText.text = message
        statusText.setTextColor(getColor(R.color.status_success))
        statusText.visibility = View.VISIBLE
    }

    private fun showError(message: String) {
        statusText.text = message
        statusText.setTextColor(getColor(R.color.status_error))
        statusText.visibility = View.VISIBLE
    }

    private fun showFullscreenResult(value: String, title: String) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_fullscreen_result)

        dialog.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            statusBarColor = getColor(R.color.window_background)
            navigationBarColor = getColor(R.color.window_background)
            setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        }

        val titleView = dialog.findViewById<TextView>(R.id.fullscreenResultTitle)
        val resultView = dialog.findViewById<TextView>(R.id.fullscreenResultText)
        val closeButton = dialog.findViewById<ImageButton>(R.id.fullscreenResultCloseButton)
        val copyButton = dialog.findViewById<Button>(R.id.fullscreenResultCopyButton)

        titleView.text = title
        resultView.text = value
        resultView.movementMethod = ScrollingMovementMethod()

        closeButton.setOnClickListener { dialog.dismiss() }
        copyButton.setOnClickListener {
            if (value.isEmpty()) {
                Toast.makeText(this, R.string.error_nothing_to_copy, Toast.LENGTH_SHORT).show()
            } else {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(
                    ClipData.newPlainText(getString(R.string.result_label), value)
                )
                Toast.makeText(this, R.string.status_copied, Toast.LENGTH_SHORT).show()
            }
        }

        dialog.setOnShowListener {
            dialog.window?.setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT
            )
        }
        dialog.show()
    }

}
