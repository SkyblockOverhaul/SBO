package net.sbo.mod.test

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.nio.file.Files

class SoundsPresentationTest {
    @Test fun sourceGroupsAndNarrowControlsRemainContained() {
        assertEquals(listOf("Spawns","Low HPs","Drops"),SoundsReviewFixture.groups.map{it.first})
        assertEquals(listOf(6,4,8),SoundsReviewFixture.groups.map{it.second.size})
        for(width in listOf(640f,360f)) {
            val f=SoundsReviewFixture(width=width,height=if(width<400)400f else 480f)
            assertEquals(18,f.frame.all(".snd-row").size)
            for(setting in f.frame.all(".snd-setting")) {
                val text=setting.querySelector(".snd-setting-text")!!.getBoundingClientRect()
                val control=setting.children.last().getBoundingClientRect()
                assertEquals(text.y+text.height/2,control.y+control.height/2,.1f,"$width whole text/control centers")
            }
            for(row in f.frame.all(".snd-row")) {
                val b=row.getBoundingClientRect()
                for(selector in listOf(".snd-label",".snd-sound",".guilib-slider-field",".snd-test")) {
                    val r=row.querySelector(selector)!!.getBoundingClientRect()
                    assertTrue(r.x>=b.x-.1&&r.right<=b.right+.1,"$width $selector $r in $b")
                }
            }
        }
    }
    @Test fun emptyMissingAndSearchablePickerStayInert() {
        val f=SoundsReviewFixture(width=360f,height=400f)
        assertFalse(f.frame.all(".snd-test:disabled").isEmpty())
        f.frame.scrollTo(".snd-row",".snd-body")
        f.frame.click(".snd-test");assertEquals(1,f.testCount)
        f.frame.click(".snd-sound")
        assertFalse(f.frame.all(".guilib-select-menu").isEmpty())
        f.frame.key("Escape");assertTrue(f.frame.all(".guilib-select-menu").isEmpty())
        assertEquals(0,f.otherActions)
    }
    @Test fun exportSoundsWhenRequested() {
        if(System.getenv("SBO_SOUNDS_REVIEW")!="1")return
        requireHeadlessReview();val out=SoundsReviewFixture.out;Files.createDirectories(out)
        val measurements=StringBuilder()
        for((label,css)in listOf("before" to StandaloneReviewFrame.before("sounds"),"after" to StandaloneReviewFrame.css("sounds")))for(width in listOf(640f,360f)) {
            val f=SoundsReviewFixture(css,width,if(width<400)400f else 480f)
            HubReviewRaster.write(f.frame,out.resolve("$label-"+(if(width<400)"narrow"else"normal")+".png"))
            measurements.append("$label $width General: "+f.frame.all(".snd-setting").map{it.getBoundingClientRect()}+"\n")
        }
        val drops=SoundsReviewFixture(width=360f,height=400f);drops.frame.scrollTo("#sounds-section-Drops",".snd-body")
        HubReviewRaster.write(drops.frame,out.resolve("after-drops-narrow.png"))
        val picker=SoundsReviewFixture(width=360f,height=400f);picker.frame.scrollTo(".snd-row",".snd-body");picker.frame.click(".snd-sound")
        HubReviewRaster.write(picker.frame,out.resolve("after-picker-narrow.png"))
        HubReviewRaster.write(SoundsReviewFixture(theme="light").frame,out.resolve("after-light-normal.png"))
        Files.writeString(out.resolve("measurements.txt"),measurements)
    }
}
