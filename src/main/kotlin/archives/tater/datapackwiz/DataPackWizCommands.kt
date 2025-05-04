package archives.tater.datapackwiz

import archives.tater.datapackwiz.lib.*
import archives.tater.datapackwiz.lib.argument.TagArgumentType
import archives.tater.datapackwiz.lib.argument.getPackContainer
import archives.tater.datapackwiz.lib.argument.getRegistryEntry
import archives.tater.datapackwiz.lib.argument.orCommandException
import com.google.gson.JsonParser
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.BoolArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.CommandSyntaxException
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import com.mojang.brigadier.suggestion.SuggestionProvider
import com.mojang.serialization.JsonOps
import net.minecraft.command.CommandRegistryAccess
import net.minecraft.command.CommandSource
import net.minecraft.command.argument.RegistryEntryArgumentType
import net.minecraft.data.DataOutput
import net.minecraft.data.DataProvider
import net.minecraft.data.DataWriter
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryKeys
import net.minecraft.registry.tag.TagEntry
import net.minecraft.registry.tag.TagFile
import net.minecraft.registry.tag.TagKey
import net.minecraft.registry.tag.TagManagerLoader
import net.minecraft.resource.*
import net.minecraft.resource.metadata.PackResourceMetadata
import net.minecraft.server.MinecraftServer
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text
import net.minecraft.util.Util
import net.minecraft.util.WorldSavePath
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import kotlin.io.path.bufferedReader
import kotlin.io.path.createDirectory
import kotlin.io.path.exists

const val DATAPACK_FORMAT = 15

val DATAPACK_SUGGEST = SuggestionProvider<ServerCommandSource> { context, builder ->
    CommandSource.suggestMatching(
        context.source.server.dataPackManager.profiles
            .stream()
            .filter { it.source == ResourcePackSource.WORLD }
            .map { StringArgumentType.escapeIfRequired(it.name) },
        builder
    )
}

val registryRefs = Registries.REGISTRIES.keys + setOf(
    RegistryKeys.BIOME,
    RegistryKeys.MESSAGE_TYPE,
    RegistryKeys.TRIM_PATTERN,
    RegistryKeys.TRIM_MATERIAL,
    RegistryKeys.DIMENSION_TYPE,
    RegistryKeys.DAMAGE_TYPE,
) // Sourced from SerializableRegistries

fun ArgumentBuilder<ServerCommandSource, *>.branchRegistries(block: ArgumentBuilder<ServerCommandSource, *>.(registryRef: RegistryKey<out Registry<Nothing>>) -> Unit) {
    for (registryRef in registryRefs) {
        sub(registryRef.value.toShortString()) {
            @Suppress("UNCHECKED_CAST")
            block(registryRef as RegistryKey<out Registry<Nothing>>) // Unspeakable crimes
            // no but seriously this generic madness is going to give me a headache
        }
    }
}

fun CommandDispatcher<ServerCommandSource>.registerDPWizCommands(registryAccess: CommandRegistryAccess, environment: CommandManager.RegistrationEnvironment) {
//    val registryRefs = Registries.REGISTRIES.keys + RegistryLoader.DYNAMIC_REGISTRIES.map { it.key }

    fun <T> ArgumentBuilder<ServerCommandSource, *>.registerTagCommand(registryRef: RegistryKey<out Registry<T>>, execute: (context: CommandContext<ServerCommandSource>, tagKey: TagKey<T>) -> Int) {
        argumentExec("tag", TagArgumentType(registryAccess, registryRef)) { context ->
            execute(
                context,
                TagArgumentType.getTagKey(context, "tag", registryRef),
            )
        }
    }

    fun <T> ArgumentBuilder<ServerCommandSource, *>.registerTagElementCommand(registryRef: RegistryKey<out Registry<T>>, allowOptional: Boolean = false, execute: (context: CommandContext<ServerCommandSource>, tagKey: TagKey<T>, element: TagEntry) -> Int) {
        argument("tag", TagArgumentType(registryAccess, registryRef)) {
            argument("entry", RegistryEntryArgumentType(registryAccess, registryRef)) {
                if (allowOptional)
                    argumentExec("optional", BoolArgumentType.bool()) { context ->
                        execute(
                            context,
                            TagArgumentType.getTagKey(context, "tag", registryRef),
                            if (BoolArgumentType.getBool(context, "optional"))
                                TagEntry.createOptional(getRegistryEntry(context, "entry", registryRef).registryKey().value)
                            else
                                TagEntry.create(getRegistryEntry(context, "entry", registryRef).registryKey().value)
                        )
                    }

                executes { context ->
                    execute(
                        context,
                        TagArgumentType.getTagKey(context, "tag", registryRef),
                        TagEntry.create(getRegistryEntry(context, "element", registryRef).registryKey().value),
                    )
                }
            }
            argument("tag_entry", TagArgumentType(registryAccess, registryRef)) {
                if (allowOptional)
                    argumentExec("optional", BoolArgumentType.bool()) { context ->
                        execute(
                            context,
                            TagArgumentType.getTagKey(context, "tag", registryRef),
                            if (BoolArgumentType.getBool(context, "optional"))
                                TagEntry.createOptionalTag(TagArgumentType.getTagKey(context, "tag_entry", registryRef).id)
                            else
                                TagEntry.createTag(TagArgumentType.getTagKey(context, "tag_entry", registryRef).id)
                        )
                    }

                executes { context ->
                    execute(
                        context,
                        TagArgumentType.getTagKey(context, "tag", registryRef),
                        TagEntry.createTag(TagArgumentType.getTagKey(context, "tag_entry", registryRef).id),
                    )
                }
            }
        }
    }

    command("datapackwiz") {
        requires { it.hasPermissionLevel(4) }

        sub("create") {
            argument("name", StringArgumentType.string()) {
                argumentExec("description", StringArgumentType.string()) { context ->
                    val name = StringArgumentType.getString(context, "name")
                    val description = StringArgumentType.getString(context, "description")
                    createDatapack(context.source.server, name, description).thenAccept {
                        it ?: throw CommandSyntaxException(SimpleCommandExceptionType { "Failed" }) { "Failed" }
                        context.source.sendFeedback(Text.literal("Created datapack ").append(it.getInformationText(false)), true)
                    }
                    1
                }
            }
        }

        sub("tag") {
            sub("add") {
                argument("datapack", StringArgumentType.string()) {
                    suggests(DATAPACK_SUGGEST)

                    branchRegistries { registryRef ->
                        registerTagElementCommand(registryRef, allowOptional = true) { context, tagKey, element ->
                            addTagEntry(
                                context.source.server,
                                getPackContainer(context, "datapack"),
                                tagKey,
                                element,
                            )
                            1
                        }
                    }
                }
            }

            sub ("remove") {
                argument("datapack", StringArgumentType.string()) {
                    suggests(DATAPACK_SUGGEST)

                    branchRegistries { registryRef ->
                        registerTagElementCommand(registryRef) { context, tagKey, element ->
                            removeTagEntry(
                                context.source.server,
                                getPackContainer(context, "datapack"),
                                tagKey,
                                element,
                            )
                            1
                        }
                    }
                }
            }

            sub("edit_manual") {
                argument("datapack", StringArgumentType.string()) {
                    suggests(DATAPACK_SUGGEST)

                    branchRegistries { registryRef ->
                        registerTagCommand(registryRef) { context, tagKey ->
                            manualEdit(context.source.server, getPackContainer(context, "datapack"), TagArgumentType.getTagKey(context, "tag", registryRef))
                            1
                        }
                    }
                }
            }
        }
    }
}

