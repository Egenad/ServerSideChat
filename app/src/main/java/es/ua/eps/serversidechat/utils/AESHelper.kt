package es.ua.eps.serversidechat.utils

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec

object AESHelper {

    fun generateSecretKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance("AES")
        keyGenerator.init(256) // You can also use 128 or 192 bits
        return keyGenerator.generateKey()
    }

    fun encrypt(
        textToEncrypt: String,
        secretKey: SecretKey
    ): String {
        val plainText = textToEncrypt.toByteArray()

        val cipher = Cipher.getInstance("AES/CBC/PKCS7PADDING")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)

        val encrypt = cipher.doFinal(plainText)
        return Base64.encodeToString(encrypt, Base64.DEFAULT)
    }

    fun decrypt(
        encryptedText: String,
        secretKey: SecretKey
    ): String {
        val textToDecrypt = Base64.decode(encryptedText, Base64.DEFAULT)

        val cipher = Cipher.getInstance("AES/CBC/PKCS7PADDING")

        cipher.init(Cipher.DECRYPT_MODE, secretKey)

        val decrypt = cipher.doFinal(textToDecrypt)
        return String(decrypt)
    }
}