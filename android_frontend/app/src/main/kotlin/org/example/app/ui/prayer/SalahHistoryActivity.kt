package org.example.app.ui.prayer

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.example.app.R
import org.example.app.prayer.SalahRepository

class SalahHistoryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_salah_history)

        val repo = SalahRepository(this)
        val adapter = SalahHistoryAdapter(repo)

        val rv = findViewById<RecyclerView>(R.id.rvHistory)
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        adapter.submit(repo.getHistory(lastDays = 14))
    }
}
