package codes.rorak.betterktor.api

import codes.rorak.betterktor.BetterKtorConfig
import codes.rorak.betterktor.internal.resolver.BetterKtorCache
import codes.rorak.betterktor.util.BetterKtorRuntimeError
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import java.io.File

/**
 * An interface for all return types, can be used
 * in endpoints returning different types. You can
 * implement this interface in your serializable data classes for
 * more flexibility.
 */
abstract class CallReturn {
	internal abstract suspend fun respond(call: ApplicationCall, cache: BetterKtorCache);
};

/**
 * An HTML text response.
 *
 * @param text The HTML text in a `String`
 * @param statusCode The response HTTP status code, defaultly 200
 */
data class HTMLReturn(
	val text: String,
	val statusCode: HttpStatusCode = HttpStatusCode.OK
): CallReturn() {
	override suspend fun respond(call: ApplicationCall, cache: BetterKtorCache) {
		call.respondText(text, ContentType.Text.Html, statusCode);
	}
};

/**
 * An HTML page response. The page is found in the resources directory inside
 * a folder specified by [BetterKtorConfig.defaultPagesDirectory] or specified by the
 * [directory] parameter.
 *
 * @param name The path to the page including its extension
 * @param directory The optional directory name overriding the default behaviour
 * @param statusCode The response HTTP status code
 */
data class PageReturn(
	val name: String,
	val directory: String? = null,
	val statusCode: HttpStatusCode = HttpStatusCode.OK
): CallReturn() {
	override suspend fun respond(call: ApplicationCall, cache: BetterKtorCache) {
		call.respondPage(name, directory ?: cache.config.defaultPagesDirectory, statusCode);
	}
};

/**
 * A template page response. It uses the [BetterKtorConfig.templateRespondMethod] to
 * respond.
 *
 * @param name The path to the page including its extension
 * @param parameters The parameters for the template
 * @param statusCode The response HTTP status code
 */
data class TemplateReturn(
	val name: String,
	val parameters: List<Pair<String, Any?>>,
	val statusCode: HttpStatusCode = HttpStatusCode.OK
): CallReturn() {
	override suspend fun respond(call: ApplicationCall, cache: BetterKtorCache) {
		// respond with a template, if the templateRespondMethod was not set, throw an error
		cache.config.templateRespondMethod?.invoke(call, name, parameters.toMap(), statusCode)
			?: BetterKtorRuntimeError("Cannot use TemplateReturn - a template engine was not set up!");
	}
};

/**
 * A normal status code response. The client receives an empty response with
 * the specified status code.
 *
 * @param statusCode The status code to return
 */
data class StatusReturn(
	val statusCode: HttpStatusCode
): CallReturn() {
	override suspend fun respond(call: ApplicationCall, cache: BetterKtorCache) {
		call.respond(statusCode, "");
	}
};

/**
 * A file response using the [ApplicationCall.respondFile] method.
 *
 * @param file The file to return
 * @param configuration The configuration for the outgoing file content
 */
data class FileReturn(
	val file: File,
	val configuration: OutgoingContent.() -> Unit = {}
): CallReturn() {
	override suspend fun respond(call: ApplicationCall, cache: BetterKtorCache) {
		call.respondFile(file, configuration);
	}
};

/**
 * Redirects the response to the specified [url] using [ApplicationCall.respondRedirect].
 *
 * @param url The url where to redirect
 * @param permanent Wheter the redirect is permanent
 */
data class RedirectReturn(
	val url: String,
	val permanent: Boolean = false
): CallReturn() {
	override suspend fun respond(call: ApplicationCall, cache: BetterKtorCache) {
		call.respondRedirect(url, permanent);
	}
};

/**
 * A template page response. The page is found in the directory inside
 * a folder specified by [BetterKtorConfig.defaultTemplatesDirectory].
 * It uses the [BetterKtorConfig.templateRespondMethod] to respond.
 *
 * @param name The path to the page including its extension
 * @param parameters The parameters for the template
 * @param statusCode The response HTTP status code
 */
fun TemplateReturn(
	name: String,
	vararg parameters: Pair<String, Any?>,
	statusCode: HttpStatusCode = HttpStatusCode.OK
) = TemplateReturn(name, parameters.toList(), statusCode = statusCode);

/**
 * A normal status code response. The client receives an empty response with
 * the specified status code.
 *
 * @param code The status code to return
 */
fun StatusReturn(code: Int) = StatusReturn(HttpStatusCode.fromValue(code));

/**
 * A file response using the [ApplicationCall.respondFile] method.
 *
 * @param fileName The path of the file to return
 * @param configuration The configuration for the outgoing file content
 */
fun FileReturn(fileName: String, configuration: OutgoingContent.() -> Unit = {}) =
	FileReturn(File(fileName), configuration);