package codes.rorak.betterktor.annotations

import codes.rorak.betterktor.util.BKRouteType

/**
 * For multi classes (must be annotated with @BKMulti) will change the named
 * route to a different route type.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class BKMultiFor(val value: BKRouteType = BKRouteType.ROUTE);