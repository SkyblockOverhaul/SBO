package net.sbo.mod.settings

import net.sbo.mod.SBOKotlin
import net.sbo.mod.SBOKotlin.mc
import net.sbo.mod.config.ConfigEntry
import net.sbo.mod.config.ModVersion
import net.sbo.mod.config.WhatsNew
import net.sbo.mod.config.isOn
import net.sbo.mod.config.newSettings
import net.sbo.mod.config.switchTo
import net.sbo.mod.config.switchable
import net.sbo.mod.guis.HubGui
import net.sbo.mod.settings.categories.General
import net.sbo.mod.utils.chat.Chat
import net.sbo.mod.utils.data.DataManager

// The settings new to a player after updating, for What's New in the settings and the "Disable All New Settings" switch.
// A beta lists what that beta added, a release everything its betas added; versions the player skipped are included too.
object SettingsChanges {
    private val current: ModVersion? by lazy { ModVersion.parse(SBOKotlin.version) }

    // Settings with a since after this one are listed, null when there is nothing to list (a new install)
    private var after: ModVersion? = null

    /** After the config and SboData are loaded, before the features read their settings. */
    fun init() {
        val current = current ?: return
        val data = DataManager.sboData
        if (data.settingsVersion != SBOKotlin.version) {
            val last = ModVersion.parse(data.settingsVersion)
            if (last == null && !Settings.existed) {
                // A new install, every setting is new so none is
                data.settingsPreviousVersion = SBOKotlin.version
            } else {
                data.settingsPreviousVersion = data.settingsVersion
                // Only what arrived since the version the player ran, a setting turned back on isn't turned off again
                if (General.disableNewSettings) turnOffNew(last ?: current.lineStart, current)
            }
            data.settingsVersion = SBOKotlin.version
            data.save()
        }
        val previous = ModVersion.parse(data.settingsPreviousVersion)
        after = when {
            previous == current -> null
            previous == null -> current.lineStart
            else -> minOf(previous, current.lineStart)
        }
    }

    private fun turnOffNew(after: ModVersion, upTo: ModVersion) {
        val turnedOff = newSettings(Settings, after, upTo)
            .map { it.element as ConfigEntry<*> }
            .filter { switchable(it) && isOn(it) && it.value == it.default }
        if (turnedOff.isEmpty()) return
        turnedOff.forEach { switchTo(it, false) }
        Settings.save()
        SBOKotlin.logger.info("Turned off {} new settings: {}", turnedOff.size, turnedOff.joinToString { it.id })
    }

    private fun hits() = after?.let { after -> current?.let { newSettings(Settings, after, it) } }.orEmpty()

    /** null while there is nothing new. [force] opens the settings on it even after it was seen once. */
    fun whatsNew(force: Boolean = false): WhatsNew? {
        val hits = hits()
        if (hits.isEmpty()) return null
        val data = DataManager.sboData
        return WhatsNew(
            title = "What's New in ${SBOKotlin.version}",
            text = "The settings added since your last update. Each one says what it does and can be switched right here, " +
                "Enable all and Disable all switch every new feature at once. Click where a setting is (under its name) to see it in its category.",
            hits = hits,
            openFirst = force || data.whatsNewOpenedVersion != SBOKotlin.version,
            seen = data.whatsNewOpenedVersion == SBOKotlin.version,
            extra = General.entries.firstOrNull { it.id == "disableNewSettings" },
            onOpened = {
                if (data.whatsNewOpenedVersion != SBOKotlin.version) {
                    data.whatsNewOpenedVersion = SBOKotlin.version
                    data.save()
                }
            },
        )
    }

    /** Once per version, after joining SkyBlock: a chat message that opens What's New. */
    fun announce() {
        val data = DataManager.sboData
        if (data.whatsNewChatVersion == SBOKotlin.version) return
        data.whatsNewChatVersion = SBOKotlin.version
        data.save()
        val count = hits().size
        // Not needed once the settings were opened, they showed What's New already
        if (count == 0 || data.whatsNewOpenedVersion == SBOKotlin.version) return
        val settings = if (count == 1) "1 new setting" else "$count new settings"
        Chat.clickableChat(
            "§6[SBO] §e$settings in §a${SBOKotlin.version}§e, see what they do and switch them off if you like. §b[Click]",
            "§eOpen What's New in the SBO settings"
        ) { mc.schedule { HubGui.openSettings(whatsNew = true) } }
    }
}
