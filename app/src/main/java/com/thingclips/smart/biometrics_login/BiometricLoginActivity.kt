package com.thingclips.smart.biometrics_login

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import com.thingclips.smart.android.user.bean.User
import com.thingclips.smart.biometrics.IThingBiometricFingerCallback
import com.thingclips.smart.biometrics.ThingBiometricsLoginSDK
import com.thingclips.smart.biometrics_login.databinding.ActivityBiometricLoginBinding

class BiometricLoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityBiometricLoginBinding
    private val biometricSDK = ThingBiometricsLoginSDK.getInstance()

    private val uid: String by lazy { UserSessionManager.getUid(this) ?: "" }
    private val accountName: String by lazy { UserSessionManager.getEmail(this) ?: "" }
    private val countryCode: String by lazy { UserSessionManager.getCountryCode(this) ?: "" }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBiometricLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initToolbar()
        initView()
    }

    private fun initToolbar() {
        supportActionBar?.title = "生物识别登录"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun initView() {
        // 设置用户信息
        binding.tvName.text = if (accountName.isNotEmpty()) accountName else "未设置用户"

        // 设置防重复点击事件
        binding.ivFingerPrint.preventRepeatedClick { fingerLogin() }
        binding.tvFingerPrint.preventRepeatedClick { fingerLogin() }

        checkBiometricSupport()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun checkBiometricSupport() {
        if (!biometricSDK.isSupportBiometricLogin(this)) {
            binding.ivFingerPrint.isEnabled = false
            binding.tvFingerPrint.isEnabled = false
            Toast.makeText(this, "设备不支持生物识别功能", Toast.LENGTH_LONG).show()
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun fingerLogin() {
        // 检查是否支持生物识别
        if (!biometricSDK.isSupportBiometricLogin(this)) {
            showErrorDialog("设备不支持生物识别")
            return
        }
        // 检查生物识别登录是否启用
        if (uid.isEmpty() || !biometricSDK.isBiometricLoginEnabled(uid)) {
            showErrorDialog("生物识别登录未启用")
            return
        }

        // 检查指纹是否发生变化
        if (biometricSDK.hasBiometricChanged(uid)) {
            showErrorDialog("指纹信息已变更，请重新设置")
            return
        }


        // 开始生物识别认证
        startBiometricAuthentication()
    }

    private fun startBiometricAuthentication() {
        // 禁用所有交互
        setViewsClickable(false)

        if (uid.isEmpty() || accountName.isEmpty() || countryCode.isEmpty()) {
            showErrorDialog("本地账号信息不完整，请使用账号密码重新登录")
            return
        }

        biometricSDK.authenticate(
            this,
            uid,
            accountName,
            countryCode,
            object : IThingBiometricFingerCallback {
                override fun onSuccess(user: User?) {
                    runOnUiThread {
                        if (!isFinishing) {
                            // 启用所有交互
                            setViewsClickable(true)
                            Toast.makeText(this@BiometricLoginActivity, "生物识别登录成功", Toast.LENGTH_SHORT).show()
                            Log.d(TAG, "登录成功，用户信息：$user")
                            // 指纹登录成功：更新会话（覆盖原 session，与涂鸦 SDK 新 session 一致）
                            UserSessionManager.updateSessionAfterBiometricLogin(
                                this@BiometricLoginActivity,
                                user,
                                accountName,
                                countryCode
                            )
                            HomeActivity.start(this@BiometricLoginActivity)
                            finish()
                        }
                    }
                }

                override fun onError(errorCode: String?, errorMsg: String?) {
                    runOnUiThread {
                        // 启用所有交互
                        setViewsClickable(true)
                        Log.e(TAG, "登录失败，错误码：$errorCode，错误信息：$errorMsg")

                        if (!TextUtils.isEmpty(errorCode) && !TextUtils.isEmpty(errorMsg)) {
                            when (errorCode) {
                                BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE.toString() -> {
                                    showErrorDialog("设备不支持生物识别")
                                }
                                BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE.toString() -> {
                                    Toast.makeText(this@BiometricLoginActivity, "生物识别功能当前不可用", Toast.LENGTH_SHORT).show()
                                }
                                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED.toString() -> {
                                    Toast.makeText(this@BiometricLoginActivity, "请先在系统设置中录入生物识别信息", Toast.LENGTH_LONG).show()
                                }
                                else -> {
                                    Toast.makeText(this@BiometricLoginActivity, errorMsg, Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                }

                override fun onNegative() {
                    runOnUiThread {
                        // 启用所有交互
                        setViewsClickable(true)
                    }
                }

                override fun onFingerInfoInvalid() {
                    runOnUiThread {
                        // 启用所有交互
                        setViewsClickable(true)
                        // 1.新增指纹  2.系统存在指纹，但未开启锁屏密码(指纹不生效)
                        Toast.makeText(
                            this@BiometricLoginActivity,
                            "指纹信息无效，请检查系统指纹设置",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        )
    }

    private fun setViewsClickable(clickable: Boolean) {
        binding.ivFingerPrint.isClickable = clickable
        binding.tvFingerPrint.isClickable = clickable
    }

    private fun showErrorDialog(tips: String) {
        AlertDialog.Builder(this)
            .setTitle("提示")
            .setMessage(tips)
            .setPositiveButton("我知道了") { dialog, _ ->
                dialog.dismiss()
                // 在实际应用中，这里应该跳转到账号密码登录页
                Toast.makeText(this, "请使用账号密码登录", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                finish()
            }
            .setCancelable(false)
            .show()
    }

    companion object {
        private const val TAG = "BiometricLogin"

        /** 未登录时进入，仅做生物识别登录 */
        fun startForBiometric(context: Context) {
            val intent = Intent(context, BiometricLoginActivity::class.java)
            context.startActivity(intent)
        }
    }
}
