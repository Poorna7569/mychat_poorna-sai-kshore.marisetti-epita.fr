package com.example.mychat1

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import android.widget.BaseAdapter
import android.widget.TextView
import android.view.View
import android.view.ViewGroup
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class Channel(
    val id: String,
    val name: String,
    val description: String = "Channel discussion"
)

class ChannelAdapter(
    private val context: android.content.Context,
    private val channels: List<Channel>
) : BaseAdapter() {

    override fun getCount(): Int = channels.size

    override fun getItem(position: Int): Channel = channels[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.list_item_channel, parent, false)
        
        val channel = channels[position]
        val tvChannelName = view.findViewById<TextView>(R.id.tvChannelName)
        val tvChannelDesc = view.findViewById<TextView>(R.id.tvChannelDesc)
        val tvMemberCount = view.findViewById<TextView>(R.id.tvMemberCount)
        
        tvChannelName.text = "# ${channel.name}"
        tvChannelDesc.text = channel.description
        tvMemberCount.text = "" // Could be updated if API provides it
        
        return view
    }
}

class ChannelsActivity : AppCompatActivity() {

    private lateinit var lvChannels: ListView
    private val channels = mutableListOf<Channel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_channels)

        lvChannels = findViewById(R.id.lvChannels)
        val btnNewChannel = findViewById<Button>(R.id.btnNewChannel)

        lvChannels.setOnItemClickListener { _, _, position, _ ->
            val channel = channels[position]
            Log.d("ChannelsActivity", "Opening channel: ${channel.name} (id: ${channel.id})")
            val intent = Intent(this, ChatActivity::class.java)
            intent.putExtra("CHANNEL_ID", channel.id)
            intent.putExtra("CHANNEL_NAME", channel.name)
            startActivity(intent)
        }

        btnNewChannel.setOnClickListener {
            val dialogView = android.widget.EditText(this)
            android.app.AlertDialog.Builder(this)
                .setTitle("New Channel")
                .setMessage("Enter channel name")
                .setView(dialogView)
                .setPositiveButton("Create") { _, _ ->
                    val channelName = dialogView.text.toString().trim()
                    if (channelName.isNotEmpty()) {
                        createChannel(channelName)
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
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

                val responseCode = conn.responseCode
                if (responseCode == 200) {
                    val response = conn.inputStream.bufferedReader().use { it.readText() }
                    Log.d("ChannelsActivity", "Channels response: $response")
                    val jsonArray = JSONArray(response)
                    channels.clear()
                    for (i in 0 until jsonArray.length()) {
                        val item = jsonArray.get(i)
                        when (item) {
                            is JSONObject -> {
                                // Prefer 'id' if available, otherwise use 'name'
                                val id = if (item.has("id")) item.getString("id") else item.getString("name")
                                val name = item.getString("name")
                                val description = item.optString("description", "Channel discussion")
                                channels.add(Channel(id, name, description))
                            }
                            is String -> {
                                channels.add(Channel(item, item))
                            }
                        }
                    }

                    runOnUiThread {
                        val adapter = ChannelAdapter(this, channels)
                        lvChannels.adapter = adapter
                    }
                } else {
                    Log.e("ChannelsActivity", "Failed to fetch channels: $responseCode")
                }
                conn.disconnect()
            } catch (e: Exception) {
                Log.e("ChannelsActivity", "Error fetching channels", e)
            }
        }.start()
    }

    private fun createChannel(channelName: String) {
        Thread {
            try {
                val token = getSharedPreferences("MyChat", MODE_PRIVATE).getString("token", "")
                val url = URL("http://mychat.fgontier.fr/channels")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Authorization", "Bearer $token")
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true

                val json = org.json.JSONObject()
                json.put("name", channelName)

                val writer = java.io.OutputStreamWriter(conn.outputStream)
                writer.write(json.toString())
                writer.flush()
                writer.close()

                if (conn.responseCode == 201 || conn.responseCode == 200) {
                    runOnUiThread {
                        Toast.makeText(this, "Channel created!", Toast.LENGTH_SHORT).show()
                        fetchChannels()
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this, "Failed to create channel", Toast.LENGTH_SHORT).show()
                    }
                }
                conn.disconnect()
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }
}
