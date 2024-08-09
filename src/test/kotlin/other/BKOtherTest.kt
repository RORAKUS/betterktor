@file:Suppress("TestFunctionName")

package other

import codes.rorak.betterktor.BKPlugin
import codes.rorak.betterktor.util.BKTransform
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import khttp.structures.authorization.BasicAuthorization
import kotlin.test.Test
import kotlin.test.assertEquals

internal class BKOtherTest {
	companion object {
		const val TOKEN = "token";
		
		init {
			embeddedServer(Netty, port = 8090, host = "0.0.0.0", module = Application::mainModule)
				.start(wait = false);
		}
	}
	
	@Test
	fun `Complicated Test`() {
		val response = khttp.get("http://localhost:8090/api/test");
		assertEquals(200, response.statusCode);
		assertEquals("Api Test", response.text);
	}
	
	@Test
	fun `Different Casing`() {
		val response = khttp.get("http://localhost:8090/api/this_should_be_snake_case");
		assertEquals(200, response.statusCode);
		assertEquals("This Should Be Snake Case", response.text);
	}
	
	@Test
	fun `Inner Test`() {
		val response = khttp.get("http://localhost:8090/api/inner/test");
		assertEquals(200, response.statusCode);
		assertEquals("Api Inner Test", response.text);
	}
	
	@Test
	fun `Absolute Route`() {
		val response = khttp.get("http://localhost:8090/completely/different");
		assertEquals(200, response.statusCode);
		assertEquals("Completely Different", response.text);
	}
	
	@Test
	fun `Relative Route`() {
		val response = khttp.get("http://localhost:8090/api/relative");
		assertEquals(200, response.statusCode);
		assertEquals("Api Relative", response.text);
	}
	
	@Test
	fun `Root Path Index`() {
		val response = khttp.get("http://localhost:8090/api");
		assertEquals(200, response.statusCode);
		assertEquals("Api", response.text);
		
		val response2 = khttp.get("http://localhost:8090/api/");
		assertEquals(200, response2.statusCode);
		assertEquals("Api", response2.text);
	}
	
	@Test
	fun Regex() {
		val response = khttp.get("http://localhost:8090/regex/test154");
		assertEquals(200, response.statusCode);
		assertEquals("Regex test154", response.text);
		
		val response2 = khttp.get("http://localhost:8090/api/regex/mango154");
		assertEquals(200, response2.statusCode);
		assertEquals("Regex mango154", response2.text);
		
		val response3 = khttp.get("http://localhost:8090/api/regex/apple154");
		assertEquals(404, response3.statusCode);
	}
	
	@Test
	fun `Multi Routes`() {
		val response = khttp.get("http://localhost:8090/api/multi/");
		assertEquals(200, response.statusCode);
		assertEquals("GET /multi", response.text);
		
		val response2 = khttp.post("http://localhost:8090/api/multi/test");
		assertEquals(200, response2.statusCode);
		assertEquals("POST /multi/test", response2.text);
		
		val response3 = khttp.post("http://localhost:8090/api/multi/err");
		assertEquals(200, response3.statusCode);
		assertEquals("ERROR /multi/err", response3.text);
		
		val response4 = khttp.post("http://localhost:8090/api/multi/generic_err");
		assertEquals(200, response4.statusCode);
		assertEquals("ERROR /multi", response4.text);
		
		val response5 = khttp.get("http://localhost:8090/api/multi/chat");
		assertEquals(400, response5.statusCode);
		
		val response6 = khttp.get("http://localhost:8090/api/multi/on-error");
		assertEquals(404, response6.statusCode);
	}
	
	@Test
	fun Auth() {
		val response = khttp.get(
			"http://localhost:8090/api/auth/",
			auth = BasicAuthorization(TOKEN, TOKEN)
		);
		assertEquals(200, response.statusCode);
		assertEquals("auth", response.text);
		
		val response2 = khttp.get("http://localhost:8090/api/auth");
		assertEquals(401, response2.statusCode);
	}
}

fun Application.mainModule() {
	install(BKPlugin) {
		basePackage = "different.pack";
		endpointsPackage = "api";
		casing = BKTransform::snakeCase;
		rootPath = "/api";
		configureAuthentication {
			basic("auth") {
				validate { (username, password) ->
					if (username == BKOtherTest.TOKEN && password == BKOtherTest.TOKEN)
						UserIdPrincipal("auth")
					else null;
				}
			}
		}
	};
}