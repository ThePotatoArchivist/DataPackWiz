package archives.tater.datapackwiz.lib.argument

import archives.tater.datapackwiz.DataPackWiz
import archives.tater.datapackwiz.mixin.RegistryKeyArgumentTypeInvoker
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.CommandSyntaxException
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType
import com.mojang.serialization.DataResult
import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry
import net.minecraft.command.argument.serialize.ConstantArgumentSerializer
import net.minecraft.registry.Registry
import net.minecraft.registry.RegistryKey
import net.minecraft.resource.ResourcePackProfile
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text

internal fun registerArgumentTypes() {
    fun <T: Any> register(name: String, type: TagArgumentType.Factory<T>) {
        ArgumentTypeRegistry.registerArgumentType(DataPackWiz.id("tag/$name"), type.`class`.java, ConstantArgumentSerializer.of(type.factory))
    }

    for ((name, type) in mapOf(
        "item" to TagArgumentType.ITEM,
        "block" to TagArgumentType.BLOCK,
        "entity_type" to TagArgumentType.ENTITY_TYPE,
        "fluid" to TagArgumentType.FLUID,
        "game_event" to TagArgumentType.GAME_EVENT,
    ))
        register(name, type)
}

val INVALID_REGISTRY_KEY = DynamicCommandExceptionType { id ->
    Text.of("Invalid registry key: $id")
}

val INVALID_DATAPACK = DynamicCommandExceptionType { id ->
    Text.of("Invalid datapack: $id")
}

@Throws(CommandSyntaxException::class)
internal fun <T> getRegistryKey(context: CommandContext<ServerCommandSource>, name: String, registry: RegistryKey<Registry<T>>): RegistryKey<T> {
    return RegistryKeyArgumentTypeInvoker.invokeGetKey(context, name, registry, INVALID_REGISTRY_KEY)
}

@Throws(CommandSyntaxException::class)
internal fun getPackContainer(context: CommandContext<ServerCommandSource>, name: String): ResourcePackProfile =
    StringArgumentType.getString(context, name).let { context.source.server.dataPackManager.getProfile(it) ?: throw INVALID_DATAPACK.create(it) }

val SERIALIZATION_ERROR = DynamicCommandExceptionType { error ->
    Text.of("Serialization Error: $error")
}

@Throws(CommandSyntaxException::class)
fun <T> DataResult<T>.orCommandException(): T = getOrThrow(false) {
    throw SERIALIZATION_ERROR.create(it)
}
