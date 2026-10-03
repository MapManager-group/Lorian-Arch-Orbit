package com.davidblackcn.lorianarchorbit.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HueBlockPickerLayoutTest {
    @Test void gridAndPreviewFitWithoutCoveringSearchOrFooter() {
        for (int w : new int[]{320,427,599,600,768,1024}) for (int h : new int[]{180,240,269,270,408,768}) {
            var layout = HueBlockPickerLayout.calculate(w,h);
            assertTrue(layout.gridBottom() <= layout.footerTop()-22);
            assertTrue(layout.gridWidth() >= 60);
            assertEquals(w>=600 && h>=270,layout.previewWidth()>0);
            if (layout.previewWidth()>0) {
                assertTrue(layout.previewLeft()>layout.left()+layout.gridWidth());
                assertTrue(layout.previewLeft()+layout.previewWidth()<=w-12);
                int size=Math.min(64,Math.max(32,(layout.gridBottom()-28)/4));
                int textureY=58+size+8+36;
                assertTrue(layout.gridBottom()-textureY-24>0);
            }
            assertTrue(layout.scrollbarLeft()+6<=layout.left()+layout.width());
            for(int slot=0;slot<layout.visibleSlots();slot++) {
                assertTrue(layout.cellX(slot)>=layout.left());
                assertTrue(layout.cellX(slot)+20<=layout.left()+layout.gridWidth());
                assertTrue(layout.cellY(slot)>=HueBlockPickerLayout.GRID_TOP);
                assertTrue(layout.cellY(slot)+20<=layout.gridBottom());
            }
        }
    }
    @Test void scrollingVisitsEveryCandidateAndTheThumbReachesBothEnds() {
        for(int count:new int[]{0,1,34,452,1906}) for(int[] size:new int[][]{{320,180},{768,408}}) {
            var layout=HueBlockPickerLayout.calculate(size[0],size[1]);
            var visited = new java.util.HashSet<Integer>();
            for(int row=0;row<=layout.maxScrollRow(count);row++)
                for(int slot=0;slot<layout.visibleSlots() && row*layout.columns()+slot<count;slot++)
                    visited.add(row*layout.columns()+slot);
            assertEquals(count,visited.size());
            int grab=layout.thumbHeight(count)/2;
            assertEquals(0,layout.rowAt(HueBlockPickerLayout.GRID_TOP+grab,grab,count));
            assertEquals(layout.maxScrollRow(count),layout.rowAt(layout.gridBottom()+100,grab,count));
            if(layout.maxScrollRow(count)>0) assertEquals(layout.gridBottom(),
                    layout.thumbTop(count,layout.maxScrollRow(count))+layout.thumbHeight(count));
        }
    }
    @Test void resizingKeepsTheOldFirstVisibleCandidateInTheFirstRow() {
        for(int[] oldSize:new int[][]{{320,180},{427,240},{768,408}})
            for(int[] newSize:new int[][]{{320,180},{427,240},{768,408}}) {
                var oldLayout=HueBlockPickerLayout.calculate(oldSize[0],oldSize[1]);
                var newLayout=HueBlockPickerLayout.calculate(newSize[0],newSize[1]);
                for(int row=0;row<10;row++) {
                    int anchor=row*oldLayout.columns(), next=newLayout.rowForAnchor(anchor);
                    assertTrue(next*newLayout.columns()<=anchor);
                    assertTrue((next+1)*newLayout.columns()>anchor);
                }
            }
    }
}
