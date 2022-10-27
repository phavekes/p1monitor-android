package eu.havekes.p1monitor

import android.content.Context
import androidx.core.content.edit

class PrefManager private constructor(context: Context) {

    // ------- Preference Variables

    var authenticationId: String?
        get() = pref.getString("KEY_AUTHENTICATION_ID", null)
        set(value) = pref.edit { putString("KEY_AUTHENTICATION_ID", value) }

    var authenticationStatus: Boolean
        get() = pref.getBoolean("KEY_AUTHENTICATION_STATUS", false)
        set(value) = pref.edit { putBoolean("KEY_AUTHENTICATION_STATUS", value) }

    // ---------------------------------------------------------------------------------------------

    private val pref = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    fun clear() = pref.edit { clear() }

    companion object : SingletonHolder<PrefManager, Context>(::PrefManager) {
        private const val FILE_NAME = "AUTHENTICATION_FILE_NAME"
    }
}