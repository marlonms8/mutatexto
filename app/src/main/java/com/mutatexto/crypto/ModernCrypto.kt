package com.mutatexto.crypto

import android.util.Base64
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Esquema de criptografia moderno do MutaTexto.
 *
 * - AES-256-GCM (confidencialidade + autenticidade / integridade em uma única operação)
 * - Chave derivada com PBKDF2-HMAC-SHA256, 310.000 iterações (recomendação OWASP 2023+)
 * - Salt aleatório de 16 bytes por operação (SecureRandom)
 * - IV/nonce aleatório de 12 bytes por operação (nunca reutilizado)
 * - GCM tag de 128 bits
 * - Formato de saída: Base64( salt(16) || iv(12) || ciphertext+tag )
 *
 * Diferente do modo legado (Collura), aqui NADA é fixo: duas criptografias do
 * mesmo texto com a mesma chave produzem resultados diferentes a cada vez,
 * e qualquer adulteração do texto cifrado é detectada na descriptografia
 * (falha de autenticação em vez de "descriptografar" lixo silenciosamente).
 */
object ModernCrypto {
    private const val SALT_LENGTH_BYTES = 16
    private const val IV_LENGTH_BYTES = 12
    private const val GCM_TAG_LENGTH_BITS = 128
    private const val KEY_LENGTH_BITS = 256
    private const val PBKDF2_ITERATIONS = 310_000

    private val secureRandom = SecureRandom()

    fun encrypt(plainText: String, password: String): String {
        require(password.isNotEmpty()) { "Informe a Chave de Segurança." }

        val salt = ByteArray(SALT_LENGTH_BYTES).also { secureRandom.nextBytes(it) }
        val iv = ByteArray(IV_LENGTH_BYTES).also { secureRandom.nextBytes(it) }
        val key = deriveKey(password, salt)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
        val encrypted = cipher.doFinal(plainText.toByteArray(StandardCharsets.UTF_8))

        val payload = ByteBuffer.allocate(salt.size + iv.size + encrypted.size)
            .put(salt)
            .put(iv)
            .put(encrypted)
            .array()

        return Base64.encodeToString(payload, Base64.NO_WRAP)
    }

    fun decrypt(cipherTextBase64: String, password: String): String {
        require(password.isNotEmpty()) { "Informe a Chave de Segurança." }
        require(cipherTextBase64.isNotBlank()) { "Informe o texto criptografado." }

        return try {
            val payload = Base64.decode(cipherTextBase64.trim(), Base64.DEFAULT)
            require(payload.size > SALT_LENGTH_BYTES + IV_LENGTH_BYTES) {
                "Texto criptografado inválido."
            }

            val buffer = ByteBuffer.wrap(payload)
            val salt = ByteArray(SALT_LENGTH_BYTES).also { buffer.get(it) }
            val iv = ByteArray(IV_LENGTH_BYTES).also { buffer.get(it) }
            val encrypted = ByteArray(buffer.remaining()).also { buffer.get(it) }

            val key = deriveKey(password, salt)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
            String(cipher.doFinal(encrypted), StandardCharsets.UTF_8)
        } catch (e: IllegalArgumentException) {
            throw e
        } catch (e: Exception) {
            // Cobre falhas de autenticação GCM (AEADBadTagException) e chave/formato incorretos.
            throw IllegalArgumentException("Chave incorreta ou texto criptografado inválido.", e)
        }
    }

    private fun deriveKey(password: String, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return SecretKeySpec(factory.generateSecret(spec).encoded, "AES")
    }
}
