package com.example.mychat1

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

class ChannelsActivity : AppCompatActivity() {

    private lateinit var lvChannels: ListView
    private val channels = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_channels)

        lvChannels = findViewById(R.id.lvChannels)

        lvChannels.setOnItemClickListener { _, _, position, _ ->
            val intent = Intent(this, ChatActivity::class.java)
            intent.putExtra("CHANNEL_NAME", channels[position])
            startActivity(intent)
        }

        fetchChannels()
    }

    private fun fetchChannels() {
        Thread {
            try {
                val token = getSharedPreferences("MyChat", MODE_PRIVATE).getString("token", "")
                val url = URL("http://mychat.fgontier.fr/channels")
                val conn = url.openConnection() as HttpURLConnection
                conn.setRequestProperty("Authorization", "Bearer $token")

                if (conn.responseCode == 200) {
                    val response = conn.inputStream.bufferedReader().use { it.readText() }
                    val jsonArray = JSONArray(response)
                    channels.clear()
                    for (i in 0 until jsonArray.length()) {
                        channels.add(jsonArray.getString(i))
                    }

                    runOnUiThread {
                        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, channels)
                        lvChannels.adapter = adapter
                    }
                }
                conn.disconnect()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }
}