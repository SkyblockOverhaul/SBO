package net.sbo.mod.guis

import gg.essential.elementa.ElementaVersion
import gg.essential.elementa.UIComponent
import gg.essential.elementa.WindowScreen
import gg.essential.elementa.components.ScrollComponent
import gg.essential.elementa.components.UIBlock
import gg.essential.elementa.components.UIRoundedRectangle
import gg.essential.elementa.components.UIText
import gg.essential.elementa.components.UIWrappedText
import gg.essential.elementa.components.input.UITextInput
import gg.essential.elementa.constraints.AdditiveConstraint
import gg.essential.elementa.constraints.CenterConstraint
import gg.essential.elementa.constraints.ChildBasedSizeConstraint
import gg.essential.elementa.constraints.FillConstraint
import gg.essential.elementa.constraints.SiblingConstraint
import gg.essential.elementa.constraints.SubtractiveConstraint
import gg.essential.elementa.dsl.childOf
import gg.essential.elementa.dsl.constrain
import gg.essential.elementa.dsl.percent
import gg.essential.elementa.dsl.pixels
import gg.essential.elementa.dsl.toConstraint
import gg.essential.universal.UKeyboard
import net.sbo.mod.diana.achievements.Achievement
import net.sbo.mod.diana.achievements.AchievementManager
import net.sbo.mod.utils.data.SboDataObject
import java.awt.Color
import kotlin.math.floor
import net.minecraft.util.Formatting
import net.sbo.mod.SBOKotlin.mc

class AchievementsGUI : WindowScreen(ElementaVersion.V10) {
    enum class AchievementFilter {
        DEFAULT, RARITY, LOCKED, UNLOCKED
    }

    enum class TypeFilter {
        ALL, REPEATABLE
    }

    private val rarityOrder = listOf("Common", "Uncommon", "Rare", "Epic", "Legendary", "Mythic", "Divine", "Celestial", "Impossible")

    private var filterType = AchievementFilter.DEFAULT
    private var typeFilter = TypeFilter.ALL
    private var achievementList: List<Achievement> = emptyList()
    private var sboData = SboDataObject.sboData
    private var searchQuery = ""

    private lateinit var contentPanel: UIComponent
    private lateinit var scrollComponent: ScrollComponent
    private lateinit var titleText : UIText
    private lateinit var unlockedCountText: UIText
    private lateinit var filterText: UIText
    private lateinit var typeText: UIText
    private lateinit var filterButtonOutline: UIRoundedRectangle
    private lateinit var typeButtonOutline: UIRoundedRectangle
    private lateinit var achievementsContainer: UIBlock
    private lateinit var searchInputOutline: UIRoundedRectangle

    init {
        renderGui()

        window.onKeyType { typedChar, keyCode ->
            if (keyCode == UKeyboard.KEY_ESCAPE) {
                mc.send {
                    displayScreen(null)
                }
            }
        }
    }

    override fun initScreen(width: Int, height: Int) {
        super.initScreen(width, height)
        loadFilterFromSboData()
        updateAchievementList()
        renderAchievements()
    }


    private fun updateAchievementList() {
        val allAchievements = AchievementManager.achievements.values.toList()

        val typeFilteredStatsBasis = when (typeFilter) {
            TypeFilter.REPEATABLE -> allAchievements.filter { it.repeatable }
            TypeFilter.ALL -> allAchievements
        }

        achievementList = typeFilteredStatsBasis.sortedBy { it.id }.let { base ->
            val statusFiltered = when (filterType) {
                AchievementFilter.RARITY -> base.sortedBy { rarityOrder.indexOf(it.rarity) }
                AchievementFilter.LOCKED -> base.filter { !it.isUnlocked(true) }
                AchievementFilter.UNLOCKED -> base.filter { it.isUnlocked(true) }
                else -> base
            }

            if (searchQuery.isNotEmpty()) {
                statusFiltered.filter { it.name.contains(searchQuery, ignoreCase = true) ||
                        it.description.contains(searchQuery, ignoreCase = true) ||
                        it.rarity.contains(searchQuery, ignoreCase = true) }
            } else {
                statusFiltered
            }
        }

        if (this::unlockedCountText.isInitialized) {
            val unlockedInType = typeFilteredStatsBasis.count { it.isUnlocked(true) }
            val totalInType = typeFilteredStatsBasis.size

            val unlockedPercentage = if (totalInType > 0) {
                (unlockedInType.toFloat() / totalInType * 100).toFixed(2)
            } else "0.00"

            val prefix = when(typeFilter) {
                TypeFilter.REPEATABLE -> "Repeatable "
                else -> ""
            }

            unlockedCountText.setText("${prefix}Unlocked: $unlockedInType/$totalInType ($unlockedPercentage%)")
        }

        if (this::filterText.isInitialized) {
            filterText.setText("Filter: ${filterType.name.lowercase().replaceFirstChar { it.uppercase() }}")
        }

        if (this::typeText.isInitialized) {
            typeText.setText("Type: ${typeFilter.name.replace("_", "-").lowercase().replaceFirstChar { it.uppercase() }}")
        }
    }

