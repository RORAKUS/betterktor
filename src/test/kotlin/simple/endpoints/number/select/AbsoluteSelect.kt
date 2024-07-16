package simple.endpoints.number.select

import codes.rorak.betterktor.BKPath
import codes.rorak.betterktor.BKRoute
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*

@BKPath("/and", "(?<str>[a-z]+-and-[a-z]+)")
class AbsoluteSelect: BKRoute {
	override suspend fun get(call: ApplicationCall, request: ApplicationRequest) {
		call.respondText("And ${call.parameters["str"]}");
	}
}