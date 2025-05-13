package codes.rorak.betterktor.internal.resolver

import codes.rorak.betterktor.annotations.Auth
import codes.rorak.betterktor.annotations.AuthOption
import codes.rorak.betterktor.annotations.Casing
import codes.rorak.betterktor.annotations.CasingOption
import codes.rorak.betterktor.annotations.NoMutex
import codes.rorak.betterktor.annotations.RelativeTo
import codes.rorak.betterktor.api.BetterKtor
import codes.rorak.betterktor.internal.endpoints.EndpointClass
import codes.rorak.betterktor.internal.other.Path
import codes.rorak.betterktor.util.BetterKtorError
import codes.rorak.betterktor.util.CasingMethod
import kotlinx.coroutines.sync.Mutex
import kotlin.reflect.KAnnotatedElement
import kotlin.reflect.KFunction
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation

internal object CommonProcessor {
	// processes a path of an element
	fun pathProcessor(
		element: KAnnotatedElement,
		parent: EndpointClass?,
		cache: BetterKtorCache,
		packageName: String,
		name: String
	): Pair<Path, CasingMethod> {
		// set the base path object -> the path of the parent class or a new path object
		val pathObject = parent?.path ?: Path(cache)
			.withPackage(packageName);
		
		// get the annotations
		val pathAnnotation = element.findAnnotation<codes.rorak.betterktor.annotations.Path>();
		val relativeAnnotation = element.findAnnotation<RelativeTo>();
		val casingAnnotation = element.findAnnotation<Casing>();
		
		// get the parent casing, if reset -> null
		val parentCasing = if (casingAnnotation?.value == CasingOption.DEFAULT) null else parent?.casing;
		// get the path provided by the annotation or null if empty
		val annotationPath = pathAnnotation?.path?.takeIf { it.isNotEmpty() };
		// get the relative value -> by relative annotation or path annotation
		val relativeOption = relativeAnnotation?.option ?: pathAnnotation?.relativeTo;
		// get the final casing -> annotation casing -> parent casing -> default casing
		val casing = casingAnnotation?.value?.takeIf { it != CasingOption.DEFAULT }?.method
			?: parentCasing ?: cache.config.defaultCasing;
		
		if (element is KFunction<*>)
			pathObject.withFunction(
				path = annotationPath ?: name,
				fileClass = parent?.clazz?.simpleName,
				relativeTo = relativeOption,
				regex = pathAnnotation?.regex == true,
				casing = casing
			);
		else
			pathObject.withClass(
				path = annotationPath ?: name,
				relativeTo = relativeOption,
				regex = pathAnnotation?.regex == true,
				casing = casing
			);
		
		return pathObject to casing;
	}
	
	// processes the auth of an element
	fun authProcessor(
		element: KAnnotatedElement,
		parent: EndpointClass?,
		cache: BetterKtorCache
	): Pair<String, AuthOption>? {
		// get the annotation
		val authAnnotation = element.findAnnotation<Auth>();
		
		// if there is no auth annotation, use the parent auth
		if (authAnnotation == null) return parent?.auth;
		
		// auth id cannot be set when option is none
		if (authAnnotation.option == AuthOption.NONE && authAnnotation.id.isNotEmpty())
			throw BetterKtorError("Auth annotation cannot have a set id, if the option is set to NONE!", cache);
		// if the auth option is none, return (endpoint.auth will be null)
		if (authAnnotation.option == AuthOption.NONE) return null;
		
		// the auth id will be config default if empty
		val authId = authAnnotation.id.ifEmpty { cache.config.defaultAuthId };
		
		// return the auth value
		return authId to authAnnotation.option;
	}
	
	// process the mutex of an element
	fun mutexProcessor(
		element: KAnnotatedElement,
		parentMutex: Mutex?,
		cache: BetterKtorCache
	): Mutex? {
		// if no mutex annotation found, return null
		if (element.hasAnnotation<NoMutex>()) return null;
		
		// get the annotation
		val mutexAnnotation = element.findAnnotation<codes.rorak.betterktor.annotations.Mutex>();
		// get the mutex object
		val mutexObject = mutexAnnotation?.name?.let { BetterKtor.mutexMap.getOrPut(it) { Mutex() } };
		// get the final mutex -> annotation -> parent -> default only if mutex must be present
		val mutex = mutexObject ?: parentMutex ?: if (cache.config.useMutex) BetterKtor.defaultMutex else null;
		
		// return the mutex value
		return mutex;
	}
}