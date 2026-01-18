package top.nekoha.ganspoofer.hook

import android.util.Log
import de.robv.android.xposed.XC_MethodHook
import top.nekoha.ganspoofer.annotations.HookClass
import top.nekoha.ganspoofer.annotations.HookMethod
import top.nekoha.ganspoofer.annotations.HookType

@HookClass(packageName = "com.gan.cubestation")
class BluetoothHook {
    companion object {
        private const val TAG = "GanSpoofer"
        // private const val GAN_DEVICE_NAME = "GAN356i"

        @HookMethod(
            className = "com.gan.bluetooth.BleModule",
            methodName = "startScan",
            parameterTypes = [
                "long"
            ],
            hookType = HookType.BEFORE
        )
        @JvmStatic
        fun hookStartScan(param: XC_MethodHook.MethodHookParam) {
            Log.i(TAG, "startScan called")
        }
    }
}