package com.example.labexam3

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

class Settings : AppCompatActivity() {

    private lateinit var etCurrency: EditText
    private lateinit var etBudget: EditText
    private lateinit var btnSaveSettings: Button
    private lateinit var btnBackup: Button
    private lateinit var btnRestore: Button

    private val PREF_NAME = "app_settings"
    private val KEY_CURRENCY = "currency"
    private val KEY_BUDGET = "monthly_budget"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val back = findViewById<ImageView>(R.id.leftArrow)

        back.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
            finish()
        }

        etCurrency = findViewById(R.id.etCurrency)
        etBudget = findViewById(R.id.etBudget)
        btnSaveSettings = findViewById(R.id.btnSaveSettings)
        btnBackup = findViewById(R.id.btnBackup)
        btnRestore = findViewById(R.id.btnRestore)

        // Load existing values
        val prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        etCurrency.setText(prefs.getString(KEY_CURRENCY, "Rs."))
        etBudget.setText(prefs.getFloat(KEY_BUDGET, 0f).toString())

        // Save settings when user clicks save
        btnSaveSettings.setOnClickListener {
            val currency = etCurrency.text.toString()
            val budget = etBudget.text.toString().toFloatOrNull()

            if (currency.isBlank() || budget == null) {
                Toast.makeText(this, "Please fill valid values", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val editor = prefs.edit()
            editor.putString(KEY_CURRENCY, currency)
            editor.putFloat(KEY_BUDGET, budget)
            editor.apply()

            Toast.makeText(this, "Settings Saved", Toast.LENGTH_SHORT).show()
            finish()
        }

        // Backup button click
        btnBackup.setOnClickListener {
            backupTransactions()
        }

        // Restore button click
        btnRestore.setOnClickListener {
            restoreTransactions()
        }
    }

    // Backup transactions to internal storage
    private fun backupTransactions() {
        val transactions = SharedPrefManager.getTransactions(this)
        val json = Gson().toJson(transactions)

        try {
            val file = File(filesDir, "backup.json")
            file.writeText(json)
            Toast.makeText(this, "Backup successful: ${file.absolutePath}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Backup failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Restore transactions from internal storage
    private fun restoreTransactions() {
        try {
            val file = File(filesDir, "backup.json")
            if (!file.exists()) {
                Toast.makeText(this, "No backup file found", Toast.LENGTH_SHORT).show()
                return
            }

            val json = file.readText()
            val type = object : TypeToken<List<Transaction>>() {}.type
            val transactionList: List<Transaction> = Gson().fromJson(json, type)

            // Restore transactions into SharedPreferences
            SharedPrefManager.saveTransactions(this, transactionList)
            Toast.makeText(this, "Restore successful", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            Toast.makeText(this, "Restore failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
