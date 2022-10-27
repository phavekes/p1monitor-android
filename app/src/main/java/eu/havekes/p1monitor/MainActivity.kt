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

interface SmartmeterApi {
    @Headers("X-APIkey: 84C586FD55D0D973E307")
    @GET("api/v1/smartmeter?limit=1&sort=dec&json=object&round=on")
    suspend fun getSmartmeter() : Response<List<Smartmeter>>
}

interface PowerGasApi {
    @Headers("X-APIkey: 84C586FD55D0D973E307")
    @GET("api/v1/powergas/day?limit=1&sort=dec&json=object&round=off")
    suspend fun getPowerGas() : Response<List<PowerGas>>
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


        try {
            updateCurrent(currentPower, productionPower)
            updateToday(todayPower, todayGas)
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

            try {
                updateCurrent(currentPower, productionPower)
                updateToday(todayPower,todayGas)
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
            val powerGasApi = RetrofitHelper.getInstance().create(PowerGasApi::class.java)
            val result = powerGasApi.getPowerGas()

            // Checking the results
            //[Smartmeter(CONSUMPTION_GAS_M3=7104, CONSUMPTION_KWH_HIGH=11948, CONSUMPTION_KWH_LOW=12003, CONSUMPTION_W=191, PRODUCTION_KWH_HIGH=524, PRODUCTION_KWH_LOW=219, PRODUCTION_W=0, RECORD_IS_PROCESSED=0, TARIFCODE=P, TIMESTAMP_UTC=1666347688, TIMESTAMP_lOCAL=2022-10-21 12:21:28)]

            Log.d("p1monitor: ", result.body().toString())
            todayPower.text = result.body()?.get(0)?.CONSUMPTION_DELTA_KWH.toString() + " Watt"
            todayGas.text = result.body()?.get(0)?.CONSUMPTION_GAS_DELTA_M3.toString() + " m3"
        } catch (e:IllegalArgumentException){
            Log.e("error", e.toString())
        }
    }
}