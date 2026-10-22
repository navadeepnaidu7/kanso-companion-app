package com.navadeep.kanso.receiver

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.math.BigInteger
import java.net.Socket
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.Principal
import java.security.PrivateKey
import java.security.SecureRandom
import java.security.spec.ECGenParameterSpec
import java.security.cert.X509Certificate
import java.util.Date
import javax.net.ssl.KeyManager
import javax.net.ssl.SSLContext
import javax.net.ssl.X509KeyManager
import javax.security.auth.x500.X500Principal

/** Device-bound private key; the paired phone pins the corresponding certificate. */
internal fun serverTls(): SSLContext {
    val alias = "kanso-remote-ec-v2"
    val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    if (!store.containsAlias(alias)) {
        val generator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore")
        generator.initialize(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY)
            .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
            // Conscrypt signs its pre-hashed TLS transcript using NONEwithECDSA.
            .setDigests(KeyProperties.DIGEST_NONE, KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA384, KeyProperties.DIGEST_SHA512)
            .setCertificateSubject(X500Principal("CN=Kanso Receiver"))
            .setCertificateSerialNumber(BigInteger(128, SecureRandom()))
            .setCertificateNotBefore(Date(0))
            .setCertificateNotAfter(Date(4102444800000L)).build())
        generator.generateKeyPair()
    }
    val keyManager = object : X509KeyManager {
        override fun getClientAliases(keyType: String?, issuers: Array<out Principal>?): Array<String>? = null
        override fun chooseClientAlias(keyType: Array<out String>?, issuers: Array<out Principal>?, socket: Socket?): String? = null
        override fun getServerAliases(keyType: String?, issuers: Array<out Principal>?): Array<String>? = if (keyType == "EC") arrayOf(alias) else null
        override fun chooseServerAlias(keyType: String?, issuers: Array<out Principal>?, socket: Socket?): String? = if (keyType == "EC") alias else null
        override fun getCertificateChain(value: String?): Array<X509Certificate> = store.getCertificateChain(alias).map { it as X509Certificate }.toTypedArray()
        override fun getPrivateKey(value: String?): PrivateKey = store.getKey(alias, null) as PrivateKey
    }
    return SSLContext.getInstance("TLS").apply { init(arrayOf<KeyManager>(keyManager), null, SecureRandom()) }
}
