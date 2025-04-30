package archives.tater.datapackwiz.mixin;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.resource.ResourcePackProfile;
import net.minecraft.server.command.DatapackCommand;
import net.minecraft.server.command.ServerCommandSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(DatapackCommand.class)
public interface DatapackCommandInvoker {
    @Invoker
    static ResourcePackProfile invokeGetPackContainer(CommandContext<ServerCommandSource> context, String name, boolean enable) throws CommandSyntaxException {
        throw new AssertionError();
    }
}
