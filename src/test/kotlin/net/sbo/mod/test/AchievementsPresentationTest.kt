package net.sbo.mod.test

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.nio.file.Files

class AchievementsPresentationTest {
    @Test fun sourceSamplesCoverRarityAndSecretStatesWithoutLiveObjects() {
        assertTrue(AchievementsReviewFixture.catalog.size>=100)
        assertEquals(AchievementsReviewFixture.rarities.toSet(),AchievementsReviewFixture.samples.map{it.rarity}.toSet())
        assertTrue(AchievementsReviewFixture.samples.any{it.hidden})
        val secret=AchievementsReviewFixture.samples.withIndex().first{it.value.hidden&&!AchievementsReviewFixture.unlocked(it.index)}.value
        val f=AchievementsReviewFixture(initialQuery=secret.description)
        assertEquals(0,f.shownCount,"Locked secret descriptions must not be searchable")
    }
    @Test fun cardsAndGroupedProgressFitNormalAndNarrowLayouts() {
        for(width in listOf(640f,360f)) for(grouped in listOf(false,true)) {
            val f=AchievementsReviewFixture(width=width,grouped=grouped)
            assertEquals(11,f.frame.all(".ach-card").size)
            if(grouped)assertEquals(9,f.frame.all(".ach-group-title").size)
            val list=f.frame.element(".ach-list").getBoundingClientRect()
            for(card in f.frame.all(".ach-card")) {
                val b=card.getBoundingClientRect()
                assertTrue(b.x>=list.x&&b.right<=list.right,"$width card $b in $list")
                for(name in card.querySelectorAll(".ach-name")) assertTrue(name.getBoundingClientRect().right<=b.right)
            }
            assertFalse(f.frame.all(".ach-progress-text").isEmpty())
        }
    }
    @Test fun searchFocusAndRarityFilteringRemainUsable() {
        val f=AchievementsReviewFixture(width=360f)
        f.frame.click(".ach-rarities .guilib-chip")
        assertEquals(10,f.shownCount)
        val search=f.frame.element(".ach-search")
        assertEquals(0,search.style.borderTopColor ushr 24)
        f.frame.root.document.keyboardModality=true;search.focus();f.frame.settle()
        assertTrue(search.style.outlineWidth>0)
        f.frame.root.input.charTyped("no-matching-achievement-xyz");f.frame.settle()
        assertEquals("no-matching-achievement-xyz",f.queryValue)
        assertEquals(0,f.shownCount)
        assertFalse(f.frame.all(".ach-empty").isEmpty())
    }
    @Test fun exportAchievementsWhenRequested() {
        if(System.getenv("SBO_ACH_REVIEW")!="1")return
        requireHeadlessReview();val out=AchievementsReviewFixture.out;Files.createDirectories(out)
        for((label,css)in listOf("before" to StandaloneReviewFrame.before("achievements"),"after" to StandaloneReviewFrame.css("achievements"))) {
            for(width in listOf(640f,360f)) {
                val f=AchievementsReviewFixture(css,width,if(width<400)400f else 480f)
                HubReviewRaster.write(f.frame,out.resolve("$label-default-"+(if(width<400)"narrow"else"normal")+".png"))
            }
            HubReviewRaster.write(AchievementsReviewFixture(css,theme="light").frame,out.resolve("$label-light-normal.png"))
        }
        HubReviewRaster.write(AchievementsReviewFixture(width=360f,height=400f,grouped=true).frame,out.resolve("after-grouped-narrow.png"))
        HubReviewRaster.write(AchievementsReviewFixture(width=360f,height=400f,repeatableOnly=true).frame,out.resolve("after-repeatable-narrow.png"))
        HubReviewRaster.write(AchievementsReviewFixture(width=360f,height=400f,initialQuery="no-matching-achievement-xyz").frame,out.resolve("after-empty-search-narrow.png"))
        if(System.getenv("SBO_EVENTS_ACH_SHEET")=="1") {
            fun eventTile(label:String,file:String,narrow:Boolean=false,page:String="list",expanded:Boolean=false):HubReviewRaster.Tile {
                val f=EventsReviewFixture(width=if(narrow)360f else 640f,height=if(narrow)400f else 480f,page=page,initialChart=if(expanded)"lines"else"value",expanded=expanded)
                f.settle()
                val selector=if(expanded)".ev-chart-modal"else if(page=="delete")".ev-modal"else".ev-window"
                return HubReviewRaster.Tile(label,EventsReviewFixture.out.resolve(file),f.frame.element(selector).getBoundingClientRect())
            }
            fun achievementTile(label:String,file:String,narrow:Boolean=false,grouped:Boolean=false,repeatable:Boolean=false,theme:String="sbo-dark"):HubReviewRaster.Tile {
                val f=AchievementsReviewFixture(width=if(narrow)360f else 640f,height=if(narrow)400f else 480f,grouped=grouped,repeatableOnly=repeatable,theme=theme)
                return HubReviewRaster.Tile(label,out.resolve(file),f.frame.element(".ach-window").getBoundingClientRect())
            }
            HubReviewRaster.partyFinderCheckpoint(ReviewResources.project.resolve("build/ui-review/events-achievements-checkpoint.png"),listOf(
                eventTile("Events · normal viewport 640×480","after-list-normal.png"),
                eventTile("Event details · narrow viewport 360×400","after-details-narrow.png",narrow=true,page="details"),
                eventTile("Expanded lines · normal viewport 640×480","after-expanded-lines.png",expanded=true),
                eventTile("Delete confirmation · narrow viewport 360×400","after-delete-narrow.png",narrow=true,page="delete"),
                achievementTile("Achievements · normal viewport 640×480","after-default-normal.png"),
                achievementTile("Grouped · narrow viewport 360×400","after-grouped-narrow.png",narrow=true,grouped=true),
                achievementTile("Repeatable · narrow viewport 360×400","after-repeatable-narrow.png",narrow=true,repeatable=true),
                achievementTile("Achievements Light · normal viewport 640×480","after-light-normal.png",theme="light")
            ))
        }
    }
}
