package eu.havekes.p1monitor

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import kotlin.math.roundToInt

data class Smartmeter(
    val CONSUMPTION_GAS_M3:     Int,
    val CONSUMPTION_KWH_HIGH:   Int,
    val CONSUMPTION_KWH_LOW:    Int,
    val CONSUMPTION_W:          Int,
    val PRODUCTION_KWH_HIGH:    Int,
    val PRODUCTION_KWH_LOW:     Int,
    val PRODUCTION_W:           Int,
    val RECORD_IS_PROCESSED:    Int,
    val TARIFCODE:              String,
    val TIMESTAMP_UTC:          Int,
    val TIMESTAMP_lOCAL:        String
)

data class PowerGas(
    val CONSUMPTION_DELTA_KWH:      Float,
    val CONSUMPTION_GAS_DELTA_M3:   Float,
    val PRODUCTION_DELTA_KWH:       Float
)

data class Financial(
    val CONSUMPTION_COST_GAS:               Float,
    val CONSUMPTION_COST_ELECTRICITY_HIGH:  Float,
    val CONSUMPTION_COST_ELECTRICITY_LOW:   Float
)

interface SmartmeterApi {
    @GET("api/v1/smartmeter?limit=1&sort=dec&json=object&round=on")
    suspend fun getSmartmeter(@Header("X-APIkey") apikey: String) : Response<List<Smartmeter>>
}

interface PowerGasDayApi {
    @GET("api/v1/powergas/day?limit=1&sort=dec&json=object&round=off")
    suspend fun getPowerGas(@Header("X-APIkey") apikey: String) : Response<List<PowerGas>>
}
interface PowerGasMonthApi {
    @GET("api/v1/powergas/month?limit=1&sort=dec&json=object&round=off")
    suspend fun getPowerGas(@Header("X-APIkey") apikey: String) : Response<List<PowerGas>>
}

interface FinancialDayApi {
    @GET("api/v1/financial/day?limit=1&sort=dec&json=object&round=off")
    suspend fun getFinancial(@Header("X-APIkey") apikey: String) : Response<List<Financial>>
}
interface FinancialMonthApi {
    @GET("api/v1/financial/month?limit=1&sort=dec&json=object&round=off")
    suspend fun getFinancial(@Header("X-APIkey") apikey: String) : Response<List<Financial>>
}

object RetrofitHelper {
    fun getInstance(
        baseUrl: String,
        basicAuth: Boolean,
        basicAuthUser: String,
        basicAutPassword: String
    ): Retrofit {
        if (basicAuth) {
            val client = OkHttpClient.Builder()
                .addInterceptor(
                    BasicAuthInterceptor(
                        basicAuthUser,
                        basicAutPassword
                    )
                )
                .build()
            return Retrofit.Builder().baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                // we need to add converter factory to
                // convert JSON object to Java object
                .build()
        } else {
            return Retrofit.Builder().baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                // we need to add converter factory to
                // convert JSON object to Java object
                .build()
        }
    }
}

