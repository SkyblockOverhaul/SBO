package net.sbo.mod.guis.partyfinder

import com.teamresourceful.resourcefulconfig.api.client.ResourcefulConfigScreen
import gg.essential.elementa.ElementaVersion
import gg.essential.elementa.UIComponent
import gg.essential.elementa.WindowScreen
import gg.essential.elementa.components.*
import gg.essential.elementa.constraints.*
import gg.essential.elementa.dsl.childOf
import gg.essential.elementa.dsl.constrain
import gg.essential.elementa.dsl.percent
import gg.essential.elementa.dsl.pixels
import gg.essential.elementa.effects.OutlineEffect
import gg.essential.universal.UKeyboard
import net.sbo.mod.SBOKotlin
import net.sbo.mod.SBOKotlin.MOD_ID
import net.sbo.mod.SBOKotlin.mc
import net.sbo.mod.guis.partyfinder.pages.CustomPage
import net.sbo.mod.guis.partyfinder.pages.DianaPage
import net.sbo.mod.guis.partyfinder.pages.Help
import net.sbo.mod.guis.partyfinder.pages.Home
import net.sbo.mod.partyfinder.PartyFinderManager
import net.sbo.mod.partyfinder.PartyFinderManager.createParty
import net.sbo.mod.partyfinder.PartyFinderManager.getActiveUsers
import net.sbo.mod.partyfinder.PartyFinderManager.getAllParties
import net.sbo.mod.partyfinder.PartyFinderManager.removePartyFromQueue
import net.sbo.mod.partyfinder.PartyFinderManager.sendJoinRequest
import net.sbo.mod.partyfinder.PartyPlayer.getPartyPlayerStats
import net.sbo.mod.settings.categories.PartyFinder
import net.sbo.mod.utils.Helper
import net.sbo.mod.utils.chat.Chat
import net.sbo.mod.utils.data.HighlightElement
import net.sbo.mod.utils.data.Party
import net.sbo.mod.utils.data.PartyPlayerStats
import net.sbo.mod.utils.data.Reqs
import net.sbo.mod.utils.data.SboDataObject.pfConfigState
import net.sbo.mod.utils.events.annotations.SboEvent
import net.sbo.mod.utils.events.impl.partyfinder.PartyFinderOpenEvent
import net.sbo.mod.utils.events.impl.partyfinder.PartyFinderRefreshListEvent
import java.util.concurrent.TimeUnit

class PartyFinderGUI : WindowScreen(ElementaVersion.V10) {

    private val elementToHighlight: MutableList<HighlightElement> = mutableListOf()
    internal var selectedPage: String = "Home"
    private val pages: MutableMap<String, () -> Unit> = mutableMapOf()
    private var partyCache: MutableMap<String, List<Party>> = mutableMapOf()
    private var lastRefreshTime: Long = 0L
    private var cpWindowOpened: Boolean = false
    private var filterWindowOpened: Boolean = false
    private var partyInfoOpened: Boolean = false

    private val dianaPage = DianaPage(this)
    private val customPage = CustomPage(this)
    private val homePage = Home(this)
    private val helpPage = Help(this)

    private lateinit var filterBackground: UIComponent
    internal lateinit var filterWindow : UIComponent
    private lateinit var partyInfoWindow : UIComponent
    internal lateinit var cpWindow : UIComponent
    private lateinit var base : UIComponent
    private lateinit var onlineUserBlock: UIComponent
    internal lateinit var onlineUserText: UIText
    private lateinit var titleBlock: UIComponent
    private lateinit var categoryBlock: UIComponent
    internal lateinit var contentBlock: UIComponent
    private lateinit var playerNameBase: UIComponent
    private lateinit var partyListContainer: UIComponent
    internal lateinit var noParties : UIComponent
    internal lateinit var reqsBox: UIComponent
    internal lateinit var createBox: UIComponent
    internal lateinit var filterBox: UIComponent
    internal lateinit var infobase: UIComponent
    internal lateinit var partyCount: UIText
    private var guiScale: Int? = null

    private var refreshing = false

    init {
        create()
    }

    companion object {
        var instance: PartyFinderGUI? = null

        @SboEvent
        fun onPartyFinderRefresh(event: PartyFinderRefreshListEvent) {
            instance?.updateCurrentPartyList(true)
        }

        @SboEvent
        fun onPartyFinderOpen(event: PartyFinderOpenEvent) {
            instance?.onScreenOpen()
        }
    }

    private fun create() {
        instance = this
        createGui()

        window.onKeyType { typedChar, keyCode ->
            if (keyCode == UKeyboard.KEY_ESCAPE) {
                mc.schedule {
                    displayScreen(null)
                }
            }
        }
    }

