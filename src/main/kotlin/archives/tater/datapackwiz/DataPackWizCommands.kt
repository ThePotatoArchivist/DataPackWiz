package archives.tater.datapackwiz

import archives.tater.datapackwiz.lib.*
import archives.tater.datapackwiz.lib.argument.*
import com.google.gson.JsonParser
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.exceptions.CommandSyntaxException
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import com.mojang.brigadier.suggestion.SuggestionProvider
import com.mojang.serialization.JsonOps
import net.minecraft.command.CommandRegistryAccess
import net.minecraft.command.CommandSource
import net.minecraft.command.argument.RegistryKeyArgumentType
import net.minecraft.data.DataOutput
import net.minecraft.data.DataProvider
import net.minecraft.data.DataWriter
import net.minecraft.registry.Registry
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryKeys
import net.minecraft.registry.tag.TagEntry
import net.minecraft.registry.tag.TagFile
import net.minecraft.registry.tag.TagKey
import net.minecraft.resource.*
import net.minecraft.resource.metadata.PackResourceMetadata
import net.minecraft.server.MinecraftServer
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text
import net.minecraft.util.WorldSavePath
import kotlin.io.path.bufferedReader
import kotlin.io.path.createDirectory
import kotlin.io.path.exists
import kotlin.jvm.optionals.getOrNull

const val DATAPACK_FORMAT = 15

@JvmRecord
data class TagType<T: Any>(
    val name: String,
    val dir: String,
    val registry: RegistryKey<Registry<T>>,
    val argumentType: TagArgumentType.Factory<T>,
)

val TAG_TYPES = listOf(
    TagType("block", "tags/blocks", RegistryKeys.BLOCK, TagArgumentType.BLOCK),
    TagType("entity_type", "tags/entity_types", RegistryKeys.ENTITY_TYPE, TagArgumentType.ENTITY_TYPE),
    TagType("fluid", "tags/fluids", RegistryKeys.FLUID, TagArgumentType.FLUID),
    TagType("game_event", "tags/game_events", RegistryKeys.GAME_EVENT, TagArgumentType.GAME_EVENT),
    TagType("item", "tags/items", RegistryKeys.ITEM, TagArgumentType.ITEM),
)

fun <T> tagTypeOf(registry: RegistryKey<out Registry<T>>) = TAG_TYPES.find { it.registry == registry }

val DATAPACK_SUGGEST = SuggestionProvider<ServerCommandSource> { context, builder ->
    CommandSource.suggestMatching(
        context.source.server.dataPackManager.profiles
            .stream()
            .filter { it.source == ResourcePackSource.WORLD }
            .map { StringArgumentType.escapeIfRequired(it.name) },
        builder
    )
}

fun CommandDispatcher<ServerCommandSource>.registerDPWizCommands(registryAccess: CommandRegistryAccess, environment: CommandManager.RegistrationEnvironment) {
    command("datapackwiz") {
        requires { it.hasPermissionLevel(4) }

        sub("create") {
            argument("name", StringArgumentType.string()) {
                argumentExec("description", StringArgumentType.string()) { context ->
                    val name = StringArgumentType.getString(context, "name")
                    val description = StringArgumentType.getString(context, "description")
                    val packProfile = createDatapack(context.source.server, name, description) ?: throw CommandSyntaxException(SimpleCommandExceptionType { "Failed" }) { "Failed" }
                    context.source.sendFeedback(Text.literal("Created datapack ").append(packProfile.getInformationText(false)), true)
                    1
                }
            }
        }

        sub("tag") {
            sub("add") {
                argument("datapack", StringArgumentType.string()) {
                    suggests(DATAPACK_SUGGEST)

                    fun <T : Any> TagType<T>.register() { // Needed due to some weird generics
                        sub(name) {
                            argument("tag", argumentType.factory(registryAccess)) {
                                argumentExec("elements", RegistryKeyArgumentType.registryKey(registry)) { context ->
                                    addTagEntry(
                                        context.source.server,
                                        getPackContainer(context, "datapack"),
                                        argumentType.get(context, "tag"),
                                        getRegistryKey(context, "elements", registry)
                                    )
                                    1
                                }
                            }
                        }
                    }

                    for (type in TAG_TYPES)
                        type.register()
                }
            }

            sub("edit_manual") {
                argument("datapack", StringArgumentType.string()) {
                    suggests(DATAPACK_SUGGEST)
                    fun <T : Any> TagType<T>.register() {
                        sub(name) {
                            argumentExec("tag", argumentType.factory(registryAccess)) { context ->
                                context.source.sendFeedback(Text.of(argumentType.get(context, "tag").toString()), false)
                                1
                            }
                        }
                    }

                    for (type in TAG_TYPES)
                        type.register()
                }
            }
        }
    }
}

fun createDatapack(server: MinecraftServer, name: String, description: String): ResourcePackProfile? {
    val metadata = PackResourceMetadata(Text.of(description), DATAPACK_FORMAT)

    val datapacksPath = server.getSavePath(WorldSavePath.DATAPACKS).resolve(name)

    DataProvider.writeToPath(
        DataWriter.UNCACHED,
        JsonObject (
            "pack" to PackResourceMetadata.SERIALIZER.toJson(metadata)
        ),
        datapacksPath.createDirectory()
            .resolve(ResourcePack.PACK_METADATA_NAME)
    )

    val resourcePackProfile = ResourcePackProfile.create(
        "file/$name",
        Text.literal(name),
        false,
        FileResourcePackProvider.getFactory(datapacksPath, false),
        ResourceType.SERVER_DATA,
        ResourcePackProfile.InsertionPosition.TOP,
        ResourcePackSource.WORLD
    )

    return resourcePackProfile
}

fun <T : Any> addTagEntry(server: MinecraftServer, datapack: ResourcePackProfile, tag: TagKey<T>, vararg entries: RegistryKey<T>) {
    DataOutput(server.getSavePath(WorldSavePath.DATAPACKS).resolve(datapack.name.removePrefix("file/")))
        .getResolver(
            DataOutput.OutputType.DATA_PACK,
        tagTypeOf(tag.registry)?.dir ?: throw INVALID_REGISTRY_KEY.create(tag.registry)
        )
        .resolveJson(tag.id)
        .run {
            val current = if (exists())
                TagFile.CODEC.decode(JsonOps.INSTANCE, JsonParser.parseReader(bufferedReader())).result().getOrNull()?.first
            else null

            val newEntries = entries.map { TagEntry.create(it.value) }
            val newTagFile = current?.let { TagFile(it.entries + newEntries, it.replace) } ?: TagFile(newEntries, false)

            DataProvider.writeToPath(DataWriter.UNCACHED, TagFile.CODEC.encodeStart(JsonOps.INSTANCE, newTagFile).orCommandException(), this)
        }
}
