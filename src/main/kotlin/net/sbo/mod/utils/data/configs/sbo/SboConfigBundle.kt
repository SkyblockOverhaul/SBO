package net.sbo.mod.utils.data.configs.sbo

import net.sbo.mod.utils.data.configs.achievements.AchievementsData
import net.sbo.mod.utils.data.configs.diana.DianaTrackerMayorData
import net.sbo.mod.utils.data.configs.diana.DianaTrackerSessionData
import net.sbo.mod.utils.data.configs.diana.DianaTrackerTotalData
import net.sbo.mod.utils.data.configs.diana.PastDianaEventsData
import net.sbo.mod.utils.data.configs.overlay.OverlayData
import net.sbo.mod.utils.data.configs.partyfinder.PartyFinderConfigState
import net.sbo.mod.utils.data.configs.partyfinder.PartyFinderData
import net.sbo.mod.utils.data.configs.sound.SoundSettingsData

data class SboConfigBundle(
    var sboData: SboData,
    var achievementsData: AchievementsData,
    var pastDianaEventsData: PastDianaEventsData,
    var dianaTrackerTotalData: DianaTrackerTotalData,
    var dianaTrackerSessionData: DianaTrackerSessionData,
    var dianaTrackerMayorData: DianaTrackerMayorData,
    var partyFinderConfigState: PartyFinderConfigState,
    var partyFinderData: PartyFinderData,
    var overlayData: OverlayData,
    var soundSettingsData: SoundSettingsData
)
