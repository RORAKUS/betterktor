package codes.rorak.betterktor.internal.other

internal object Interrupt: Error() {
	private fun readResolve(): Any = Interrupt;
};