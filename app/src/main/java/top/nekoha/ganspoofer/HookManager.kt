package top.nekoha.ganspoofer

import android.util.Log
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import top.nekoha.ganspoofer.annotations.HookClass
import top.nekoha.ganspoofer.annotations.HookConstructor
import top.nekoha.ganspoofer.annotations.HookMethod
import java.lang.reflect.Method
import java.lang.reflect.Modifier

class HookManager {
    companion object {
        private const val TAG = "GanSpoofer"
        private const val HOOK_PACKAGE = "top.nekoha.ganspoofer.hook"

        fun registerHooks(lpparam: XC_LoadPackage.LoadPackageParam) {
            try {
                val hookClasses = findHookClasses(lpparam.classLoader)
                var totalHooks = 0

                hookClasses.forEach { clazz ->
                    val hookClassAnnotation = clazz.getAnnotation(HookClass::class.java)
                    if (hookClassAnnotation?.packageName == lpparam.packageName) {
                        val hooksCount = registerHooksFromClass(clazz, lpparam)
                        totalHooks += hooksCount
                        Log.d(TAG, "Registered $hooksCount hooks from ${clazz.simpleName}")
                    }
                }

                Log.d(TAG, "Successfully registered $totalHooks hooks for ${lpparam.packageName}")
            } catch (e: Exception) {
                Log.e(TAG, "Error registering hooks", e)
            }
        }

        private fun findHookClasses(classLoader: ClassLoader): List<Class<*>> {
            val hookClasses = mutableListOf<Class<*>>()

            try {
                val potentialClassNames = listOf(
                    "$HOOK_PACKAGE.BluetoothHook",
                    "$HOOK_PACKAGE.DeviceHook",
                    "$HOOK_PACKAGE.PermissionHook",
                    "$HOOK_PACKAGE.LocationHook"
                )

                potentialClassNames.forEach { className ->
                    try {
                        val clazz = classLoader.loadClass(className)
                        if (clazz.isAnnotationPresent(HookClass::class.java)) {
                            hookClasses.add(clazz)
                            Log.d(TAG, "Found hook class: $className")
                        }
                    } catch (e: ClassNotFoundException) {
                        Log.w(TAG, "Hook class not found: $className", e)
                    } catch (e: Exception) {
                        Log.w(TAG, "Error loading hook class: $className", e)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error finding hook classes", e)
            }

            return hookClasses
        }

        private fun registerHooksFromClass(clazz: Class<*>, lpparam: XC_LoadPackage.LoadPackageParam): Int {
            val methods = clazz.declaredMethods
            val classLoader = lpparam.classLoader
            var registeredCount = 0

            methods.forEach { method ->
                try {
                    if (!Modifier.isStatic(method.modifiers)) {
                        Log.w(TAG, "Skipping non-static method: ${method.name}")
                        return@forEach
                    }

                    method.getAnnotation(HookMethod::class.java)?.let { annotation ->
                        val parameterTypes = annotation.parameterTypes.map {
                            loadClassByName(it, classLoader)
                        }.toTypedArray()

                        XposedHelpers.findAndHookMethod(
                            annotation.className,
                            classLoader,
                            annotation.methodName,
                            *parameterTypes,
                            object : XC_MethodHook() {
                                override fun beforeHookedMethod(param: MethodHookParam) {
                                    try {
                                        method.invoke(null, param)
                                    } catch (e: Exception) {
                                    }
                                }

                                override fun afterHookedMethod(param: MethodHookParam) {
                                    try {
                                        val afterMethodName = method.name.substringBefore("Hook") + "AfterHook"
                                        val afterMethod = findMethodByName(clazz, afterMethodName)
                                        afterMethod?.invoke(null, param)
                                    } catch (e: Exception) {
                                    }
                                }
                            }
                        )

                        registeredCount++
                        Log.d(TAG, "Hooked method: ${annotation.className}.${annotation.methodName}")
                    }

                    method.getAnnotation(HookConstructor::class.java)?.let { annotation ->
                        val parameterTypes = annotation.parameterTypes.map {
                            loadClassByName(it, classLoader)
                        }.toTypedArray()

                        XposedHelpers.findAndHookConstructor(
                            annotation.className,
                            classLoader,
                            *parameterTypes,
                            object : XC_MethodHook() {
                                override fun beforeHookedMethod(param: MethodHookParam) {
                                    try {
                                        method.invoke(null, param)
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Error in hook constructor: ${method.name}", e)
                                    }
                                }

                                override fun afterHookedMethod(param: MethodHookParam) {
                                    try {
                                        val afterMethodName = method.name.substringBefore("Hook") + "AfterHook"
                                        val afterMethod = findMethodByName(clazz, afterMethodName)
                                        afterMethod?.invoke(null, param)
                                    } catch (e: Exception) {
                                    }
                                }
                            }
                        )

                        registeredCount++
                        Log.d(TAG, "Hooked constructor: ${annotation.className}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error registering hook from method: ${method.name}", e)
                }
            }

            return registeredCount
        }

        private fun findMethodByName(clazz: Class<*>, methodName: String): Method? {
            return clazz.declaredMethods.firstOrNull { it.name == methodName }
        }

        private fun loadClassByName(className: String, classLoader: ClassLoader): Class<*> {
            return when (className) {
                "int" -> Int::class.java
                "long" -> Long::class.java
                "float" -> Float::class.java
                "double" -> Double::class.java
                "boolean" -> Boolean::class.java
                "byte" -> Byte::class.java
                "short" -> Short::class.java
                "char" -> Char::class.java
                "void" -> Void::class.java
                else -> Class.forName(className, true, classLoader)
            }
        }
    }
}

data class HookInfo(
    val className: String,
    val methodName: String,
    val hookType: HookType,
    val parameterTypes: Array<String> = emptyArray(),
)

enum class HookType {
    METHOD,
    CONSTRUCTOR,
}
