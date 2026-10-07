package net.sbo.mod.settings.categories

import com.google.gson.JsonObject
import net.sbo.mod.config.Category
import net.sbo.mod.config.Choice
import net.sbo.mod.guis.look.SboThemes
import net.sbo.mod.guis.look.UiScale

object Themes : Category("Themes") {
    override val description = "The look of every SBO window. Windows with their own Theme or Size setting can still pick something else there."

    // False until a config file with this category was loaded, SboLook takes the party finder theme over then
    var inFile = false
        private set

    override fun load(json: JsonObject) {
        inFile = true
        super.load(json)
    }

    init {
        separator {
            this.title = "Theme"
        }
    }

    var theme by choice(SboThemes.DEFAULT) {
        this.name = Literal("Theme")
        this.description = Literal("Colors of all SBO windows: party finder, settings, events, achievements, sounds, cloud sync and the hub. Hover a theme in the list to see what it does.")
        this.options = { SboThemes.all().map { Choice(it.id, it.label, it.description) } }
    }

    init {
        button {
            title = "Your own themes"
            text = "Open theme folder"
            description = "Put theme files into this folder. The README in it explains every color, example.json is a theme to copy. New files show up the next time you open the settings."
            onClick { SboThemes.openFolder() }
        }

        separator {
            this.title = "Window Size"
        }
    }

    var uiScale by choice(UiScale.id(UiScale.AUTO)) {
        this.name = Literal("Window Size")
        this.description = Literal("How big all SBO windows are. Auto uses your Minecraft GUI scale. Windows with their own Size setting keep it unless it is set to Global.")
        this.options = { UiScale.GLOBAL_CHOICES.map { Choice(UiScale.id(it), UiScale.label(it)) } }
    }
}
