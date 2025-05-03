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

class TagArgumentType(private val registry: RegistryKey<Registry<Any>>, registryAccess: CommandRegistryAccess) : ArgumentType<TagKey<*>> {
    private val registryWrapper = registryAccess.createWrapper(registry)

    @Throws(CommandSyntaxException::class)
    override fun parse(stringReader: StringReader): TagKey<*>? =
        TagKey.of(registry, Identifier.fromCommandInput(stringReader))

    override fun <S> listSuggestions(
        context: CommandContext<S>,
        builder: SuggestionsBuilder
    ): CompletableFuture<Suggestions> =
        CommandSource.suggestMatching(registryWrapper.streamTagKeys().map { it.id.toString() }, builder)

//    override fun getExamples(): Collection<String> = EXAMPLES

    class Serializer : ArgumentSerializer<TagArgumentType, Serializer.Properties> {
        override fun writePacket(properties: Properties, buf: PacketByteBuf) {
            buf.writeRegistryKey(properties.registry);
        }

        override fun fromPacket(buf: PacketByteBuf): Properties =
            Properties(buf.readRegistryKey(ROOT_KEY))

        override fun getArgumentTypeProperties(argumentType: TagArgumentType): Properties =
            Properties(argumentType.registry)

        override fun writeJson(properties: Properties, json: JsonObject) {
            json["registry"] = properties.registry.value.toString()
        }

        companion object {
            val ROOT_KEY: RegistryKey<Registry<Registry<Any>>> = RegistryKey.ofRegistry(Identifier("root"))
        }

        inner class Properties(
            val registry: RegistryKey<Registry<Any>> // wip
        ) : ArgumentTypeProperties<TagArgumentType> {
            override fun createType(commandRegistryAccess: CommandRegistryAccess): TagArgumentType =
                TagArgumentType(registry, commandRegistryAccess)

            override fun getSerializer(): ArgumentSerializer<TagArgumentType, Properties> {
                return this@Serializer
            }

        }

    }

    companion object {
//        private val EXAMPLES = listOf("minecraft:item minecraft:sticks", "minecraft:block c:glass_blocks", "minecraft:enchantment minecraft:riptide")
    }
}
