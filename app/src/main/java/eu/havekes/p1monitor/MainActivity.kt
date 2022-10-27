package eu.havekes.p1monitor

import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Headers
import kotlin.math.roundToInt


//https://power.havekes.eu/api/v1/smartmeter?limit=1&sort=dec&json=object&round=on

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
    val CONSUMPTION_GAS_DELTA_M3:   Float
)

data class Financial(
    val CONSUMPTION_COST_GAS:               Float,
    val CONSUMPTION_COST_ELECTRICITY_HIGH:  Float,
    val CONSUMPTION_COST_ELECTRICITY_LOW:   Float
)

interface SmartmeterApi {
    @Headers("X-APIkey: 84C586FD55D0D973E307")
    @GET("api/v1/smartmeter?limit=1&sort=dec&json=object&round=on")
    suspend fun getSmartmeter() : Response<List<Smartmeter>>
}

interface PowerGasDayApi {
    @Headers("X-APIkey: 84C586FD55D0D973E307")
    @GET("api/v1/powergas/day?limit=1&sort=dec&json=object&round=off")
    suspend fun getPowerGas() : Response<List<PowerGas>>
}
interface PowerGasMonthApi {
    @Headers("X-APIkey: 84C586FD55D0D973E307")
    @GET("api/v1/powergas/month?limit=1&sort=dec&json=object&round=off")
    suspend fun getPowerGas() : Response<List<PowerGas>>
}

interface FinancialDayApi {
    @Headers("X-APIkey: 84C586FD55D0D973E307")
    @GET("api/v1/financial/day?limit=1&sort=dec&json=object&round=off")
    suspend fun getFinancial() : Response<List<Financial>>
}
interface FinancialMonthApi {
    @Headers("X-APIkey: 84C586FD55D0D973E307")
    @GET("api/v1/financial/month?limit=1&sort=dec&json=object&round=off")
    suspend fun getFinancial() : Response<List<Financial>>
}

object RetrofitHelper {
    var baseUrl: String = "https://p1.havekes.eu/"
    fun getInstance(): Retrofit {
        return Retrofit.Builder().baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            // we need to add converter factory to
            // convert JSON object to Java object
            .build()
    }
}

class MainActivity : AppCompatActivity() {
    var handler: Handler = Handler()
    var runnable: Runnable? = null
    var delay = 10000
    lateinit var settingsBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        var currentPower: TextView = findViewById(R.id.currentPower)
        var productionPower: TextView = findViewById(R.id.productionPower)
        var todayPower: TextView = findViewById(R.id.todayPower)
        var todayGas: TextView = findViewById(R.id.todayGas)
        var monthPower: TextView = findViewById(R.id.monthPower)
        var monthGas: TextView = findViewById(R.id.monthGas)
        var todayFinancialPower: TextView = findViewById(R.id.todayFinancialPower)
        var todayFinancialGas: TextView = findViewById(R.id.todayFinancialGas)
        var monthFinancialPower: TextView = findViewById(R.id.monthFinancialPower)
        var monthFinancialGas: TextView = findViewById(R.id.monthFinancialGas)


