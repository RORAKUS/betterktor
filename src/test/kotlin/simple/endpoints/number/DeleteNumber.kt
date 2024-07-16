package simple.endpoints.number

import codes.rorak.betterktor.BKPath
import codes.rorak.betterktor.BKRoute
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*

@BKPath("delete/", "/(?<num>\\d+)")
class DeleteNumber: BKRoute {
	override suspend fun get(call: ApplicationCall, request: ApplicationRequest) {
		call.respondText("Number Delete ${call.parameters["num"]}");
	}
}