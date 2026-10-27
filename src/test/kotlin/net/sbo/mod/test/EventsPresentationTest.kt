package net.sbo.mod.test

import net.sbo.mod.guis.niceTicks
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.nio.file.Files

class EventsPresentationTest {
    @Test fun sourceTablesAndProfitTicksRemainComplete() {
        requireHeadlessReview()
        assertEquals(7,EventsReviewFixture.summaryLabels.size)
        assertEquals(27,EventsReviewFixture.itemLabels.size)
        assertEquals(17,EventsReviewFixture.mobLabels.size)
        for(max in listOf(0L,950_000_000L,12_340_000_000L)) {
            val ticks=niceTicks(max);assertEquals(0L,ticks.first());assertTrue(ticks.last()>=max)
            assertTrue(ticks.zipWithNext().all{it.first<it.second})
        }
    }
    @Test fun detailsKeepReadableLabelAndNumericColumns() {
        for(width in listOf(640f,360f)) {
            val f=EventsReviewFixture(width=width,page="details");f.settle()
            assertEquals(43,f.frame.all(".ev-row").size)
            for(row in f.frame.all(".ev-row")) {
                val bounds=row.getBoundingClientRect()
                val label=row.querySelector(".ev-row-label")!!.getBoundingClientRect()
                assertTrue(label.width>=75f,"$width label budget $label")
                for(s in listOf(".ev-row-value",".ev-row-percent",".ev-trend")) {
                    val b=row.querySelector(s)!!.getBoundingClientRect()
                    assertTrue(b.x>=bounds.x&&b.right<=bounds.right+.1,"$width $s $b in $bounds")
                }
            }
        }
    }
    @Test fun chartModesAndExpandedAxesAreContained() {
        for(mode in listOf("value","split","lines","luck")) {
            val f=EventsReviewFixture(width=360f,initialChart=mode,expanded=mode=="lines");f.settle()
            assertEquals(mode,f.chartMode)
            val chart=f.frame.all(".ev-chart").last().getBoundingClientRect()
            assertTrue(chart.x>=0&&chart.right<=360.1)
            if(mode=="lines") {
                assertEquals(5,f.frame.all(".ev-chart.big .ev-lines-series").size)
                val plot=f.frame.element(".ev-chart.big .ev-lines-plot").getBoundingClientRect()
                assertTrue(plot.width>100&&plot.height>50)
            }
        }
    }
    @Test fun deletionConfirmationIsDistinctAndInert() {
        val f=EventsReviewFixture(width=360f,page="delete");f.settle()
        val cancel=f.frame.element("#cancel-delete");val delete=f.frame.element("#confirm-delete")
        assertNotEquals(cancel.style.backgroundColor,delete.style.backgroundColor)
        f.frame.click("#cancel-delete");assertEquals(0,f.deletes)
        val confirmed=EventsReviewFixture(width=360f,page="delete")
        confirmed.frame.click("#confirm-delete");assertEquals(1,confirmed.deletes)
        val escaped=EventsReviewFixture(width=360f,page="delete");escaped.frame.key("Escape")
        assertTrue(escaped.frame.all(".ev-modal").isEmpty());assertEquals(0,escaped.deletes)
    }
    @Test fun exportEventsWhenRequested() {
        if(System.getenv("SBO_EVENTS_REVIEW")!="1")return
        requireHeadlessReview();val out=EventsReviewFixture.out;Files.createDirectories(out)
        val measurements=StringBuilder()
        for((label,css) in listOf("before" to StandaloneReviewFrame.before("events"),"after" to StandaloneReviewFrame.css("events"))) {
            for(page in listOf("list","details")) for(width in listOf(640f,360f)) {
                val f=EventsReviewFixture(css,width,if(width<400)400f else 480f,page=page)
                val name="$label-$page-"+if(width<400)"narrow"else"normal"
                HubReviewRaster.write(f,out.resolve("$name.png"))
                measurements.append("$name "+f.frame.all(".ev-row-label").take(3).map{it.getBoundingClientRect()}+"\n")
            }
        }
        for((name,page,mode) in listOf(Triple("split","list","split"),Triple("luck-narrow","list","luck"),Triple("expanded-lines","list","lines"),Triple("delete-narrow","delete","value"),Triple("empty-narrow","empty","value"),Triple("total","total","value"),Triple("current-narrow","current","value"))) {
            val narrow=name.endsWith("narrow");val f=EventsReviewFixture(width=if(narrow)360f else 640f,height=if(narrow)400f else 480f,page=page,initialChart=mode,expanded=name=="expanded-lines")
            HubReviewRaster.write(f,out.resolve("after-$name.png"))
        }
        Files.writeString(out.resolve("measurements.txt"),measurements)
    }
}
