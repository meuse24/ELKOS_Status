package info.meuse24.elkos_status


import android.Manifest
import android.app.Activity
import android.graphics.Color
import android.icu.text.SimpleDateFormat
import androidx.appcompat.app.AppCompatActivity
import android.widget.*
import java.util.*
import android.content.pm.PackageManager
import android.telephony.SmsManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.app.PendingIntent
import android.content.*
import android.os.*
import android.view.*
import kotlin.IntArray as IntArray1
import android.telephony.TelephonyManager
import android.content.Intent
import android.provider.Settings
import android.R.attr.start
import android.media.MediaPlayer
import androidx.core.app.ComponentActivity.ExtraData
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T



private var PRIVATE_MODE = 0
private val PREF_NAME = "SETTINGS"

class MainActivity : AppCompatActivity() {
    val MY_PERMISSIONS_REQUEST_SMS = 1225

    companion object {
        const val RC_CODE = 101
        const val TAG = "debug_MainActivity"
        const val SENT = "SMS_SENT"
        const val DELIVERED = "SMS_DELIVERED"
    }

    private lateinit var sentPI: PendingIntent
    private lateinit var timeStamps: MyTimeStamps
    private lateinit var button1: Button
    private lateinit var button2: Button
    private lateinit var button3: Button
    private lateinit var button4: Button
    private lateinit var button5: Button
    private lateinit var button6: Button
    private lateinit var txt_Rufname: String
    private var colorBtn: Int = 0
    private var mute:Boolean = false

    fun isSimExists(): Boolean {
        val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val SIM_STATE = telephonyManager.simState

        if (SIM_STATE == TelephonyManager.SIM_STATE_READY)
            return true
        else {
            // we can inform user about sim state
            when (SIM_STATE) {
                TelephonyManager.SIM_STATE_ABSENT, TelephonyManager.SIM_STATE_NETWORK_LOCKED, TelephonyManager.SIM_STATE_PIN_REQUIRED, TelephonyManager.SIM_STATE_PUK_REQUIRED, TelephonyManager.SIM_STATE_UNKNOWN -> {
                }
            }
            return false
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray1
    ) {
        when (requestCode) {
            MY_PERMISSIONS_REQUEST_SMS -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return
            }
        }// other 'case' lines to check for other
        // permissions this app might request.
    }


    //Klasse für die Liste der LogDatensätze
    private class MyTimeStamps(context: Context, pref: SharedPreferences) {
        private val items = ArrayList<String>()
        private val mContext: Context
        private val mPref: SharedPreferences

        init {
            mContext = context
            mPref = pref
        }

        fun save() {
            val editor = mPref.edit()
            editor.putInt("Size", items.size)
            var i = 0
            for (l in items) {
                editor.putString("l$i", l)
                i++
            }
            editor.apply()
        }

        fun load() {
            items.clear()
            val c = mPref.getInt("Size", 0)
            var i = 0
            while (i < c) {
                items.add(mPref.getString("l$i", "").toString())
                i++
            }
        }

        fun putTimeStamp(st: String) {
            items.add(0, this.getTimeStampString() + ": " + st)
            if (items.size > 40) {
                items.removeAt(40)
            }
            save()
        }

        fun getList(): String {
            return items.joinToString(separator = ", ")
        }

        fun getTimeStampString(): String {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
            return dateFormat.format(Date())
        }

