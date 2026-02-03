package com.thingclips.smart.biometrics_login

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
import com.thingclips.smart.android.user.api.ILogoutCallback
import com.thingclips.smart.biometrics_login.databinding.ActivityHomeBinding
import com.thingclips.smart.home.sdk.ThingHomeSdk

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private val biometricSDK = ThingBiometricsLoginSDK.getInstance()
    private val uid: String get() = UserSessionManager.getUid(this) ?: ""

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initView()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun initView() {
        val email = UserSessionManager.getEmail(this) ?: ""
        binding.tvWelcome.text = "欢迎，$email"

        // 启用生物识别登录
        binding.btnEnableBiometric.setOnClickListener { enableBiometricLogin() }
        // 禁用生物识别登录
        binding.btnDisableBiometric.setOnClickListener { disableBiometricLogin() }

        binding.btnLogout.setOnClickListener {
            ThingHomeSdk.getUserInstance().logout(object : ILogoutCallback {
                override fun onSuccess() {
                    runOnUiThread {
                        UserSessionManager.markLogout(this@HomeActivity)
                        Toast.makeText(this@HomeActivity, "已退出登录", Toast.LENGTH_SHORT).show()

                        // 若已开启生物识别登录，退出后进入指纹登录界面；否则进入默认登录界面
                        val currentUid = UserSessionManager.getUid(this@HomeActivity)
                        val goToBiometric = !currentUid.isNullOrEmpty() &&
                                biometricSDK.isBiometricLoginEnabled(currentUid)

                        if (goToBiometric) {
                            startActivity(Intent(this@HomeActivity, BiometricLoginActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                            })
                        } else {
                            startActivity(Intent(this@HomeActivity, MainActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                            })
                        }
                        finish()
                    }
                }

                override fun onError(errorCode: String?, errorMsg: String?) {
                    runOnUiThread {
                        Log.e("HomeActivity", "退出登录失败: ${errorMsg ?: errorCode}")
                        Toast.makeText(
                            this@HomeActivity,
                            "退出登录失败: ${errorMsg ?: errorCode}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            })
        }

        checkBiometricSupport()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun checkBiometricSupport() {
        if (!biometricSDK.isSupportBiometricLogin(this)) {
            binding.btnEnableBiometric.isEnabled = false
            binding.btnDisableBiometric.isEnabled = false
            Toast.makeText(this, "设备不支持生物识别功能", Toast.LENGTH_LONG).show()
        }
    }

    private fun disableBiometricLogin() {
        if (uid.isEmpty()) {
            Toast.makeText(this, "当前无可用用户", Toast.LENGTH_SHORT).show()
            return
        }
        biometricSDK.disableBiometricLogin(uid)
        Toast.makeText(this, "生物识别登录已禁用", Toast.LENGTH_SHORT).show()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun enableBiometricLogin() {
        if (!biometricSDK.isSupportBiometricLogin(this)) {
            Toast.makeText(this, "设备不支持生物识别", Toast.LENGTH_SHORT).show()
            return
        }
        if (uid.isEmpty()) {
            Toast.makeText(this, "当前无可用用户，请先通过账号密码登录", Toast.LENGTH_SHORT).show()
            return
        }
        biometricSDK.enableBiometricLogin(
            this,
            uid,
            object : IThingBiometricFingerCallback {
                override fun onSuccess(user: User?) {
                    runOnUiThread {
                        Toast.makeText(this@HomeActivity, "生物识别登录已启用", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onError(errorCode: String?, errorMsg: String?) {
                    runOnUiThread {
                        if (!TextUtils.isEmpty(errorCode) && !TextUtils.isEmpty(errorMsg)) {
                            when (errorCode) {
                                BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE.toString() -> {
                                    Toast.makeText(this@HomeActivity, "设备不支持生物识别", Toast.LENGTH_SHORT).show()
                                }
                                BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE.toString() -> {
                                    Toast.makeText(this@HomeActivity, "生物识别功能当前不可用", Toast.LENGTH_SHORT).show()
                                }
                                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED.toString() -> {
                                    Toast.makeText(this@HomeActivity, "请先在系统设置中录入生物识别信息", Toast.LENGTH_LONG).show()
                                }
                                else -> {
                                    Toast.makeText(this@HomeActivity, errorMsg, Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                }

                override fun onNegative() {
                    runOnUiThread {
                        Toast.makeText(this@HomeActivity, "用户取消了生物识别设置", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFingerInfoInvalid() {
                    runOnUiThread {
                        Toast.makeText(this@HomeActivity, "指纹信息无效，请检查系统指纹设置", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, HomeActivity::class.java)
            context.startActivity(intent)
        }
    }
}

