package codes.rorak.betterktor.annotations

import codes.rorak.betterktor.util.BKHttpMethod

/**
 * Sets a named route method's method to the `method` parameter value
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class BKMethod(val method: BKHttpMethod);