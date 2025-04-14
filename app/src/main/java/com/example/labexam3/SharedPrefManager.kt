package com.example.labexam3


import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken


object SharedPrefManager {
    private const val PREF_NAME = "transactions_pref"
    private const val KEY_TRANSACTIONS = "transactions"

    fun saveTransactions(context: Context, list: List<Transaction>) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        val json = Gson().toJson(list)
        editor.putString(KEY_TRANSACTIONS, json)
        editor.apply()
    }

    fun getTransactions(context: Context): MutableList<Transaction> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_TRANSACTIONS, null)
        return try {
            if (json != null) {
                val type = object : TypeToken<List<Transaction>>() {}.type
                Gson().fromJson(json, type)
            } else mutableListOf()
        } catch (e: Exception) {
            e.printStackTrace()
            mutableListOf()
        }

    }

}
