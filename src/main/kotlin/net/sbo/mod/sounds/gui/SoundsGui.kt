package net.sbo.mod.sounds.gui

import net.sbo.guilib.core.dom.component
import net.sbo.guilib.core.dsl.NodeBuilder
import net.sbo.guilib.core.dsl.button
import net.sbo.guilib.core.dsl.div
import net.sbo.guilib.core.dsl.h2
import net.sbo.guilib.core.dsl.header
import net.sbo.guilib.core.dsl.scroll
import net.sbo.guilib.core.dsl.select
import net.sbo.guilib.core.dsl.slider
import net.sbo.guilib.core.dsl.span
import net.sbo.guilib.core.dsl.useToast
import net.sbo.guilib.fabric.GuiLib
import net.sbo.mod.utils.SoundHandler
import net.sbo.mod.utils.data.DataManager
import net.sbo.mod.utils.data.configs.sound.SoundSettingsData
import kotlin.math.roundToInt
import kotlin.reflect.KMutableProperty1

object SoundsGui {
    private val STYLES = listOf("sbo:ui/sounds/sounds.css")

    private val SCALES: List<Float?> = listOf(null, 1f, 1.5f, 2f, 2.5f, 3f, 4f)

    /** Opens the window. Must run on the client thread. */
    fun open() {
        GuiLib.open(App, STYLES, title = "SBO Sounds")
    }

    private data class SoundSetting(
        val label: String,
        val sound: KMutableProperty1<SoundSettingsData, String>,
        val volume: KMutableProperty1<SoundSettingsData, Float>
    )

    private val GROUPS = listOf(
        "Spawns" to listOf(
            SoundSetting("Rare Mob Spawn", SoundSettingsData::rareMobSound, SoundSettingsData::rareMobVolume),
            SoundSetting("Inquisitor Spawn", SoundSettingsData::inqSound, SoundSettingsData::inqVolume),
            SoundSetting("Sphinx Spawn", SoundSettingsData::sphinxSound, SoundSettingsData::sphinxVolume),
            SoundSetting("King Minos Spawn", SoundSettingsData::kingSound, SoundSettingsData::kingVolume),
            SoundSetting("Manticore Spawn", SoundSettingsData::mantiSound, SoundSettingsData::mantiVolume),
            SoundSetting("Cocoon", SoundSettingsData::cocoonSound, SoundSettingsData::cocoonVolume),
            SoundSetting("Burrow Found", SoundSettingsData::burrowFoundSound, SoundSettingsData::burrowVolume),
        ),
        "Low HPs" to listOf(
            SoundSetting("Inquisitor Low HP", SoundSettingsData::lowInqHpSound, SoundSettingsData::lowInqHpVoume),
            SoundSetting("Sphinx Low HP", SoundSettingsData::lowSphinxHpSound, SoundSettingsData::lowSphinxHpVoume),
            SoundSetting("King Minos Low HP", SoundSettingsData::lowKingHpSound, SoundSettingsData::lowKingHpVoume),
            SoundSetting("Manticore Low HP", SoundSettingsData::lowMantiHpSound, SoundSettingsData::lowMantiHpVoume),
        ),
        "Drops" to listOf(
            SoundSetting("Chimera Drop", SoundSettingsData::chimSound, SoundSettingsData::chimVolume),
            SoundSetting("Brain Food Drop", SoundSettingsData::bfSound, SoundSettingsData::bfVolume),
            SoundSetting("Manti-Core Drop", SoundSettingsData::coreSound, SoundSettingsData::coreVolume),
            SoundSetting("Fateful Stinger Drop", SoundSettingsData::stingerSound, SoundSettingsData::stingerVolume),
            SoundSetting("Shimmering Wool Drop", SoundSettingsData::woolSound, SoundSettingsData::woolVolume),
            SoundSetting("Minos Relic Drop", SoundSettingsData::relicSound, SoundSettingsData::relicVolume),
            SoundSetting("Daedalus Stick Drop", SoundSettingsData::stickSound, SoundSettingsData::stickVolume),
            SoundSetting("Misc Drop", SoundSettingsData::miscDropSound, SoundSettingsData::miscDropVolume),
        ),
    )

