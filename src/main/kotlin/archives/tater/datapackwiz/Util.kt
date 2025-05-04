package archives.tater.datapackwiz

import net.minecraft.util.Identifier

fun Identifier.toShortString() = if (namespace == Identifier.DEFAULT_NAMESPACE) path else toString()
