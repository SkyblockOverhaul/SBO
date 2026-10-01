package net.sbo.mod.partyfinder

import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import net.sbo.mod.SBOKotlin.API_URL
import net.sbo.mod.SBOKotlin.LIVE_API_URL
import net.sbo.mod.SBOKotlin.logger
import net.sbo.mod.partyfinder.api.CheckBody
import net.sbo.mod.partyfinder.api.PartyFinderApi
import net.sbo.mod.partyfinder.api.PartyView
import net.sbo.mod.utils.chat.Chat
import net.sbo.mod.utils.data.configs.partyfinder.PartyDraft
import net.sbo.mod.utils.events.Register

/**
 * Temporary chat command to use the party finder until the new GUI exists. Remove with the old GUI cleanup (4f).
 *
 *   /sbopftest types
 *   /sbopftest me <type> [sub]
 *   /sbopftest list <type> [sub]
 *   /sbopftest create <type> [sub] [size] [stat=value] [option=value] [roles=dps,stunner] [note=text_with_underscores]
 *   /sbopftest join <leader> [role]
 *   /sbopftest bph
 */
object PartyFinderTestCommand {
    private var lastList: List<PartyView> = emptyList()

    fun init() {
        // Against a dev backend, check the connection once at startup and log the result
        if (API_URL != LIVE_API_URL) selfTest()

        Register.command("sbopftest") { args ->
            when (args.getOrNull(0)?.lowercase()) {
                "types" -> withCategories { showTypes() }
                "me" -> withCategories { target(args)?.let(::showOwnStats) }
                "list" -> withCategories { target(args)?.let(::showList) }
                "create" -> withCategories { create(args) }
                "join" -> join(args)
                "bph" -> {
                    StatReporter.report()
                    Chat.chat("§6[SBO] §eBurrows per hour sent, if there is at least one hour of Diana data.")
                }
                else -> Chat.chat("§6[SBO] §eUse /sbopftest types | me | list | create | join | bph")
            }
        }
    }

    private fun selfTest() {
        PartyCategories.get { data ->
            if (data == null) {
                logger.error("[SBO] PF self test: categories not loaded from $API_URL")
                return@get
            }
            logger.info("[SBO] PF self test: ${data.categories.size} categories, version ${data.version}")
            PartyFinderApi.parties("diana", onError = { logger.error("[SBO] PF self test: list failed ${it.code} ${it.message}") }) {
                logger.info("[SBO] PF self test: ${it.size} diana parties listed")
            }
            PartyFinderApi.checkMembers(
                CheckBody("diana", uuids = listOf("33cd429790564c91a7579f56a3739431")),
                onError = { logger.info("[SBO] PF self test: member check answered ${it.code} (${ProblemText.error(it)})") }
            ) { logger.info("[SBO] PF self test: member check ok, ${it.members.size} member") }
        }
    }

    private fun withCategories(action: () -> Unit) {
        PartyCategories.get { data ->
            if (data == null) Chat.chat("§6[SBO] §4Could not load the party types from the SBO server.")
            else action()
        }
    }

    private fun target(args: Array<String>): PartyTarget? {
        val type = args.getOrNull(1)
        val target = type?.let { PartyCategories.target(it, args.getOrNull(2).orEmpty()) }
        if (target == null) Chat.chat("§6[SBO] §4Unknown party type. See /sbopftest types")
        return target
    }

    private fun showTypes() {
        PartyCategories.categories.forEach { category ->
            val subs = category.subcategories.joinToString(", ") { sub ->
                sub.id + if (sub.open) "" else " §7(closed)§e"
            }
            Chat.chat("§6[SBO] §b${category.id} §7max ${category.maxSize}§e ${if (subs.isEmpty()) "" else "subtypes: $subs"}")
            val reqs = PartyCategories.target(category.id)!!.reqs
            Chat.chat("§7   requirements: ${reqs.joinToString(", ") { "${it.stat} (${it.type})" }}")
        }
    }

