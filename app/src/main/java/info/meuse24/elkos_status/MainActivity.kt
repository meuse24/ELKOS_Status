package info.meuse24.elkos_status

import android.Manifest
import android.app.Activity
import android.graphics.Color
import android.icu.text.SimpleDateFormat
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.widget.*
import java.text.DateFormat
import java.util.*
import android.text.TextUtils
import java.util.Arrays.asList
import android.content.Context.MODE_PRIVATE
import android.content.pm.PackageManager
import android.telephony.SmsManager
import androidx.core.view.accessibility.AccessibilityEventCompat.setAction
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.app.PendingIntent
import android.content.*
import android.graphics.drawable.ColorDrawable
import android.view.*
import java.util.Arrays.parallelPrefix
import java.util.prefs.Preferences
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
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
                items.add(mPref.getString("l$i", ""))
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
            var mReturn: String = ""
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
        var selectedOpion: String = ""
        when (item?.itemId) {
            R.id.optionen -> {
                selectedOpion = "Optionen"
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
            }
            R.id.info -> {
                selectedOpion = "Info"
                Toast.makeText(this, "ELKOS/STATUS\n(C)2019 G.Meusburger/LPD.V/A1", Toast.LENGTH_SHORT).show()           }

        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val pref = getPreferences(Context.MODE_PRIVATE)

        val sharedPref: SharedPreferences = getSharedPreferences(PREF_NAME, PRIVATE_MODE)
        if (sharedPref.getString("PhoneNumber", "xxx") == "xxx"){
            val editor =sharedPref.edit()
            editor.putString("PhoneNumber","+436648457008")
            editor.apply()
            val toast = Toast.makeText(                this,
                "SMS-Telefonnummer wurde zurückgesetzt. Einstellungen prüfen!",                Toast.LENGTH_SHORT).show()

        }

        timeStamps = MyTimeStamps(this, pref)

        val listView = findViewById<ListView>(R.id.main_listview)
        val adapter = MyCustomAdapter(this, timeStamps)

        listView.adapter = adapter

        sentPI = PendingIntent.getBroadcast(this, 0, Intent(SENT), 0)



        this.registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (resultCode) {
                    Activity.RESULT_OK -> {
                        val toast = Toast.makeText(
                            context,
                            "SMS erfolgreich versendet!",
                            Toast.LENGTH_SHORT
                        )
                        toast.show()
                        timeStamps.putTimeStamp("Nachricht erfolgreich gesendet.")
                        adapter.notifyDataSetChanged()
                    }

                    SmsManager.RESULT_ERROR_NO_SERVICE -> {
                        val toast = Toast.makeText(
                            context,
                            "Fehler! Kein aktives Netzwerk für SMS!",
                            Toast.LENGTH_SHORT
                        )
                        toast.show()
                        timeStamps.putTimeStamp("Fehler! Kein aktives Netzwerk für SMS!")
                        adapter.notifyDataSetChanged()
                    }

                    SmsManager.RESULT_ERROR_RADIO_OFF -> {
                        val toast = Toast.makeText(
                            context,
                            "Fehler! SMS nicht gesendet!",
                            Toast.LENGTH_SHORT
                        )
                        toast.show()
                        timeStamps.putTimeStamp("Fehler! SMS nicht gesendet!")
                        adapter.notifyDataSetChanged()
                    }
                }
            }
        }, IntentFilter(SENT))

        val button1 = findViewById<Button>(R.id.button1)
        val button2 = findViewById<Button>(R.id.button2)
        val button3 = findViewById<Button>(R.id.button3)
        val button4 = findViewById<Button>(R.id.button4)
        val button5 = findViewById<Button>(R.id.button5)
        val button6 = findViewById<Button>(R.id.button6)


        button1.setOnClickListener {
            if (sendSMS(button1, "1")) {
                timeStamps.putTimeStamp(getString(R.string.mCode1) + " SMS 1")
                adapter.notifyDataSetChanged()
            }
        }

        button2.setOnClickListener {
            if (sendSMS(button1, "2")) {
                timeStamps.putTimeStamp(getString(R.string.mCode2) + " SMS 2")
                adapter.notifyDataSetChanged()
            }            //
        }

        button3.setOnClickListener {
            if (sendSMS(button1, "3")) {
                timeStamps.putTimeStamp(getString(R.string.mCode3) + " SMS 3")
                adapter.notifyDataSetChanged()
            }            //
        }

        button4.setOnClickListener {
            if (sendSMS(button1, "4")) {
                timeStamps.putTimeStamp(getString(R.string.mCode4) + " SMS 4")
                adapter.notifyDataSetChanged()
            }            //
        }

        button5.setOnClickListener {
            if (sendSMS(button1, "6")) {
                timeStamps.putTimeStamp(getString(R.string.mCode5) + " SMS 6")
                adapter.notifyDataSetChanged()
            }            //
        }

        button6.setOnClickListener {
            if (sendSMS(button1, "7")) {
                timeStamps.putTimeStamp(getString(R.string.mCode6) + " SMS 7")
                adapter.notifyDataSetChanged()
            }            //
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


            val SENT_SMS_FLAG = "SENT_SMS"
            val DELIVER_SMS_FLAG = "DELIVER_SMS"

            val sharedPref: SharedPreferences = getSharedPreferences(PREF_NAME, PRIVATE_MODE)
            destination=(sharedPref.getString("PhoneNumber", "keine SMS-Telefonnummer hinterlegt"))

            smsManager.sendTextMessage(destination, null, text, sentPI, null)

//            val toast = Toast.makeText(v.context, "SMS wird gesendet...", Toast.LENGTH_SHORT)
//            toast.show()
            rOK = true

        }
        return rOK
    }
}
