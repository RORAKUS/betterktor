package codes.rorak.betterktor.annotations

/**
 * Makes BK ignore this method
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class BKIgnore;