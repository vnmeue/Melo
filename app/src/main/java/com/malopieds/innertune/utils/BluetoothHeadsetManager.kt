package com.malopieds.innertune.utils

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.Intent
import android.provider.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class BluetoothHeadsetManager(private val context: Context) {
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var bluetoothHeadset: BluetoothHeadset? = null
    
    private val _isHeadsetConnected = MutableStateFlow(false)
    val isHeadsetConnected: StateFlow<Boolean> = _isHeadsetConnected

    private val _connectedHeadsetName = MutableStateFlow<String?>(null)
    val connectedHeadsetName: StateFlow<String?> = _connectedHeadsetName

    private val profileListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
            if (profile == BluetoothProfile.HEADSET) {
                bluetoothHeadset = proxy as BluetoothHeadset
                updateHeadsetConnectionState()
            }
        }

        override fun onServiceDisconnected(profile: Int) {
            if (profile == BluetoothProfile.HEADSET) {
                bluetoothHeadset = null
                _isHeadsetConnected.value = false
                _connectedHeadsetName.value = null
            }
        }
    }

    init {
        if (BluetoothPermissionHandler.hasRequiredPermissions(context)) {
            initializeBluetooth()
        }
    }

    private fun initializeBluetooth() {
        try {
            bluetoothAdapter?.getProfileProxy(context, profileListener, BluetoothProfile.HEADSET)
        } catch (e: SecurityException) {
            // Handle permission not granted
            _isHeadsetConnected.value = false
        }
    }

    private fun updateHeadsetConnectionState() {
        try {
            bluetoothHeadset?.let { headset ->
                val connectedDevices = headset.connectedDevices
                _isHeadsetConnected.value = connectedDevices.isNotEmpty()
                _connectedHeadsetName.value = connectedDevices.firstOrNull()?.name
            }
        } catch (e: SecurityException) {
            _isHeadsetConnected.value = false
            _connectedHeadsetName.value = null
        }
    }

    fun openBluetoothSettings() {
        try {
            val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        } catch (e: SecurityException) {
            // Handle permission not granted
        }
    }

    fun isNoiseReductionSupported(): Boolean {
        return try {
            bluetoothHeadset?.let { headset ->
                headset.connectedDevices.firstOrNull()?.let { device ->
                    headset.isNoiseReductionSupported(device)
                } ?: false
            } ?: false
        } catch (e: SecurityException) {
            false
        }
    }

    fun isVoiceRecognitionSupported(): Boolean {
        return try {
            bluetoothHeadset?.let { headset ->
                headset.connectedDevices.firstOrNull()?.let { device ->
                    headset.isVoiceRecognitionSupported(device)
                } ?: false
            } ?: false
        } catch (e: SecurityException) {
            false
        }
    }

    fun startVoiceRecognition(): Boolean {
        return try {
            bluetoothHeadset?.let { headset ->
                headset.connectedDevices.firstOrNull()?.let { device ->
                    headset.startVoiceRecognition(device)
                } ?: false
            } ?: false
        } catch (e: SecurityException) {
            false
        }
    }

    fun stopVoiceRecognition(): Boolean {
        return try {
            bluetoothHeadset?.let { headset ->
                headset.connectedDevices.firstOrNull()?.let { device ->
                    headset.stopVoiceRecognition(device)
                } ?: false
            } ?: false
        } catch (e: SecurityException) {
            false
        }
    }
} 