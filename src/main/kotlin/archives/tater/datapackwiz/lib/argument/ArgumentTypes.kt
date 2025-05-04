package archives.tater.datapackwiz.lib.argument

import archives.tater.datapackwiz.DataPackWiz
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.CommandSyntaxException
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType
import com.mojang.serialization.DataResult
import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry
import net.minecraft.command.argument.RegistryEntryArgumentType
import net.minecraft.registry.Registry
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.entry.RegistryEntry
import net.minecraft.resource.ResourcePackProfile
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text

internal fun registerArgumentTypes() {
    ArgumentTypeRegistry.registerArgumentType(DataPackWiz.id("tagkey"), TagArgumentType::class.java, TagArgumentType.Serializer)
}

val INVALID_DATAPACK = DynamicCommandExceptionType { id ->
    Text.of("Invalid datapack: $id")
}

@Throws(CommandSyntaxException::class)
internal fun getPackContainer(context: CommandContext<ServerCommandSource>, name: String): ResourcePackProfile =
    StringArgumentType.getString(context, name).let { context.source.server.dataPackManager.getProfile(it) ?: throw INVALID_DATAPACK.create(it) }

@Suppress("UNCHECKED_CAST")
@Throws(CommandSyntaxException::class)
internal fun <T> getRegistryEntry(context: CommandContext<ServerCommandSource>, name: String, registryRef: RegistryKey<out Registry<T>>): RegistryEntry.Reference<T> =
        RegistryEntryArgumentType.getRegistryEntry(context, name, registryRef as RegistryKey<Registry<T>>) // should be safe right?

val SERIALIZATION_ERROR = DynamicCommandExceptionType { error ->
    Text.of("Serialization Error: $error")
}

@Throws(CommandSyntaxException::class)
fun <T> DataResult<T>.orCommandException(): T = result().orElseThrow {
    SERIALIZATION_ERROR.create(error())
}