    override fun onKeyPressed(keyCode: Int, typedChar: Char, modifiers: UKeyboard.Modifiers?) {
        if (keyCode == UKeyboard.KEY_ESCAPE) {
            if (cpWindowOpened) {
                closeCpWindow()
                return
            }
            if (filterWindowOpened) {
                closeFilterWindow()
                return
            }
            if (partyInfoOpened) {
                closePartyInfoWindow()
                return
            }
        }

        super.onKeyPressed(keyCode, typedChar, modifiers)
    }

    private fun onScreenOpen() {
        updateSelectedPage()
        updateOnlineUser()
        updatePageHighlight()
        //for the unlucky event that someone spams opening and closing the cp winodw/filter window
        closeCpWindow()
        closeFilterWindow()

        if (mc.options.guiScale().get() == 2) return
        guiScale = mc.options.guiScale().get()
        mc.options.guiScale().set(2) // this is a workaround for text scaling
    }

    override fun onScreenClose() {
        super.onScreenClose()
        partyCache = mutableMapOf() // clear party cache on close
        if (mc.options.guiScale().get() != 2 || guiScale == null) return
        mc.options.guiScale().set(guiScale!!) // restore original gui scale
        guiScale = null
    }

    internal fun getTextScaleOfScaleText(base: Float = 1f): PixelConstraint {
        return if (base + PartyFinder.scaleText <= 0f) 0.1f.pixels()
        else (base + PartyFinder.scaleText).pixels()
    }

    internal fun getFilter(pageType: String, callback: (((Party) -> Boolean)?) -> Unit) {
        getPartyPlayerStats { stats ->
            val filter = when (pageType) {
                "Diana" -> {
                    val isEman9 = pfConfigState.filters.diana.eman9Filter
                    val isLooting5 = pfConfigState.filters.diana.looting5Filter
                    val canIJoin = pfConfigState.filters.diana.canIjoinFilter

                    if (!isEman9 && !isLooting5 && !canIJoin) null
                    else fun(party: Party): Boolean {
                        if (isEman9 && !party.reqs.eman9) return false
                        if (isLooting5 && !party.reqs.looting5) return false
                        if (canIJoin) {
                            party.reqs.let { req ->
                                if (req.lvl > 0 && stats.sbLvl < req.lvl) return false
                                if (req.kills > 0 && stats.mythosKills < req.kills) return false
                                if (req.eman9 && !stats.eman9) return false
                                if (req.looting5 && !stats.looting5daxe) return false
                            }
                        }
                        return true
                    }
                }
                "Custom" -> {
                    val isEman9 = pfConfigState.filters.custom.eman9Filter
                    val canIJoin = pfConfigState.filters.custom.canIjoinFilter

                    if (!isEman9 && !canIJoin) null
                    else fun(party: Party): Boolean {
                        if (isEman9 && !party.reqs.eman9) return false
                        if (canIJoin) {
                            party.reqs.let { req ->
                                if (req.lvl > 0 && stats.sbLvl < req.lvl) return false
                                if (req.mp > 0 && stats.magicalPower < req.mp) return false
                            }
                        }
                        return true
                    }
                }
                else -> null
            }
            callback(filter)
        }
    }


    private fun getPartyInfo(type: String, list: PartyPlayerStats) : String {
        return when (type) {
            "Diana" -> dianaPage.getPartyInfo(list)
            "Custom" -> customPage.getPartyInfo(list)
            else -> "No party info available."
        }
    }

    private fun joinParty(leader: String, reqs: Reqs) {
        if (!PartyFinderManager.inQueue && !PartyFinderManager.isInParty) {
            sendJoinRequest(leader, reqs)
        } else {
            val leaderCheck = leader == mc.player?.name?.string
            if (PartyFinderManager.inQueue && !PartyFinderManager.isInParty && !leaderCheck) Chat.chat("§6[SBO] §eYou are already in queue.")
            if (PartyFinderManager.isInParty && !PartyFinderManager.inQueue && !leaderCheck) Chat.chat("§6[SBO] §eYou are already in a party.")
            if (leaderCheck) Chat.chat("§6[SBO] §eYou can't join your own party.")
        }
    }

    private fun openPartyInfoWindow() {
        base.hide()
        partyInfoWindow.unhide(false)
        partyInfoOpened = true
    }

    private fun closePartyInfoWindow() {
        partyInfoWindow.hide()
        checkWindows()
        base.unhide(true)
        partyInfoOpened = false
    }

    private fun openFilterWindow() {
        filterBackground.unhide(false)
        filterWindow.unhide(false)
        filterWindowOpened = true
    }

    internal fun closeFilterWindow() {
        filterBackground.hide()
        filterWindow.hide()
        checkWindows()
        filterWindowOpened = false
    }

    internal fun openCpWindow() {
        base.hide()
        cpWindow.unhide(true)
        cpWindowOpened = true
    }

    internal fun closeCpWindow() {
        cpWindow.hide()
        checkWindows()
        base.unhide(true)
        cpWindowOpened = false
    }

