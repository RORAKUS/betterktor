package codes.rorak.betterktor.annotations

import codes.rorak.betterktor.util.BKRouteType

/**
 * Sets the class named route preference for multi-route.
 * Default: Route
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class BKMulti(val namedRoutesFor: BKRouteType = BKRouteType.ROUTE);