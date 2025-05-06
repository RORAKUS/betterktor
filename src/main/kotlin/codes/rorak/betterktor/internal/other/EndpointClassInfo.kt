package codes.rorak.betterktor.internal.other

import codes.rorak.betterktor.annotations.InjectOption
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

typealias InjectedProperties = Map<KProperty1<*, *>, Pair<InjectOption, String?>>;
typealias MutableInjectedProperties = MutableMap<KProperty1<*, *>, Pair<InjectOption, String?>>;

val InjectedProperties.string
	get() = entries.joinToString("\n\t") { (prop, option) ->
		"${prop.name}: ${option.first.name}(${option.second ?: ""})"
	}

internal data class EndpointClassInfo(
	val clazz: KClass<*>,
	val injectedProperties: InjectedProperties
);