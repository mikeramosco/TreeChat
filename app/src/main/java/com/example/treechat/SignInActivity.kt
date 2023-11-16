package com.example.treechat

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.activity_sign_in.*

class SignInActivity : AppCompatActivity() {

    private val usersTableName = "users"
    private val emailChild = "email"
    private var loggedOutCode = 42

    private lateinit var mGoogleSignInClient: GoogleSignInClient
    private var googleSignInCode = 10
    private var googleSignOutCode = 11

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id)).requestEmail().build()
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)
        initSignInWithGoogleButton()
    }

    private fun initSignInWithGoogleButton() {
        sign_in_with_google_button.setOnClickListener {
            signInWithGoogleButtonClicked()
        }
    }

    private fun signInWithGoogleButtonClicked() {
        val signInIntent = mGoogleSignInClient.signInIntent
        startActivityForResult(signInIntent, googleSignInCode)
    }

    @SuppressLint("DefaultLocale")
    fun signInClicked(view: View) {
        val email = email_field.text.toString().toLowerCase()
        val password = password_field.text.toString()
        if(email == "" || password == "") {
            Toast.makeText(this, "Text fields cannot be blank",
                Toast.LENGTH_LONG).show()
        } else {
            checkIfEmailAlreadyExists(email, password)
        }
    }

    private fun checkIfEmailAlreadyExists(email: String, password: String) {
        val thisInstance = this
        val fb = FirebaseDatabase.getInstance().reference
        val usersTable = fb.child(usersTableName)
        val usersWithSubmittedEmail = usersTable.orderByChild(emailChild).equalTo(email)
        usersWithSubmittedEmail.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(data: DataSnapshot) {
                var userExists = false
                for (user in data.children) {
                    userExists = true
                    break
                }

                if (userExists) {
                    signIn(email, password)
                } else {
                    Toast.makeText(
                        thisInstance, "No user exists with that email",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(thisInstance, "Unable to connect",
                    Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun signIn(email: String, password: String) {
        val auth = FirebaseAuth.getInstance()
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if(task.isSuccessful) {
                    saveLoggedInState(email)

                    val openPage = Intent(this, ChannelListActivity::class.java)
                    openPage.putExtra("email", email)
                    startActivityForResult(openPage, loggedOutCode)
                } else {
                    var taskFailedMessage = "Unable to login"
                    val taskException = task.exception.toString()
                    Log.d(dtag, taskException)
                    if(taskException.contains("The password is invalid"))
                        taskFailedMessage = "The password is invalid"
                    Toast.makeText(this, taskFailedMessage,
                        Toast.LENGTH_LONG).show()
                }
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            loggedOutCode -> finish()
            googleSignInCode -> {
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                handleGoogleSignInResult(task)
            }
            googleSignOutCode -> mGoogleSignInClient.signOut().addOnCompleteListener {
                finish()
            }
        }
    }

    private fun handleGoogleSignInResult(task: Task<GoogleSignInAccount>) {
        try {
            val account = task.getResult(ApiException::class.java)!!
            val email = "(${account.email})"

            saveLoggedInState(email)

            val openPage = Intent(this, ChannelListActivity::class.java)
            openPage.putExtra("email", email)
            startActivityForResult(openPage, googleSignOutCode)
        } catch (e: ApiException) {
            Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show()
        }
    }

    private fun saveLoggedInState(email: String) {
        if(remember_me_checkbox.isChecked) {
            val sharedPreferences = getSharedPreferences(
                SHARED_PREF, MODE_PRIVATE)
            val editor = sharedPreferences.edit()

            editor.putString(EMAIL_REF, email)
            editor.putBoolean(LOGIN_REF, true)
            editor.apply()
        }
    }
}
