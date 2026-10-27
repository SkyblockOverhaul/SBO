package net.sbo.mod.test

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.nio.file.Files

class CloudPresentationTest {
    @Test fun compareChoicesAndFooterFitNormalAndNarrowWindows() {
        assertEquals(7,CloudReviewFixture.info.size)
        assertEquals(6,CloudReviewFixture.statuses.size)
        for(width in listOf(640f,360f)) {
            val f=CloudReviewFixture(width=width,height=if(width<400)400f else 480f,view="compare")
            val window=f.frame.element(".cs-window").getBoundingClientRect()
            for(selector in listOf(".cs-pick",".cs-footer button"))for(e in f.frame.all(selector)) {
                val b=e.getBoundingClientRect()
                assertTrue(b.x>=window.x&&b.right<=window.right,"$width $selector $b in $window")
            }
            for(row in f.frame.all(".cs-perfield .cs-field")) {
                val text=row.querySelector(".cs-field-label")!!.getBoundingClientRect()
                val control=row.querySelector(".cs-pick")!!.getBoundingClientRect()
                assertEquals(text.y+text.height/2,control.y+control.height/2,.1f,"$width wrapped label/choice centers")
            }
            f.frame.click("#all-cloud");assertEquals(6,f.picks.size);assertTrue(f.picks.values.all{it=="cloud"})
            f.frame.click("#all-pc");assertTrue(f.picks.isEmpty())
            assertEquals(0,f.actionCount)
        }
    }
    @Test fun confirmationCancelEscapeAndDisabledActionsStayInert() {
        for(view in listOf("delete","restore")) {
            val f=CloudReviewFixture(width=360f,height=400f,view=view)
            val confirm=f.frame.element("#cloud-confirm")
            assertNotEquals(f.frame.element("#cloud-cancel").style.backgroundColor,confirm.style.backgroundColor)
            f.frame.click("#cloud-cancel");assertTrue(f.frame.all(".cs-modal").isEmpty());assertEquals(0,f.actionCount)
            val escaped=CloudReviewFixture(view=view);escaped.frame.key("Escape");assertTrue(escaped.frame.all(".cs-modal").isEmpty());assertEquals(0,escaped.actionCount)
        }
        assertEquals(4,CloudReviewFixture(status="busy").frame.all(".cs-actions button:disabled").size)
        assertEquals(3,CloudReviewFixture(status="no-save").frame.all(".cs-actions button:disabled").size)
    }
    @Test fun exportCloudWhenRequested() {
        if(System.getenv("SBO_CLOUD_REVIEW")!="1")return
        requireHeadlessReview();val out=CloudReviewFixture.out;Files.createDirectories(out)
        for((label,css)in listOf("before" to StandaloneReviewFrame.before("cloud"),"after" to StandaloneReviewFrame.css("cloud")))for(view in listOf("main","compare")) {
            val narrow=view=="compare";val f=CloudReviewFixture(css,width=if(narrow)360f else 640f,height=if(narrow)400f else 480f,view=view)
            HubReviewRaster.write(f.frame,out.resolve("$label-$view-"+(if(narrow)"narrow"else"normal")+".png"))
        }
        for(view in listOf("main","compare","backups","delete","restore","empty-backups","empty-compare","failure")) {
            val narrow=view!="compare"&&view!="backups";val f=CloudReviewFixture(width=if(narrow)360f else 640f,height=if(narrow)400f else 480f,view=view)
            HubReviewRaster.write(f.frame,out.resolve("after-$view-"+(if(narrow)"narrow"else"normal")+".png"))
        }
        for(state in listOf("busy","no-save","supporter"))HubReviewRaster.write(CloudReviewFixture(width=360f,height=400f,status=state).frame,out.resolve("after-$state-narrow.png"))
        HubReviewRaster.write(CloudReviewFixture(theme="light",view="compare").frame,out.resolve("after-compare-light.png"))
        val bottom=CloudReviewFixture(width=360f,height=400f,view="compare")
        bottom.frame.element(".cs-body").scrollTop=Float.MAX_VALUE;bottom.frame.settle()
        HubReviewRaster.write(bottom.frame,out.resolve("after-compare-bottom-narrow.png"))
        if(System.getenv("SBO_SOUNDS_CLOUD_SHEET")=="1") {
            fun soundTile(label:String,file:String)=HubReviewRaster.Tile(label,SoundsReviewFixture.out.resolve(file),SoundsReviewFixture(width=if(file.contains("narrow"))360f else 640f,height=if(file.contains("narrow"))400f else 480f).frame.element(".snd-window").getBoundingClientRect())
            fun cloudTile(label:String,file:String,view:String="main",narrow:Boolean=false,status:String="conflict"):HubReviewRaster.Tile {
                val f=CloudReviewFixture(width=if(narrow)360f else 640f,height=if(narrow)400f else 480f,view=view,status=status)
                return HubReviewRaster.Tile(label,out.resolve(file),f.frame.element(if(view in listOf("delete","restore"))".cs-modal"else".cs-window").getBoundingClientRect())
            }
            HubReviewRaster.partyFinderCheckpoint(ReviewResources.project.resolve("build/ui-review/sounds-cloud-checkpoint.png"),listOf(
                soundTile("Sounds · normal viewport 640×480","after-normal.png"),
                soundTile("Sounds Drops · narrow viewport 360×400","after-drops-narrow.png"),
                cloudTile("Cloud main · normal viewport 640×480","after-main-normal.png"),
                cloudTile("Compare · narrow viewport 360×400","after-compare-narrow.png",view="compare",narrow=true),
                cloudTile("Compare scroll end · narrow viewport 360×400","after-compare-bottom-narrow.png",view="compare",narrow=true),
                cloudTile("Backups · normal viewport 640×480","after-backups-normal.png",view="backups"),
                cloudTile("Delete confirmation · narrow viewport 360×400","after-delete-narrow.png",view="delete",narrow=true),
                cloudTile("Restore confirmation · narrow viewport 360×400","after-restore-narrow.png",view="restore",narrow=true)
            ))
        }
    }
}
