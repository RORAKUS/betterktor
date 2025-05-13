package codes.rorak.betterktor.internal.other

import codes.rorak.betterktor.BetterKtorConfig
import codes.rorak.betterktor.annotations.*
import codes.rorak.betterktor.api.BetterKtor
import io.ktor.http.*
import java.net.URL
import java.util.*
import kotlin.reflect.KAnnotatedElement
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.valueParameters
import kotlin.reflect.jvm.javaMethod

internal val ANY_CALL_METHOD = HttpMethod("ANY");

// Internal Util API
internal object Util {
	
	fun fetchBasePackage(): String {
		// the package for this plugin
		val pluginPackage = BetterKtorConfig::class.java.`package`.name;
		// all packages starts, that will be excluded from the search
		val excludedPackages = listOf(pluginPackage, "java.lang", "io.ktor", "jdk", "executors");
		
		// for every entry in the stack trace / call stack
		val currentClass = Thread.currentThread().stackTrace.find { se ->
			// return it if it doesn't start with any value from excluded packages
			excludedPackages.none { se.className.startsWith(it) }
		};
		
		// if there is no valid class in the call stack throw
		checkNotNull(currentClass);
		
		// remove the name of the class, if the class isn't in any package, return an empty string
		val currentPackage = currentClass.className.substringBeforeLast(".", "");
		
		return currentPackage;
	}
	
	fun resource(name: String): URL? = {}::class.java.getResource(name);
}

// Logger
internal val log = BetterKtor.logger;
internal fun debug(message: String) = log.debug(message);

// Other Util

internal fun String.capitalize() =
	this.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() };

internal fun String.decapitalize() =
	this.replaceFirstChar { if (it.isUpperCase()) it.lowercase(Locale.getDefault()) else it.toString() };

internal fun String.dropLines(num: Int) = split("\n").drop(num).joinToString("\n");

// if both strings contain something, join them with a dot
internal infix fun String.dotJoin(s: String) = if (this.isNotEmpty() && s.isNotEmpty()) "$this.$s" else "$this$s";

// returns true if any element from the collection is also contained in the other collection
internal fun <T> Iterable<T>.containsAnyFrom(collection: Iterable<T>) = this.intersect(collection).isNotEmpty();

// finds a key by value in a map
internal fun <K, V> Map<K, V>.getKey(value: V): K? = this.entries.firstOrNull { it.value == value }?.key;

// removes the first element of a collection matching a predicate
internal fun <T> MutableList<T>.removeFirst(predicate: (T) -> Boolean): Boolean {
	val index = indexOfFirst(predicate);
	if (index == -1) return false;
	
	removeAt(index);
	return true;
}

// Reflection API Extensions
private val List<KParameter>.info get() = map { it.type to it.isVararg }
private fun KFunction<*>.isSimilarTo(other: KFunction<*>) =
	name == other.name && valueParameters.info == other.valueParameters.info && returnType == other.returnType;

internal val KFunction<*>.isTopLevel get() = javaMethod?.declaringClass?.getAnnotation(Metadata::class.java)?.kind == 2;
internal val KAnnotatedElement.annotationClasses get() = annotations.map { it.annotationClass };
internal val Annotation.isTypeAnnotation
	get() = this is Endpoint ||
			this is Websocket ||
			this is SSE ||
			this is ErrorHandler ||
			this is ComplexWebsocket;
internal val Annotation.isHandleAnnotation
	get() = this is WebsocketHandle ||
			this is SSEHandle ||
			this is ErrorHandlerHandle;
internal val Annotation.isMethodAnnotation
	get() = this is AnyCall ||
			this is Get ||
			this is Post ||
			this is Put ||
			this is Delete ||
			this is Patch ||
			this is Method;
internal val Annotation.isEndpointMethodAnnotation
	get() = this is EndpointAnyCall ||
			this is EndpointGet ||
			this is EndpointPost ||
			this is EndpointPut ||
			this is EndpointDelete ||
			this is EndpointPatch ||
			this is EndpointMethod;
internal val Annotation.isAnyMethodAnnotation get() = isMethodAnnotation || isEndpointMethodAnnotation;

internal val KClass<*>.isCWType
	get() = isSubclassOf(codes.rorak.betterktor.api.ComplexWebsocket::class);

internal val KFunction<*>.declaringClass get() = javaMethod?.declaringClass?.kotlin!!;

internal fun KFunction<*>.isOverridenFrom(clazz: KClass<*>) =
	// if the method's declaring class extends the class and the class contains a method with the same signature
	this.declaringClass.isSubclassOf(clazz) && clazz.declaredMemberFunctions.any { it.isSimilarTo(this) };

internal fun KFunction<*>.isOverridenFrom(function: KFunction<*>) =
	this.declaringClass.isSubclassOf(function.declaringClass) && function.isSimilarTo(this);

// call a function autosorting its arguments
internal fun KFunction<*>.callSorted(self: Any?, args: List<Any?>) {
	TODO();
}

internal fun KFunction<*>.callSorted(self: Any?, vararg args: Any?) = callSorted(self, args.toList());