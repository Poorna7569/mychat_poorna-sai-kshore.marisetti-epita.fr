package com.example.mychat1

import android.content.Context
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class ChatActivity : AppCompatActivity() {

    private lateinit var tvChannelName: TextView
    private lateinit var lvMessages: ListView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: Button
    private lateinit var btnJoin: Button
    private lateinit var btnLeave: Button
    
    private var channelName: String = ""
    private val messages = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        channelName = intent.getStringExtra("CHANNEL_NAME") ?: ""
        
        tvChannelName = findViewById(R.id.tvChannelName)
        lvMessages = findViewById(R.id.lvMessages)
        etMessage = findViewById(R.id.etMessage)
        btnSend = findViewById(R.id.btnSend)
        btnJoin = findViewById(R.id.btnJoin)
        btnLeave = findViewById(R.id.btnLeave)

        tvChannelName.text = channelName

        btnSend.setOnClickListener {
            val text = etMessage.text.toString()
            if (text.isNotEmpty()) sendMessage(text)
        }

        btnJoin.setOnClickListener { joinChannel() }
        btnLeave.setOnClickListener { leaveChannel() }

        fetchMessages()
    }

    private fun getToken(): String {
        val sharedPref = getSharedPreferences("MyChat", Context.MODE_PRIVATE)
        return sharedPref.getString("token", "") ?: ""
    }

    private fun fetchMessages() {
        Thread {
            try {
                val url = URL("http://mychat.fgontier.fr/channels/$channelName/messages")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.setRequestProperty("Authorization", "Bearer ${getToken()}")

                if (conn.responseCode == 200) {
                    val response = conn.inputStream.bufferedReader().use { it.readText() }
                    val jsonArray = JSONArray(response)
                    messages.clear()
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val user = obj.getString("username")
                        val text = obj.getString("text")
                        messages.add("$user: $text")
                    }

                    runOnUiThread {
                        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, messages)
                        lvMessages.adapter = adapter
                    }
                }
                conn.disconnect()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    private fun sendMessage(text: String) {
        Thread {
            try {
                val url = URL("http://mychat.fgontier.fr/channels/$channelName/messages")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("Authorization", "Bearer ${getToken()}")
                conn.doOutput = true

                val jsonParam = JSONObject()
                jsonParam.put("text", text)

                val os = OutputStreamWriter(conn.outputStream)
                os.write(jsonParam.toString())
                os.flush()
                os.close()

                if (conn.responseCode == 201 || conn.responseCode == 200) {
                    runOnUiThread {
                        etMessage.setText("")
                        fetchMessages()
                    }
                }
                conn.disconnect()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    private fun joinChannel() {
        Thread {
            try {
                val url = URL("http://mychat.fgontier.fr/channels/$channelName/members")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Authorization", "Bearer ${getToken()}")
                
                if (conn.responseCode == 200 || conn.responseCode == 201) {
                    runOnUiThread { Toast.makeText(this, "Joined", Toast.LENGTH_SHORT).show() }
                }
                conn.disconnect()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    private fun leaveChannel() {
        Thread {
            try {
                val url = URL("http://mychat.fgontier.fr/channels/$channelName/members/me")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "DELETE"
                conn.setRequestProperty("Authorization", "Bearer ${getToken()}")
                
                if (conn.responseCode == 200 || conn.responseCode == 204) {
                    runOnUiThread { Toast.makeText(this, "Left", Toast.LENGTH_SHORT).show() }
                }
                conn.disconnect()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }
}