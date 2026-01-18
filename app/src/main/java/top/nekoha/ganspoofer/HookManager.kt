package top.nekoha.ganspoofer

import android.util.Log
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import top.nekoha.ganspoofer.annotations.HookClass
import top.nekoha.ganspoofer.annotations.HookConstructor
import top.nekoha.ganspoofer.annotations.HookMethod
import top.nekoha.ganspoofer.annotations.HookType
import java.lang.reflect.Modifier

class HookManager {
    companion object {
        private const val TAG = "GanSpoofer"

        private var hookClasses: List<Class<*>> = emptyList()

        fun addHook(hookClass: Class<*>) {
            hookClasses = hookClasses.plus(hookClass)
        }

        fun registerHooks(lpparam: XC_LoadPackage.LoadPackageParam) {
            try {
                Log.i(TAG, "Starting registering hooks")

                var totalHooks = 0

                hookClasses.forEach { clazz ->
                    val hookClassAnnotation = clazz.getAnnotation(HookClass::class.java)
                    if (hookClassAnnotation?.packageName == lpparam.packageName) {
                        val hooksCount = registerHooksFromClass(clazz, lpparam)
                        totalHooks += hooksCount
                        Log.i(TAG, "Registered $hooksCount hooks from ${clazz.simpleName}")
                    }
                }

                Log.i(TAG, "Successfully registered $totalHooks hooks for ${lpparam.packageName}")
            } catch (e: Exception) {
                Log.e(TAG, "Error registering hooks", e)
            }
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

                    Log.i(TAG, "Registering ${method.name}")

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
                                    if(annotation.hookType == HookType.BEFORE) {
                                        method.invoke(null,param)
                                    }
                                }

                                override fun afterHookedMethod(param: MethodHookParam) {
                                    if(annotation.hookType == HookType.AFTER) {
                                        method.invoke(null, param)
                                    }
                                }
                            }
                        )

                        registeredCount++
                        Log.i(TAG, "Hooked method: ${annotation.className}.${annotation.methodName}")
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
                                override fun afterHookedMethod(param: MethodHookParam) {
                                    method?.invoke(null, param)
                                }
                            }
                        )

                        registeredCount++
                        Log.i(TAG, "Hooked constructor: ${annotation.className}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error registering hook from method: ${method.name}", e)
                }
            }

            return registeredCount
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
