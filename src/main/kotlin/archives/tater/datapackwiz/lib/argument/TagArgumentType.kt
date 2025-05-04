package archives.tater.datapackwiz.lib.argument

import archives.tater.datapackwiz.lib.set
import com.google.gson.JsonObject
import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.CommandSyntaxException
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import net.minecraft.command.CommandRegistryAccess
import net.minecraft.command.CommandSource
import net.minecraft.command.argument.serialize.ArgumentSerializer
import net.minecraft.command.argument.serialize.ArgumentSerializer.ArgumentTypeProperties
import net.minecraft.network.PacketByteBuf
import net.minecraft.registry.*
import net.minecraft.registry.tag.TagKey
import net.minecraft.util.Identifier
import java.util.concurrent.CompletableFuture

class TagArgumentType<T>(private val registry: RegistryKey<out Registry<T>>, registryAccess: CommandRegistryAccess) : ArgumentType<TagKey<T>> {
    private val registryWrapper: RegistryWrapper<T> = registryAccess.createWrapper(registry)

    @Throws(CommandSyntaxException::class)
    override fun parse(stringReader: StringReader): TagKey<T>? =
        TagKey.of(registry, Identifier.fromCommandInput(stringReader))

    override fun <S> listSuggestions(
        context: CommandContext<S>,
        builder: SuggestionsBuilder
    ): CompletableFuture<Suggestions> =
        CommandSource.suggestMatching(registryWrapper.streamTagKeys().map { it.id.toString() }, builder)

//    override fun getExamples(): Collection<String> = EXAMPLES

    private fun getProperties() = Properties(registry)

    companion object Serializer : ArgumentSerializer<TagArgumentType<*>, Properties<*>> {

        private val ROOT_KEY: RegistryKey<Registry<Registry<Any>>> = RegistryKey.ofRegistry(Identifier("root"))

        fun getTagKey(context: CommandContext<*>, name: String): TagKey<*> = context.getArgument(name, TagKey::class.java)

        @Suppress("UNCHECKED_CAST")
        fun <T> getTagKey(context: CommandContext<*>, name: String, registryRef: RegistryKey<out Registry<T>>): TagKey<T> = getTagKey(context, name).also {
            assert(it.registry == registryRef) { "TagKeyArgument $name expected registry ${registryRef.value} but got ${it.registry.value}" }
        } as TagKey<T>

        override fun writePacket(properties: Properties<*>, buf: PacketByteBuf) {
            buf.writeRegistryKey(properties.registry)
        }

        override fun fromPacket(buf: PacketByteBuf): Properties<*> =
            Properties(buf.readRegistryKey(ROOT_KEY))

        override fun getArgumentTypeProperties(argumentType: TagArgumentType<*>): Properties<*> =
            argumentType.getProperties()

        override fun writeJson(properties: Properties<*>, json: JsonObject) {
            json["registry"] = properties.registry.value.toString()
        }
    }

    @JvmRecord
    data class Properties<T>(
        val registry: RegistryKey<out Registry<T>>
    ) : ArgumentTypeProperties<TagArgumentType<*>> {
        override fun createType(commandRegistryAccess: CommandRegistryAccess): TagArgumentType<*> =
            TagArgumentType(registry, commandRegistryAccess)

        override fun getSerializer(): ArgumentSerializer<TagArgumentType<*>, Properties<*>> {
            return Serializer
        }
    }
}
