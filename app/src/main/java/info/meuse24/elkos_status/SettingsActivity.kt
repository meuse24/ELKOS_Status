package info.meuse24.elkos_status

import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import java.util.regex.Pattern


class SettingsActivity : AppCompatActivity() {
    private var PRIVATE_MODE = 0
    private val PREF_NAME = "SETTINGS"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        val etPhoneNumber = findViewById<EditText>(R.id.editText_PhoneNumber)
        val etCode = findViewById<EditText>(R.id.editText_Code)
        val btnSave = findViewById<Button>(R.id.btn_Save)

        val sharedPref: SharedPreferences = getSharedPreferences(PREF_NAME, PRIVATE_MODE)
        etPhoneNumber.setText(sharedPref.getString("PhoneNumber", "+436648457008"))

        btnSave.setOnClickListener {
            if (etCode.text.toString() == "01081966") {
                if (etPhoneNumber.text.toString().matches("^[0-9+\\(\\)#\\.\\s\\/ext-]+\$".toRegex())) {

                    val editor = sharedPref.edit()
                    editor.putString("PhoneNumber", etPhoneNumber.text.toString())
                    editor.apply()
                    Toast.makeText(this, "Einstellungen wurden gespeichert.", Toast.LENGTH_SHORT)
                        .show()
                    this.finish()
                } else {
                    Toast.makeText(
                        this,
                        "Bitte eine gültige Telefonnummer eingeben.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                Toast.makeText(
                    this,
                    "Code falsch!\nEinstellungen wurden nicht gespeichert.",
                    Toast.LENGTH_SHORT
                ).show()
                this.finish()
            }

        }

    }
}
