package test.other.endpoints.api

import codes.rorak.betterktor.annotations.BKAuth
import codes.rorak.betterktor.annotations.BKDefaultMethod
import codes.rorak.betterktor.handlers.BKRoute
import codes.rorak.betterktor.util.BKHttpMethod
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*

@BKAuth("auth")
@BKDefaultMethod(BKHttpMethod.GET)
class Auth: BKRoute {
	override suspend fun get(call: ApplicationCall) {
		call.respondText(call.principal<UserIdPrincipal>()?.name ?: "null");
	}
	
	@BKAuth("auth", strategy = AuthenticationStrategy.Optional)
	suspend fun one(call: ApplicationCall) {
		call.respondText(call.principal<UserIdPrincipal>()?.name ?: "null");
	}
}