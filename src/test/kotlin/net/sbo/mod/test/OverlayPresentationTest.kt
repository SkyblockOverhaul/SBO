package net.sbo.mod.test

import net.sbo.mod.utils.overlay.overlayReadoutPosition
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.nio.file.Files

class OverlayPresentationTest {
    @Test fun productionReadoutPositionKeepsCornersVisible() {
        for((w,h)in listOf(640 to 360,360 to 240))for((left,top)in listOf(0 to 0,w-140 to 0,0 to h-30,w-140 to h-30,28 to 45)) {
            val (x,y)=overlayReadoutPosition(left,top,30,180,15,w,h)
            assertTrue(x>=0&&x+180<=w);assertTrue(y>=0&&y+15<=h)
            if(top==0)assertEquals(32,y)else assertEquals(top-17,y)
        }
    }
    @Test fun syntheticReadoutAndSelectionFitTheCanvas() {
        for((w,h)in listOf(640f to 360f,360f to 240f))for(edge in listOf(false,true)) {
            val f=OverlayReviewFixture(w,h,edge)
            val b=f.frame.element(".ov-readout").getBoundingClientRect()
            assertTrue(b.x>=0&&b.right<=w+.1&&b.y>=0&&b.bottom<=h+.1)
            assertEquals(1,f.frame.all(".ov-selected").size)
        }
    }
    @Test fun exportOverlayWhenRequested() {
        if(System.getenv("SBO_OVERLAY_REVIEW")!="1")return
        requireHeadlessReview();val out=OverlayReviewFixture.out;Files.createDirectories(out)
        for((w,h)in listOf(640f to 360f,360f to 240f))for(edge in listOf(false,true)) {
            val f=OverlayReviewFixture(w,h,edge)
            HubReviewRaster.write(f.frame,out.resolve((if(w<400)"narrow"else"normal")+if(edge)"-edge.png"else"-selected.png"))
        }
        if(System.getenv("SBO_BADGE_OVERLAY_SHEET")=="1") {
            fun badgeTile(label:String,file:String,tab:String="places",state:String="ready",narrow:Boolean=false):HubReviewRaster.Tile {
                val f=BadgeReviewFixture(width=if(narrow)360f else 640f,height=if(narrow)300f else 480f,initialTab=tab,state=state)
                return HubReviewRaster.Tile(label,BadgeReviewFixture.out.resolve(file),f.frame.element(if(state=="unsaved")".bd-modal"else".bd-window").getBoundingClientRect())
            }
            val overlay=OverlayReviewFixture(360f,240f,true)
            HubReviewRaster.partyFinderCheckpoint(ReviewResources.project.resolve("build/ui-review/badge-overlay-checkpoint.png"),listOf(
                badgeTile("Badge Places · normal viewport 640×480","after-places-normal.png"),
                badgeTile("Badge Colors · narrow viewport 360×300","after-colors-narrow.png",tab="colors",narrow=true),
                badgeTile("Badge Effects · normal viewport 640×480","after-effects-normal.png",tab="effects"),
                badgeTile("Badge Stat · normal viewport 640×480","after-stat-normal.png",tab="stat"),
                badgeTile("Badge View scroll end · viewport 360×300","after-view-bottom-narrow.png",tab="view",narrow=true),
                badgeTile("Badge eligibility · narrow viewport 360×300","after-ineligible-narrow.png",state="ineligible",narrow=true),
                badgeTile("Badge unsaved · narrow viewport 360×300","after-unsaved-narrow.png",state="unsaved",narrow=true),
                HubReviewRaster.Tile("Overlay edge · narrow viewport 360×240",out.resolve("narrow-edge.png"),overlay.frame.element(".ov-canvas").getBoundingClientRect())
            ))
        }
    }
}
