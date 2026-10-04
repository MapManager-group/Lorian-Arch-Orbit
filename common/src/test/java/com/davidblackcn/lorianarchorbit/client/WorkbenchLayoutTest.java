package com.davidblackcn.lorianarchorbit.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WorkbenchLayoutTest {
    @Test void dropdownsMatchTheirHeadersAndOpenAwayFromWindowEdges() {
        assertTrue(WorkbenchMenuLayout.preferredWidth(54, false) < 100);
        assertTrue(WorkbenchMenuLayout.preferredWidth(110, false) > WorkbenchMenuLayout.preferredWidth(54, false));
        assertEquals(240, WorkbenchMenuLayout.preferredWidth(1000, true));
        for (int[] size : new int[][]{{320,180},{427,240},{768,408}}) for (int count : new int[]{2,3,100}) {
            var down = WorkbenchMenuLayout.anchored(size[0], size[1], 12, 4, 102, 20, count);
            assertEquals(102, down.width());
            assertEquals(12, down.x());
            assertEquals(24, down.y());
            var up = WorkbenchMenuLayout.anchored(size[0], size[1], size[0]-72, size[1]-26, 60, 20, count);
            assertEquals(60, up.width());
            assertEquals(size[0]-72, up.x());
            assertEquals(size[1]-26, up.y()+up.height());
            assertTrue(up.x()+up.width() <= size[0]-8);
            assertTrue(up.y() >= 8);
            assertTrue(down.y()+down.height() <= size[1]-8);
        }
    }
    @Test void menuScrollbarCanReachEveryEntryWithoutJumpingAtGrabPoint() {
        var menu = WorkbenchMenuLayout.anchored(320,180,12,4,60,20,100);
        int count = 100, grab = menu.thumbHeight(count)/2;
        assertEquals(0,menu.scrollAt(menu.y()+grab,grab,count));
        assertEquals(count-menu.rows(),menu.scrollAt(menu.y()+menu.height(),grab,count));
        int previous = -1;
        for (int y=menu.y();y<=menu.y()+menu.height();y++) {
            int start=menu.scrollAt(y,grab,count);
            assertTrue(start>=previous && start<=count-menu.rows());
            previous=start;
        }
    }
    @Test void homeCardsExpandFromSplitPanelsToFourCellsWithinTheCanvas() {
        for(int w:new int[]{320,427,768}) for(int h:new int[]{180,240,408}) for(int count:new int[]{1,2,3,4}) {
            var layout=EditorHomeLayout.calculate(w,h,count);
            assertEquals(count==1?1:2,layout.columns());
            assertEquals(count<=2?1:2,layout.rows());
            for(int i=0;i<count;i++) {
                assertTrue(layout.x(i)>=12 && layout.y(i)>=32);
                assertTrue(layout.x(i)+layout.cardWidth()<=w-12);
                assertTrue(layout.y(i)+layout.cardHeight()<=h-16);
                assertTrue(layout.cardWidth()>=140 && layout.cardHeight()>=50);
                for(int j=0;j<i;j++) assertTrue(layout.x(j)+layout.cardWidth()<=layout.x(i)
                        || layout.y(j)+layout.cardHeight()<=layout.y(i));
            }
        }
        var reference = EditorHomeLayout.calculate(768,408,2);
        assertEquals(176,reference.cardWidth());
        assertEquals(163,reference.cardHeight());
        assertEquals(768-reference.left(), reference.x(1)+reference.cardWidth());
    }
    @Test void popupMenusClampAtEveryEdgeAndScrollLongLists() {
        for (int[] size : new int[][]{{320,180},{427,240},{768,408},{1024,768}})
            for (int count : new int[]{2,3,8,100}) for (int x : new int[]{-100,12,size[0]}) for(int y : new int[]{-20,4,size[1]}) {
                var menu = WorkbenchMenuLayout.calculate(size[0],size[1],x,y,224,count);
                assertTrue(menu.x() >= 8 && menu.y() >= 8);
                assertTrue(menu.x()+menu.width() <= size[0]-8);
                assertTrue(menu.y()+menu.height() <= size[1]-8);
                assertTrue(menu.rows() > 0 && menu.rows() <= count);
            }
    }
    @Test void gradientPanelsAndSmallWindowFooterNeverOverlap() {
        for(int w : new int[]{320,427,599,600,768}) for(int h : new int[]{180,240,269,270,408}) {
            var l=HueGradientLayout.calculate(w,h);
            assertEquals(w<600 || h<270,l.compact());
            assertTrue(l.previewTop()+l.rows()*24 <= l.bodyBottom());
            assertTrue(l.bodyBottom() <= l.footerTop()-14);
            if(!l.compact()) assertTrue(l.left()+l.settingsWidth()<l.resultLeft());
            assertTrue(l.resultLeft()+l.resultWidth()<=w-12);
            assertTrue(l.left()+64 < l.targetLeft());
            assertTrue(l.targetLeft()+l.targetWidth()<l.backLeft());
            assertEquals(68,l.applyLeft()-l.backLeft());
            assertEquals(l.left()+l.width(),l.applyLeft()+64);
        }
    }
    @Test void eightNodesFitEachRowAndAdditionalRowsKeepSettingsReachable() {
        for(int width:new int[]{320,600,768}) for(int height:new int[]{180,270,408}) {
            var l = HueGradientLayout.calculate(width,height);
            for(int count=2;count<=16;count++) {
                int extra = HueGradientLayout.nodeExtraHeight(count);
                for (int i=0;i<count;i++) {
                    assertTrue(HueGradientLayout.nodeX(i)+26 <= l.settingsWidth()-16);
                    assertTrue(HueGradientLayout.nodeY(i)+24 <= 42+extra);
                    assertEquals(i/8, (HueGradientLayout.nodeY(i)-16)/28);
                }
                int generateBottom = l.bodyTop()+278+extra+20-l.settingsScrollMax(count);
                assertTrue(generateBottom <= l.bodyBottom());
                assertTrue(generateBottom-20 >= l.bodyTop());
            }
        }
    }
    @Test void tilesRenderOnlyVisibleSamplesAndReachBothEnds() {
        for(int count:new int[]{0,1,8,36,1906}) for(int cell:new int[]{16,24,32})
            for(int extent:new int[]{30,128,500}) for(int offset:new int[]{-1,0,15,100000}) {
                var t=TextureTilingLayout.calculate(count,cell,extent,34,offset,10000);
                assertTrue(t.first()>=0 && t.last()<=count && t.first()<=t.last());
                assertTrue(t.last()-t.first()<= (extent+cell-1)/cell+1);
                assertTrue(t.lastCross()<=4 && t.firstCross()>=0);
                if(offset==100000) assertEquals(count,t.last());
            }
    }
}