        fun getStringFromPos(pos: Int): String {
            var mReturn = ""
            if ((pos >= 0) && (pos < items.count())) {
                mReturn = items.get(pos)
            }
            return mReturn
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.optionen -> {

                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
            }
            R.id.info -> {
                Toast.makeText(
                    this,
                    "ELKOS/STATUS\n(C)2019 Günther Meusburger/LPD.V/A1",
                    Toast.LENGTH_SHORT
                ).show()
            }
            R.id.toolbar_SMS -> {
                val defaultApplication =
                    Settings.Secure.getString(contentResolver, "sms_default_application")
                val pm = packageManager
                val intent = pm.getLaunchIntentForPackage(defaultApplication)
                if (intent != null) {
                    startActivity(intent)
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val pref = getPreferences(Context.MODE_PRIVATE)

        val sharedPref: SharedPreferences = getSharedPreferences(PREF_NAME, PRIVATE_MODE)
        if (sharedPref.getString("PhoneNumber", "xxx") == "xxx") {
            val editor = sharedPref.edit()
            editor.putString("PhoneNumber", "+436648457008")
            editor.putString("Rufname", "BREGENZ SEKTOR 1")
            editor.putBoolean("Mute", false)
            editor.apply()
            Toast.makeText(
                this,
                "SMS-Telefonnummer wurde zurückgesetzt. Einstellungen prüfen!",
                Toast.LENGTH_SHORT
            ).show()
        }
        txt_Rufname = sharedPref.getString("Rufname", "BREGENZ SEKTOR 1").toString()
        mute = sharedPref.getBoolean("Mute", false)
        val etRufname = findViewById<TextView>(R.id.txt_Rufname)
        etRufname.text = txt_Rufname

        timeStamps = MyTimeStamps(this, pref)

        val listView = findViewById<ListView>(R.id.main_listview)
        val adapter = MyCustomAdapter(this, timeStamps)
        listView.adapter = adapter

        sentPI = PendingIntent.getBroadcast(this, 0, Intent(SENT), 0)

        this.registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (resultCode) {
                    Activity.RESULT_OK -> {
                        timeStamps.putTimeStamp("Nachricht erfolgreich gesendet.")
                        adapter.notifyDataSetChanged()
                        if (!mute) {
                            val mp = MediaPlayer.create(applicationContext, R.raw.beep)
                            mp.start()
                        }
                    }

                    SmsManager.RESULT_ERROR_NO_SERVICE -> {
                        timeStamps.putTimeStamp("Fehler! Kein aktives Netzwerk für SMS!")
                        adapter.notifyDataSetChanged()
                    }

                    SmsManager.RESULT_ERROR_RADIO_OFF -> {
                        timeStamps.putTimeStamp("Fehler! SMS nicht gesendet!")
                        adapter.notifyDataSetChanged()
                    }
                }
            }
        }, IntentFilter(SENT))

        button1 = findViewById<Button>(R.id.button1)
        button2 = findViewById<Button>(R.id.button2)
        button3 = findViewById<Button>(R.id.button3)
        button4 = findViewById<Button>(R.id.button4)
        button5 = findViewById<Button>(R.id.button5)
        button6 = findViewById<Button>(R.id.button6)

        colorBtn = sharedPref.getInt("colorBtn", 0)
        setColorBtn(colorBtn)

        if (!isSimExists()) {
            val toast = Toast.makeText(
                this,
                "Keine SMS-Funktionalität!",
                Toast.LENGTH_SHORT
            )
            toast.show()
        }

        button1.setOnLongClickListener {
            if (sendSMS(button1, "1")) {
                colorBtn = 1
                setColorBtn(colorBtn)
                timeStamps.putTimeStamp(getString(R.string.mCode1) + " SMS 1")
                adapter.notifyDataSetChanged()
            }
            true
        }

        button2.setOnLongClickListener {
            if (sendSMS(button2, "2")) {
                colorBtn = 2
                setColorBtn(colorBtn)
                timeStamps.putTimeStamp(getString(R.string.mCode2) + " SMS 2")
                adapter.notifyDataSetChanged()
            }
            true
        }

        button3.setOnLongClickListener {
            if (sendSMS(button3, "3")) {
                colorBtn = 3
                setColorBtn(colorBtn)
                timeStamps.putTimeStamp(getString(R.string.mCode3) + " SMS 3")
                adapter.notifyDataSetChanged()
            }
            true
        }

        button4.setOnLongClickListener {
            if (sendSMS(button4, "4")) {
                colorBtn = 4
                setColorBtn(colorBtn)
                timeStamps.putTimeStamp(getString(R.string.mCode4) + " SMS 4")
                adapter.notifyDataSetChanged()
            }
            true
        }

        button5.setOnLongClickListener {
            if (sendSMS(button5, "6")) {
                colorBtn = 5
                setColorBtn(colorBtn)
                timeStamps.putTimeStamp(getString(R.string.mCode5) + " SMS 6")
                adapter.notifyDataSetChanged()
            }
            true
        }

