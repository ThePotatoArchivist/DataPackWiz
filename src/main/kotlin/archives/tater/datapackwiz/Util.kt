package archives.tater.datapackwiz

import archives.tater.datapackwiz.mixin.TagEntryAccessor
import net.minecraft.registry.tag.TagEntry
import net.minecraft.util.Identifier

fun Identifier.toShortString(): String = if (namespace == Identifier.DEFAULT_NAMESPACE) path else toString()

infix fun TagEntry.equals(other: TagEntry): Boolean = this == other || (
        (this as TagEntryAccessor).tag == (other as TagEntryAccessor).tag &&
        (this as TagEntryAccessor).id == (other as TagEntryAccessor).id
)
