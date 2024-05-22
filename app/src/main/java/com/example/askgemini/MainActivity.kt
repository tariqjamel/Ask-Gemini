package com.example.askgemini

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.ActionBarDrawerToggle
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

    private val API_KEY = "Your Api Key"
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

        startNewChat()

        val menu = navigationView.menu
        menu.removeGroup(R.id.group_search_history)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_new_chat -> startNewChat()
            R.id.nav_clear_history -> clearSearchHistory()
            R.id.nav_history -> showSearchHistory()
            else -> {
                val searchQuery = item.title.toString()
                addChatToAdapter(searchQuery, true)

                MainScope().launch {
                    val result = chat.sendMessage(searchQuery)
                    result.text?.let { addChatToAdapter(it, false) }
                }
            }
        }
        return true
    }

    private fun startNewChat() {
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
        drawerLayout.closeDrawer(GravityCompat.START)
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
        val menu = navigationView.menu

        for ((index, searchQuery) in searchHistoryList.withIndex()) {
            val menuItem = menu.add(R.id.group_search_history, Menu.NONE, index, searchQuery)
            menuItem.setOnMenuItemClickListener { menuItem ->
                drawerLayout.closeDrawer(GravityCompat.START)
                val userMessage = menuItem.title.toString()
                addChatToAdapter(userMessage, true)

                MainScope().launch {
                    val result = chat.sendMessage(userMessage)
                    result.text?.let { addChatToAdapter(it, false)
                    }
                }

                true
            }
        }
    }

    private fun clearSearchHistory() {
        val editor = sharedPreferences.edit()
        editor.putString("search_history", "")
        editor.apply()
        val menu = navigationView.menu
        menu.removeGroup(R.id.group_search_history)
        chatAdapter.clear()
        drawerLayout.closeDrawer(GravityCompat.START)
    }

    private fun retrieveSearchHistory(context: Context): List<String> {
        val searchHistoryString: String? = sharedPreferences.getString("search_history", "")
        return searchHistoryString?.split(",")?.filterNotNull()?.filter { it.trim().isNotEmpty() }?.toList() ?: emptyList()
    }
}
