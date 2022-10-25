
import android.app.Activity
import android.content.Context

import android.content.SharedPreferences


object SharedPreferenceUtils {
    private var prefs: SharedPreferences? = null
    private var editor: SharedPreferences.Editor? = null
    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.getSharedPreferences(context.getPackageName(), Activity.MODE_PRIVATE)
            editor = prefs!!.edit()
        }
    }

    fun putString(key: String?, value: String?) {
        editor!!.putString(key, value)
        editor!!.commit()
    }

    fun getString(key: String?, defValue: String?): String? {
        return prefs!!.getString(key, defValue)
    }
}