    private fun showOwnStats(target: PartyTarget) {
        OwnStats.get(target, onError = { Chat.chat("§6[SBO] §4${ProblemText.error(it)}") }) { me ->
            Chat.chat("§6[SBO] §eYour stats for §b${target.label}§e:")
            (target.reqs.map { it.stat } + target.display + "ironman").distinct().forEach { stat ->
                val label = PartyCategories.stat(stat)?.label ?: stat
                Chat.chat("§7   $label: §f${ProblemText.value(stat, me.stats[stat])}")
            }
        }
    }

    private fun showList(target: PartyTarget) {
        // Own stats first for "can I join?", the list also works without them
        OwnStats.get(target, onError = { showParties(target) }) { showParties(target) }
    }

    private fun showParties(target: PartyTarget) {
        PartyFinderManager.listParties(target.partyType, target.subType, onComplete = { parties ->
            lastList = parties
            if (parties.isEmpty()) Chat.chat("§6[SBO] §eNo ${target.label} parties right now.")
            parties.forEach { party ->
                val me = OwnStats.cached(target)
                val canJoin = me?.let { if (ReqMatcher.checkJoin(party, target, it).isEmpty()) " §a(you can join)" else " §c(you can't join)" } ?: ""
                Chat.chat("§6[SBO] §b${party.leader?.name} §7${party.memberCount}/${party.partySize}$canJoin §f${party.note}")
                val reqs = party.reqs.entries.joinToString(", ") { (stat, need) ->
                    "${PartyCategories.stat(stat)?.label ?: stat} ${ProblemText.value(stat, need)}"
                }
                if (reqs.isNotEmpty()) Chat.chat("§7   needs: $reqs")
                if (party.roles.wanted.isNotEmpty()) Chat.chat("§7   roles: ${party.roles.wanted.joinToString(", ")}")
            }
        })
    }

    private fun create(args: Array<String>) {
        val target = target(args) ?: return
        val rest = args.drop(2).toMutableList()
        if (rest.firstOrNull()?.contains('=') == false && rest.first().toIntOrNull() == null) rest.removeAt(0)
        val size = rest.firstOrNull()?.toIntOrNull()?.also { rest.removeAt(0) } ?: target.maxSize
        val draft = PartyDraft(partyType = target.partyType, subType = target.subType, partySize = size)
        for (pair in rest) {
            val (key, value) = pair.split('=', limit = 2).takeIf { it.size == 2 } ?: continue
            when {
                key == "note" -> draft.note = value.replace('_', ' ')
                key == "roles" -> draft.wantedRoles = value.split(',').filter { it.isNotBlank() }.toMutableList()
                target.option(key) != null -> draft.options[key] = value
                target.req(key) != null -> draft.reqs[key] = reqValue(target.req(key)!!.type, value).toString()
                else -> Chat.chat("§6[SBO] §c$key is not a requirement or setting of ${target.label}, ignored.")
            }
        }
        PartyFinderManager.createParty(draft)
    }

    /** Numbers, true, a rarity, or for anyOf a list like TERROR:FIERY,AURORA. */
    private fun reqValue(type: String, value: String) = when (type) {
        "min" -> JsonPrimitive(value.toDoubleOrNull() ?: 0.0)
        "flag" -> JsonPrimitive(true)
        "rarity" -> JsonPrimitive(value.uppercase())
        else -> buildJsonArray {
            value.split(',').filter { it.isNotBlank() }.forEach { entry ->
                val (id, tier) = entry.split(':').let { it[0].uppercase() to it.getOrNull(1)?.uppercase() }
                add(buildJsonObject { put("id", id); tier?.let { put("minTier", it) } })
            }
        }
    }

    private fun join(args: Array<String>) {
        val leader = args.getOrNull(1)
        val party = lastList.firstOrNull { it.leader?.name.equals(leader, ignoreCase = true) }
        if (party == null) {
            Chat.chat("§6[SBO] §cRun /sbopftest list <type> first, then join one of the listed leaders.")
            return
        }
        PartyFinderManager.sendJoinRequest(party, args.getOrNull(2)?.lowercase())
    }
}
