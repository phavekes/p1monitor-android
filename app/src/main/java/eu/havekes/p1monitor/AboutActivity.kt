package eu.havekes.p1monitor

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Html
import android.text.method.LinkMovementMethod
import android.widget.TextView

class AboutActivity : AppCompatActivity() {
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)

        val versionCode = BuildConfig.VERSION_CODE
        val versionName = BuildConfig.VERSION_NAME

        val version: TextView = findViewById(R.id.version)
        version.text= getString(R.string.app_name) + " " + versionName + " (" + versionCode + ")"
        val aboutText: TextView = findViewById(R.id.AboutText)
        val html=getString(R.string.AboutText)
        aboutText.movementMethod= LinkMovementMethod.getInstance()
        aboutText.text= Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT)

    }
}