    private fun checkWindows() {
        if (this::reqsBox.isInitialized) cpWindow.removeChild(reqsBox)
        if (this::createBox.isInitialized) cpWindow.removeChild(createBox)
        if (this::filterBox.isInitialized) window.removeChild(filterBox)
        if (this::infobase.isInitialized) partyInfoWindow.removeChild(infobase)
    }

    private fun unqueueParty() {
        if (PartyFinderManager.inQueue) {
            PartyFinderManager.usedPf = false
            removePartyFromQueue { success ->
                Window.enqueueRenderOperation {
                    updateCurrentPartyList(true)
                }
            }
        }
    }

    internal fun partyCreate(reqs: Reqs, note: String, type: String, size: Int = 6) {
        createParty(
            reqs = reqs,
            note = note,
            type = type,
            size = size
        )
    }

    internal fun filterPartyList(filterPredicate: ((Party) -> Boolean)? = null) {
        val partyList = partyCache[selectedPage] ?: run {
            updateCurrentPartyList(true)
            return@filterPartyList
        }
        val resultList = filterPredicate?.let { partyList.filter(it) } ?: partyList
        addPartyList(resultList, true)
    }

    private fun updateSelectedPage() {
        if (selectedPage.isNotEmpty() && pages.containsKey(selectedPage)) {
            contentBlock.clearChildren()
            contentBlock.addChild(partyListContainer)
            Helper.sleep(100) {
                pages[selectedPage]?.invoke()
            }
        }
    }

    private fun updatePageHighlight() {
        elementToHighlight.forEach { element ->
            if (element.obj is UIBlock) {
                if (element.page == selectedPage) {
                    element.obj.setColor(Theme.DARK_GRAY)
                } else {
                    element.obj.setColor(Theme.TRANSPARENT)
                }
            } else {
                if (element.page == selectedPage) {
                    element.obj.setColor(Theme.ROYAL_BLUE)
                } else {
                    element.obj.setColor(Theme.WHITE)
                }
            }
        }
    }

    internal fun updateCurrentPartyList(ignoreCooldown: Boolean = false) {
        val now = System.nanoTime()
        if (!ignoreCooldown && now - this.lastRefreshTime < TimeUnit.MILLISECONDS.toNanos(5000)) {
            Chat.chat("§6[SBO] §ePlease wait 5 seconds before refreshing the party list again.")
            return
        }
        lastRefreshTime = now

        if (refreshing) {
            // If ignoreCooldown is false means the user explicitly refreshed. Otherwise, we most likely tried to refresh due to a new party being queued by the user or delisted; in which case we should be silent if it was already trying to refresh.
            if (!ignoreCooldown) {
                Chat.chat("§6[SBO] §eA refresh is already in progress, please wait.")
            }
            return
        }

        refreshing = true

        getAllParties(selectedPage,
            onComplete = { parties ->
                refreshing = false

                partyCache[selectedPage] = parties

                getFilter(selectedPage) { filter ->
                    Window.enqueueRenderOperation {
                        if (filter != null) {
                            filterPartyList(filter)
                        } else {
                            addPartyList(parties)
                        }
                    }
                }
            },
            onError = {
                refreshing = false
            }
        )
    }

    private fun updateOnlineUser() {
        if (!::onlineUserText.isInitialized) return
        getActiveUsers { activeUsers ->
            onlineUserText.setText("Online: $activeUsers")
        }
    }

    private fun updatePartyCount(count: Int) {
        if (!::partyCount.isInitialized) return
        partyCount.setText("Parties: $count")
    }

    private fun addFilterPage(listName: String, x: PositionConstraint, y: PositionConstraint) {
        if (filterWindowOpened) {
            filterWindowOpened = false
            return
        }
        openFilterWindow()

        when (listName) {
            "Diana Party List" -> {
                dianaPage.addDianaFilter(x,y)
            }

            "Custom Party List" -> {
                customPage.addCustomFilter(x, y)
            }
            else -> return
        }
    }

