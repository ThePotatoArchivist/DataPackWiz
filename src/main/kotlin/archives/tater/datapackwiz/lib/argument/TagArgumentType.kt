package archives.tater.datapackwiz.lib.argument

import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.CommandSyntaxException
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import net.minecraft.block.Block
import net.minecraft.command.CommandRegistryAccess
import net.minecraft.command.CommandSource
import net.minecraft.entity.EntityType
import net.minecraft.fluid.Fluid
import net.minecraft.item.Item
import net.minecraft.registry.*
import net.minecraft.registry.tag.TagKey
import net.minecraft.util.Identifier
import net.minecraft.world.event.GameEvent
import java.util.concurrent.CompletableFuture
import kotlin.reflect.KClass

abstract class TagArgumentType<T>(private val registry: RegistryKey<Registry<T>>, registryAccess: CommandRegistryAccess) : ArgumentType<TagKey<*>> {
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

    abstract class Factory<T: Any>(private val registry: RegistryKey<Registry<T>>, val `class`: KClass<out TagArgumentType<T>>, val factory: (CommandRegistryAccess) -> TagArgumentType<T>) {
        @Suppress("UNCHECKED_CAST")
        fun get(context: CommandContext<*>, name: String): TagKey<T> =
            context.getArgument(name, TagKey::class.java) as TagKey<T>
    }

    class ITEM(registryAccess: CommandRegistryAccess) : TagArgumentType<Item>(RegistryKeys.ITEM, registryAccess) {
        companion object : Factory<Item>(RegistryKeys.ITEM, ITEM::class, ::ITEM)
    }

    class BLOCK(registryAccess: CommandRegistryAccess) : TagArgumentType<Block>(RegistryKeys.BLOCK, registryAccess) {
        companion object : Factory<Block>(RegistryKeys.BLOCK, BLOCK::class, ::BLOCK)
    }

    class FLUID(registryAccess: CommandRegistryAccess) : TagArgumentType<Fluid>(RegistryKeys.FLUID, registryAccess) {
        companion object : Factory<Fluid>(RegistryKeys.FLUID, FLUID::class, ::FLUID)
    }

    class ENTITY_TYPE(registryAccess: CommandRegistryAccess) : TagArgumentType<EntityType<*>>(RegistryKeys.ENTITY_TYPE, registryAccess) {
        companion object : Factory<EntityType<*>>(RegistryKeys.ENTITY_TYPE, ENTITY_TYPE::class, ::ENTITY_TYPE)
    }

    class GAME_EVENT(registryAccess: CommandRegistryAccess) : TagArgumentType<GameEvent>(RegistryKeys.GAME_EVENT, registryAccess) {
        companion object : Factory<GameEvent>(RegistryKeys.GAME_EVENT, GAME_EVENT::class, ::GAME_EVENT)
    }

    companion object {
//        private val EXAMPLES = listOf("minecraft:item minecraft:sticks", "minecraft:block c:glass_blocks", "minecraft:enchantment minecraft:riptide")
    }
}
