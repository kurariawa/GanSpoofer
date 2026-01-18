package top.nekoha.ganspoofer.annotations

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class HookClass(val packageName: String = "")

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class HookMethod(
    val className: String = "",
    val methodName: String = "",
    val parameterTypes: Array<String> = [],
    val hookType: HookType,
)

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class HookConstructor(
    val className: String = "",
    val parameterTypes: Array<String> = [],
)

enum class HookType {
    BEFORE,
    AFTER,
}
