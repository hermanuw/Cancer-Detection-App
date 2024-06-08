package com.dicoding.asclepius.view

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dicoding.asclepius.R
import com.dicoding.asclepius.database.DatabaseApp
import com.dicoding.asclepius.database.HistoryPrediction
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class ActivityHistory : AppCompatActivity(), HistoryAdapter.OnDeleteClickListener {
    private lateinit var predictionRecyclerView: RecyclerView
    private lateinit var predictionAdapter: HistoryAdapter
    private var predictionList: MutableList<HistoryPrediction> = mutableListOf()
    private lateinit var tvNotFound: TextView
    private lateinit var bottomNavigationView: BottomNavigationView

    companion object{
        const val TAG = "history data"
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)
        bottomNavigationView = findViewById(R.id.menu_bar)
        predictionRecyclerView = findViewById(R.id.rv_history)
        tvNotFound = findViewById(R.id.tv_not_found)
        bottomNavigationView.selectedItemId = R.id.history_menu
        bottomNavigationView.setOnNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    true
                }
                R.id.history_menu -> {
                    true
                }
                else -> false
            }
        }
        predictionRecyclerView = findViewById(R.id.rv_history)
        tvNotFound = findViewById(R.id.tv_not_found)
        predictionAdapter = HistoryAdapter(predictionList)
        predictionAdapter.setOnDeleteClickListener(this)
        predictionRecyclerView.adapter = predictionAdapter
        predictionRecyclerView.layoutManager = LinearLayoutManager(this)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        GlobalScope.launch(Dispatchers.Main) {
            loadPredictionHistoryFromDatabase()
        }
    }
    private fun loadPredictionHistoryFromDatabase() {
        GlobalScope.launch(Dispatchers.Main) {
            val predictions = DatabaseApp.getDatabase(this@ActivityHistory).historyPredictionDao().getAllPrediction()
            Log.d(TAG, "Number of predictions: ${predictions.size}")
            predictionList.clear()
            predictionList.addAll(predictions)
            predictionAdapter.notifyDataSetChanged()
            showOrHideNoHistoryText()
        }
    }
    private fun showOrHideNoHistoryText() {
        if (predictionList.isEmpty()) {
            tvNotFound.visibility = View.VISIBLE
            predictionRecyclerView.visibility = View.GONE
        } else {
            tvNotFound.visibility = View.GONE
            predictionRecyclerView.visibility = View.VISIBLE
        }
    }
    override fun onDeleteClick(position: Int) {
        val prediction = predictionList[position]
        if (prediction.result.isNotEmpty()) {
            GlobalScope.launch(Dispatchers.IO) {
                DatabaseApp.getDatabase(this@ActivityHistory).historyPredictionDao().predictionDelete(prediction)
            }
            predictionList.removeAt(position)
            predictionAdapter.notifyDataSetChanged()
            showOrHideNoHistoryText()
        }
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ResultActivity.REQUEST_HISTORY_UPDATE && resultCode == RESULT_OK) {
            GlobalScope.launch(Dispatchers.Main) {
                loadPredictionHistoryFromDatabase()
            }
        }
    }
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return true
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}