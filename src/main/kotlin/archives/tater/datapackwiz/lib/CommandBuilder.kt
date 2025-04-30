package archives.tater.datapackwiz.lib

import com.mojang.brigadier.Command
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text

internal fun ServerCommandSource.sendFeedback(feedback: Text, broadcastToOps: Boolean) {
    this.sendFeedback({ feedback }, broadcastToOps)
}

internal inline fun <S> CommandDispatcher<S>.command(name: String, init: LiteralArgumentBuilder<S>.() -> Unit) {
    register(LiteralArgumentBuilder.literal<S>(name).apply(init))
}

internal fun <S> CommandDispatcher<S>.commandExec(name: String, command: Command<S>) {
    command(name) {
        executes(command)
    }
}

internal inline fun <S> ArgumentBuilder<S, *>.sub(name: String, init: LiteralArgumentBuilder<S>.() -> Unit) {
    then(LiteralArgumentBuilder.literal<S>(name).apply(init))
}

internal fun <S> ArgumentBuilder<S, *>.subExec(name: String, command: Command<S>) {
    sub(name) {
        executes(command)
    }
}

internal inline fun ArgumentBuilder<ServerCommandSource, *>.argument(name: String, type: ArgumentType<*>, init: RequiredArgumentBuilder<ServerCommandSource, *>.() -> Unit) {
    then(CommandManager.argument(name, type).apply(init))
}

internal fun ArgumentBuilder<ServerCommandSource, *>.argumentExec(name: String, type: ArgumentType<*>, command: Command<ServerCommandSource>) {
    argument(name, type) {
        executes(command)
    }
}

internal fun RequiredArgumentBuilder<ServerCommandSource, *>.arguments(vararg arguments: RequiredArgumentBuilder<ServerCommandSource, *>, init: RequiredArgumentBuilder<ServerCommandSource, *>.() -> Unit) {
    arguments.foldRight(init) { arg, acc ->
        {
            then(arg).acc()
        }
    }.invoke(this)
}

internal fun RequiredArgumentBuilder<ServerCommandSource, *>.argumentsExec(vararg arguments: RequiredArgumentBuilder<ServerCommandSource, *>, command: Command<ServerCommandSource>) {
    arguments(*arguments) {
        executes(command)
    }
}
