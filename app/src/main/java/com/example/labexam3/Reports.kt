package com.example.labexam3

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import android.graphics.Color
import android.view.View
import android.widget.ImageView


class Reports: AppCompatActivity() {

    private lateinit var reportList: LinearLayout
    private lateinit var btnIncome: Button
    private lateinit var btnExpense: Button
    private var allTransactions: List<Transaction> = listOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reports)

        val back= findViewById<ImageView>(R.id.leftArrow)

        back.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
            finish()
        }

        reportList = findViewById(R.id.reportList)
        btnIncome = findViewById(R.id.btnReportIncome)
        btnExpense = findViewById(R.id.btnReportExpense)

        // Load transactions from SharedPreferences
        allTransactions = SharedPrefManager.getTransactions(this)

        btnIncome.setOnClickListener { showReport("Income") }
        btnExpense.setOnClickListener { showReport("Expense") }

        showReport("Expense") // default
    }

    private fun showReport(type: String) {
        reportList.removeAllViews()

        //currency type added
        val prefs = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val currency = prefs.getString("currency", "Rs.")

        val filtered = allTransactions.filter { it.type == type }

        // Group by category and sum totals
        val grouped = filtered.groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }

        if (grouped.isEmpty()) {
            val tv = TextView(this)
            tv.text = "No $type transactions found."
            tv.setPadding(0, 16, 0, 16)
            tv.textSize = 16f
            tv.setTextColor(Color.WHITE)
            reportList.addView(tv)
            return
        }

        for ((category, total) in grouped) {
            val container = LinearLayout(this)
            container.orientation = LinearLayout.VERTICAL
            container.setPadding(24, 16, 24, 16)
            container.setBackgroundResource(R.drawable.popup_box)

            val label = TextView(this)
            label.text = "Category: $category"
            label.setTextColor(Color.BLACK)
            label.setTextSize(16f)
            label.setPadding(0, 0, 0, 6)

            val value = TextView(this)
            value.text = "Total: $currency %.2f".format(total)
            value.setTextColor(Color.DKGRAY)
            value.setTextSize(16f)

            container.setMargins(0, 12, 0, 12)
            container.addView(label)
            container.addView(value)

            reportList.addView(container)
        }
    }

    // Extension to set margins
    fun View.setMargins(left: Int, top: Int, right: Int, bottom: Int) {
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(left, top, right, bottom)
        this.layoutParams = params
    }

}
