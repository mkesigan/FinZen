package com.example.labexam3

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import java.util.*

data class Transaction(
    val title: String,
    val amount: Double,
    val category: String,
    val type: String,
    val date: String
)

class AddTransaction : AppCompatActivity() {
    private var isEdit = false
    private var editIndex = -1

    private lateinit var etTitle: EditText
    private lateinit var etAmount: EditText
    private lateinit var spinnerCategory: Spinner
    private lateinit var tvDate: TextView
    private lateinit var rgType: RadioGroup
    private lateinit var btnSave: Button
    private lateinit var rbIncome: RadioButton
    private lateinit var rbExpense: RadioButton

    private var selectedDate: String = ""

    private val incomeCategories = listOf(
        "Select Category","Salary", "Freelance", "Business", "Interest", "Dividends", "Main Income", "Others"
    )

    private val expenseCategories = listOf(
        "Select Category","Food", "Transport", "Bills", "Entertainment", "Shopping", "Health", "Travel", "Education","Rent", "Others"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_transaction)

        val back= findViewById<ImageView>(R.id.leftArrow)

        back.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
            finish()
        }


        etTitle = findViewById(R.id.etTitle)
        etAmount = findViewById(R.id.etAmount)
        spinnerCategory = findViewById(R.id.spinnerCategory)
        tvDate = findViewById(R.id.tvDate)
        rgType = findViewById(R.id.rgType)
        btnSave = findViewById(R.id.btnSave)
        rbIncome = findViewById(R.id.rbIncome)
        rbExpense = findViewById(R.id.rbExpense)

        // Date picker
        tvDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(this, { _, year, month, day ->
                selectedDate = "%04d-%02d-%02d".format(year, month + 1, day)
                tvDate.text = selectedDate
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        // Radio button logic to change category list
        rgType.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.rbIncome) {
                updateCategorySpinner("Income")
            } else {
                updateCategorySpinner("Expense")
            }
        }

        // Save button
        btnSave.setOnClickListener {
            saveTransaction()
        }

        // Handle edit mode
        isEdit = intent.getBooleanExtra("isEdit", false)
        if (isEdit) {
            editIndex = intent.getIntExtra("editIndex", -1)
            val json = intent.getStringExtra("transaction")
            val transaction = Gson().fromJson(json, Transaction::class.java)

            etTitle.setText(transaction.title)
            etAmount.setText(transaction.amount.toString())
            selectedDate = transaction.date
            tvDate.text = transaction.date

            // Set radio and category
            if (transaction.type == "Income") rbIncome.isChecked = true else rbExpense.isChecked = true
            updateCategorySpinner(transaction.type)
            spinnerCategory.setSelection(getCategoryList(transaction.type).indexOf(transaction.category))
        } else {
            rbIncome.isChecked = true
            updateCategorySpinner("Income")
        }
    }

    private fun getCategoryList(type: String): List<String> {
        return if (type == "Income") incomeCategories else expenseCategories
    }

    private fun updateCategorySpinner(type: String) {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, getCategoryList(type))
        spinnerCategory.adapter = adapter
    }

    private fun saveTransaction() {
        val title = etTitle.text.toString().trim()
        val amount = etAmount.text.toString().toDoubleOrNull()
        val selectedTypeId = rgType.checkedRadioButtonId
        val type = if (selectedTypeId == R.id.rbIncome) "Income" else "Expense"
        val category = spinnerCategory.selectedItem.toString()

        if (title.isBlank() || amount == null || selectedDate.isBlank() || selectedTypeId == -1) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val transaction = Transaction(title, amount, category, type, selectedDate)

        val list = SharedPrefManager.getTransactions(this)
        if (isEdit && editIndex != -1) {
            list[editIndex] = transaction
            Toast.makeText(this, "Transaction updated", Toast.LENGTH_SHORT).show()
        } else {
            list.add(transaction)
            Toast.makeText(this, "Transaction saved", Toast.LENGTH_SHORT).show()
        }
        SharedPrefManager.saveTransactions(this, list)
        finish()
    }
}
