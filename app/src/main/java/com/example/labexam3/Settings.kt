package com.example.labexam3

import androidx.appcompat.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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
        btnSaveSettings.setOnClickListener {
            val prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

            val currentCurrency = prefs.getString(KEY_CURRENCY, null)
            val currentBudget = prefs.getFloat(KEY_BUDGET, -1f)

            val inputCurrency = etCurrency.text.toString().trim()
            val inputBudgetStr = etBudget.text.toString().trim()
            val inputBudget = inputBudgetStr.toFloatOrNull()

            // First time:both must be filled
            val isFirstTime = currentCurrency == null || currentBudget == -1f

            if (isFirstTime) {
                if (inputCurrency.isBlank() || inputBudget == null) {
                    Toast.makeText(this, "Please fill both currency and budget", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }

            // If both fields are empty
            if (inputCurrency.isBlank() && inputBudgetStr.isBlank()) {
                Toast.makeText(this, "Please fill at least one field", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Confirm before saving
            MaterialAlertDialogBuilder(this)
                .setTitle("Save Settings")
                .setMessage("Do you want to save the updated settings?")
                .setPositiveButton("Yes") { _, _ ->
                    val editor = prefs.edit()

                    if (inputCurrency.isNotBlank()) {
                        editor.putString(KEY_CURRENCY, inputCurrency)
                    }

                    if (inputBudget != null) {
                        editor.putFloat(KEY_BUDGET, inputBudget)
                    }

                    editor.apply()
                    Toast.makeText(this, "Settings Saved", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .setNegativeButton("Cancel") { _, _ ->
                    // Reset fields
                    etCurrency.setText(currentCurrency ?: "")
                    etBudget.setText(if (currentBudget == -1f) "" else currentBudget.toString())
                }
                .show()
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
        AlertDialog.Builder(this)
            .setTitle("Backup Transactions")
            .setMessage("Do you want to back up all your transactions to internal storage?")
            .setPositiveButton("Yes") { _, _ ->
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
            .setNegativeButton("Cancel", null)
            .show()
    }


    // Restore transactions from internal storage
    private fun restoreTransactions() {
        AlertDialog.Builder(this)
            .setTitle("Restore Transactions")
            .setMessage("This will replace your current data. Continue?")
            .setPositiveButton("Yes") { _, _ ->
                try {
                    val file = File(filesDir, "backup.json")
                    if (!file.exists()) {
                        Toast.makeText(this, "No backup file found", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }

                    val json = file.readText()
                    val type = object : TypeToken<List<Transaction>>() {}.type
                    val transactionList: List<Transaction> = Gson().fromJson(json, type)

                    SharedPrefManager.saveTransactions(this, transactionList)
                    Toast.makeText(this, "Restore successful", Toast.LENGTH_SHORT).show()

                } catch (e: Exception) {
                    Toast.makeText(this, "Restore failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

}
