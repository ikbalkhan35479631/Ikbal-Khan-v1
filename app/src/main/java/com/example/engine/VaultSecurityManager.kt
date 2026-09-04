package com.example.engine

import android.content.Context
import android.content.SharedPreferences
import java.security.MessageDigest

class VaultSecurityManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("vault_security_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_PIN_HASH = "vault_pin_hash"
    }

    fun hasPin(): Boolean {
        return !prefs.getString(KEY_PIN_HASH, "").isNullOrBlank()
    }

    fun setPin(pin: String): Boolean {
        if (pin.length != 4 || !pin.all { it.isDigit() }) return false
        val hash = hashString(pin)
        prefs.edit().putString(KEY_PIN_HASH, hash).apply()
        return true
    }

    fun verifyPin(pin: String): Boolean {
        val storedHash = prefs.getString(KEY_PIN_HASH, "") ?: return false
        if (storedHash.isBlank()) return false
        return hashString(pin) == storedHash
    }

    fun changePin(oldPin: String, newPin: String): Boolean {
        if (!verifyPin(oldPin)) return false
        return setPin(newPin)
    }

    fun resetPin() {
        prefs.edit().remove(KEY_PIN_HASH).apply()
    }

    private fun hashString(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
