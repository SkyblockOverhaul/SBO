package net.sbo.mod

import com.teamresourceful.resourcefulconfig.api.client.ResourcefulConfigScreen
import com.teamresourceful.resourcefulconfig.api.loader.Configurator
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.IdentifierException
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.components.toasts.SystemToast
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.util.Util
import net.sbo.mod.compat.IrisCompatibility
import net.sbo.mod.diana.DianaMobDetect
import net.sbo.mod.diana.DianaTracker
import net.sbo.mod.diana.RareMobHighlight
import net.sbo.mod.diana.achievements.AchievementManager
import net.sbo.mod.diana.achievements.AchievementManager.unlockAchievement
import net.sbo.mod.diana.burrows.BurrowDetector
import net.sbo.mod.diana.sphinx.SphinxSolver
import net.sbo.mod.general.HelpCommand
import net.sbo.mod.general.PartyCommands
import net.sbo.mod.general.Pickuplog
import net.sbo.mod.guis.Guis
import net.sbo.mod.overlays.*
import net.sbo.mod.partyfinder.PartyCheck
import net.sbo.mod.partyfinder.PartyFinderManager
import net.sbo.mod.partyfinder.PartyPlayer
import net.sbo.mod.qol.MessageHider
import net.sbo.mod.settings.Settings
import net.sbo.mod.utils.*
import net.sbo.mod.utils.data.SboDataObject
import net.sbo.mod.utils.events.*
import net.sbo.mod.utils.game.InventoryUtils
import net.sbo.mod.utils.game.Mayor
import net.sbo.mod.utils.game.TabList
import net.sbo.mod.utils.game.World
import net.sbo.mod.utils.overlay.OverlayManager
import net.sbo.mod.utils.version.UpdateChecker
import net.sbo.mod.utils.waypoint.WaypointManager
import org.slf4j.LoggerFactory

object SBOKotlin : ClientModInitializer {
	@JvmField
	val mc: Minecraft = Minecraft.getInstance()

	const val API_URL: String = "https://api.skyblockoverhaul.com"

	internal const val MOD_ID = "sbo"
	internal val logger = LoggerFactory.getLogger(MOD_ID)

	private val configurator = Configurator(MOD_ID)
	val settings = Settings.register(configurator)

	lateinit var version: String
	lateinit var mcVersion: String

	fun id(path: String, owner: String = MOD_ID): Identifier = Identifier.fromNamespaceAndPath(owner, path)

    fun userSuppliedId(path: String, owner: String = MOD_ID, onInvalid: (IdentifierException) -> Unit): Identifier? {
        return try {
            id(path = path, owner = owner)
        } catch (invalidIdentifierException: IdentifierException) {
            onInvalid(invalidIdentifierException)
            null
        }
    }

    fun toast(title: Component, message: Component) {
        mc.toastManager.addToast(
            SystemToast.multiline(mc, SystemToast.SystemToastId.PERIODIC_NOTIFICATION, title, message)
        )
    }

    fun openInBrowser(url: String) {
        Util.getPlatform().openUri(url)
    }

	override fun onInitializeClient() {
		version = FabricLoader.getInstance().getModContainer(MOD_ID)
			.map { it.metadata.version.friendlyString }
			.orElse("unknown")!!

		mcVersion = FabricLoader.getInstance().getModContainer("minecraft")
			.map { it.metadata.version.friendlyString }
			.orElse("unknown")!!

		logger.info("Initializing SBO-Kotlin, version: $version...")

		// Initialize Mayor Data
		Mayor.init()

        // Initialize scheduled tab list fetch
        TabList.init()

		// Check for updates
		UpdateChecker.check()

		// Load configuration and data
		SboDataObject.init()

		// Load Custom Sound System
		SoundHandler.init()

		// Register Annotation Processor and Events
		SboEventGeneratedRegistry.registerAll()
		SBOEvent.init()
		DianaEvents.init()

		// load Main Features
		PartyCommands.init()
		Register.command("sbo") {
			mc.schedule {
				mc.setScreen(ResourcefulConfigScreen.getFactory(MOD_ID).apply(null))
			}
		}

		Guis.register()
		HelpCommand.init()
		ClickActionManager.init()
		SboKeyBinds.init()
		WaypointManager.init()
		HypixelModApi.init()
		PartyFinderManager.init()
		PartyCheck.init()
		BurrowDetector.init()
		DianaTracker.init()
		PartyPlayer.init()
		Pickuplog.init()
		OverlayManager.init()
		SboTimerManager.init()
		Helper.init()
		Bobber.init()
		Legion.init()
		DianaStats.init()
		MagicFind.init()
		DianaMobs.init()
		DianaMobDetect.init()
		DianaLoot.init()
		AchievementManager.init()
		MessageHider.init()
		SphinxSolver.init()
		RareMobHighlight.init()
		InventoryUtils.init()

		Register.onTick(100) { unregister ->
			val player = mc.player
			if (player != null && World.isInSkyblock()) {
				if (UpdateChecker.isUpdateAvailable) UpdateChecker.printUpdateMessage()
				DianaTracker.checkMayorTracker()
				PartyPlayer.load()
				unlockAchievement(38)
				unregister()
			}
		}

		if (FabricLoader.getInstance().isModLoaded("iris")) {
			IrisCompatibility.init()
		}

		logger.info("SBO-Kotlin initialized successfully!")
	}
}
