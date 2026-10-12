package br.com.dashcamadas

import org.junit.Assert.*
import org.junit.Test

class FocusedCameraTransformTest {
    @Test fun focusOffsetIsSharedByProjectionAndInverseEvenWithMirroring(){
        for(mirror in listOf(false,true)){
            val mapping=CameraTransform(1280,720,240,500,0,mirror,true,offsetX=92f)
            val projected=mapping.project(930f,412f);val recovered=mapping.unproject(projected.first,projected.second)
            assertEquals(930f,recovered.first,.01f);assertEquals(412f,recovered.second,.01f)
        }
    }
}
