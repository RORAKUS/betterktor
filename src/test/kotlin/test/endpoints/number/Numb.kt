package test.endpoints.number

import codes.rorak.betterktor.handlers.BKRoute
import io.ktor.server.application.*

class Numb: BKRoute {
	override suspend fun get(call: ApplicationCall) {
		throw Error();
	}
}