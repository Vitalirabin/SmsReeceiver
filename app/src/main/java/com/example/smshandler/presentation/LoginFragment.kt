package com.example.smshandler.presentation

import android.Manifest
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.example.smshandler.Constants
import com.example.smshandler.Constants.ENABLE
import com.example.smshandler.Constants.SP
import com.example.smshandler.R
import com.example.smshandler.broadcast.SmsReceiver
import com.example.smshandler.databinding.FragmentLoginBinding
import com.example.smshandler.network.LoginAuthorizationModel
import com.example.smshandler.network.SendRepository
import com.example.smshandler.service.SmsListenerService
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.schedulers.Schedulers

class LoginFragment : Fragment() {

    private lateinit var repository: SendRepository

    companion object {
        private const val PERMISSION_REQUEST_CODE = 416
    }

    private val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS,
            Manifest.permission.POST_NOTIFICATIONS,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CALL_LOG
        )
    } else {
        arrayOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CALL_LOG
        )
    }

    private lateinit var binding: FragmentLoginBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentLoginBinding.inflate(inflater, container, false)
        repository = SendRepository()
        binding.stopSendSmsWithTokenButton.setOnClickListener {
            context?.getSharedPreferences(SP, Context.MODE_PRIVATE)?.edit()
                ?.putBoolean(ENABLE, false)
                ?.apply()
            onServiceRunning(false)
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        onServiceRunning(
            context?.getSharedPreferences(SP, Context.MODE_PRIVATE)?.getBoolean(ENABLE, false)
                ?: false
        )
        binding.startServiceWithLoginButton.setOnClickListener {
            onClickStartService()
        }
    }

    private fun onServiceRunning(isRunning: Boolean) {
        if (isRunning) {
            binding.textInputLogin.visibility = View.INVISIBLE
            binding.textInputPassword.visibility = View.INVISIBLE
            binding.textInputNumber.visibility = View.INVISIBLE
            binding.startServiceWithLoginButton.visibility = View.INVISIBLE
            binding.startServiceWithLoginButton.isClickable = false
            binding.stopSendSmsWithTokenButton.isClickable = true
            binding.serviceWorkedMessage.visibility = View.VISIBLE
            binding.stopSendSmsWithTokenButton.visibility = View.VISIBLE
        } else {
            binding.textInputLogin.visibility = View.VISIBLE
            binding.textInputPassword.visibility = View.VISIBLE
            binding.textInputNumber.visibility = View.VISIBLE
            binding.startServiceWithLoginButton.visibility = View.VISIBLE
            binding.startServiceWithLoginButton.isClickable = true
            binding.stopSendSmsWithTokenButton.isClickable = false
            binding.serviceWorkedMessage.visibility = View.INVISIBLE
            binding.stopSendSmsWithTokenButton.visibility = View.INVISIBLE
        }
    }

    private fun isServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = context?.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }

    @SuppressLint("CheckResult")
    private fun onClickStartService() {
        val login = binding.textInputLogin.text.toString()
        val password = binding.textInputPassword.text.toString()
        val number = binding.textInputNumber.text.toString()
        if (login != "" || password != "" || number != "") {
            repository.authorizationWithLogin(LoginAuthorizationModel(login, password))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    Log.d("TokenFragment", "subscribe")
                    context?.getSharedPreferences(Constants.SP, Context.MODE_PRIVATE)?.edit()
                        ?.putBoolean(Constants.TOKEN_OR_LOGIN, false)
                        ?.putString(Constants.LOGIN, login)
                        ?.putString(Constants.PASSWORD, password)
                        ?.putString(Constants.NUMBER, number)
                        ?.putBoolean(Constants.ENABLE, true)
                        ?.apply()
                    //startSmsListenerService()

                    onServiceRunning(true)
                }, {
                    Log.e("TokenFragment", it.message, it)
                    if (it.message.equals("401"))
                        Toast.makeText(
                            context,
                            getString(R.string.bad_authorization_message),
                            Toast.LENGTH_LONG
                        ).show()
                    else Toast.makeText(
                        context,
                        it.message,
                        Toast.LENGTH_LONG
                    ).show()
                })
        } else Toast.makeText(context, getString(R.string.enter_all), Toast.LENGTH_SHORT).show()
    }

    private fun startSmsListenerService() {
        requireContext().stopService(
            Intent(
                requireContext(),
                SmsListenerService::class.java
            )
        )
        requireContext().startForegroundService(
            Intent(
                requireContext(),
                SmsListenerService::class.java
            )
        )

    }

    private fun checkPermissions() {
        activity?.let { mActivity ->
            permissions.forEach {
                if (ActivityCompat.checkSelfPermission(
                        mActivity,
                        it
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    ActivityCompat.requestPermissions(
                        mActivity, permissions,
                        PERMISSION_REQUEST_CODE
                    )
                    return
                }
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            checkPermissions()
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

}