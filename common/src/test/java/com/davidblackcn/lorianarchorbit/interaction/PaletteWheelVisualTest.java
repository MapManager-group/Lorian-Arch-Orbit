package com.davidblackcn.lorianarchorbit.interaction;

import com.davidblackcn.lorianarchorbit.config.PaletteArrowStyle;
import com.davidblackcn.lorianarchorbit.config.PaletteTargetPosition;
import org.junit.jupiter.api.Test;
import java.util.HashSet;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class PaletteWheelVisualTest {
    @Test void spritesShareMapGreensAndKeepEveryPixelInsideTheTargetTip() {
        for (var style : PaletteArrowStyle.values()) {
            var sprite = PaletteArrowSprite.of(style);
            if (style == PaletteArrowStyle.NONE) {
                assertTrue(sprite.rows().isEmpty());
                continue;
            }
            var colors = new HashSet<Integer>();
            assertEquals(0xFF000000, sprite.color(sprite.tipX(), 0));
            for (var position : PaletteTargetPosition.values()) {
                var indicator = PaletteTargetIndicator.at(100,100,80,position,0).orElseThrow();
                for (int y=0;y<sprite.rows().size();y++) for(int x=0;x<sprite.rows().get(y).length();x++) {
                    int color = sprite.color(x,y);
                    if (color==0) continue;
                    colors.add(color);
                    var pixel = indicator.pixel(sprite.tipX()-x,-y);
                    assertTrue((pixel.x()-indicator.tipX())*indicator.forwardX()
                            +(pixel.y()-indicator.tipY())*indicator.forwardY()<=0);
                }
            }
            assertEquals(Set.of(0xFF000000,0xFF00BC38,0xFF00E043,0xFF00FF4C),colors);
        }
        var pointer = PaletteArrowSprite.of(PaletteArrowStyle.POINTER);
        assertEquals(1,pointer.rows().getFirst().chars().filter(c->c!='.').count());
    }

    @Test void visibleSpritesKeepComparableBoundsInEveryDirection() {
        for (var style : PaletteArrowStyle.values()) {
            if (style == PaletteArrowStyle.NONE) continue;
            var sprite = PaletteArrowSprite.of(style);
            int width = style == PaletteArrowStyle.VANILLA ? 10 : 11;
            assertEquals(14, sprite.rows().size());
            assertTrue(sprite.rows().stream().allMatch(row -> row.length() == width));
            for (var position : PaletteTargetPosition.values()) {
                var indicator = PaletteTargetIndicator.at(100,100,80,position,0).orElseThrow();
                var pixels = new HashSet<HudPoint>();
                int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
                int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
                for (int y = 0; y < 14; y++) for (int x = 0; x < width; x++) {
                    if (sprite.color(x,y) == 0) continue;
                    var pixel = indicator.pixel(sprite.tipX()-x,-y);
                    assertTrue(pixels.add(pixel), "Sprite cells must stay separate: " + style);
                    minX = Math.min(minX,pixel.x()); maxX = Math.max(maxX,pixel.x());
                    minY = Math.min(minY,pixel.y()); maxY = Math.max(maxY,pixel.y());
                }
                assertEquals(indicator.forwardX() == 0 ? width : 14, maxX-minX+1);
                assertEquals(indicator.forwardX() == 0 ? 14 : width, maxY-minY+1);
            }
        }
    }

    @Test void emphasisFollowsPositionsContinuouslyAcrossSelectionAndWraparound() {
        for(int count:new int[]{2,8,34,100}) for(int selected=0;selected<count;selected++) {
            assertEquals(1.1F,PaletteRadialLayout.selectionScale(selected,selected,count,0));
            assertEquals(1,PaletteRadialLayout.selectionScale((selected+1)%count,selected,count,0),1e-6);
            for(int step:new int[]{-1,1,3}) {
                int next = Math.floorMod(selected+step,count);
                double offset = 2*Math.PI*step/count;
                for(int slot=0;slot<count;slot++) assertEquals(
                        PaletteRadialLayout.selectionScale(slot,selected,count,0),
                        PaletteRadialLayout.selectionScale(slot,next,count,offset),1e-6);
            }
            double halfStep = Math.PI/count;
            assertEquals(1.05F,PaletteRadialLayout.selectionScale(selected,selected,count,halfStep),1e-6);
        }
        assertEquals(1,PaletteRadialLayout.selectionScale(0,0,0,0));
        assertTrue(16*1.2*1.1/2<=PaletteRadialLayout.ITEM_HALF_SIZE);
        assertTrue(16*1.2*(1.1+1)/2<PaletteRadialLayout.SLOT_SPACING);
    }
}
