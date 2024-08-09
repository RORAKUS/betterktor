package codes.rorak.betterktor.annotations

import codes.rorak.betterktor.util.BKHttpMethod

/**
 * Sets all named route method's method default to the value of the `method` parameter.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class BKDefaultMethod(val method: BKHttpMethod);