package com.app.mobmaxime.ui

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.app.mobmaxime.R
import com.app.mobmaxime.databinding.ActivityLoginBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions


class LoginActivity : AppCompatActivity(), View.OnClickListener {

    companion object {
        const val RC_SIGN_IN = 101
        const val My_Pref = "MyPref"
        const val IsLogin = "is_login"
    }

    private lateinit var mBinding: ActivityLoginBinding

    private lateinit var mPreference: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(mBinding.root)
        supportActionBar!!.hide()
        mPreference = getSharedPreferences(My_Pref, MODE_PRIVATE)
        if (mPreference.contains(IsLogin) && mPreference.getBoolean(IsLogin, false)) {
            redirectToDashBoard()
        }

        initListeners()
    }

    private fun initListeners() {
        mBinding.imgGoogleLogin.setOnClickListener(this)
        mBinding.imgFaceBookLogin.setOnClickListener(this)
        mBinding.cirLoginButton.setOnClickListener(this)
    }

    override fun onClick(view: View?) {
        when (view!!.id) {

            R.id.imgGoogleLogin -> {
                doGoogleLogin()
            }

            R.id.cirLoginButton -> {
                loginValidation()
            }

            R.id.imgFaceBookLogin -> {
                // TODO: 13-03-2022 Do FaceBook Login code here
            }
        }
    }

    private fun loginValidation(): Boolean {

        var isValidLogin = true

        if (mBinding.editTextEmail.text.isNullOrEmpty()) {
            mBinding.editTextEmail.error = getString(R.string.error_email)
            isValidLogin = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(mBinding.editTextEmail.text).matches()) {
            mBinding.editTextEmail.error = getString(R.string.error_email)
            isValidLogin = false
        } else if (mBinding.editTextPassword.text.isNullOrEmpty()) {
            mBinding.editTextPassword.error = getString(R.string.error_passwors)
            isValidLogin = false

        } else if (mBinding.editTextPassword.text.length < 6) {
            mBinding.editTextPassword.error = getString(R.string.error_passwors)
            isValidLogin = false
        }
        return isValidLogin
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            handleSignData(data)
        }
    }

    private fun getGoogleSingInClient(): GoogleSignInClient {
        val gso = GoogleSignInOptions
            .Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()
            .build()
        return GoogleSignIn.getClient(this, gso);
    }

    private fun isUserSignedIn(): Boolean {

        val account = GoogleSignIn.getLastSignedInAccount(this)
        return account != null

    }

    private fun doGoogleLogin() {
        if (!isUserSignedIn()) {

            val signInIntent = getGoogleSingInClient().signInIntent
            startActivityForResult(signInIntent, RC_SIGN_IN)
        } else {
            // TODO: 13-03-2022 user already signIn
        }
    }

    private fun handleSignData(data: Intent?) {
        GoogleSignIn.getSignedInAccountFromIntent(data)
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    val bundle = Bundle()
                    mPreference.edit().putBoolean(IsLogin, true).apply()
                    redirectToDashBoard()
                } else {
                    toast("exception " + it.exception)
                }
            }
    }


    private fun redirectToDashBoard() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }

    private fun Context.toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}