package different.pack.api

import codes.rorak.betterktor.annotations.BKAuth
import codes.rorak.betterktor.handlers.BKRoute
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*

@BKAuth("auth")
class Auth: BKRoute {
	override suspend fun get(call: ApplicationCall) {
		call.respondText(call.principal<UserIdPrincipal>("auth")?.name ?: "null");
	}
}