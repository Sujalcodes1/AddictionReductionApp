package com.example.addictionreductionapp.data.local.database

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

/**
 * Manages the SQLCipher passphrase for encrypted Room database.
 *
 * The passphrase is:
 * 1. Generated randomly using [SecureRandom] on first launch.
 * 2. Stored in EncryptedSharedPreferences (backed by Android Keystore AES-256).
 * 3. Retrieved on subsequent launches.
 *
 * This means the database encryption key is:
 * - Unique per device (random generation).
 * - Protected at rest by the Android Keystore (hardware-backed on most devices).
 * - Never exposed in code, logs, or shared preferences in plaintext.
 *
 * ## Resilience
 * If the EncryptedSharedPreferences become corrupted (rare, but possible during
 * OS updates or backup/restore), a new passphrase is generated. In this case,
 * the old encrypted database becomes unreadable — this is an acceptable tradeoff
 * since the corruption is unrecoverable and the database can be recreated from
 * cloud data.
 */
object DatabaseSecurity {

    private const val PREFS_FILE = "db_passphrase_prefs"
    private const val PASSPHRASE_KEY = "sqlcipher_passphrase"
    private const val PASSPHRASE_LENGTH_BYTES = 32

    /**
     * Returns the SQLCipher passphrase for this device.
     * Creates and securely stores a new random passphrase on first call.
     *
     * @param context Application context.
     * @return 32-byte passphrase suitable for SQLCipher [SupportFactory].
     */
    fun getOrCreatePassphrase(context: Context): ByteArray {
        val applicationContext = context.applicationContext

        return try {
            val masterKey = MasterKey.Builder(applicationContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            val prefs = EncryptedSharedPreferences.create(
                applicationContext,
                PREFS_FILE,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )

            val existing = prefs.getString(PASSPHRASE_KEY, null)
            if (existing != null) {
                Base64.decode(existing, Base64.DEFAULT)
            } else {
                val newPassphrase = ByteArray(PASSPHRASE_LENGTH_BYTES).also {
                    SecureRandom().nextBytes(it)
                }
                prefs.edit()
                    .putString(PASSPHRASE_KEY, Base64.encodeToString(newPassphrase, Base64.DEFAULT))
                    .apply()
                newPassphrase
            }
        } catch (e: Exception) {
            android.util.Log.e("DatabaseSecurity", "Failed to create/retrieve passphrase", e)
            // Fallback: generate an ephemeral passphrase. Database will not
            // survive app restart but this prevents a hard crash.
            ByteArray(PASSPHRASE_LENGTH_BYTES).also { SecureRandom().nextBytes(it) }
        }
    }
}