        button6.setOnLongClickListener {
            if (sendSMS(button6, "7")) {
                colorBtn = 6
                setColorBtn(colorBtn)
                timeStamps.putTimeStamp(getString(R.string.mCode6) + " SMS 7")
                adapter.notifyDataSetChanged()
            }
            true
        }

    }

    private fun setColorBtn(btnNr: Int) {

        var c = Array<Int>(7) { 0 }
        val sharedPref: SharedPreferences = getSharedPreferences(PREF_NAME, PRIVATE_MODE)
        val editor = sharedPref.edit()
        editor.putInt("colorBtn", btnNr)
        editor.apply()

        if (btnNr > 0) {
            for (i in 1..6) {
                c[i] = Color.parseColor("#00DDFF")
                if (i == btnNr) c[i] = Color.RED

            }
            val vibratorService = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= 26) {
                vibratorService.vibrate(VibrationEffect.createOneShot(150, 10))
            } else {
                vibratorService.vibrate(150)
            }
            button1.setBackgroundColor(c[1])
            button2.setBackgroundColor(c[2])
            button3.setBackgroundColor(c[3])
            button4.setBackgroundColor(c[4])
            button5.setBackgroundColor(c[5])
            button6.setBackgroundColor(c[6])
        }

    }

    private class MyCustomAdapter(context: Context, ts: MyTimeStamps) : BaseAdapter() {

        private val mContext: Context
        private var mTimeStamp: MyTimeStamps

        init {
            mContext = context
            mTimeStamp = ts
        }

        override fun getCount(): Int {
            return 35
        }

        override fun getItem(position: Int): Any {
            return ""
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val layoutInflater = LayoutInflater.from(mContext)
            val rowMain = layoutInflater.inflate(R.layout.row_main, parent, false)
            val position_textView = rowMain.findViewById<TextView>(R.id.position_textView)
            position_textView.text = mTimeStamp.getStringFromPos(position)
            return rowMain
        }
    }

    override fun onResume() {
        super.onResume()
        timeStamps.load()
        val sharedPref: SharedPreferences = getSharedPreferences(PREF_NAME, PRIVATE_MODE)
        colorBtn = sharedPref.getInt("colorBtn", 0)
        setColorBtn(colorBtn)
        val etRufname = findViewById<TextView>(R.id.txt_Rufname)
        txt_Rufname = sharedPref.getString("Rufname", "default").toString()
        mute = sharedPref.getBoolean("Mute", false)
        etRufname.text = txt_Rufname
    }

    override fun onPause() {
        super.onPause()
        timeStamps.save()
    }


    fun sendSMS(v: View, t: String): Boolean {
        var rOK: Boolean = false
        if (ContextCompat.checkSelfPermission(
                v.getContext(),
                Manifest.permission.SEND_SMS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Permission is not granted
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this@MainActivity,
                    Manifest.permission.SEND_SMS
                )
            ) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                val toast = Toast.makeText(
                    v.context,
                    "Bitte Berechtigung für SMS erteilen!",
                    Toast.LENGTH_SHORT
                )
                toast.show()
            } else {
                ActivityCompat.requestPermissions(
                    this@MainActivity,
                    arrayOf(Manifest.permission.SEND_SMS),
                    MY_PERMISSIONS_REQUEST_SMS
                )

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        } else {
            val smsManager = SmsManager.getDefault()
            var destination = "+4368120261353"
            val text = t

//            val SENT_SMS_FLAG = "SENT_SMS"
//            val DELIVER_SMS_FLAG = "DELIVER_SMS"

            val sharedPref: SharedPreferences = getSharedPreferences(PREF_NAME, PRIVATE_MODE)
            destination =
                (sharedPref.getString("PhoneNumber", "keine SMS-Telefonnummer hinterlegt").toString())

            smsManager.sendTextMessage(destination, null, text, sentPI, null)

//            val toast = Toast.makeText(v.context, "SMS wird gesendet...", Toast.LENGTH_SHORT)
//            toast.show()
            rOK = true

        }
        return rOK
    }
}
