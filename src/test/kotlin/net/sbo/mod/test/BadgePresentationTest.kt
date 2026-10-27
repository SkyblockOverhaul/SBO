package net.sbo.mod.test

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.nio.file.Files

class BadgePresentationTest {
    @Test fun sourceTabsControlsAndFitGeometryStayIntact() {
        assertEquals(listOf("Places","Colors","Effects","Stat","View"),BadgeReviewFixture.tabNames.map{it.second})
        assertEquals(5,BadgeReviewFixture.viewRows.size);assertEquals(7,BadgeReviewFixture.effects.size);assertEquals(5,BadgeReviewFixture.motions.size)
        for(width in listOf(640f,360f)) {
            val windows=BadgeReviewFixture.tabNames.map{(tab,_)->
                val f=BadgeReviewFixture(width=width,height=if(width<400)300f else 480f,initialTab=tab,initialPart="rank")
                val b=f.frame.element(".bd-window").getBoundingClientRect()
                assertTrue(b.x>=-.2&&b.right<=width+.2,"$width $tab $b")
                for(row in f.frame.all(".bd-setting")) {
                    val text=row.querySelector(".bd-setting-text")!!.getBoundingClientRect();val control=row.children.last().getBoundingClientRect()
                    assertEquals(text.y+text.height/2,control.y+control.height/2,.2f,"$width $tab text/control centers")
                    assertTrue(control.right<=b.right+.2)
                };b
            }
            assertTrue(windows.map{it.height}.max()-windows.map{it.height}.min()<.2,"Tab switch changed window height")
        }
    }
    @Test fun matrixDraftUndoAndUnsavedDismissalStayInert() {
        val f=BadgeReviewFixture();assertEquals(15,f.frame.all(".bd-mcell .guilib-checkbox").size)
        f.frame.click(".bd-mcell .guilib-checkbox");assertTrue(f.changedValue)
        f.frame.click("#badge-undo");assertFalse(f.changedValue);assertEquals(0,f.actionCount)
        val modal=BadgeReviewFixture(width=360f,height=300f,state="unsaved")
        modal.frame.click("#badge-keep");assertTrue(modal.frame.all(".bd-modal").isEmpty());assertTrue(modal.changedValue)
        modal.frame.key("Escape");assertFalse(modal.frame.all(".bd-modal").isEmpty())
        modal.frame.key("Escape");assertTrue(modal.frame.all(".bd-modal").isEmpty());assertEquals(0,modal.actionCount)
    }
    @Test fun scaledSelectionRemainsOnItsActualControl() {
        val before=BadgeReviewFixture(StandaloneReviewFrame.before("badge"),width=360f,height=300f,initialTab="colors",initialPart="rank")
        val oldPick=before.frame.element(".bd-parts .guilib-segment.selected").getBoundingClientRect()
        val oldIndicator=before.frame.element(".bd-parts .guilib-segment-indicator").getBoundingClientRect()
        assertTrue(kotlin.math.abs(oldPick.x-oldIndicator.x)>10,"Baseline must reproduce the scaled highlight drift")
        val after=BadgeReviewFixture(width=360f,height=300f,initialTab="colors",initialPart="rank")
        val pick=after.frame.element(".bd-parts .guilib-segment.selected")
        assertTrue(pick.style.backgroundColor ushr 24>0,"The selected control itself must paint its highlight")
        assertEquals("NONE",after.frame.element(".bd-parts .guilib-segment-indicator").style.display.toString())
        after.frame.click(".bd-tabs .guilib-tab");assertEquals("places",after.tabValue)
        after.frame.key("ArrowRight");assertEquals("colors",after.tabValue);assertEquals(0,after.actionCount)
    }
    @Test fun exportBadgeWhenRequested() {
        if(System.getenv("SBO_BADGE_REVIEW")!="1")return
        requireHeadlessReview();val out=BadgeReviewFixture.out;Files.createDirectories(out)
        for(tab in listOf("places","effects","view"))HubReviewRaster.write(BadgeReviewFixture(StandaloneReviewFrame.before("badge"),initialTab=tab).frame,out.resolve("before-$tab-normal.png"))
        val before=BadgeReviewFixture(StandaloneReviewFrame.before("badge"),width=360f,height=300f,initialTab="colors",initialPart="rank")
        HubReviewRaster.write(before.frame,out.resolve("before-colors-narrow.png"))
        Files.writeString(out.resolve("measurements.txt"),"Before selected Rank: "+before.frame.element(".bd-parts .guilib-segment.selected").getBoundingClientRect()+"\nBefore moving highlight: "+before.frame.element(".bd-parts .guilib-segment-indicator").getBoundingClientRect()+"\n")
        for((tab,_)in BadgeReviewFixture.tabNames)HubReviewRaster.write(BadgeReviewFixture(initialTab=tab).frame,out.resolve("after-$tab-normal.png"))
        for(tab in listOf("colors","effects","view"))HubReviewRaster.write(BadgeReviewFixture(width=360f,height=300f,initialTab=tab,initialPart="rank").frame,out.resolve("after-$tab-narrow.png"))
        for(state in listOf("loading","error","ineligible","unsaved","hidden","saving"))HubReviewRaster.write(BadgeReviewFixture(width=360f,height=300f,state=state).frame,out.resolve("after-$state-narrow.png"))
        HubReviewRaster.write(BadgeReviewFixture(initialTab="colors",initialPart="rank",theme="light").frame,out.resolve("after-colors-light.png"))
        HubReviewRaster.write(BadgeReviewFixture(initialTab="effects",phase=1200L).frame,out.resolve("after-preview-frame1200.png"))
        val picker=BadgeReviewFixture(initialTab="colors",initialPart="rank",width=360f,height=300f)
        picker.frame.click(".guilib-color-input")
        HubReviewRaster.write(picker.frame,out.resolve("after-picker-narrow.png"))
        for(tab in listOf("colors","view")) {
            val bottom=BadgeReviewFixture(width=360f,height=300f,initialTab=tab,initialPart="rank")
            bottom.frame.element(".bd-panel").scrollTop=Float.MAX_VALUE;bottom.frame.settle()
            HubReviewRaster.write(bottom.frame,out.resolve("after-$tab-bottom-narrow.png"))
        }
    }
}
