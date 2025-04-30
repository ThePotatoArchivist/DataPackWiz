package archives.tater.datapackwiz.mixin;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.minecraft.command.argument.RegistryKeyArgumentType;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.command.ServerCommandSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(RegistryKeyArgumentType.class)
public interface RegistryKeyArgumentTypeInvoker {
    @Invoker
    static <T> RegistryKey<T> invokeGetKey(
            CommandContext<ServerCommandSource> context, String name, RegistryKey<Registry<T>> registryRef, DynamicCommandExceptionType invalidException
    ) throws CommandSyntaxException {
        throw new AssertionError();
    }
}
