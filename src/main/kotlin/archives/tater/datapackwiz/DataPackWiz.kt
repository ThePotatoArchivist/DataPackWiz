package archives.tater.datapackwiz

import archives.tater.datapackwiz.lib.argument.registerArgumentTypes
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.util.Identifier
import org.slf4j.LoggerFactory

object DataPackWiz : ModInitializer {
	const val MOD_ID = "datapackwiz"

	fun id(path: String) = Identifier(MOD_ID, path)

    val logger = LoggerFactory.getLogger(MOD_ID)

	override fun onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.
		registerArgumentTypes()
		CommandRegistrationCallback.EVENT.register { dispatcher, registryAccess, environment ->
			dispatcher.registerDPWizCommands(registryAccess, environment)
		}
	}
}
