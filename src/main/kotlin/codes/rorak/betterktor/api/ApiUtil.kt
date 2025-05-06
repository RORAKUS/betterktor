package codes.rorak.betterktor.api

import codes.rorak.betterktor.internal.other.Util
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*

/**
 * Responds to a call with an HTML file from the program resources in the directory
 * specified in the BetterKtor configuration.
 *
 * @param path Path to the file including its extension
 * @param directory Setting this you can override the default directory specified in the configuration
 * @param status The response status code, defaultly 200
 */
suspend fun ApplicationCall.respondPage(
	path: String,
	directory: String = BetterKtor.defaultPagesDirectory,
	status: HttpStatusCode = HttpStatusCode.OK
) {
	// get the resource
	val resource = Util.resource("/$directory/$path");
	
	// check if the resource exists
	checkNotNull(resource) { "The file '$path' in the program resources was not found!" };
	
	// respond with the HTML content of the resource
	respondText(resource.readText(), ContentType.Text.Html, status);
}