    private fun renderGui() {
        UIBlock().constrain {
            width = 100.percent
            height = 100.percent
        }.setColor(Color(0, 0, 0, 200)) childOf window

        achievementsContainer = UIBlock().constrain {
            x = CenterConstraint()
            y = 20.percent
            width = 80.percent
            height = 70.percent
        } childOf window
        achievementsContainer.setColor(Color(0, 0, 0, 0))

        titleText = UIText("SBO Achievements").constrain {
            x = CenterConstraint()
            y = (achievementsContainer.getTop() - 40).pixels
            textScale = 1.5.pixels
        } childOf window
        titleText.setColor(Color.WHITE)

        unlockedCountText = UIText("").constrain {
            x = CenterConstraint()
            y = SiblingConstraint(5f)
            textScale = 1.2.pixels
        } childOf window
        unlockedCountText.setColor(Color(0, 255, 35, 224))

        scrollComponent = ScrollComponent().constrain {
            x = 0.pixels
            y = 0.pixels
            width = FillConstraint()
            height = FillConstraint()
        } childOf achievementsContainer
        scrollComponent.setColor(Color(0, 0, 0, 0))

        contentPanel = UIBlock().constrain {
            x = 0.pixels
            y = 0.pixels
            width = FillConstraint()
            height = ChildBasedSizeConstraint()
        }.setColor(Color(0, 0, 0, 0)) childOf scrollComponent

        filterButtonOutline = UIRoundedRectangle(5f).constrain {
            x = achievementsContainer.getLeft().pixels
            y = (achievementsContainer.getTop() - 40).pixels
            width = 90.pixels
            height = 32.pixels
        } childOf window
        filterButtonOutline.setColor(Color.WHITE)

        val filterButton = UIRoundedRectangle(5f).constrain {
            x = CenterConstraint()
            y = CenterConstraint()
            width = SubtractiveConstraint(FillConstraint(), 2.pixels)
            height = SubtractiveConstraint(FillConstraint(), 2.pixels)
        } childOf filterButtonOutline
        filterButton.setColor(Color.BLACK)

        filterText = UIText("").constrain {
            x = CenterConstraint()
            y = CenterConstraint()
        } childOf filterButton

        filterButton.onMouseClick {
            val options = AchievementFilter.entries.toTypedArray()
            filterType = options[(filterType.ordinal + 1) % options.size]
            sboData.achievementFilter = filterType.name
            SboDataObject.save("SboData")
            updateAchievementList()
            renderAchievements()
        }

        typeButtonOutline = UIRoundedRectangle(5f).constrain {
            x = SiblingConstraint(10f)
            y = (achievementsContainer.getTop() - 40).pixels
            width = 110.pixels
            height = 32.pixels
        } childOf window
        typeButtonOutline.setColor(Color.WHITE)

        val typeButton = UIRoundedRectangle(5f).constrain {
            x = CenterConstraint()
            y = CenterConstraint()
            width = SubtractiveConstraint(FillConstraint(), 2.pixels)
            height = SubtractiveConstraint(FillConstraint(), 2.pixels)
        } childOf typeButtonOutline
        typeButton.setColor(Color.BLACK)

        typeText = UIText("").constrain {
            x = CenterConstraint()
            y = CenterConstraint()
        } childOf typeButton

        typeButton.onMouseClick {
            val options = TypeFilter.entries.toTypedArray()
            typeFilter = options[(typeFilter.ordinal + 1) % options.size]
            updateAchievementList()
            renderAchievements()
        }

        searchInputOutline = UIRoundedRectangle(5f).constrain {
            x = (achievementsContainer.getRight() - 100).pixels
            y = (achievementsContainer.getTop() - 40).pixels
            width = 100.pixels
            height = 32.pixels
        } childOf window
        searchInputOutline.setColor(Color.WHITE)

        val searchInputBg = UIRoundedRectangle(5f).constrain {
            x = CenterConstraint()
            y = CenterConstraint()
            width = SubtractiveConstraint(FillConstraint(), 2.pixels)
            height = SubtractiveConstraint(FillConstraint(), 2.pixels)
        } childOf searchInputOutline
        searchInputBg.setColor(Color.BLACK)

        val searchInput = UITextInput("Search...").constrain {
            x = 5.pixels
            y = AdditiveConstraint(CenterConstraint(), 3.pixels)
            width = SubtractiveConstraint(FillConstraint(), 5.pixels)
            height = 15.pixels
        } childOf searchInputBg

        searchInputBg.onMouseClick {
            searchInput.grabWindowFocus()
            searchInput.focus()
        }

        searchInput.onKeyType { _, _ ->
            searchQuery = searchInput.getText()
            updateAchievementList()
            renderAchievements()
        }

        searchInputBg.onMouseEnter { searchInputOutline.setColor(Color.CYAN) }
        searchInputBg.onMouseLeave { searchInputOutline.setColor(Color.WHITE) }
        typeButton.onMouseEnter { typeButtonOutline.setColor(Color.CYAN) }
        typeButton.onMouseLeave { typeButtonOutline.setColor(Color.WHITE) }
        filterButton.onMouseEnter { filterButtonOutline.setColor(Color.CYAN) }
        filterButton.onMouseLeave { filterButtonOutline.setColor(Color.WHITE) }

    }

