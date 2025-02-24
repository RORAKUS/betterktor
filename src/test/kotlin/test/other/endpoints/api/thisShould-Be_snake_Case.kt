package test.other.endpoints.api

import codes.rorak.betterktor.handlers.BKRoute
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*

@Suppress("ClassName")
class `thisShould-Be_snake_Case`: BKRoute {
	override suspend fun get(call: ApplicationCall, request: ApplicationRequest) {
		call.respondText("This Should Be Snake Case");
	}
}