        try {
            updateCurrent(currentPower, productionPower)
            updateToday(todayPower, todayGas)
            updateMonth(monthPower,monthGas)
            updateFinancialToday(todayFinancialPower,todayFinancialGas)
            updateFinancialMonth(monthFinancialPower,monthFinancialGas)
        } catch (e:IllegalArgumentException){
             Log.e("error", e.toString())
             Toast.makeText(applicationContext,"Could not communicate with API. Check settings",Toast.LENGTH_SHORT).show()
        }
    }
    override fun onResume() {
        handler.postDelayed(Runnable {
            handler.postDelayed(runnable!!, delay.toLong())
            var currentPower: TextView = findViewById(R.id.currentPower)
            var productionPower: TextView = findViewById(R.id.productionPower)
            var todayPower: TextView = findViewById(R.id.todayPower)
            var todayGas: TextView = findViewById(R.id.todayGas)
            var monthPower: TextView = findViewById(R.id.monthPower)
            var monthGas: TextView = findViewById(R.id.monthGas)
            var todayFinancialPower: TextView = findViewById(R.id.todayFinancialPower)
            var todayFinancialGas: TextView = findViewById(R.id.todayFinancialGas)
            var monthFinancialPower: TextView = findViewById(R.id.monthFinancialPower)
            var monthFinancialGas: TextView = findViewById(R.id.monthFinancialGas)

            try {
                updateCurrent(currentPower, productionPower)
                updateToday(todayPower,todayGas)
                updateMonth(monthPower,monthGas)
                updateFinancialToday(todayFinancialPower,todayFinancialGas)
                updateFinancialMonth(monthFinancialPower,monthFinancialGas)
            } catch (e:IllegalArgumentException){
                Log.e("error", e.toString())
                Toast.makeText(applicationContext,"Could not communicate with API. Check settings",Toast.LENGTH_SHORT).show()
            }

        }.also { runnable = it }, delay.toLong())
        super.onResume()
    }
}

fun updateCurrent(currentPower: TextView,productionPower: TextView) {
    // launching a new coroutine
    GlobalScope.launch(Dispatchers.Main) {
        try {
            val smartmeterApi = RetrofitHelper.getInstance().create(SmartmeterApi::class.java)
            val result = smartmeterApi.getSmartmeter()

            // Checking the results
            //[Smartmeter(CONSUMPTION_GAS_M3=7104, CONSUMPTION_KWH_HIGH=11948, CONSUMPTION_KWH_LOW=12003, CONSUMPTION_W=191, PRODUCTION_KWH_HIGH=524, PRODUCTION_KWH_LOW=219, PRODUCTION_W=0, RECORD_IS_PROCESSED=0, TARIFCODE=P, TIMESTAMP_UTC=1666347688, TIMESTAMP_lOCAL=2022-10-21 12:21:28)]

            Log.d("p1monitor: ", result.body().toString())
            currentPower.text = result.body()?.get(0)?.CONSUMPTION_W.toString() + " Watt"
            productionPower.text = result.body()?.get(0)?.PRODUCTION_W.toString() + " Watt"
        } catch (e:IllegalArgumentException){
            Log.e("error", e.toString())
        }
    }
}

fun updateToday(todayPower: TextView, todayGas: TextView) {
    // launching a new coroutine
    GlobalScope.launch(Dispatchers.Main) {
        try {
            val powerGasApi = RetrofitHelper.getInstance().create(PowerGasDayApi::class.java)
            val result = powerGasApi.getPowerGas()

            // Checking the results
            //[Smartmeter(CONSUMPTION_GAS_M3=7104, CONSUMPTION_KWH_HIGH=11948, CONSUMPTION_KWH_LOW=12003, CONSUMPTION_W=191, PRODUCTION_KWH_HIGH=524, PRODUCTION_KWH_LOW=219, PRODUCTION_W=0, RECORD_IS_PROCESSED=0, TARIFCODE=P, TIMESTAMP_UTC=1666347688, TIMESTAMP_lOCAL=2022-10-21 12:21:28)]

            Log.d("p1monitor: ", result.body().toString())
            todayPower.text = result.body()?.get(0)?.CONSUMPTION_DELTA_KWH.toString() + " kWh"
            todayGas.text = result.body()?.get(0)?.CONSUMPTION_GAS_DELTA_M3.toString() + " m3"
        } catch (e:IllegalArgumentException){
            Log.e("error", e.toString())
        }
    }
}

