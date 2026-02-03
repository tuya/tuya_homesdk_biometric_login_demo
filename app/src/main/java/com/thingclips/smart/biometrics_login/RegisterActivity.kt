package com.thingclips.smart.biometrics_login

import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.thingclips.smart.android.user.api.IRegisterCallback
import com.thingclips.smart.android.user.bean.User
import com.thingclips.smart.biometrics_login.databinding.ActivityRegisterBinding
import com.thingclips.smart.home.sdk.ThingHomeSdk
import com.thingclips.smart.sdk.api.IResultCallback

class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterBinding
    private var countDownTimer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initView()
    }

    private fun initView() {
        supportActionBar?.title = "注册账号"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.btnSendCode.setOnClickListener {
            sendVerifyCode()
        }

        binding.btnRegister.setOnClickListener {
            doRegister()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun sendVerifyCode() {
        val email = binding.etEmail.text.toString().trim()
        val countryCode = binding.etCountryCode.text.toString().trim()

        if (email.isEmpty()) {
            Toast.makeText(this, "请输入邮箱地址", Toast.LENGTH_SHORT).show()
            return
        }

        if (countryCode.isEmpty()) {
            Toast.makeText(this, "请输入国家码", Toast.LENGTH_SHORT).show()
            return
        }

        // 验证邮箱格式
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "邮箱格式不正确", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnSendCode.isEnabled = false
        binding.progressBar.visibility = View.VISIBLE

        // type=1 表示注册时发送验证码
        ThingHomeSdk.getUserInstance().sendVerifyCodeWithUserName(
            email,
            "", // region 默认填写空字符串
            countryCode,
            1, // type=1 表示注册
            object : IResultCallback {
                override fun onSuccess() {
                    runOnUiThread {
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(this@RegisterActivity, "验证码已发送", Toast.LENGTH_SHORT).show()
                        startCountDown()
                    }
                }

                override fun onError(code: String?, error: String?) {
                    runOnUiThread {
                        binding.progressBar.visibility = View.GONE
                        binding.btnSendCode.isEnabled = true
                        Toast.makeText(
                            this@RegisterActivity,
                            "发送验证码失败：$error",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        )
    }

    private fun startCountDown() {
        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(60000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = millisUntilFinished / 1000
                binding.btnSendCode.text = "${seconds}秒后重发"
            }

            override fun onFinish() {
                binding.btnSendCode.isEnabled = true
                binding.btnSendCode.text = "发送验证码"
            }
        }.start()
    }

    private fun doRegister() {
        val countryCode = binding.etCountryCode.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val confirmPassword = binding.etConfirmPassword.text.toString().trim()
        val code = binding.etCode.text.toString().trim()

        // 输入验证
        if (countryCode.isEmpty()) {
            Toast.makeText(this, "请输入国家码", Toast.LENGTH_SHORT).show()
            return
        }

        if (email.isEmpty()) {
            Toast.makeText(this, "请输入邮箱地址", Toast.LENGTH_SHORT).show()
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "邮箱格式不正确", Toast.LENGTH_SHORT).show()
            return
        }

        if (password.isEmpty()) {
            Toast.makeText(this, "请输入密码", Toast.LENGTH_SHORT).show()
            return
        }

        if (password.length < 6) {
            Toast.makeText(this, "密码长度至少6位", Toast.LENGTH_SHORT).show()
            return
        }

        if (password != confirmPassword) {
            Toast.makeText(this, "两次输入的密码不一致", Toast.LENGTH_SHORT).show()
            return
        }

        if (code.isEmpty()) {
            Toast.makeText(this, "请输入验证码", Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.btnRegister.isEnabled = false

        // 先校验验证码
        ThingHomeSdk.getUserInstance().checkCodeWithUserName(
            email,
            "", // region 默认填写空字符串
            countryCode,
            code,
            1, // type=1 表示注册时校验验证码
            object : IResultCallback {
                override fun onSuccess() {
                    // 验证码校验成功，进行注册
                    ThingHomeSdk.getUserInstance().registerAccountWithEmail(
                        countryCode,
                        email,
                        password,
                        code,
                        object : IRegisterCallback {
                            override fun onSuccess(user: User?) {
                                runOnUiThread {
                                    binding.progressBar.visibility = View.GONE
                                    binding.btnRegister.isEnabled = true

                                    // 注册成功后，自动保存登录信息并跳转到首页
                                    UserSessionManager.saveLogin(
                                        this@RegisterActivity,
                                        user,
                                        email,
                                        countryCode
                                    )
                                    Log.d("RegisterActivity", "注册成功: $user")
                                    Toast.makeText(
                                        this@RegisterActivity,
                                        "注册成功",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    HomeActivity.start(this@RegisterActivity)
                                    finish()
                                }
                            }

                            override fun onError(code: String?, error: String?) {
                                runOnUiThread {
                                    binding.progressBar.visibility = View.GONE
                                    binding.btnRegister.isEnabled = true
                                    Log.e("RegisterActivity", "注册失败：$error ($code)")
                                    Toast.makeText(
                                        this@RegisterActivity,
                                        "注册失败：$error",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    )
                }

                override fun onError(code: String?, error: String?) {
                    runOnUiThread {
                        binding.progressBar.visibility = View.GONE
                        binding.btnRegister.isEnabled = true
                        Toast.makeText(
                            this@RegisterActivity,
                            "验证码校验失败：$error",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }

    companion object {
        fun start(context: android.content.Context) {
            val intent = android.content.Intent(context, RegisterActivity::class.java)
            context.startActivity(intent)
        }
    }
}
