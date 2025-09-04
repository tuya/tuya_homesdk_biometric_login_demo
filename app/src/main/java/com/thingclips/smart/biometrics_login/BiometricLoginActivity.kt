package com.thingclips.smart.biometrics_login

import android.app.AlertDialog
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import com.thingclips.smart.android.user.bean.User
import com.thingclips.smart.biometrics.ThingBiometricsLoginSDK
import com.thingclips.smart.biometrics.IThingBiometricFingerCallback
import com.thingclips.smart.biometrics_login.databinding.ActivityBiometricLoginBinding

class BiometricLoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityBiometricLoginBinding
    private val biometricSDK = ThingBiometricsLoginSDK.getInstance()
    private val testUid = "az1754990865951OW23j"
    private val testAccountName = "xichen@yopmail.net"
    private val testCountryCode = "AZ"

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
        binding.tvName.text = "测试用户"

        // 设置防重复点击事件
        binding.ivFingerPrint.preventRepeatedClick { fingerLogin() }
        binding.tvFingerPrint.preventRepeatedClick { fingerLogin() }

        // 添加启用生物识别登录按钮的点击事件
        binding.tvEnableBiometric.preventRepeatedClick {
            enableBiometricLogin()
        }

        // 添加禁用生物识别登录按钮的点击事件
        binding.tvDisableBiometric.preventRepeatedClick {
            disableBiometricLogin()
        }

        // 检查设备是否支持生物识别
        checkBiometricSupport()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun checkBiometricSupport() {
        if (!biometricSDK.isSupportBiometricLogin(this)) {
            binding.tvEnableBiometric.isEnabled = false
            binding.tvDisableBiometric.isEnabled = false
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
        if (!biometricSDK.isBiometricLoginEnabled(testUid)) {
            showErrorDialog("生物识别登录未启用")
            return
        }

        // 检查指纹是否发生变化
        if (biometricSDK.hasBiometricChanged(testUid)) {
            showErrorDialog("指纹信息已变更，请重新设置")
            return
        }


        // 开始生物识别认证
        startBiometricAuthentication()
    }

    private fun disableBiometricLogin() {
        biometricSDK.disableBiometricLogin(testUid)
        Toast.makeText(this, "生物识别登录已禁用", Toast.LENGTH_SHORT).show()
    }

    private fun enableBiometricLogin() {
        // 检查是否支持生物识别
        if (!biometricSDK.isSupportBiometricLogin(this)) {
            Toast.makeText(this, "设备不支持生物识别", Toast.LENGTH_SHORT).show()
            return
        }

        biometricSDK.enableBiometricLogin(
            this,
            testUid,
            object : IThingBiometricFingerCallback {
                override fun onSuccess(user: User?) {
                    runOnUiThread {
                        Toast.makeText(this@BiometricLoginActivity, "生物识别登录已启用", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onError(errorCode: String?, errorMsg: String?) {
                    runOnUiThread {
                        if (!TextUtils.isEmpty(errorCode) && !TextUtils.isEmpty(errorMsg)) {
                            when (errorCode) {
                                BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE.toString() -> {
                                    Toast.makeText(this@BiometricLoginActivity, "设备不支持生物识别", Toast.LENGTH_SHORT).show()
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
                        Toast.makeText(this@BiometricLoginActivity, "用户取消了生物识别设置", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFingerInfoInvalid() {
                    runOnUiThread {
                        Toast.makeText(this@BiometricLoginActivity, "指纹信息无效，请检查系统指纹设置", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    private fun startBiometricAuthentication() {
        // 禁用所有交互
        setViewsClickable(false)

        biometricSDK.authenticate(
            this,
            testUid,
            testAccountName,
            testCountryCode,
            object : IThingBiometricFingerCallback {
                override fun onSuccess(user: User?) {
                    runOnUiThread {
                        if (!isFinishing) {
                            // 启用所有交互
                            setViewsClickable(true)
                            Toast.makeText(this@BiometricLoginActivity, "生物识别登录成功", Toast.LENGTH_SHORT).show()
                            // 在实际应用中，这里应该跳转到主页面
                        }
                    }
                }

                override fun onError(errorCode: String?, errorMsg: String?) {
                    runOnUiThread {
                        // 启用所有交互
                        setViewsClickable(true)

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
        binding.tvEnableBiometric.isClickable = clickable
        binding.tvDisableBiometric.isClickable = clickable
    }

    private fun showErrorDialog(tips: String) {
        AlertDialog.Builder(this)
            .setTitle("提示")
            .setMessage(tips)
            .setPositiveButton("我知道了") { dialog, _ ->
                dialog.dismiss()
                // 在实际应用中，这里应该跳转到账号密码登录页
                Toast.makeText(this, "请使用其他方式登录", Toast.LENGTH_SHORT).show()
            }
            .setCancelable(false)
            .show()
    }
}