fun updateMonth(monthPower: TextView, monthGas: TextView) {
    // launching a new coroutine
    GlobalScope.launch(Dispatchers.Main) {
        try {
            val powerGasApi = RetrofitHelper.getInstance().create(PowerGasMonthApi::class.java)
            val result = powerGasApi.getPowerGas()

            // Checking the results
            //[Smartmeter(CONSUMPTION_GAS_M3=7104, CONSUMPTION_KWH_HIGH=11948, CONSUMPTION_KWH_LOW=12003, CONSUMPTION_W=191, PRODUCTION_KWH_HIGH=524, PRODUCTION_KWH_LOW=219, PRODUCTION_W=0, RECORD_IS_PROCESSED=0, TARIFCODE=P, TIMESTAMP_UTC=1666347688, TIMESTAMP_lOCAL=2022-10-21 12:21:28)]

            Log.d("p1monitor: ", result.body().toString())
            monthPower.text = result.body()?.get(0)?.CONSUMPTION_DELTA_KWH.toString() + " kWh"
            monthGas.text = result.body()?.get(0)?.CONSUMPTION_GAS_DELTA_M3.toString() + " m3"
        } catch (e:IllegalArgumentException){
            Log.e("error", e.toString())
        }
    }
}

fun updateFinancialToday(todayFinancialPower: TextView, todayFinancialGas: TextView) {
    // launching a new coroutine
    GlobalScope.launch(Dispatchers.Main) {
        try {
            val financialDayApi = RetrofitHelper.getInstance().create(FinancialDayApi::class.java)
            val result = financialDayApi.getFinancial()

            // Checking the results
            //[Smartmeter(CONSUMPTION_GAS_M3=7104, CONSUMPTION_KWH_HIGH=11948, CONSUMPTION_KWH_LOW=12003, CONSUMPTION_W=191, PRODUCTION_KWH_HIGH=524, PRODUCTION_KWH_LOW=219, PRODUCTION_W=0, RECORD_IS_PROCESSED=0, TARIFCODE=P, TIMESTAMP_UTC=1666347688, TIMESTAMP_lOCAL=2022-10-21 12:21:28)]

            Log.d("p1monitor: ", result.body().toString())
            val cost=result.body()?.get(0)?.CONSUMPTION_COST_ELECTRICITY_HIGH!! + result.body()?.get(0)?.CONSUMPTION_COST_ELECTRICITY_HIGH!!
            val roundoffcost = (cost * 100.0).roundToInt() / 100.0
            todayFinancialPower.text = "€"+ roundoffcost.toString()
            val costsGas=result.body()?.get(0)?.CONSUMPTION_COST_GAS!!
            val roundoffcostgas = (costsGas * 100.0).roundToInt() / 100.0
            todayFinancialGas.text =  "€"+ roundoffcostgas.toString()
        } catch (e:IllegalArgumentException){
            Log.e("error", e.toString())
        }
    }
}

fun updateFinancialMonth(monthFinancialPower: TextView, monthFinancialGas: TextView) {
    // launching a new coroutine
    GlobalScope.launch(Dispatchers.Main) {
        try {
            val financialMonthApi = RetrofitHelper.getInstance().create(FinancialMonthApi::class.java)
            val result = financialMonthApi.getFinancial()

            // Checking the results
            //[Smartmeter(CONSUMPTION_GAS_M3=7104, CONSUMPTION_KWH_HIGH=11948, CONSUMPTION_KWH_LOW=12003, CONSUMPTION_W=191, PRODUCTION_KWH_HIGH=524, PRODUCTION_KWH_LOW=219, PRODUCTION_W=0, RECORD_IS_PROCESSED=0, TARIFCODE=P, TIMESTAMP_UTC=1666347688, TIMESTAMP_lOCAL=2022-10-21 12:21:28)]

            Log.d("p1monitor: ", result.body().toString())
            val cost=result.body()?.get(0)?.CONSUMPTION_COST_ELECTRICITY_HIGH!! + result.body()?.get(0)?.CONSUMPTION_COST_ELECTRICITY_HIGH!!
            val roundoffcost = (cost * 100.0).roundToInt() / 100.0
            monthFinancialPower.text = "€"+ roundoffcost.toString()
            val costsGas=result.body()?.get(0)?.CONSUMPTION_COST_GAS!!
            val roundoffcostgas = (costsGas * 100.0).roundToInt() / 100.0
            monthFinancialGas.text =  "€"+ roundoffcostgas.toString()
        } catch (e:IllegalArgumentException){
            Log.e("error", e.toString())
        }
    }
}