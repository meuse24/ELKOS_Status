package info.meuse24.elkos_status


import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Color
import android.icu.text.SimpleDateFormat
import android.location.Location
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.telephony.SmsManager
import android.telephony.TelephonyManager
import android.util.Log
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.itextpdf.text.Document
import com.itextpdf.text.PageSize
import com.itextpdf.text.Paragraph
import com.itextpdf.text.pdf.PdfWriter
import java.io.File
import java.io.FileOutputStream
import java.util.*
import kotlin.IntArray as IntArray1


private const val PRIVATE_MODE = 0
private const val PREF_NAME = "SETTINGS"

class MainActivity : AppCompatActivity() {
    private val MY_PERMISSIONS_REQUEST_SMS = 1225
    private val BACKUP_FILE_REQUEST_CODE = 15432

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
    private var mute: Boolean = false
    private var fusedLocationClient: FusedLocationProviderClient? = null

    fun test() {
        val file = File(this.getExternalFilesDir(null), "Text.txt")
        file.createNewFile()
        val outputStream: FileOutputStream = FileOutputStream(file, true)

        outputStream.write("Test...".toByteArray())
        outputStream.flush()
        outputStream.close()
    }

    private fun savePdf() {
        val mDoc = Document()
        val mFileName = SimpleDateFormat(
            "yyyyMMdd_HHmmss",
            Locale.getDefault()
        ).format(System.currentTimeMillis()) + ".pdf"
        //val mFilePath = this.getExternalFilesDir(null)!!.absolutePath
        try {
            PdfWriter.getInstance(mDoc, openFileOutput(mFileName, Context.MODE_PRIVATE))
            mDoc.open()
            val mText = timeStamps.getList()
            mDoc.addAuthor("ELKOS-Status.." + packageName)
            mDoc.addCreationDate()
            mDoc.pageSize = PageSize.A4
            mDoc.add(Paragraph("ELKOS/Status-App - Log Export "))
            mDoc.add(
                Paragraph(
                    "Erzeugt am/um:" + SimpleDateFormat(
                        "yyyy-MM-dd/HH:mm:ss",
                        Locale.getDefault()
                    ).format(System.currentTimeMillis())
                )
            )
            mDoc.add(Paragraph("\n[Start]\n$mText\n[Ende]\n"))
            mDoc.close()
            val file = File(filesDir, mFileName)
            if (file.exists()) {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                val uri = FileProvider.getUriForFile(this, "info.meuse24.FileProvider", file)
                intent.setDataAndType(uri, "application/pdf")
                val pm = this.packageManager
                if (intent.resolveActivity(pm) != null) startActivity(intent)
            } else Toast.makeText(
                this,
                "Keine App zur Verarbeitung von PDF-Dateien vorhanden.",
                Toast.LENGTH_LONG
            ).show()
        } catch (e: Exception) {
            //if anything goes wrong causing exception, get and show exception message
            Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun isSimExists(): Boolean {
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

    val PERMISSION_ID = 42 // Berechtigung für Standortbestimmung erfragen
    private fun checkPermission(vararg perm: String): Boolean {
        val havePermissions = perm.toList().all {
            ContextCompat.checkSelfPermission(this, it) ==
                    PackageManager.PERMISSION_GRANTED
        }
        if (!havePermissions) {
            if (perm.toList().any {
                    ActivityCompat.shouldShowRequestPermissionRationale(this, it)
                }
            ) {
                val dialog = AlertDialog.Builder(this)
                    .setTitle("Standortbestimmung")
                    .setMessage("Für die Verortung ist die Berechtigung für die Standortbestimmung erforderlich. Wollen Sie diese erteilen?")
                    .setPositiveButton("Ja") { id, v ->
                        ActivityCompat.requestPermissions(
                            this, perm, PERMISSION_ID
                        )
                    }
                    .setNegativeButton("Keine Berechtigung erteilen.") { id, v -> }

                    .create()
                dialog.show()
            } else {
                ActivityCompat.requestPermissions(this, perm, PERMISSION_ID)
            }
            return false
        }
        return true
    }


    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray1
    ) {
        when (requestCode) {
            MY_PERMISSIONS_REQUEST_SMS -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                } else {
                    infoDialog("Info", "Ohne SMS-Berechtigung funktioniert die App nicht!")
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
        private val mContext: Context = context
        private val mPref: SharedPreferences = pref

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
            items.add(0, this.getTimeStampString() + "  " + st)
            if (items.size > 100) {
                items.removeAt(100)
            }
            save()
        }

        fun getList(): String {
            return items.joinToString(separator = "\n")
        }

        fun getTimeStampString(): String {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd/HH:mm:ss")
            return dateFormat.format(Date())
        }

        fun getStringFromPos(pos: Int): String {
            var mReturn = ""
            if ((pos >= 0) && (pos < items.count())) {
                mReturn = items.get(pos)
            }
            return mReturn
        }

        fun getCount(): Int {
            return items.count()
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
                infoDialog("Info", "ELKOS/Status\n(Copyleft) 2019 Günther Meusburger/LPD.V/A1\nProduced under the GNU Affero General Public License, Sourcecode at: Github meuse24/ELKOS_Status")
            }
            R.id.toolbar_SMS -> {
                val defaultApplication =
                    Settings.Secure.getString(contentResolver, "sms_default_application")
                val intent = packageManager.getLaunchIntentForPackage(defaultApplication)
                if (intent != null) {
                    startActivity(intent)
                }
            }


            //Erzeugt private Log-Datei und öffnet sie mit einer Txt-App
            R.id.toolbar_export -> {
                savePdf()
/*                val FILE_NAME= SimpleDateFormat("yyyyMMddHHmmSS'.txt'").format(Date())
                val t=timeStamps.getList()
                val fos: FileOutputStream = openFileOutput(FILE_NAME, Context.MODE_PRIVATE)
                fos.write(t.toByteArray())

                fos.close()

                val file = File(filesDir,FILE_NAME)
                if (file.exists()){
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    val uri = FileProvider.getUriForFile(this, "info.meuse24.FileProvider", file)
                    intent.setDataAndType(uri, "text/txt")
                    val pm = this.packageManager
                   if (intent.resolveActivity(pm) != null) startActivity(intent)
                } else Toast.makeText(this, "Keine App zur Verarbeitung von Text-Dateien vorhanden.",Toast.LENGTH_LONG).show()*/


            }

        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fusedLocationClient =
            LocationServices.getFusedLocationProviderClient(this)

        val pref = getPreferences(Context.MODE_PRIVATE)

        val sharedPref: SharedPreferences = getSharedPreferences(PREF_NAME, PRIVATE_MODE)
        if (sharedPref.getString("PhoneNumber", "xxx") == "xxx") {

            val editor = sharedPref.edit()
            editor.putString("PhoneNumber", "+436648457008")
            editor.putString("Rufname", "BREGENZ SEKTOR 1")
            editor.putBoolean("Mute", false)
            editor.putBoolean("Location", true)
            editor.apply()
            checkPermission(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.SEND_SMS
            )
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


        listView.onItemClickListener =
            AdapterView.OnItemClickListener { parent, view, position, id ->
                // This is your listview's selected item
                val item = parent.getItemAtPosition(position)
                val eintrag = timeStamps.getStringFromPos(position)
                if (eintrag.contains("Lat:", false)) {
                    val lat = eintrag.substringAfter("Lat:").substringBefore("°")
                    val lon = eintrag.substringAfter("Lon:").substringBefore("°")
                    val uriPattern = "geo:$lat,$lon"
                    Toast.makeText(
                        this, "Öffne Google Maps mit $uriPattern",
                        Toast.LENGTH_LONG
                    ).show()
                    val gmmIntentUri = Uri.parse(uriPattern)
                    val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                    mapIntent.setPackage("com.google.android.apps.maps")
                    if (mapIntent.resolveActivity(packageManager) != null) {
                        startActivity(mapIntent)
                    }
                }
            }

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
        //setColorBtn(colorBtn)

        if (!isSimExists()) {
            val dialog = AlertDialog.Builder(this)
                .setTitle("Fehler")
                .setMessage("Keine gültige SIM-Karte erkannt. App wird beendet!")
                .setPositiveButton("Ok") { _, _ ->
                    finishAffinity()
                }
                .setIcon(R.drawable.ic_action_info)
                .create()
            dialog.show()
        }

        button1.setOnLongClickListener {
            if (sendSMS(button1, "1")) {
                colorBtn = 1
                setColorBtn(colorBtn, true)
                timeStamps.putTimeStamp(getString(R.string.mCode1) + " SMS 1")
                adapter.notifyDataSetChanged()
            }
            true
        }

        button2.setOnLongClickListener {
            if (sendSMS(button2, "2")) {
                colorBtn = 2
                setColorBtn(colorBtn, true)
                timeStamps.putTimeStamp(getString(R.string.mCode2) + " SMS 2")
                adapter.notifyDataSetChanged()
            }
            true
        }

        button3.setOnLongClickListener {
            if (sendSMS(button3, "3")) {
                colorBtn = 3
                setColorBtn(colorBtn, true)
                timeStamps.putTimeStamp(getString(R.string.mCode3) + " SMS 3")
                adapter.notifyDataSetChanged()
            }
            true
        }

        button4.setOnLongClickListener {
            if (sendSMS(button4, "4")) {
                colorBtn = 4
                setColorBtn(colorBtn, true)
                timeStamps.putTimeStamp(getString(R.string.mCode4) + " SMS 4")
                adapter.notifyDataSetChanged()
            }
            true
        }

        button5.setOnLongClickListener {
            if (sendSMS(button5, "6")) {
                colorBtn = 5
                setColorBtn(colorBtn, true)
                timeStamps.putTimeStamp(getString(R.string.mCode5) + " SMS 6")
                adapter.notifyDataSetChanged()
            }
            true
        }

        button6.setOnLongClickListener {
            if (sendSMS(button6, "7")) {
                colorBtn = 6
                setColorBtn(colorBtn, true)
                timeStamps.putTimeStamp(getString(R.string.mCode6) + " SMS 7")
                adapter.notifyDataSetChanged()
            }
            true
        }
    }

    private fun setColorBtn(btnNr: Int, loc: Boolean) {

        var c = Array<Int>(7) { 0 }
        val sharedPref: SharedPreferences = getSharedPreferences(PREF_NAME, PRIVATE_MODE)

        if (sharedPref.getBoolean("Location", true) && loc) {
            if (checkPermission(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            ) {
                fusedLocationClient?.lastLocation?.addOnSuccessListener(this)
                { location: Location? ->
                    // Got last known location. In some rare
                    // situations this can be null.
                    if (location == null) {
                        timeStamps.putTimeStamp("Kein Standort festgestellt!")
                    } else location.apply {
                        // Handle location object
                        Log.e("LOG", location.toString())
                        timeStamps.putTimeStamp("Lat:${location.latitude}° Lon:${location.longitude}°")
                    }
                }
            }
        }

        val editor = sharedPref.edit()
        editor.putInt("colorBtn", btnNr)
        editor.apply()

        if (btnNr > 0) {
            for (i in 1..6) {
                c[i] = Color.parseColor("#00DDFF")
                if (i == btnNr) c[i] = Color.RED
            }
            if (loc) {
                val vibratorService = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= 26) {
                    vibratorService.vibrate(VibrationEffect.createOneShot(150, 10))
                } else {
                    vibratorService.vibrate(150)
                }
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

        private val mContext: Context = context
        private var mTimeStamp: MyTimeStamps = ts

        override fun getCount(): Int {
            return mTimeStamp.getCount()
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
            val positionTextview = rowMain.findViewById<TextView>(R.id.position_textView)
            positionTextview.text = mTimeStamp.getStringFromPos(position)
            return rowMain
        }
    }

    override fun onResume() {
        super.onResume()
        timeStamps.load()
        val sharedPref: SharedPreferences = getSharedPreferences(PREF_NAME, PRIVATE_MODE)
        colorBtn = sharedPref.getInt("colorBtn", 0)
        setColorBtn(colorBtn, false)
        val etRufname = findViewById<TextView>(R.id.txt_Rufname)
        txt_Rufname = sharedPref.getString("Rufname", "Nicht definiert!").toString()
        mute = sharedPref.getBoolean("Mute", false)
        etRufname.text = txt_Rufname
    }

    override fun onPause() {
        super.onPause()
        timeStamps.save()
    }


    fun sendSMS(v: View, t: String): Boolean {
        var rOK = false
        if (ContextCompat.checkSelfPermission(
                v.context,
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
                Toast.makeText(
                    v.context,
                    "Bitte Berechtigung für SMS erteilen!",
                    Toast.LENGTH_SHORT
                ).show()
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
                (sharedPref.getString(
                    "PhoneNumber",
                    "keine SMS-Telefonnummer hinterlegt"
                ).toString())
            smsManager.sendTextMessage(destination, null, text, sentPI, null)
//            Toast.makeText(v.context, "SMS wird gesendet...", Toast.LENGTH_SHORT).show()
            rOK = true
        }
        return rOK
    }

    fun infoDialog(title: String, text: String) {
        val dialog = AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(text)
            .setPositiveButton("Ok") { _, _ -> }
            .setIcon(R.drawable.ic_action_info)
            .create()
        dialog.show()

    }
}
