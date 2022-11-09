package eu.havekes.p1monitor

import android.content.SharedPreferences
import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager


class SettingsActivity : AppCompatActivity(), SharedPreferences.OnSharedPreferenceChangeListener{

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, SettingsFragment())
                .commit()
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        PreferenceManager.getDefaultSharedPreferences(this)
            .registerOnSharedPreferenceChangeListener(this)


    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)

            val passwordPreference: EditTextPreference? = findPreference("basic_auth_password")
            passwordPreference?.setOnBindEditTextListener {editText ->
                editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            }

            val apikeyPreference: EditTextPreference? = findPreference("apikey")
            apikeyPreference?.setOnBindEditTextListener {editText ->
                editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            }

            val userPreference: EditTextPreference? = findPreference("basic_auth_user")
            userPreference?.setOnBindEditTextListener {editText ->
                editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            }

            val hostnamePreference: EditTextPreference? = findPreference("hostname")
            hostnamePreference?.setOnBindEditTextListener {editText ->
                editText.inputType = InputType.TYPE_TEXT_VARIATION_URI
            }
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {

    }

    override fun onDestroy() {
        super.onDestroy()
        PreferenceManager.getDefaultSharedPreferences(this)
            .unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}