class MainActivity : AppCompatActivity() {
    var handler: Handler = Handler(Looper.getMainLooper())
    var runnable: Runnable? = null
    var delay = 10000
    lateinit var settingsBtn: Button

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.settings_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.setting -> {
                //Toast.makeText(this,"You are getting ready for android preferences",Toast.LENGTH_LONG).show()
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
            }
            R.id.about -> {
                //Toast.makeText(this,"You are getting ready for android preferences",Toast.LENGTH_LONG).show()
                val intent = Intent(this, AboutActivity::class.java)
                startActivity(intent)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onPause() {
        handler.removeCallbacksAndMessages(null)
        super.onPause()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val prefs =  PreferenceManager.getDefaultSharedPreferences(this)
        val baseUrl = prefs.getString("hostname","")
        val apikey = prefs.getString("apikey","").toString()
        val basicAuth = prefs.getBoolean("basic_auth_enabled",false)
        val basicAuthUser = prefs.getString("basic_auth_user","").toString()
        val basicAutPassword = prefs.getString("basic_auth_password","").toString()

        val currentPower: TextView = findViewById(R.id.currentPower)
        val productionPower: TextView = findViewById(R.id.productionPower)
        val todayPower: TextView = findViewById(R.id.todayPower)
        val todayProduction: TextView = findViewById(R.id.todayProduction)
        val todayGas: TextView = findViewById(R.id.todayGas)
        val monthPower: TextView = findViewById(R.id.monthPower)
        val monthProduction: TextView = findViewById(R.id.monthProduction)
        val monthGas: TextView = findViewById(R.id.monthGas)
        val todayFinancialPower: TextView = findViewById(R.id.todayFinancialPower)
        val todayFinancialGas: TextView = findViewById(R.id.todayFinancialGas)
        val monthFinancialPower: TextView = findViewById(R.id.monthFinancialPower)
        val monthFinancialGas: TextView = findViewById(R.id.monthFinancialGas)

        val textViewError: TextView = findViewById(R.id.textViewError)

        try {
            if (baseUrl != null) {
                //clear errors
                textViewError.text = ""
                updateCurrent(currentPower, productionPower, baseUrl, apikey,applicationContext, handler,basicAuth,basicAuthUser,basicAutPassword,textViewError )
                updateToday(todayPower, todayProduction, todayGas, baseUrl, apikey,basicAuth,basicAuthUser,basicAutPassword,textViewError)
                updateMonth(monthPower, monthProduction, monthGas, baseUrl, apikey,basicAuth,basicAuthUser,basicAutPassword,textViewError)
                updateFinancialToday(todayFinancialPower, todayFinancialGas, baseUrl, apikey,basicAuth,basicAuthUser,basicAutPassword,textViewError)
                updateFinancialMonth(monthFinancialPower, monthFinancialGas, baseUrl, apikey,basicAuth,basicAuthUser,basicAutPassword,textViewError)
            } else {
                textViewError.setText(R.string.error_no_basurl)
            }
        } catch (e:IllegalArgumentException){
             Log.e("error", e.toString())
             textViewError.setText(e.message)
             Toast.makeText(applicationContext,"Could not communicate with API. Check settings",Toast.LENGTH_SHORT).show()
        }
    }
    override fun onResume() {
        val textViewError: TextView = findViewById(R.id.textViewError)
        val currentPower: TextView = findViewById(R.id.currentPower)
        val productionPower: TextView = findViewById(R.id.productionPower)
        val todayPower: TextView = findViewById(R.id.todayPower)
        val todayProduction: TextView = findViewById(R.id.todayProduction)
        val todayGas: TextView = findViewById(R.id.todayGas)
        val monthPower: TextView = findViewById(R.id.monthPower)
        val monthProduction: TextView = findViewById(R.id.monthProduction)
        val monthGas: TextView = findViewById(R.id.monthGas)
        val todayFinancialPower: TextView = findViewById(R.id.todayFinancialPower)
        val todayFinancialGas: TextView = findViewById(R.id.todayFinancialGas)
        val monthFinancialPower: TextView = findViewById(R.id.monthFinancialPower)
        val monthFinancialGas: TextView = findViewById(R.id.monthFinancialGas)
        textViewError.setText("")
        currentPower.setText(R.string.refreshing)
        productionPower.setText(R.string.refreshing)
        todayPower.setText(R.string.refreshing)
        todayProduction.setText(R.string.refreshing)
        todayGas.setText(R.string.refreshing)
        monthPower.setText(R.string.refreshing)
        monthProduction.setText(R.string.refreshing)
        monthGas.setText(R.string.refreshing)
        todayFinancialPower.setText(R.string.refreshing)
        todayFinancialGas.setText(R.string.refreshing)
        monthFinancialPower.setText(R.string.refreshing)
        monthFinancialGas.setText(R.string.refreshing)

        handler.postDelayed(Runnable {
            val prefs =  PreferenceManager.getDefaultSharedPreferences(this)
            val baseUrl = prefs.getString("hostname","")
            val apikey = prefs.getString("apikey","").toString()
            val basicAuth = prefs.getBoolean("basic_auth_enabled",false)
            val basicAuthUser = prefs.getString("basic_auth_user","").toString()
            val basicAutPassword = prefs.getString("basic_auth_password","").toString()

            handler.postDelayed(runnable!!, delay.toLong())
            val currentPower: TextView = findViewById(R.id.currentPower)
            val productionPower: TextView = findViewById(R.id.productionPower)
            val todayPower: TextView = findViewById(R.id.todayPower)
            val todayProduction: TextView = findViewById(R.id.todayProduction)
            val todayGas: TextView = findViewById(R.id.todayGas)
            val monthPower: TextView = findViewById(R.id.monthPower)
            val monthProduction: TextView = findViewById(R.id.monthProduction)
            val monthGas: TextView = findViewById(R.id.monthGas)
            val todayFinancialPower: TextView = findViewById(R.id.todayFinancialPower)
            val todayFinancialGas: TextView = findViewById(R.id.todayFinancialGas)
            val monthFinancialPower: TextView = findViewById(R.id.monthFinancialPower)
            val monthFinancialGas: TextView = findViewById(R.id.monthFinancialGas)

            val textViewError: TextView = findViewById(R.id.textViewError)


            if (baseUrl != null) {
                textViewError.text=""
                updateCurrent(currentPower, productionPower, baseUrl, apikey,applicationContext, handler,basicAuth,basicAuthUser,basicAutPassword,textViewError)
                updateToday(todayPower, todayProduction, todayGas, baseUrl, apikey,basicAuth,basicAuthUser,basicAutPassword,textViewError)
                updateMonth(monthPower, monthProduction, monthGas, baseUrl, apikey,basicAuth,basicAuthUser,basicAutPassword,textViewError)
                updateFinancialToday(todayFinancialPower, todayFinancialGas, baseUrl, apikey,basicAuth,basicAuthUser,basicAutPassword,textViewError)
                updateFinancialMonth(monthFinancialPower, monthFinancialGas, baseUrl, apikey,basicAuth,basicAuthUser,basicAutPassword,textViewError)
            } else {
                textViewError.setText(R.string.error_no_basurl)
            }

        }.also { runnable = it }, delay.toLong())
        super.onResume()
    }
}

@SuppressLint("SetTextI18n")
@OptIn(DelicateCoroutinesApi::class)
fun updateCurrent(currentPower: TextView, productionPower: TextView, baseUrl: String, apikey: String, applicationContext: Context, handler: Handler, basicAuth: Boolean, basicAuthUser: String, basicAutPassword: String, textViewError: TextView ) {
    // launching a new coroutine
    GlobalScope.launch(Dispatchers.Main) {
        try {
            val smartmeterApi = RetrofitHelper.getInstance(baseUrl,basicAuth,basicAuthUser,basicAutPassword).create(SmartmeterApi::class.java)
            val result = smartmeterApi.getSmartmeter(apikey)
            Log.d("p1monitor: ", result.body().toString())
            if (result.code() == 200) {
                currentPower.text = result.body()?.get(0)?.CONSUMPTION_W.toString() + " Watt";
                productionPower.text = result.body()?.get(0)?.PRODUCTION_W.toString() + " Watt"
            } else {
                displayError(textViewError,result.code())
                throw IllegalArgumentException("Communication error")
            }
        } catch (e: java.lang.Exception){
            Log.e("error", e.toString())
            textViewError.setText(e.message)
            Toast.makeText(applicationContext,"Could not communicate with API. Check settings",Toast.LENGTH_SHORT).show()
            currentPower.text = "Error"
            productionPower.text = "Error"
            handler.removeCallbacksAndMessages(null);
        }
    }
}

@SuppressLint("SetTextI18n")
@OptIn(DelicateCoroutinesApi::class)
fun updateToday(todayPower: TextView, todayProduction: TextView, todayGas: TextView, baseUrl: String, apikey: String, basicAuth: Boolean, basicAuthUser: String, basicAutPassword: String, textViewError: TextView) {
    // launching a new coroutine
    GlobalScope.launch(Dispatchers.Main) {
        try {
            val powerGasApi = RetrofitHelper.getInstance(baseUrl,basicAuth,basicAuthUser,basicAutPassword).create(PowerGasDayApi::class.java)
            val result = powerGasApi.getPowerGas(apikey)
            Log.d("p1monitor: ", result.body().toString())
            if (result.code() == 200) {
                todayPower.text = result.body()?.get(0)?.CONSUMPTION_DELTA_KWH.toString() + " kWh"
                todayProduction.text = result.body()?.get(0)?.PRODUCTION_DELTA_KWH.toString() + " kWh"
                todayGas.text = result.body()?.get(0)?.CONSUMPTION_GAS_DELTA_M3.toString() + " m3"
            } else {
                displayError(textViewError,result.code())
                todayPower.text = "Error"
                todayProduction.text = "Error"
                todayGas.text = "Error"
            }
        } catch (e:Exception) {
            Log.e("error", e.toString())
            todayPower.text = "Error"
            todayProduction.text = "Error"
            todayGas.text = "Error"
        }
    }
}

@SuppressLint("SetTextI18n")
@OptIn(DelicateCoroutinesApi::class)
fun updateMonth(monthPower: TextView, monthProduction:TextView, monthGas: TextView, baseUrl: String, apikey: String, basicAuth: Boolean, basicAuthUser: String, basicAutPassword: String, textViewError: TextView) {
    // launching a new coroutine
    GlobalScope.launch(Dispatchers.Main) {
        try {
            val powerGasApi = RetrofitHelper.getInstance(baseUrl,basicAuth,basicAuthUser,basicAutPassword).create(PowerGasMonthApi::class.java)
            val result = powerGasApi.getPowerGas(apikey)
            Log.d("p1monitor: ", result.body().toString())
            if (result.code() == 200) {
                monthPower.text = result.body()?.get(0)?.CONSUMPTION_DELTA_KWH.toString() + " kWh"
                monthProduction.text = result.body()?.get(0)?.PRODUCTION_DELTA_KWH.toString() + " kWh"
                monthGas.text = result.body()?.get(0)?.CONSUMPTION_GAS_DELTA_M3.toString() + " m3"
            } else {
                displayError(textViewError,result.code())
                monthPower.text = "Error"
                monthProduction.text = "Error"
                monthGas.text = "Error"
            }
        } catch (e:Exception) {
            Log.e("error", e.toString())
            monthPower.text = "Error"
            monthProduction.text = "Error"
            monthGas.text = "Error"
        }
    }
}

@SuppressLint("SetTextI18n")
@OptIn(DelicateCoroutinesApi::class)
fun updateFinancialToday(todayFinancialPower: TextView, todayFinancialGas: TextView, baseUrl: String, apikey: String, basicAuth: Boolean, basicAuthUser: String, basicAutPassword: String, textViewError: TextView) {
    // launching a new coroutine
    GlobalScope.launch(Dispatchers.Main) {
        try {
            val financialDayApi = RetrofitHelper.getInstance(baseUrl,basicAuth,basicAuthUser,basicAutPassword).create(FinancialDayApi::class.java)
            val result = financialDayApi.getFinancial(apikey)

            Log.d("p1monitor: ", result.body().toString())
            if (result.code() == 200) {
                val cost = result.body()?.get(0)?.CONSUMPTION_COST_ELECTRICITY_HIGH!! + result.body()?.get(0)?.CONSUMPTION_COST_ELECTRICITY_LOW!!
                val roundoffcost = (cost * 100.0).roundToInt() / 100.0
                todayFinancialPower.text = "€" + roundoffcost.toString()
                val costsGas = result.body()?.get(0)?.CONSUMPTION_COST_GAS!!
                val roundoffcostgas = (costsGas * 100.0).roundToInt() / 100.0
                todayFinancialGas.text = "€" + roundoffcostgas.toString()
            } else {
                displayError(textViewError,result.code())
                todayFinancialPower.text = "Error"
                todayFinancialGas.text = "Error"
            }
        } catch (e:Exception) {
            Log.e("error", e.toString())
            todayFinancialPower.text = "Error"
            todayFinancialGas.text = "Error"
        }
    }
}

@SuppressLint("SetTextI18n")
@OptIn(DelicateCoroutinesApi::class)
fun updateFinancialMonth(monthFinancialPower: TextView, monthFinancialGas: TextView, baseUrl: String, apikey: String, basicAuth: Boolean, basicAuthUser: String, basicAutPassword: String, textViewError: TextView) {
    // launching a new coroutine
    GlobalScope.launch(Dispatchers.Main) {
        try {
            val financialMonthApi = RetrofitHelper.getInstance(baseUrl,basicAuth,basicAuthUser,basicAutPassword).create(FinancialMonthApi::class.java)
            val result = financialMonthApi.getFinancial(apikey)
            Log.d("p1monitor: ", result.body().toString())
            if (result.code() == 200) {
                val cost =
                    result.body()?.get(0)?.CONSUMPTION_COST_ELECTRICITY_HIGH!! + result.body()
                        ?.get(0)?.CONSUMPTION_COST_ELECTRICITY_LOW!!
                val roundoffcost = (cost * 100.0).roundToInt() / 100.0
                monthFinancialPower.text = "€" + roundoffcost.toString()
                val costsGas = result.body()?.get(0)?.CONSUMPTION_COST_GAS!!
                val roundoffcostgas = (costsGas * 100.0).roundToInt() / 100.0
                monthFinancialGas.text = "€" + roundoffcostgas.toString()
            } else {
                displayError(textViewError,result.code())
                monthFinancialPower.text = "Error"
                monthFinancialGas.text = "Error"
            }
        } catch (e:Exception) {
            Log.e("error", e.toString())
            monthFinancialPower.text = "Error"
            monthFinancialGas.text = "Error"
        }
    }
}

fun displayError(textViewError: TextView,result: Int){
    Log.e("p1monitor","HTTP status "+result)
    when (result) {
        401 -> textViewError.setText("Authentication failure ("+result.toString()+")")
        else -> {
            textViewError.setText("HTTP error " +result.toString() + " while accessing API")
        }
    }

}