package simple.endpoints.number.select

import codes.rorak.betterktor.BKPath
import codes.rorak.betterktor.BKRoute
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*

@BKPath(regex = "(?<num>\\d+)")
class SelectNumber: BKRoute {
	override suspend fun get(call: ApplicationCall, request: ApplicationRequest) {
		call.respondText("Number Select ${call.parameters["num"]}");
	}
}