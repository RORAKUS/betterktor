package simple.endpoints

import codes.rorak.betterktor.handlers.BKRoute
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*

@Suppress("ClassName")
class `ThisShould-be_just-Kebab_Case`: BKRoute {
	override suspend fun get(call: ApplicationCall, request: ApplicationRequest) {
		call.respondText("This Should Be Just Kebab Case");
	}
}