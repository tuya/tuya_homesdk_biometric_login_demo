package com.thingclips.smart.biometrics_login

import android.content.Context
import android.content.SharedPreferences
import com.thingclips.smart.android.user.bean.User

object UserSessionManager {

    private const val PREF_NAME = "user_session_prefs"
    private const val KEY_UID = "key_uid"
    private const val KEY_EMAIL = "key_email"
    private const val KEY_COUNTRY_CODE = "key_country_code"
    private const val KEY_IS_LOGGED_IN = "key_is_logged_in"

    private fun prefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun saveLogin(context: Context, user: User?, email: String, countryCode: String) {
        val uid = user?.uid ?: ""
        prefs(context).edit()
            .putString(KEY_UID, uid)
            .putString(KEY_EMAIL, email)
            .putString(KEY_COUNTRY_CODE, countryCode)
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .apply()
    }

    /**
     * 指纹/生物识别登录成功后更新会话。
     * 用本次登录返回的 user 覆盖本地会话，确保与涂鸦 SDK 侧新 session 一致；
     * 原 session 会因本次登录而失效。
     */
    fun updateSessionAfterBiometricLogin(context: Context, user: User?, email: String, countryCode: String) {
        saveLogin(context, user, email, countryCode)
    }

    fun markLogout(context: Context) {
        // 仅标记登出，保留 uid/email/country 以便指纹登录继续使用
        prefs(context).edit()
            .putBoolean(KEY_IS_LOGGED_IN, false)
            .apply()
    }

    fun clearAll(context: Context) {
        prefs(context).edit().clear().apply()
    }

    fun isLoggedIn(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_IS_LOGGED_IN, false)
    }

    fun getUid(context: Context): String? {
        return prefs(context).getString(KEY_UID, null)
    }

    fun getEmail(context: Context): String? {
        return prefs(context).getString(KEY_EMAIL, null)
    }

    fun getCountryCode(context: Context): String? {
        return prefs(context).getString(KEY_COUNTRY_CODE, null)
    }

    fun hasBiometricUser(context: Context): Boolean {
        // 只要有 uid 就认为是可以尝试指纹登录的用户
        return !getUid(context).isNullOrEmpty()
    }
}

