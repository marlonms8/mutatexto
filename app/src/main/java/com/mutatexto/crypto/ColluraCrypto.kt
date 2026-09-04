package com.mutatexto.crypto

import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Modo LEGADO do MutaTexto: implementação compatível byte a byte com o
 * comportamento do Collura Decrypter v1.0 e do módulo compatível do SINAPSE.
 * Mantido apenas para interoperabilidade com mensagens/formatos antigos.
 * Para criptografia nova, use ModernCrypto (AES-256-GCM).
 *
 * Parâmetros legados:
 * - AES-128-CBC
 * - PKCS5Padding
 * - IV = 16 bytes 0x00
 * - SHA-1 da senha em UTF-8
 * - Base64 intermediário sem '=' e com '\n' final (sem NO_WRAP)
 * - PBKDF2WithHmacSHA1
 * - salt = "Salt"
 * - 1 iteração
 * - chave de 128 bits
 * - ciphertext em Base64
 */
object ColluraCrypto {
    private const val SALT = "Salt"
    private const val ITERATIONS = 1
    private const val KEY_LENGTH_BITS = 128
    private val ZERO_IV = ByteArray(16)

    fun encrypt(plainText: String, password: String): String {
        require(password.isNotEmpty()) { "Informe a Chave de Segurança." }

        val key = deriveKey(password)
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, key, IvParameterSpec(ZERO_IV))
        val encrypted = cipher.doFinal(plainText.toByteArray(StandardCharsets.UTF_8))

        // NO_WRAP deixa o texto final mais prático para copiar/colar. O Collura
        // usa Base64.DEFAULT na leitura, portanto o formato é interoperável.
        return Base64.encodeToString(encrypted, Base64.NO_WRAP)
    }

    fun decrypt(cipherTextBase64: String, password: String): String {
        require(password.isNotEmpty()) { "Informe a Chave de Segurança." }
        require(cipherTextBase64.isNotBlank()) { "Informe o texto criptografado." }

        return try {
            val encrypted = Base64.decode(cipherTextBase64.trim(), Base64.DEFAULT)
            require(encrypted.isNotEmpty() && encrypted.size % 16 == 0) {
                "Texto criptografado inválido."
            }

            val key = deriveKey(password)
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, key, IvParameterSpec(ZERO_IV))
            String(cipher.doFinal(encrypted), StandardCharsets.UTF_8)
        } catch (e: IllegalArgumentException) {
            throw e
        } catch (e: Exception) {
            throw IllegalArgumentException("Chave incorreta ou texto criptografado inválido.", e)
        }
    }

    private fun deriveKey(password: String): SecretKeySpec {
        val sha1 = MessageDigest.getInstance("SHA-1")
        val digest = sha1.digest(password.toByteArray(StandardCharsets.UTF_8))

        // Equivale ao Base64.NO_PADDING do Android sem NO_WRAP. Para o SHA-1
        // (20 bytes), a codificação não excede a largura de linha e termina em LF.
        val hashBase64 = Base64.encodeToString(digest, Base64.NO_WRAP or Base64.NO_PADDING) + "\n"

        val spec = PBEKeySpec(
            hashBase64.toCharArray(),
            SALT.toByteArray(StandardCharsets.UTF_8),
            ITERATIONS,
            KEY_LENGTH_BITS
        )
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
        return SecretKeySpec(factory.generateSecret(spec).encoded, "AES")
    }
}
