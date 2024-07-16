package codes.rorak.betterktor

import io.ktor.server.application.*
import io.ktor.server.request.*

/**
 * THE interface for all routes. All routes **MUST** implement this interface.
 * Contains handles for all possible HTTP methods.
 */
interface BKRoute {
	suspend fun get(call: ApplicationCall, request: ApplicationRequest = call.request) {};
	suspend fun post(call: ApplicationCall, request: ApplicationRequest = call.request) {};
	suspend fun put(call: ApplicationCall, request: ApplicationRequest = call.request) {};
	suspend fun delete(call: ApplicationCall, request: ApplicationRequest = call.request) {};
	suspend fun patch(call: ApplicationCall, request: ApplicationRequest = call.request) {};
	suspend fun head(call: ApplicationCall, request: ApplicationRequest = call.request) {};
	suspend fun options(call: ApplicationCall, request: ApplicationRequest = call.request) {};
}