fun createDatapack(server: MinecraftServer, name: String, description: String): CompletableFuture<ResourcePackProfile?> {
    val metadata = PackResourceMetadata(Text.of(description), DATAPACK_FORMAT)

    val datapacksPath = server.getSavePath(WorldSavePath.DATAPACKS).resolve(name)

    return DataProvider.writeToPath(
        DataWriter.UNCACHED,
        JsonObject(
            "pack" to PackResourceMetadata.SERIALIZER.toJson(metadata)
        ),
        datapacksPath.createDirectory().resolve(ResourcePack.PACK_METADATA_NAME)
    ).thenApply {
        ResourcePackProfile.create(
            "file/$name",
            Text.literal(name),
            false,
            FileResourcePackProvider.getFactory(datapacksPath, false),
            ResourceType.SERVER_DATA,
            ResourcePackProfile.InsertionPosition.TOP,
            ResourcePackSource.WORLD
        )
    }
}

private fun <T : Any> getTagPath(
    server: MinecraftServer,
    datapack: ResourcePackProfile,
    tag: TagKey<T>
): Path =
    DataOutput(server.getSavePath(WorldSavePath.DATAPACKS).resolve(datapack.name.removePrefix("file/")))
        .getResolver(
            DataOutput.OutputType.DATA_PACK,
            TagManagerLoader.getPath(tag.registry),
        )
        .resolveJson(tag.id)

fun addTagEntry(server: MinecraftServer, datapack: ResourcePackProfile, tag: TagKey<*>, entry: TagEntry) {
    getTagPath(server, datapack, tag).run {
        val current = if (exists())
            TagFile.CODEC.decode(JsonOps.INSTANCE, JsonParser.parseReader(bufferedReader())).orCommandException().first
        else null

        val newTagFile = current?.let { TagFile(it.entries.filterNot { it equals entry } + entry, it.replace) } ?: TagFile(listOf(entry), false)

        DataProvider.writeToPath(DataWriter.UNCACHED, TagFile.CODEC.encodeStart(JsonOps.INSTANCE, newTagFile).orCommandException(), this)
    }
}

fun removeTagEntry(server: MinecraftServer, datapack: ResourcePackProfile, tag: TagKey<*>, entry: TagEntry): Boolean {
    getTagPath(server, datapack, tag).run {
        if (!exists()) return false

        val current = TagFile.CODEC.decode(JsonOps.INSTANCE, JsonParser.parseReader(bufferedReader())).orCommandException().first

        val newTagFile = TagFile(current.entries.filterNot { it equals entry }, current.replace)

        DataProvider.writeToPath(DataWriter.UNCACHED, TagFile.CODEC.encodeStart(JsonOps.INSTANCE, newTagFile).orCommandException(), this)
    }

    return true
}

fun <T> manualEdit(server: MinecraftServer, datapack: ResourcePackProfile, tag: TagKey<T>) {
    DataOutput(server.getSavePath(WorldSavePath.DATAPACKS).resolve(datapack.name.removePrefix("file/")))
        .getResolver(
            DataOutput.OutputType.DATA_PACK,
            TagManagerLoader.getPath(tag.registry),
        )
        .resolveJson(tag.id)
        .run {
            if(!exists())
                DataProvider.writeToPath(DataWriter.UNCACHED, TagFile.CODEC.encodeStart(JsonOps.INSTANCE, TagFile(listOf(), false)).orCommandException(), this)
            Util.getOperatingSystem().open(toFile())
        }
}
