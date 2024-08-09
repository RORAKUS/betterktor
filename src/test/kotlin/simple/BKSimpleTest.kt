package simple

import codes.rorak.betterktor.BKPlugin
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlin.test.Test
import kotlin.test.assertEquals

@Suppress("TestFunctionName")
internal class BKSimpleTest {
	companion object {
		init {
			embeddedServer(Netty, port = 8091, host = "0.0.0.0", module = Application::mainModule)
				.start(wait = false);
		}
	}
	
	@Test
	fun Index() {
		val response = khttp.get("http://localhost:8091");
		
		assertEquals(200, response.statusCode);
		assertEquals("Index", response.text);
		
		val response2 = khttp.get("http://localhost:8091/");
		
		assertEquals(200, response2.statusCode);
		assertEquals("Index", response2.text);
	}
	
	@Test
	fun `Normal Route`() {
		val response = khttp.get("http://localhost:8091/test");
		assertEquals(200, response.statusCode);
		assertEquals("Test", response.text);
		
		val response2 = khttp.get("http://localhost:8091/test/");
		assertEquals(200, response2.statusCode);
		assertEquals("Test", response2.text);
	}
	
	@Test
	fun `Inner Route`() {
		val response = khttp.get("http://localhost:8091/user/get");
		assertEquals(200, response.statusCode);
		assertEquals("User Get", response.text);
		
		val response2 = khttp.get("http://localhost:8091/user/get/");
		assertEquals(200, response2.statusCode);
		assertEquals("User Get", response2.text);
	}
	
	@Test
	fun `Inner Route Index`() {
		val response = khttp.get("http://localhost:8091/user");
		assertEquals(200, response.statusCode);
		assertEquals("User Index", response.text);
		
		val response2 = khttp.get("http://localhost:8091/user/");
		assertEquals(200, response2.statusCode);
		assertEquals("User Index", response2.text);
	}
	
	@Test
	fun `Inner Inner Route`() {
		val response = khttp.get("http://localhost:8091/user/when/not/done");
		assertEquals(200, response.statusCode);
		assertEquals("User When Not Done", response.text);
		
		val response2 = khttp.get("http://localhost:8091/user/when/not/done/");
		assertEquals(200, response2.statusCode);
		assertEquals("User When Not Done", response2.text);
	}
	
	@Test
	fun `Absolute Path`() {
		val response = khttp.get("http://localhost:8091/completely/different/path");
		assertEquals(200, response.statusCode);
		assertEquals("Completely Different Path", response.text);
		
		val response2 = khttp.get("http://localhost:8091/completely/different/path/");
		assertEquals(200, response2.statusCode);
		assertEquals("Completely Different Path", response2.text);
	}
	
	@Test
	fun `Absolute Relative Path`() {
		val response = khttp.get("http://localhost:8091/user/relative/path");
		assertEquals(200, response.statusCode);
		assertEquals("User Relative Path", response.text);
		
		val response2 = khttp.get("http://localhost:8091/user/relative/path/");
		assertEquals(200, response2.statusCode);
		assertEquals("User Relative Path", response2.text);
	}
	
	@Test
	fun `Resolver Path`() {
		val response = khttp.get("http://localhost:8091/category/amongus");
		assertEquals(200, response.statusCode);
		assertEquals("Category Amongus", response.text);
		
		val response2 = khttp.get("http://localhost:8091/category/no");
		assertEquals(200, response2.statusCode);
		assertEquals("Category No", response2.text);
		
		val response3 = khttp.get("http://localhost:8091/category/test");
		assertEquals(200, response3.statusCode);
		assertEquals("Category Test", response3.text);
		
		val response4 = khttp.get("http://localhost:8091/category/amongus/");
		assertEquals(200, response4.statusCode);
		assertEquals("Category Amongus", response4.text);
	}
	
	@Test
	fun Post() {
		val response = khttp.post("http://localhost:8091/test");
		assertEquals(200, response.statusCode);
		assertEquals("Test Post", response.text);
	}
	
	@Test
	fun Casing() {
		val response = khttp.get("http://localhost:8091/this-should-be-just-kebab-case");
		assertEquals(200, response.statusCode);
		assertEquals("This Should Be Just Kebab Case", response.text);
	}
	
	@Test
	fun `Relative Regex`() {
		val response = khttp.get("http://localhost:8091/number/select/158");
		assertEquals(200, response.statusCode);
		assertEquals("Number Select 158", response.text);
		
		val response2 = khttp.get("http://localhost:8091/number/select/test");
		assertEquals(404, response2.statusCode);
	}
	
	@Test
	fun `Absolute Regex`() {
		val response = khttp.get("http://localhost:8091/and/this-and-that");
		assertEquals(200, response.statusCode);
		assertEquals("And this-and-that", response.text);
		
		val response2 = khttp.get("http://localhost:8091/and/this-and-that/");
		assertEquals(200, response2.statusCode);
		assertEquals("And this-and-that", response2.text);
	}
	
	@Test
	fun `Cursed Regex`() {
		val response = khttp.get("http://localhost:8091/number/delete/54");
		assertEquals(200, response.statusCode);
		assertEquals("Number Delete 54", response.text);
		
		val response2 = khttp.get("http://localhost:8091/number/delete/54/");
		assertEquals(200, response2.statusCode);
		assertEquals("Number Delete 54", response2.text);
	}
	
	@Test
	fun `Named Route Methods`() {
		val response = khttp.get("http://localhost:8091/user/new-user");
		assertEquals(200, response.statusCode);
		assertEquals("New user", response.text);
		
		val response2 = khttp.post("http://localhost:8091/user/update-user");
		assertEquals(200, response2.statusCode);
		assertEquals("Updated user", response2.text);
		
		val response3 = khttp.delete("http://localhost:8091/user/delete-user");
		assertEquals(200, response3.statusCode);
		assertEquals("User deleted", response3.text);
	}
	
	@Test
	fun `Absolute Named Routes`() {
		val response = khttp.post("http://localhost:8091/completely/different/path/test");
		assertEquals(200, response.statusCode);
		assertEquals("Works!", response.text);
	}
	
	@Test
	fun `Error Handler`() {
		val response = khttp.post("http://localhost:8091/user/when/not/done");
		assertEquals(200, response.statusCode);
		assertEquals("Error caught in user!", response.text);
		
		val response2 = khttp.get("http://localhost:8091/number/numb");
		assertEquals(200, response2.statusCode);
		assertEquals("Error caught in number", response2.text);
		
		val response3 = khttp.post("http://localhost:8091/completely/different/path/amogus");
		assertEquals(200, response3.statusCode);
		assertEquals("Error caught in /!", response3.text);
		
		val response4 = khttp.put("http://localhost:8091/test");
		assertEquals(200, response4.statusCode);
		assertEquals("Error caught in /test!", response4.text);
	}
}

fun Application.mainModule() {
	install(BKPlugin);
}