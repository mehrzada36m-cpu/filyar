package com.filyar.app.core

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import com.filyar.app.BuildConfig
import com.filyar.app.FileService
import com.filyar.app.IFileService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import rikka.shizuku.Shizuku

enum class ShizukuStatus { NotInstalled, NotRunning, NoPermission, Connecting, Ready }

object ShizukuManager {
    const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
    private const val REQUEST_CODE = 4021

    private lateinit var appContext: Context
    private val _status = MutableStateFlow(ShizukuStatus.NotRunning)
    val status: StateFlow<ShizukuStatus> = _status.asStateFlow()

    @Volatile
    private var service: IFileService? = null
    private var binding = false

    fun backend(): FileBackend = service?.let { ShizukuBackend(it) } ?: LocalBackend

    private val serviceArgs by lazy {
        Shizuku.UserServiceArgs(ComponentName(BuildConfig.APPLICATION_ID, FileService::class.java.name))
            .daemon(false)
            .processNameSuffix("files")
            .debuggable(BuildConfig.DEBUG)
            .version(BuildConfig.VERSION_CODE)
    }

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            binding = false
            if (binder != null && binder.pingBinder()) {
                service = IFileService.Stub.asInterface(binder)
                _status.value = ShizukuStatus.Ready
            } else {
                refresh()
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            binding = false
            service = null
            refresh()
        }
    }

    fun init(context: Context) {
        appContext = context.applicationContext
        Shizuku.addBinderReceivedListenerSticky { refresh() }
        Shizuku.addBinderDeadListener {
            service = null
            binding = false
            refresh()
        }
        Shizuku.addRequestPermissionResultListener { _, _ -> refresh() }
        refresh()
    }

    fun isInstalled(): Boolean = try {
        appContext.packageManager.getPackageInfo(SHIZUKU_PACKAGE, 0)
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }

    fun refresh() {
        if (!::appContext.isInitialized) return
        try {
            if (!Shizuku.pingBinder()) {
                service = null
                _status.value = if (isInstalled()) ShizukuStatus.NotRunning else ShizukuStatus.NotInstalled
                return
            }
            if (Shizuku.isPreV11() || Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                service = null
                _status.value = ShizukuStatus.NoPermission
                return
            }
            if (service?.asBinder()?.pingBinder() == true) {
                _status.value = ShizukuStatus.Ready
                return
            }
            service = null
            if (!binding) {
                binding = true
                _status.value = ShizukuStatus.Connecting
                Shizuku.bindUserService(serviceArgs, connection)
            }
        } catch (_: Throwable) {
            binding = false
            service = null
            _status.value = ShizukuStatus.NotRunning
        }
    }

    fun requestPermission() {
        try {
            if (Shizuku.pingBinder()) {
                if (Shizuku.shouldShowRequestPermissionRationale()) {
                    // User chose "deny and don't ask again" – permission can only be granted from the Shizuku app.
                    _status.value = ShizukuStatus.NoPermission
                }
                Shizuku.requestPermission(REQUEST_CODE)
            } else {
                refresh()
            }
        } catch (_: Throwable) {
            refresh()
        }
    }
}