    private fun renderAchievements() {
        contentPanel.clearChildren()

        val achievementBoxWidth = 200f
        val achievementBoxHeight = 55f
        val spacingX = 20f
        val spacingY = 20f
        val columns = floor((scrollComponent.getWidth() - spacingX) / (achievementBoxWidth + spacingX)).toInt()
        if (columns <= 0) return
        val totalGridWidth = (columns * achievementBoxWidth) + ((columns - 1) * spacingX)
        val centeringOffset = ((scrollComponent.getWidth() - totalGridWidth) / 2f).coerceAtLeast(10f)
        var lastY = 0f

        filterButtonOutline.constrain {
            x = when (columns) {
                2 -> {
                    (achievementsContainer.getLeft() + centeringOffset - 80).pixels
                }
                1 -> {
                    (achievementsContainer.getLeft() + centeringOffset - 120).pixels
                }
                else -> {
                    (achievementsContainer.getLeft() + centeringOffset).pixels
                }
            }
            y = if (columns == 1) {
                (achievementsContainer.getTop() - 15).pixels
            } else {
                (achievementsContainer.getTop() - 40).pixels
            }
        }

        typeButtonOutline.constrain {
            x = SiblingConstraint(10f)
            y = if (columns == 1) {
                (achievementsContainer.getTop() - 15).pixels
            } else {
                (achievementsContainer.getTop() - 40).pixels
            }
        }

        searchInputOutline.constrain {
            x = (achievementsContainer.getRight() - centeringOffset - 100).pixels
            y = if (columns == 1) {
                (achievementsContainer.getTop() - 15).pixels
            } else {
                (achievementsContainer.getTop() - 40).pixels
            }
        }

        titleText.constrain {
            y = (achievementsContainer.getTop() - 40).pixels
            textScale = if (columns == 1) 1.2.pixels else 1.5.pixels
        }

        unlockedCountText.constrain {
            y = SiblingConstraint(5f)
            textScale = if (columns == 1) 1.0.pixels else 1.2.pixels
        }

        achievementList.forEachIndexed { index, achievement ->
            val column = index % columns
            val row = floor(index.toFloat() / columns).toInt()
            val posX = centeringOffset + (column * (achievementBoxWidth + spacingX))
            val posY = spacingY + (row * (achievementBoxHeight + spacingY))
            lastY = posY
            val borderColor = if (achievement.isUnlocked(true)) Color(0, 255, 0) else Color(255, 0, 0)
            val achievementColor =  if (achievement.rarity != "Celestial") Color(Formatting.byCode(achievement.color[1])?.colorValue ?: 0xFFFFFF) else Color(0x7D00FF)

            val roundedOutline = UIRoundedRectangle(5f).constrain {
                x = posX.pixels
                y = posY.pixels
                width = (achievementBoxWidth + (1f * 2)).pixels
                height = (achievementBoxHeight + (1f * 2)).pixels
            }.setColor(borderColor)

            UIRoundedRectangle(5f).constrain {
                x = CenterConstraint()
                y = CenterConstraint()
                width = achievementBoxWidth.pixels
                height = achievementBoxHeight.pixels
            }.setColor(Color(0, 0, 0, 255))
                .addChild(UIText(achievement.name).constrain {
                    x = 5.pixels
                    y = 5.pixels
                    textScale = 1.0.pixels
                    color = achievementColor.toConstraint()
                })
                .addChild(UIWrappedText("§7${achievement.description}").constrain {
                    x = 5.pixels
                    y = SiblingConstraint(5f)
                    width = SubtractiveConstraint(achievementBoxWidth.pixels, 5.pixels)
                    height = 18.pixels
                    textScale = 1.0.pixels
                })
                .addChild(UIText(achievement.rarity).constrain {
                    x = 5.pixels
                    y = SiblingConstraint(5f)
                    textScale = 0.8.pixels
                    color = achievementColor.toConstraint()
                }) childOf roundedOutline

            roundedOutline childOf contentPanel
        }

        val requiredHeight = if (achievementList.isNotEmpty()) {
            lastY + achievementBoxHeight + spacingY
        } else {
            0f
        }

        contentPanel.constrain {
            height = requiredHeight.pixels
        }
    }

    private fun loadFilterFromSboData() {
        filterType = try {
            AchievementFilter.valueOf(sboData.achievementFilter.uppercase())
        } catch (_: Exception) {
            AchievementFilter.DEFAULT
        }
    }

    private fun Float.toFixed(digits: Int): String {
        return String.format("%.${digits}f", this)
    }
}