    private fun addPage(pageTitle: String, pageContent: () -> Unit, isSubPage: Boolean = false, y1: PositionConstraint? = null, isClickable: Boolean = false) {
        pages[pageTitle] = pageContent
        val finalY = y1 ?: if (isSubPage) SiblingConstraint(0f, true) else SiblingConstraint()

        val block = UIBlock().constrain {
            x = CenterConstraint()
            y = finalY
            width = 75.percent()
            height = 5.percent()
        }.setColor(Theme.PAGE_BLOCK)

        val text = UIText("・ $pageTitle").constrain {
            y = CenterConstraint()
            textScale = getTextScaleOfScaleText()
        }.setColor(Theme.PAGE_TITLE)

        block.onMouseClick {
            if (selectedPage == pageTitle) return@onMouseClick
            if (isClickable) return@onMouseClick pageContent()
            selectedPage = pageTitle
            contentBlock.clearChildren()
            partyListContainer.clearChildren()
            if (selectedPage != "Home" && selectedPage != "Help" && selectedPage != "Settings") {
                contentBlock.addChild(partyListContainer)
            }
            updatePageHighlight()
            pageContent()
        }

        block.addChild(text)
            .onMouseEnter {
                if (selectedPage == pageTitle) return@onMouseEnter
                block.setColor(Theme.PAGE_HOVER_IN)
            }
            .onMouseLeave {
                if (selectedPage == pageTitle) return@onMouseLeave
                block.setColor(Theme.PAGE_HOVER_OUT)
            }

        categoryBlock.addChild(block)
            .addChild(GuiHandler.UILine(
                x = CenterConstraint(),
                y = if (isSubPage) SiblingConstraint(0f, true) else SiblingConstraint(),
                width = 75.percent(),
                height = 0.3f.percent(),
                color = Theme.PAGE_CATEGORY_LINE
            ).get())

        elementToHighlight.add(HighlightElement(pageTitle, text, "pageTitle"))
        elementToHighlight.add(HighlightElement(pageTitle, block, "pageBlock"))
    }

    private fun addPartyList(partyList: List<Party>, ignoreCache: Boolean = false) {
        var list: List<Party> = partyList
        if (!ignoreCache && partyCache[selectedPage] != null) {
            list = partyCache[selectedPage]!!
        }
        updatePartyCount(list.size)
        Window.enqueueRenderOperation {
            renderPartyList(list)
        }
    }

    private fun createPartyBlock(party: Party, reqsString: String): UIComponent {
        val partyBlock = UIBlock().constrain {
            y = SiblingConstraint()
            width = 100.percent()
            height = 22.percent()
        }.setColor(Theme.PARTY_LIST_BG)
            .enableEffect(OutlineEffect(Theme.PARTY_LIST_OUTLINE, 1f))
            .addChild(UIBlock().constrain {
                width = 20.percent()
                height = 100.percent()
            }.setColor(Theme.TRANSPARENT)
                .addChild(UIText(party.leaderName).constrain {
                    x = CenterConstraint()
                    y = CenterConstraint()
                    textScale = getTextScaleOfScaleText()
                }.setColor(Theme.PARTY_LIST_LEADER))
            )
            .addChild(GuiHandler.UILine(
                x = SiblingConstraint(),
                y = CenterConstraint(),
                width = 0.3f.percent(),
                height = 80.percent(),
                color = Theme.PARTY_LIST_INFO_SEPARATOR,
                rounded = true
            ).get())

        val reqsNote = UIBlock().constrain {
            x = SiblingConstraint()
            y = CenterConstraint()
            width = 50.percent()
            height = 100.percent()
        }.setColor(Theme.TRANSPARENT)
            .addChild(UIBlock().constrain {
                x = CenterConstraint()
                y = 0.pixels
                width = 100.percent()
                height = 50.percent()
            }.setColor(Theme.TRANSPARENT)
                .addChild(UIBlock().constrain {
                    x = CenterConstraint()
                    y = SiblingConstraint()
                    width = 90.percent()
                    height = 100.percent()
                }.setColor(Theme.TRANSPARENT)
                    .addChild(UIWrappedText(reqsString).constrain {
                        x = 0.pixels
                        y = CenterConstraint()
                        width = 100.percent()
                        textScale = getTextScaleOfScaleText()
                    }.setColor(Theme.TEXT_PRIMARY))
                )
            )
            .addChild(UIBlock().constrain {
                x = CenterConstraint()
                y = SiblingConstraint()
                width = 100.percent()
                height = 50.percent()
            }.setColor(Theme.TRANSPARENT)
                .addChild(UIBlock().constrain {
                    x = CenterConstraint()
                    y = CenterConstraint()
                    width = 90.percent()
                    height = 100.percent()
                }.setColor(Theme.TRANSPARENT)
                    .addChild(UIWrappedText("&bNote: &7" + party.note.replace("%20", " ")).constrain {
                        x = 0.pixels
                        y = CenterConstraint()
                        width = 100.percent()
                        textScale = getTextScaleOfScaleText()
                    }.setColor(Theme.TEXT_PRIMARY))
                )
            )

        partyBlock.addChild(reqsNote)
            .addChild(GuiHandler.UILine(
                x = SiblingConstraint(),
                y = CenterConstraint(),
                width = 0.3f.percent(),
                height = 80.percent(),
                color = Theme.PARTY_LIST_INFO_SEPARATOR,
                rounded = true
            ).get())
            .addChild(UIBlock().constrain {
                x = SiblingConstraint()
                y = CenterConstraint()
                width = 10.percent()
                height = 100.percent()
            }.setColor(Theme.TRANSPARENT)
                .addChild(UIText("${party.partyMembersCount}/${party.partySize}").constrain {
                    x = CenterConstraint()
                    y = CenterConstraint()
                    textScale = getTextScaleOfScaleText()
                }.setColor(Theme.getMemberColor(party.partyMembersCount, party.partySize)))
            )
            .addChild(GuiHandler.UILine(
                x = SiblingConstraint(),
                y = CenterConstraint(),
                width = 0.3f.percent(),
                height = 80.percent(),
                color = Theme.PARTY_LIST_INFO_SEPARATOR,
                rounded = true
            ).get())

        val joinBlock = UIBlock().constrain {
            x = SiblingConstraint()
            y = CenterConstraint()
            width = FillConstraint()
            height = 100.percent()
        }.setColor(Theme.TRANSPARENT)

        val joinButton = GuiHandler.Button(
            text = "Join",
            x = CenterConstraint(),
            y = CenterConstraint(),
            width = 70.percent(),
            height = 40.percent(),
            color = Theme.BUTTON_JOIN_BG,
            textColor = Theme.BUTTON_JOIN_TEXT,
            rounded = true
        )
        joinBlock.addChild(joinButton.uiObject)
        partyBlock.addChild(joinBlock)
        joinButton.textObject.setTextScale(getTextScaleOfScaleText())

        joinButton.setOnClick {
            joinParty(party.leaderName, party.reqs)
        }

        joinButton.uiObject.onMouseEnter {
            if (filterWindowOpened) return@onMouseEnter
            this.setColor(Theme.BUTTON_JOIN_HOVER_IN)
            partyBlock.setColor(Theme.BLACK_HALF_TRANS)
        }
        joinButton.uiObject.onMouseLeave {
            if (filterWindowOpened) return@onMouseLeave
            this.setColor(Theme.BUTTON_JOIN_HOVER_OUT)
            partyBlock.setColor(Theme.BLACK_SEMI_TRANS)
        }

        partyBlock.onMouseEnter {
            if (filterWindowOpened) return@onMouseEnter
            partyBlock.setColor(Theme.BLACK_SEMI_TRANS)
        }
        partyBlock.onMouseLeave {
            if (filterWindowOpened) return@onMouseLeave
            partyBlock.setColor(Theme.BLACK_HALF_TRANS)
        }

        partyBlock.onMouseClick {
            renderPartyInfo(party.partyInfo)
        }

        return partyBlock
    }


