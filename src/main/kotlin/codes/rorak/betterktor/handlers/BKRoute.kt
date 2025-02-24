package codes.rorak.betterktor.handlers

import io.ktor.server.application.*
import io.ktor.server.request.*

/**
 * THE interface for all routes. All routes **MUST** implement this interface.
 * Contains handles for all possible HTTP methods.
 */
interface BKRoute {
	suspend fun get(call: ApplicationCall, request: ApplicationRequest) {};
	suspend fun post(call: ApplicationCall, request: ApplicationRequest) {};
	suspend fun put(call: ApplicationCall, request: ApplicationRequest) {};
	suspend fun delete(call: ApplicationCall, request: ApplicationRequest) {};
	suspend fun patch(call: ApplicationCall, request: ApplicationRequest) {};
	suspend fun head(call: ApplicationCall, request: ApplicationRequest) {};
	suspend fun options(call: ApplicationCall, request: ApplicationRequest) {};
	
	suspend fun get(call: ApplicationCall) {};
	suspend fun post(call: ApplicationCall) {};
	suspend fun put(call: ApplicationCall) {};
	suspend fun delete(call: ApplicationCall) {};
	suspend fun patch(call: ApplicationCall) {};
	suspend fun head(call: ApplicationCall) {};
	suspend fun options(call: ApplicationCall) {};
}