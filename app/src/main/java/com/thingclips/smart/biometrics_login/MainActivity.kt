package com.thingclips.smart.biometrics_login

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.thingclips.smart.android.user.api.ILoginCallback
import com.thingclips.smart.android.user.bean.User
import com.thingclips.smart.biometrics_login.databinding.ActivityMainBinding
import com.thingclips.smart.home.sdk.ThingHomeSdk

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 如果已经登录，则直接进入首页
        if (UserSessionManager.isLoggedIn(this) && ThingHomeSdk.getUserInstance().isLogin) {
            HomeActivity.start(this)
            finish()
            return
        }

        // 如果未登录但已开启指纹登录，则直接进入指纹登录页
        val uid = UserSessionManager.getUid(this)
        if (!uid.isNullOrEmpty()) {
            val biometricSdk = com.thingclips.smart.biometrics.ThingBiometricsLoginSDK.getInstance()
            if (biometricSdk.isBiometricLoginEnabled(uid)) {
                BiometricLoginActivity.startForBiometric(this)
                finish()
                return
            }
        }

        initView()
    }

    private fun initView() {
        binding.btnLogin.setOnClickListener {
            doEmailLogin()
        }

        binding.tvRegister.setOnClickListener {
            RegisterActivity.start(this)
        }
    }

    private fun doEmailLogin() {
        val countryCode = binding.etCountryCode.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        if (countryCode.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "国家码、邮箱、密码不能为空", Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.btnLogin.isEnabled = false

        ThingHomeSdk.getUserInstance().loginWithEmail(
            countryCode,
            email,
            password,
            object : ILoginCallback {
                override fun onSuccess(user: User?) {
                    runOnUiThread {
                        binding.progressBar.visibility = View.GONE
                        binding.btnLogin.isEnabled = true

                        // 记录会话信息（为后续指纹登录做准备）
                        UserSessionManager.saveLogin(this@MainActivity, user, email, countryCode)
                        Log.d("MainActivity", "登录成功，用户信息：$user")
                        Toast.makeText(this@MainActivity, "登录成功", Toast.LENGTH_SHORT).show()
                        HomeActivity.start(this@MainActivity)
                        finish()
                    }
                }

                override fun onError(code: String?, error: String?) {
                    runOnUiThread {
                        binding.progressBar.visibility = View.GONE
                        binding.btnLogin.isEnabled = true
                        Log.e("MainActivity", "登录失败：$error ($code)")
                        Toast.makeText(
                            this@MainActivity,
                            "登录失败：$error",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        )
    }
}