    private fun renderPartyList(list: List<Party>) {
        if (list.isEmpty()) {
            partyListContainer.clearChildren()
            noParties.unhide(true)
            return
        }
        partyListContainer.clearChildren()
        list.forEach { party ->
            when (selectedPage) {
                "Diana" -> dianaPage.getReqsString(party.reqs) { reqsString ->
                    val partyBlock = createPartyBlock(party, reqsString)
                    partyListContainer.addChild(partyBlock)
                }
                "Custom" -> customPage.getReqsString(party.reqs) { reqsString ->
                    val partyBlock = createPartyBlock(party, reqsString)
                    partyListContainer.addChild(partyBlock)
                }
                else -> {
                    val partyBlock = createPartyBlock(party, "No requirements available.")
                    partyListContainer.addChild(partyBlock)
                }
            }
        }
    }

    private fun renderPartyInfo(partyInfoList: List<PartyPlayerStats>) {
        playerNameBase.clearChildren()
        openPartyInfoWindow()
        partyInfoWindow.constrain {
            x = CenterConstraint()
            y = CenterConstraint()
            width = 60.percent()
            height = 65.percent()
        }.setColor(Theme.TRANSPARENT)
        infobase = UIRoundedRectangle(10f).constrain {
            x = 0.percent()
            y = 0.percent()
            width = 100.percent()
            height = 100.percent()
        }.setColor(Theme.INFO_BG) childOf partyInfoWindow
        val infoDisplay = UIRoundedRectangle(10f).constrain {
            x = SiblingConstraint()
            y = CenterConstraint()
            width = 48.percent()
            height = 95.percent()
        }.setColor(Theme.BLACK_HALF_TRANS)
        val infoScroll = ScrollComponent().constrain {
            x = 0.percent()
            y = 0.percent()
            width = 100.percent()
            height = 100.percent()
        }.setColor(Theme.TRANSPARENT)
        infobase.addChild(playerNameBase)
        infoDisplay.addChild(infoScroll)
        infobase.addChild(infoDisplay)
        partyInfoList.forEach { party ->
            val objheight = infobase.getHeight() / 6
            val infoString = getPartyInfo(selectedPage, party)
            val playerBlock = UIRoundedRectangle(10f).constrain {
                x = CenterConstraint()
                y = CenterConstraint()
                width = 60.percent()
                height = 70.percent()
            }.setColor(Theme.BLACK_HALF_TRANS)
                .addChild(UIText(party.name).constrain {
                    x = CenterConstraint()
                    y = CenterConstraint()
                    textScale = getTextScaleOfScaleText()
                }.setColor(Theme.TEXT_PRIMARY))
            playerBlock.onMouseEnter {
                playerBlock.setColor(Theme.INFO_PLAYER_HOVER_IN)
                infoScroll.clearChildren()
                infoScroll.addChild(UIWrappedText(infoString).constrain {
                    x = 4.percent()
                    y = 4.percent()
                    width = 96.percent()
                    textScale = getTextScaleOfScaleText()
                })
            }
            playerBlock.onMouseLeave {
                playerBlock.setColor(Theme.INFO_PLAYER_HOVER_OUT)
            }
            playerNameBase.addChild(UIBlock().constrain {
                x = 0.percent()
                y = SiblingConstraint(0f)
                width = 100.percent()
                height = objheight.pixels()
            }.setColor(Theme.TRANSPARENT)
                .addChild(playerBlock)
            )
        }

    }

