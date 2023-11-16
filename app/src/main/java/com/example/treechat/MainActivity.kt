package com.example.treechat

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import com.google.firebase.FirebaseApp

class MainActivity : AppCompatActivity() {

    private var loggedOutCode = 42

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        FirebaseApp.initializeApp(this)
        checkIfAlreadyLoggedIn()
    }

    private fun checkIfAlreadyLoggedIn() {
        val sharedPreferences = getSharedPreferences(
            SHARED_PREF, MODE_PRIVATE)
        val email = sharedPreferences.getString(EMAIL_REF, "")!!
        val alreadyLoggedIn = sharedPreferences.getBoolean(LOGIN_REF, false)
        if(alreadyLoggedIn) {
            val openPage = Intent(this, ChannelListActivity::class.java)
            openPage.putExtra("email", email)
            startActivityForResult(openPage, loggedOutCode)
        }
    }

    fun openSignInPage(view: View) {
        val openPage = Intent(this, SignInActivity::class.java)
        startActivityForResult(openPage, loggedOutCode)
    }

    fun openCreateAccountPage(view: View) {
        val openPage = Intent(this, CreateAccountActivity::class.java)
        startActivityForResult(openPage, loggedOutCode)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == loggedOutCode) {
            val sharedPreferences = getSharedPreferences(
                SHARED_PREF, MODE_PRIVATE)
            val alreadyLoggedIn = sharedPreferences.getBoolean(LOGIN_REF, false)
            if(alreadyLoggedIn)
                finish()
        }
    }
}