    private val App = component("Sounds") {
        val settings = DataManager.soundSettingsData
        val sounds = useStateLazy { SoundHandler.getAvailableSoundsWithExt() }
        val toast = useToast()

        fun reload() {
            val before = sounds.value
            val after = SoundHandler.getAvailableSoundsWithExt()
            sounds.set(after)
            val added = after.count { it !in before }
            val removed = before.count { it !in after }
            val changes = listOfNotNull(
                "$added new".takeIf { added > 0 },
                "$removed removed".takeIf { removed > 0 }
            )
            if (changes.isEmpty()) toast.info("No changes, ${after.size} sounds in the folder.", title = "Sounds reloaded")
            else toast.success("${changes.joinToString(", ")}, ${after.size} sounds in the folder.", title = "Sounds reloaded")
        }
        var master by useState(percent(settings.masterVolume))
        var uiScale by useState(settings.uiScale?.takeIf { it in SCALES })
        useScreenScale(uiScale)

        // Changes are saved right away, this catches a slider still being dragged when the window closes
        useEffect { onCleanup { settings.save() } }

        div(className = "snd-window") {
            header(className = "snd-header") {
                span(className = "snd-title") { +"Sounds" }
                div(className = "snd-spacer")
                button(className = "snd-close", title = "Close", onClick = { GuiLib.close() }) { +"✕" }
            }

            scroll(className = "snd-body guilib-autohide") {
                h2(className = "snd-section") { +"General" }
                settingRow("Master Volume", "Applies to every sound below.") {
                    slider(
                        value = master,
                        onChange = { master = it; settings.masterVolume = it / 100f },
                        onChangeEnd = { settings.save() },
                        showValue = true,
                        format = { "$it%" },
                        className = "snd-volume"
                    )
                }
                settingRow("Size", "How big the sound window is. Auto uses your Minecraft GUI scale.") {
                    select(value = scaleId(uiScale), onChange = { e ->
                        uiScale = e.value.toFloatOrNull()
                        settings.uiScale = uiScale
                        settings.save()
                    }, className = "snd-scale-select") {
                        SCALES.forEach { scale -> option(scaleId(scale), if (scale == null) "Auto" else scaleId(scale)) }
                    }
                }
                settingRow(
                    "Sound Folder",
                    "Put your own sounds in here (${SoundHandler.SUPPORTED_EXTENSIONS.joinToString(", ")}), then reload the list."
                ) {
                    div(className = "snd-buttons") {
                        button(onClick = { SoundHandler.openSoundFolder() }) { +"Open Folder" }
                        button(title = "Looks for new files in the sound folder", onClick = { reload() }) { +"Reload" }
                    }
                }

                GROUPS.forEach { (title, group) ->
                    h2(className = "snd-section", key = "title:$title") { +title }
                    group.forEach { setting -> SoundRow(SoundRowProps(setting, sounds.value), key = setting.label) }
                }
            }
        }
    }

    private data class SoundRowProps(val setting: SoundSetting, val sounds: List<String>)

    private val SoundRow = component<SoundRowProps>("SoundRow") { (setting, sounds) ->
        val settings = DataManager.soundSettingsData
        var sound by useState(setting.sound.get(settings))
        var volume by useState(percent(setting.volume.get(settings)))

        div(className = "snd-row") {
            span(className = "snd-label", title = setting.label) { +setting.label }
            select(
                value = sound,
                onChange = { e ->
                    sound = e.value
                    setting.sound.set(settings, e.value)
                    settings.save()
                },
                searchable = true,
                searchPlaceholder = "Search sounds",
                className = "snd-sound"
            ) {
                option("", "(None)")
                // A file that was removed from the folder still shows as picked
                if (sound.isNotEmpty() && sound !in sounds) option(sound, sound, title = "Not in the sound folder anymore")
                sounds.forEach { option(it, it) }
            }
            slider(
                value = volume,
                onChange = { volume = it; setting.volume.set(settings, it / 100f) },
                onChangeEnd = { settings.save() },
                showValue = true,
                format = { "$it%" },
                className = "snd-volume"
            )
            button(
                className = "snd-test",
                disabled = sound.isEmpty(),
                title = if (sound.isEmpty()) "Pick a sound first" else "Plays the sound at this volume",
                onClick = { SoundHandler.playCustomSound(sound, volume / 100f) }
            ) { +"Test" }
        }
    }

    private fun NodeBuilder.settingRow(title: String, text: String, control: NodeBuilder.() -> Unit) {
        div(className = "snd-setting") {
            div(className = "snd-setting-text") {
                div(className = "snd-setting-title") { +title }
                div(className = "snd-hint") { +text }
            }
            control()
        }
    }

    private fun scaleId(scale: Float?): String = when {
        scale == null -> "auto"
        scale % 1f == 0f -> scale.toInt().toString()
        else -> scale.toString()
    }

    private fun percent(volume: Float): Int = (volume * 100).roundToInt().coerceIn(0, 100)
}
