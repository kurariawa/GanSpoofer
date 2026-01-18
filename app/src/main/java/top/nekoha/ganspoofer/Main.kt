package top.nekoha.ganspoofer

import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.callbacks.XC_LoadPackage
import top.nekoha.ganspoofer.hook.BluetoothHook

class Main : IXposedHookLoadPackage {
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName != "com.gan.cubestation") return
        HookManager.addHook(BluetoothHook::class.java)
        HookManager.registerHooks(lpparam)
    }
}
