package com.example.labexam3

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.icu.util.Calendar
import android.os.Build
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.gson.Gson

class HomeActivity : AppCompatActivity() {
    private lateinit var transactionListLayout: LinearLayout
    private lateinit var btnExpense: Button
    private lateinit var btnIncome: Button
    private lateinit var tvIncome: TextView
    private lateinit var tvExpense: TextView
    private lateinit var tvBalance: TextView
    private lateinit var ivNotification: ImageView

    private val PREF_NAME = "notification_prefs"
    private val KEY_NOTIFICATION_ENABLED = "daily_reminder_enabled"
    private var allTransactions = mutableListOf<Transaction>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)


        // Footer Navigation
        findViewById<LinearLayout>(R.id.footerAdd).setOnClickListener {
            startActivity(Intent(this, AddTransaction::class.java))
        }
        findViewById<LinearLayout>(R.id.footerReports).setOnClickListener {
            startActivity(Intent(this, Reports::class.java))
        }
        findViewById<LinearLayout>(R.id.footerSettings).setOnClickListener {
            startActivity(Intent(this, Settings::class.java))
        }



        // UI references
        transactionListLayout = findViewById(R.id.transactionList)
        btnExpense = findViewById(R.id.btnExpense)
        btnIncome = findViewById(R.id.btnIncome)
        tvIncome = findViewById(R.id.tvIncome)
        tvExpense = findViewById(R.id.tvExpense)
        tvBalance = findViewById(R.id.tvBalance)
        ivNotification = findViewById(R.id.ivNotification)



        // Toggle buttons
        btnExpense.setOnClickListener { displayTransactions("Expense") }
        btnIncome.setOnClickListener { displayTransactions("Income") }



        // Load notification preference
        val prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val isReminderEnabled = prefs.getBoolean(KEY_NOTIFICATION_ENABLED, false)
        updateNotificationIcon(isReminderEnabled)



        // Toggle on icon click
        ivNotification.setOnClickListener {
            val prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            val isCurrentlyEnabled = prefs.getBoolean(KEY_NOTIFICATION_ENABLED, false)

            val message = if (isCurrentlyEnabled) {
                "Do you want to turn OFF daily notifications?"
            } else {
                "Do you want to turn ON daily notifications?"
            }

            val builder = android.app.AlertDialog.Builder(this)
            builder.setTitle("Notification Reminder")
            builder.setMessage(message)

            builder.setPositiveButton("Yes") { dialog, _ ->
                val updated = !isCurrentlyEnabled
                prefs.edit().putBoolean(KEY_NOTIFICATION_ENABLED, updated).apply()
                updateNotificationIcon(updated)

                if (updated) {
                    scheduleDailyReminder()
                    Toast.makeText(this, "🔔 Daily reminder ON", Toast.LENGTH_SHORT).show()
                } else {
                    cancelDailyReminder()
                    Toast.makeText(this, "🔕 Daily reminder OFF", Toast.LENGTH_SHORT).show()
                }

                dialog.dismiss()
            }

            builder.setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }

            builder.show()
        }


        // Request notification permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                101
            )
        }

        // Update summary and reminder
        updateSummary()
        if (isReminderEnabled) {
            scheduleDailyReminder()
        }
    }

    override fun onResume() {
        super.onResume()
        allTransactions = SharedPrefManager.getTransactions(this).toMutableList()
        updateSummary()
        displayTransactions("Expense")
    }



    private fun displayTransactions(type: String) {
        transactionListLayout.removeAllViews()

        val filtered = allTransactions.filter { it.type == type }
        val marginPx = (8 * resources.displayMetrics.density + 0.5f).toInt()

        for (transaction in filtered) {
            val view = layoutInflater.inflate(R.layout.activity_item_transaction, null)
            val layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            layoutParams.setMargins(0, marginPx, 0, marginPx)
            view.layoutParams = layoutParams

            view.findViewById<TextView>(R.id.tvTitle).text = "Title : ${transaction.title}"
            view.findViewById<TextView>(R.id.tvDate).text = "Date : ${transaction.date}"
            view.findViewById<TextView>(R.id.tvAmount).text = "Amount : Rs.${transaction.amount}"

            view.findViewById<ImageButton>(R.id.btnEdit).setOnClickListener {
                val intent = Intent(this, AddTransaction::class.java)
                intent.putExtra("isEdit", true)
                intent.putExtra("editIndex", allTransactions.indexOf(transaction))
                intent.putExtra("transaction", Gson().toJson(transaction))
                startActivity(intent)
            }

            view.findViewById<ImageButton>(R.id.btnDelete).setOnClickListener {
                val updatedList = SharedPrefManager.getTransactions(this).toMutableList()
                val removeIndex = updatedList.indexOf(transaction)
                if (removeIndex != -1) {
                    updatedList.removeAt(removeIndex)
                    SharedPrefManager.saveTransactions(this, updatedList)
                    allTransactions = updatedList
                    updateSummary()
                    displayTransactions(type)
                }
            }

            transactionListLayout.addView(view)
        }
    }



    private fun updateSummary() {
        val incomeTotal = allTransactions.filter { it.type == "Income" }.sumOf { it.amount }
        val expenseTotal = allTransactions.filter { it.type == "Expense" }.sumOf { it.amount }
        val balance = incomeTotal - expenseTotal

        val prefs = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val currency = prefs.getString("currency", "Rs.")
        val budget = prefs.getFloat("monthly_budget", 0f)

        tvIncome.text = "$currency ${"%.2f".format(incomeTotal)}"
        tvExpense.text = "$currency ${"%.2f".format(expenseTotal)}"
        tvBalance.text = "$currency ${"%.2f".format(balance)}"

        if (expenseTotal > budget) {
            Toast.makeText(this, "⚠️ You've exceeded your monthly budget!", Toast.LENGTH_LONG).show()
            showBudgetExceededNotification()
        }
    }


    private fun updateNotificationIcon(enabled: Boolean) {
        ivNotification.setColorFilter(
            ContextCompat.getColor(this,
                if (enabled) R.color.red else android.R.color.darker_gray
            )
        )
    }


    private fun scheduleDailyReminder() {
        val intent = Intent(this, DailyReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 20)  //night 8'clock
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }
//        val calendar = Calendar.getInstance().apply {
//            add(Calendar.MINUTE, 2)  // Set reminder for 2 minutes from now
//        }


        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }


    private fun cancelDailyReminder() {
        val intent = Intent(this, DailyReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
    }



    private fun showBudgetExceededNotification() {
        val channelId = "budget_alert_channel"
        val channelName = "Budget Alerts"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH)
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
            == PackageManager.PERMISSION_GRANTED) {

            val notification = NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Budget Alert")
                .setContentText("⚠️ You've exceeded your monthly budget!")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()

            NotificationManagerCompat.from(this).notify(1001, notification)
        }
    }
}
