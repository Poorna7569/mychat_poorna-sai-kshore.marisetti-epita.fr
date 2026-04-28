package com.example.mychat1

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class ChatActivity : AppCompatActivity() {
    private lateinit var tvChannelName: TextView
    private lateinit var tvMemberCount: TextView
    private lateinit var lvMessages: ListView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: Button
    private lateinit var btnBack: TextView
    private lateinit var btnMembers: TextView
    
    private var channelName: String = ""
    private val messages = mutableListOf<String>()
    private val membersList = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Hide the default Action Bar as requested to use only the custom header
        supportActionBar?.hide()
        
        setContentView(R.layout.activity_chat)

        channelName = intent.getStringExtra("CHANNEL_NAME")?.trim() ?: ""
        
        if (channelName.isBlank()) {
            Toast.makeText(this, "Invalid channel name", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        
        tvChannelName = findViewById(R.id.tvChannelName)
        tvMemberCount = findViewById(R.id.tvMemberCount)
        lvMessages = findViewById(R.id.lvMessages)
        etMessage = findViewById(R.id.etMessage)
        btnSend = findViewById(R.id.btnSend)
        btnBack = findViewById(R.id.btnBack)
        btnMembers = findViewById(R.id.btnMembers)

        // Back button functionality
        btnBack.setOnClickListener {
            finish()
        }

        // Members functionality
        btnMembers.setOnClickListener {
            showMembersDialog()
        }

        tvChannelName.text = "# $channelName"

        btnSend.setOnClickListener {
            val text = etMessage.text.toString().trim()
            if (text.isNotEmpty()) sendMessage(text)
        }

        joinChannelAndFetchMessages()
    }

    private fun joinChannelAndFetchMessages() {
        val token = getToken()
        if (token.isEmpty()) {
            Toast.makeText(this, "Session expired", Toast.LENGTH_LONG).show()
            return
        }

        Thread {
            try {
                val encodedName = Uri.encode(channelName)
                val url = URL("http://mychat.fgontier.fr/channels/$encodedName/members")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Authorization", "Bearer $token")
                conn.doOutput = true
                conn.responseCode
                conn.disconnect()
                
                runOnUiThread {
                    fetchMessages()
                    fetchMembers()
                }
            } catch (e: Exception) {
                Log.e("ChatActivity", "Join failed", e)
            }
        }.start()
    }

    private fun getToken(): String {
        return getSharedPreferences("MyChat", Context.MODE_PRIVATE).getString("token", "") ?: ""
    }

    private fun fetchMessages() {
        val token = getToken()
        Thread {
            try {
                val encodedName = Uri.encode(channelName)
                val url = URL("http://mychat.fgontier.fr/channels/$encodedName/messages")
                val conn = url.openConnection() as HttpURLConnection
                conn.setRequestProperty("Authorization", "Bearer $token")

                if (conn.responseCode == 200) {
                    val response = conn.inputStream.bufferedReader().use { it.readText() }
                    val jsonArray = JSONArray(response)
                    messages.clear()
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val user = obj.optString("username", "Unknown")
                        val content = obj.optString("content", "")
                        messages.add("$user: $content")
                    }
                    messages.reverse()

                    runOnUiThread {
                        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, messages)
                        lvMessages.adapter = adapter
                        lvMessages.setSelection(adapter.count - 1)
                    }
                }
                conn.disconnect()
            } catch (e: Exception) {
                Log.e("ChatActivity", "Fetch messages failed", e)
            }
        }.start()
    }

    private fun fetchMembers() {
        val token = getToken()
        Thread {
            try {
                val encodedName = Uri.encode(channelName)
                val url = URL("http://mychat.fgontier.fr/channels/$encodedName/members")
                val conn = url.openConnection() as HttpURLConnection
                conn.setRequestProperty("Authorization", "Bearer $token")

                if (conn.responseCode == 200) {
                    val response = conn.inputStream.bufferedReader().use { it.readText() }
                    val jsonArray = JSONArray(response)
                    membersList.clear()
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        membersList.add(obj.optString("username", "Unknown"))
                    }

                    runOnUiThread {
                        tvMemberCount.text = "${membersList.size} members"
                    }
                }
                conn.disconnect()
            } catch (e: Exception) {
                Log.e("ChatActivity", "Fetch members failed", e)
            }
        }.start()
    }

    private fun showMembersDialog() {
        if (membersList.isEmpty()) {
            Toast.makeText(this, "Loading member list...", Toast.LENGTH_SHORT).show()
            fetchMembers()
            return
        }
        
        AlertDialog.Builder(this)
            .setTitle("Members in #$channelName")
            .setItems(membersList.toTypedArray(), null)
            .setPositiveButton("Close", null)
            .show()
    }

    private fun sendMessage(text: String) {
        val token = getToken()
        Thread {
            try {
                val encodedName = Uri.encode(channelName)
                val url = URL("http://mychat.fgontier.fr/channels/$encodedName/messages")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("Authorization", "Bearer $token")
                conn.doOutput = true

                val jsonParam = JSONObject()
                jsonParam.put("content", text)

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
                Log.e("ChatActivity", "Send message failed", e)
            }
        }.start()
    }
}
