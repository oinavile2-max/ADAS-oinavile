package br.com.dashcamadas

import org.junit.Assert.*
import org.junit.Test

class CameraTransformTest {
    @Test fun centerAlwaysMapsToCenter() {
        for(rotation in listOf(0,90,180,270)) for(mirror in listOf(false,true)) for(fill in listOf(false,true)) {
            val p=CameraTransform(1280,720,1024,600,rotation,mirror,fill).project(640f,360f)
            assertEquals(512f,p.first,.001f); assertEquals(300f,p.second,.001f)
        }
    }
    @Test fun quarterTurnMapsCorners() {
        val t=CameraTransform(640,480,480,640,90,false,false)
        assertEquals(480f,t.project(0f,0f).first,.001f)
        assertEquals(0f,t.project(0f,0f).second,.001f)
        assertEquals(0f,t.project(640f,480f).first,.001f)
        assertEquals(640f,t.project(640f,480f).second,.001f)
    }
    @Test fun fitLetterboxesWithoutDistortion() {
        val t=CameraTransform(1280,720,1024,600,0,false,false)
        assertEquals(12f,t.project(0f,0f).second,.001f)
        assertEquals(1024f,t.project(1280f,720f).first,.001f)
        assertEquals(588f,t.project(1280f,720f).second,.001f)
    }
    @Test fun fillCropsWithoutDistortion() {
        val t=CameraTransform(1280,720,1024,600,0,false,true)
        assertTrue(t.project(0f,0f).first<0f)
        assertEquals(0f,t.project(0f,0f).second,.001f)
    }
    @Test fun mirrorIsInDisplayCoordinates() {
        val a=CameraTransform(640,480,480,640,90,false,false).project(110f,75f)
        val b=CameraTransform(640,480,480,640,90,true,false).project(110f,75f)
        assertEquals(480f-a.first,b.first,.001f);assertEquals(a.second,b.second,.001f)
    }
    @Test fun mappingRoundTripsForAllModes() {
        for(rotation in listOf(0,90,180,270)) for(mirror in listOf(false,true)) for(fill in listOf(false,true)) {
            val t=CameraTransform(1280,720,1024,600,rotation,mirror,fill)
            for(p in listOf(0f to 0f, 120f to 430f,1280f to 720f)) {
                val v=t.project(p.first,p.second);val q=t.unproject(v.first,v.second)
                assertEquals(p.first,q.first,.001f);assertEquals(p.second,q.second,.001f)
            }
        }
    }
}
