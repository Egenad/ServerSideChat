package es.ua.eps.serversidechat.utils

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey
import javax.crypto.Cipher

const val KEYSTORE_ALIAS = "keys_1"
const val KEYSTORE_PROVIDER = "AndroidKeyStore"

object RSAHelper {
    fun generateKeyPair() : KeyPair {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER)
        keyStore.load(null)
        if (!keyStore.containsAlias(KEYSTORE_ALIAS)) {
            val keyGen = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, KEYSTORE_PROVIDER)
            keyGen.initialize(
                KeyGenParameterSpec.Builder(
                    KEYSTORE_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1)
                    .setDigests(KeyProperties.DIGEST_SHA1)
                    .setKeySize(2048)
                    .build()
            )
            return keyGen.generateKeyPair()
        } else {
            val privateKey = keyStore.getKey(KEYSTORE_ALIAS, null) as PrivateKey
            val publicKey = keyStore.getCertificate(KEYSTORE_ALIAS).publicKey
            return KeyPair(publicKey, privateKey)
        }
    }

    fun encrypt(
        textToEncrypt: String,
        publicKey: PublicKey
    ): String {
        val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
        cipher.init(Cipher.ENCRYPT_MODE, publicKey)
        val encryptedBytes = cipher.doFinal(textToEncrypt.toByteArray(StandardCharsets.UTF_8))
        return Base64.encodeToString(encryptedBytes, Base64.DEFAULT)
    }

    fun decrypt(
        encryptedText: String,
        privateKey: PrivateKey
    ): String {
        val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
        cipher.init(Cipher.DECRYPT_MODE, privateKey)
        val decryptedBytes = cipher.doFinal(Base64.decode(encryptedText, Base64.DEFAULT))
        return String(decryptedBytes)
    }
}