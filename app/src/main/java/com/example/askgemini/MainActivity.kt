package com.example.askgemini

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.ai.client.generativeai.Chat
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private val API_KEY = "AIzaSyCDvOqxT3TMnVslS38ToxTFFiS3JTad6Cc"
    private lateinit var chat: Chat
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var editTextInput: EditText
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sharedPreferences = getSharedPreferences(
            "MySharedPreferences",
            Context.MODE_PRIVATE
        )

        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.nav_view)
        navigationView.setNavigationItemSelectedListener(this)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        chatAdapter = ChatAdapter()
        recyclerView.adapter = chatAdapter

        editTextInput = findViewById(R.id.editTextInput)

        val generativeModel = GenerativeModel(
            modelName = "gemini-pro",
            apiKey = API_KEY
        )

        chat = generativeModel.startChat(
            history = listOf(
                content(role = "user") { text("Hello") },
                content(role = "model") { text("Hello, how can I help you?") }
            )
        )
        addChatToAdapter("Hello", true)
        addChatToAdapter("Hello, how can I help you?", false)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_new_chat -> startNewChat()
            R.id.nav_history -> showSearchHistory()

        }

        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun startNewChat() {
        chatAdapter.clear()

        val generativeModel = GenerativeModel(
            modelName = "gemini-pro",
            apiKey = API_KEY
        )

        chat = generativeModel.startChat(
            history = listOf(
                content(role = "user") { text("Hello") },
                content(role = "model") { text("Hello, how can I help you?") }
            )
        )

        addChatToAdapter("Hello", true)
        addChatToAdapter("Hello, how can I help you?", false)
        chatAdapter.clear()
    }

    fun buttonSendChat(view: View) {
        val userMessage = editTextInput.text.toString()
        addChatToAdapter(userMessage, true)

        MainScope().launch {
            val result = chat.sendMessage(userMessage)
            result.text?.let { addChatToAdapter(it, false) }
        }
        editTextInput.setText("")
        saveSearchToHistory(userMessage)
    }

    private fun addChatToAdapter(message: String, isUser: Boolean) {
        val chatItem = ChatItem(message, isUser)
        chatAdapter.addChat(chatItem)
        recyclerView.scrollToPosition(chatAdapter.itemCount - 1)
    }

    private fun saveSearchToHistory(search: String) {
        if (search.trim().isNotEmpty()) {
            val currentHistory = retrieveSearchHistory(this).toMutableList()
            currentHistory.add(search)

            val historyString = currentHistory.joinToString(",")
            val editor = sharedPreferences.edit()
            editor.putString("search_history", historyString)
            editor.apply()
        }
    }

    private fun showSearchHistory() {
        val searchHistoryList = retrieveSearchHistory(this)
        val historyDialog = AlertDialog.Builder(this)
        historyDialog.setTitle("Search History")

        historyDialog.setItems(searchHistoryList.toTypedArray()) { _, i ->
            val searchQuery = searchHistoryList[i]
            addChatToAdapter(searchQuery, true)

            MainScope().launch {
                val result = chat.sendMessage(searchQuery)
                result.text?.let { addChatToAdapter(it, false) }
            }
        }

        historyDialog.setPositiveButton("Clear History") { _, _ ->
            clearSearchHistory()
        }

        historyDialog.show()
    }

    private fun clearSearchHistory() {
        val editor = sharedPreferences.edit()
        editor.putString("search_history", "")
        editor.apply()
        chatAdapter.clear()
    }

    private fun retrieveSearchHistory(context: Context): List<String> {
        val searchHistoryString: String? = sharedPreferences.getString("search_history", "")
        return searchHistoryString?.split(",")?.filterNotNull()?.filter { it.trim().isNotEmpty() }?.toList() ?: emptyList()
    }
}
