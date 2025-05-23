package codes.rorak.betterktor.internal.resolver

import codes.rorak.betterktor.annotations.*
import codes.rorak.betterktor.api.BetterKtor
import codes.rorak.betterktor.internal.endpoints.EndpointClass
import codes.rorak.betterktor.internal.other.InjectedProperties
import codes.rorak.betterktor.internal.other.Path
import codes.rorak.betterktor.util.BetterKtorError
import codes.rorak.betterktor.util.CasingMethod
import kotlinx.coroutines.sync.Mutex
import kotlin.reflect.KAnnotatedElement
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.memberProperties

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
	
	// process injected properties
	fun injectedPropertiesProcessor(
		clazz: KClass<*>,
		cache: BetterKtorCache
	): InjectedProperties = clazz.memberProperties.mapNotNull { p ->
		cache.current(p);
		
		// get the inject annotation
		var injectAnnotation = p.findAnnotation<Inject>();
		
		// check for the inject call annotation
		if (p.hasAnnotation<InjectCall>()) {
			if (injectAnnotation != null) throw BetterKtorError(
				"A property cannot have the Inject annotation and the InjectCall annotation at the same time!", cache
			);
			// set the inject annotation
			injectAnnotation = Inject(InjectOption.CALL);
		}
		
		// skip normal properties
		if (injectAnnotation == null) return@mapNotNull null;
		
		// check if the property is mutable
		if (p !is KMutableProperty1<*, *>) throw BetterKtorError("An injected property must be mutable!", cache);
		
		// check the parameter -> cannot be empty if required
		if (injectAnnotation.parameter.isEmpty() && injectAnnotation.option.parameter == InjectOption.Parameter.REQUIRED)
			throw BetterKtorError("Parameter for the option '${injectAnnotation.option.name}' is required!", cache);
		
		// check the property type
		val propertyType = p.returnType.classifier as? KClass<*>;
		if (propertyType !in injectAnnotation.option.allowedTypes)
			throw BetterKtorError(
				"The type '${p.returnType}' for the option '${injectAnnotation.option.name}' is not allowed!", cache
			);
		
		// return the pair
		return@mapNotNull p to (injectAnnotation.option to injectAnnotation.parameter);
	}.toMap();
}