    internal fun addPartyListFunctions(listName: String, createParty: () -> Unit) {
        val line = GuiHandler.UILine(
            x = 0.percent(),
            y = 7.percent(),
            width = 100.percent(),
            height = 0.3f.percent(),
            color = Theme.PARTY_LIST_SEPARATOR
        ).get()
        partyCount = UIText("").constrain {
            x = SiblingConstraint()
            y = CenterConstraint()
            textScale = getTextScaleOfScaleText()
        }
        partyCount.setColor(Theme.TEXT_PRIMARY)
        val filterText = UIText("Filter").constrain {
            x = CenterConstraint()
            y = CenterConstraint()
            textScale = getTextScaleOfScaleText()
        }.setColor(Theme.PARTY_LIST_FILTER)
        val filterBlock = UIBlock().constrain {
            x = SiblingConstraint()
            y = CenterConstraint()
            width = 8.percent()
            height = 80.percent()
        }.setColor(Theme.TRANSPARENT)
        filterBlock.addChild(filterText)
        filterBlock.onMouseClick {
            val x = filterBlock.getLeft() + filterBlock.getWidth() / 2f
            val y = line.getBottom()
            addFilterPage(listName, x.pixels(), y.pixels())
        }
        filterBlock.onMouseEnter {
            filterText.setColor(Theme.PARTY_LIST_FILTER_HOVER_IN)
        }
        filterBlock.onMouseLeave {
            filterText.setColor(Theme.PARTY_LIST_FILTER_HOVER_OUT)
        }
        val refreshText = UIText("Refresh").constrain {
            x = CenterConstraint()
            y = CenterConstraint()
            textScale = getTextScaleOfScaleText()
        }.setColor(Theme.PARTY_LIST_REFRESH)
        val refreshBlock = UIBlock().constrain {
            x = SiblingConstraint(5f)
            y = CenterConstraint()
            width = 8.percent()
            height = 80.percent()
        }.setColor(Theme.TRANSPARENT)
        refreshBlock.addChild(refreshText)
        refreshBlock.onMouseClick {
            updateCurrentPartyList()
        }
        refreshBlock.onMouseEnter {
            refreshText.setColor(Theme.PARTY_LIST_REFRESH_HOVER_IN)
        }
        refreshBlock.onMouseLeave {
            refreshText.setColor(Theme.PARTY_LIST_REFRESH_HOVER_OUT)
        }
        val unqueuePartyText = UIText("Delete").constrain {
            x = CenterConstraint()
            y = CenterConstraint()
            textScale = getTextScaleOfScaleText()
        }.setColor(Theme.PARTY_LIST_UNQUEUE)
        val unqueuePartyBlock = UIBlock().constrain {
            x = SiblingConstraint(5f)
            y = CenterConstraint()
            width = 8.percent()
            height = 80.percent()
        }.setColor(Theme.TRANSPARENT)
        unqueuePartyBlock.addChild(unqueuePartyText)
        unqueuePartyBlock.onMouseClick {
            unqueueParty()
        }
        unqueuePartyBlock.onMouseEnter {
            unqueuePartyText.setColor(Theme.PARTY_LIST_UNQUEUE_HOVER_IN)
        }
        unqueuePartyBlock.onMouseLeave {
            unqueuePartyText.setColor(Theme.PARTY_LIST_UNQUEUE_HOVER_OUT)
        }
        val createPartyText = UIText("Create").constrain {
            x = CenterConstraint()
            y = CenterConstraint()
            textScale = getTextScaleOfScaleText()
        }.setColor(Theme.PARTY_LIST_CREATE)
        val createPartyBlock = UIBlock().constrain {
            x = SiblingConstraint(5f)
            y = CenterConstraint()
            width = 8.percent()
            height = 80.percent()
        }.setColor(Theme.TRANSPARENT)
        createPartyBlock.addChild(createPartyText)
        createPartyBlock.onMouseClick {
            createParty()
        }
        createPartyBlock.onMouseEnter {
            createPartyText.setColor(Theme.PARTY_LIST_CREATE_HOVER_IN)
        }
        createPartyBlock.onMouseLeave {
            createPartyText.setColor(Theme.PARTY_LIST_CREATE_HOVER_OUT)
        }
        // maybe add svgfix if needed
        contentBlock.addChild(line)
        contentBlock.addChild(UIBlock().constrain {
            width = 100.percent()
            height = 7.percent()
        }.setColor(Theme.TRANSPARENT)
            .addChild(UIBlock().constrain {
                x = 1.percent()
                y = CenterConstraint()
                width = 20.percent()
                height = 70.percent()
            }.setColor(Theme.TRANSPARENT)
                .addChild(partyCount)
            )

            .addChild(UIBlock().constrain {
                x = SiblingConstraint()
                y = CenterConstraint()
                width = 42.percent()
                height = 100.percent()
            }.setColor(Theme.TRANSPARENT)
                .addChild(UIText(listName).constrain {
                    x = CenterConstraint()
                    y = CenterConstraint()
                    textScale = getTextScaleOfScaleText(1.5f)
                }.setColor(Theme.TEXT_PRIMARY))
            )
            .addChild(filterBlock)
            .addChild(refreshBlock)
            .addChild(unqueuePartyBlock)
            .addChild(createPartyBlock)
        )
    }

