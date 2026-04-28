package com.example.mychat1

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class RegisterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val etUsername = findViewById<EditText>(R.id.etUsername)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnRegister = findViewById<Button>(R.id.btnRegister)
        val tvLoginLink = findViewById<TextView>(R.id.tvLoginLink)

        btnRegister.setOnClickListener {
            val username = etUsername.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Enter username and password", Toast.LENGTH_SHORT).show()
            } else {
                register(username, password)
            }
        }

        tvLoginLink.setOnClickListener {
            finish()
        }
    }

    private fun register(username: String, psw: String) {
        Thread {
            try {
                val url = URL("http://mychat.fgontier.fr/register")
                val conn = url.openConnection() as HttpURLConnection

                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true

                val json = JSONObject()
                json.put("username", username)
                json.put("password", psw)

                val writer = OutputStreamWriter(conn.outputStream)
                writer.write(json.toString())
                writer.flush()
                writer.close()

                val responseCode = conn.responseCode

                if (responseCode == 200 || responseCode == 201) {
                    val response = conn.inputStream.bufferedReader().readText()
                    val token = try {
                        JSONObject(response).getString("token")
                    } catch (e: Exception) {
                        null
                    }
                    if (token != null) {
                        val pref = getSharedPreferences("MyChat", MODE_PRIVATE)
                        val editor = pref.edit()
                        editor.putString("token", token)
                        editor.apply()
                    }
                    runOnUiThread {
                        Toast.makeText(this, "Registration success", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                } else {
                    val errorResponse = try {
                        conn.errorStream.bufferedReader().readText()
                    } catch (e: Exception) {
                        "No error details"
                    }
                    
                    val errorMessage = when (responseCode) {
                        400 -> "Invalid input - check username and password"
                        409 -> "Username already exists - choose a different one"
                        500 -> "Server error - try again later"
                        else -> "Registration failed: $responseCode - $errorResponse"
                    }
                    
                    runOnUiThread {
                        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                    }
                }

                conn.disconnect()

            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }
}