package net.sbo.mod.utils.overlay

object OverlayExamples {
    val mythosMobHpExample: List<OverlayTextLine> = listOf(OverlayTextLine("§8[§7Lv1250§8] §2§e§d §2Empyrean King Minos §a100M§f/§a100M§c❤ §b✯", centered = true), OverlayTextLine("§bLootshared", centered = true), OverlayTextLine("§6King Minos §7- §575 Hits", centered = true))

    val lootsharePartyExample: List<OverlayTextLine> = listOf(
        OverlayTextLine("§6Lootshare §7- §dKing Minos"),
        OverlayTextLine("§dSaotzuri §7(spawned)"),
        OverlayTextLine("§bFriend ✔"),
        OverlayTextLine("§cOtherFriend ✖"),
    )

    val dianaStarlessMobExample: List<OverlayTextLine> = listOf(OverlayTextLine("§cNO SHURIKEN!"))

    val pickupLogExample: List<OverlayTextLine> = listOf(
        OverlayTextLine("§a+ 1x §fRotten Flesh"),
        OverlayTextLine("§c- 1x §5Empty Thunder Bottle")
    )
}