    private fun settings() {
        mc.schedule {
            displayScreen(ResourcefulConfigScreen.getFactory(MOD_ID).apply(null))
        }
    }

    private fun stpBtn(btn: GuiHandler.Button) {
        btn.textObject.setTextScale(getTextScaleOfScaleText())
        btn.uiObject.addChild(GuiHandler.UILine(
            x = CenterConstraint(),
            y = 100.percent(),
            (btn.textObject.getWidth() + 10).pixels(),
            10.percent(),
            Theme.BUTTON_TITLE_DISC_GIT_PAT_UNDERLINE
        ).get())
    }

    private fun createGui() {
        filterBackground = UIBlock().constrain {
            width = 100.percent()
            height = 100.percent()
            x = 0.percent()
            y = 0.percent()
        }.setColor(Theme.CREATE_FILTER_BG) childOf window
        filterBackground.hide()
        filterWindow = UIRoundedRectangle(10f) childOf window
        filterWindow.hide()
        partyInfoWindow = UIRoundedRectangle(10f) childOf window
        partyInfoWindow.hide()

        cpWindow = UIRoundedRectangle(10f).constrain {
            width = 30.percent()
            height = 40.percent()
            x = CenterConstraint()
            y = CenterConstraint()
        }.setColor(Theme.CREATE_BG)
            .addChild(UIBlock().constrain {
                width = 100.percent()
                height = 12.percent()
            }.setColor(Theme.TRANSPARENT)
                .addChild(UIText("Create Party").constrain {
                    x = CenterConstraint()
                    y = CenterConstraint()
                    textScale = getTextScaleOfScaleText(1.5f)
                }.setColor(Theme.TEXT_PRIMARY)))
            .addChild(GuiHandler.UILine(
                x = 0.percent(),
                y = SiblingConstraint(),
                width = 100.percent(),
                height = 1f.percent(),
                color = Theme.SBO_BLUE
            ).get())

        window.addChild(cpWindow)
        cpWindow.hide()

        base = UIRoundedRectangle(10f).constrain {
            width = 60.percent()
            height = 65.percent()
            x = CenterConstraint()
            y = CenterConstraint()
        }.setColor(Theme.BASE) childOf window
        //-----------------Title Block-----------------
        GuiHandler.UILine(
            x = 0.percent(),
            y = 5.percent(),
            width = 100.percent(),
            height = 0.3f.percent(),
            color = Theme.SBO_BLUE,
            parent = base
        )
        onlineUserBlock = UIBlock().constrain {
            x = 10.percent()
            y = CenterConstraint()
            width = 40.percent()
            height = 80.percent()
        }.setColor(Theme.TRANSPARENT)

        onlineUserText = UIText("Online: 0").constrain {
            x = 0.percent()
            y = CenterConstraint()
            textScale = getTextScaleOfScaleText()
        } childOf onlineUserBlock

        titleBlock = UIBlock().constrain {
            width = 100.percent()
            height = 5.percent()
        }.setColor(Theme.TRANSPARENT)
            .setChildOf(base)
            .addChild(UIBlock().constrain {
                width = 25.percent()
                height = 100.percent()
                x = SiblingConstraint()
                y = CenterConstraint()
            }.setColor(Theme.TRANSPARENT)
                .addChild(onlineUserBlock))
            .addChild(UIBlock().constrain {
                width = 35.percent()
                height = 100.percent()
                x = CenterConstraint()
                y = CenterConstraint()
            }.setColor(Theme.TRANSPARENT)
                .addChild(
                UIText("SBO Party Finder").constrain {
                    x = CenterConstraint()
                    y = CenterConstraint()
                    textScale = getTextScaleOfScaleText()
                }.setColor(Theme.TEXT_PRIMARY))
            )
        val discordBlock = UIBlock().constrain {
            width = 11.percent()
            height = 100.percent()
            x = SiblingConstraint()
        }.setColor(Theme.TRANSPARENT) childOf titleBlock

        val discord = GuiHandler.Button(
            text = "Discord",
            x = CenterConstraint(),
            y = CenterConstraint(),
            width = 80.percent(),
            height = 60.percent(),
            color = Theme.TRANSPARENT,
            textColor = Theme.TEXT_PRIMARY,
            parent = discordBlock
        )
            .textHoverEffect(Theme.BUTTON_TITLE_DISC_GIT_PAT_HOVER_OUT, Theme.BUTTON_TITLE_DISC_GIT_PAT_HOVER_IN)
            .setTextOnClick {
                SBOKotlin.openInBrowser("https://discord.gg/QvM6b9jsJD")
            }
        stpBtn(discord)

        val githubBlock = UIBlock().constrain {
            width = 11.percent()
            height = 100.percent()
            x = SiblingConstraint()
        }.setColor(Theme.TRANSPARENT) childOf titleBlock

        val github = GuiHandler.Button(
            text = "GitHub",
            x = CenterConstraint(),
            y = CenterConstraint(),
            width = 80.percent(),
            height = 60.percent(),
            color = Theme.TRANSPARENT,
            textColor = Theme.TEXT_PRIMARY,
            parent = githubBlock
        )
            .textHoverEffect(Theme.BUTTON_TITLE_DISC_GIT_PAT_HOVER_OUT, Theme.BUTTON_TITLE_DISC_GIT_PAT_HOVER_IN)
            .setTextOnClick {
                SBOKotlin.openInBrowser("https://github.com/SkyblockOverhaul/SBO")
            }
        stpBtn(github)

        val patreonBlock = UIBlock().constrain {
            width = 11.percent()
            height = 100.percent()
            x = SiblingConstraint()
        }.setColor(Theme.TRANSPARENT) childOf titleBlock

        val patreon = GuiHandler.Button(
            text = "Patreon",
            x = CenterConstraint(),
            y = CenterConstraint(),
            width = 80.percent(),
            height = 60.percent(),
            color = Theme.TRANSPARENT,
            textColor = Theme.TEXT_PRIMARY,
            parent = patreonBlock
        )
            .textHoverEffect(Theme.BUTTON_TITLE_DISC_GIT_PAT_HOVER_OUT, Theme.BUTTON_TITLE_DISC_GIT_PAT_HOVER_IN)
            .setTextOnClick {
                SBOKotlin.openInBrowser("https://www.patreon.com/Skyblock_Overhaul")
            }
        stpBtn(patreon)

        //-----------------End Title Block-----------------
        //-----------------Category Block-----------------
        GuiHandler.UILine(
            x = 15.percent(),
            y = 5.percent(),
            width = 0.2f.percent(),
            height = 95.percent(),
            color = Theme.SBO_BLUE,
            parent = base
        )
        categoryBlock = UIBlock().constrain {
            width = 15.percent()
            height = 94.3f.percent()
            x = 0.percent()
            y = 5.7f.percent()
        }.setColor(Theme.TRANSPARENT) childOf base
        //-----------------End Category Block-----------------
        //-----------------Content Block-----------------
        contentBlock = UIBlock().constrain {
            width = 84.8f.percent()
            height = 94.7f.percent()
            x = 15.2f.percent()
            y = 5.3f.percent()
        }.setColor(Theme.TRANSPARENT) childOf base
        //-----------------End Content Block-----------------
        //-----------------Party Info-----------------
        playerNameBase = UIBlock().constrain {
            width = 50.percent()
            height = 100.percent()
            x = 0.percent()
            y = 0.percent()
        }.setColor(Theme.TRANSPARENT)
        //-----------------End Party Info-----------------
        //-----------------Party List-----------------
        partyListContainer = ScrollComponent().constrain {
            width = 100.percent()
            height = 92.3.percent()
            x = 0.percent()
            y = 7.3f.percent()
        }.setColor(Theme.TRANSPARENT)
        noParties = UIText("No parties found").constrain {
            x = CenterConstraint()
            y = CenterConstraint()
            textScale = getTextScaleOfScaleText()
        }.setColor(Theme.TEXT_PRIMARY)
        partyListContainer.addChild(noParties)
        noParties.hide()
        //-----------------Pages-----------------
        addPage("Home", homePage::render, isSubPage = true, y1 = 93.percent())
        addPage("Help", helpPage::render, isSubPage = true)
        addPage("Settings", ::settings, isSubPage = true, isClickable = true)
        addPage("Diana", dianaPage::render, y1 = 0.percent())
        addPage("Custom", customPage::render)
    }
}
