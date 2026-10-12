package br.com.dashcamadas.vision

import java.nio.FloatBuffer
import org.junit.Assert.*
import org.junit.Test

class NeuralImageTest {
    @Test fun landscapeLetterboxPreservesAspectAndExcludesPadding(){
        val b=Letterbox(1280,720);assertEquals(320,b.resizedWidth);assertEquals(180,b.resizedHeight);assertEquals(70,b.top)
        assertEquals(70*320,b.modelIndex(0,0,320,180));assertEquals(249*320+319,b.modelIndex(319,179,320,180))
    }
    @Test fun portraitLetterboxUsesSidePadding(){val b=Letterbox(720,1280);assertEquals(70,b.left);assertEquals(0,b.top);assertEquals(70,b.modelIndex(0,0,180,320))}
    @Test fun rgbAndChannelOrderMatchOfficialImageNetNormalization(){
        val output=FloatBuffer.allocate(3*4*4);NeuralImage.encode(IntArray(16){0xffff0000.toInt()},Letterbox(4,4,4),output)
        assertEquals((1-.485f)/.229f,output.get(0),.00001f);assertEquals(-.456f/.224f,output.get(16),.00001f);assertEquals(-.406f/.225f,output.get(32),.00001f)
    }
    @Test fun paddingIsNormalizedRgb114(){
        val out=FloatBuffer.allocate(3*8*8);NeuralImage.encode(IntArray(8*4){0xff000000.toInt()},Letterbox(8,4,8),out)
        assertEquals((114/255f-.485f)/.229f,out.get(0),.00001f);assertEquals(-.485f/.229f,out.get(16),.00001f)
    }
    @Test fun masksMapSourcePixelsInsteadOfSquarePadding(){
        val b=Letterbox(320,180);val plane=320*320;val road=FloatArray(2*plane){.9f};val lane=FloatArray(2*plane){.1f}
        for(i in 0 until plane)road[plane+i]=.1f
        for(x in 0 until 320){lane[x]=.1f;lane[plane+x]=.95f}
        val selected=100*320+160;lane[selected]=.1f;lane[plane+selected]=.95f
        val result=NeuralMasks.decode(b,road,lane)
        assertEquals(1,result.lanePixels);assertTrue(result.lanes[30*320+160]>0);assertEquals(0,result.roadPixels)
    }
    @Test fun ambiguousClassScoresDoNotBecomeLanePixels(){
        val scores=FloatArray(2*320*320){.8f};val mask=NeuralMasks.decode(Letterbox(320,180),scores,scores);assertEquals(0,mask.lanePixels);assertEquals(0,mask.roadPixels)
    }
    @Test(expected=IllegalArgumentException::class) fun nonFiniteOutputIsRejected(){val p=FloatArray(2*320*320){.5f};p[0]=Float.NaN;NeuralMasks.decode(Letterbox(320,180),p,p)}
    @Test fun oneUlpSigmoidOvershootIsAcceptedAndClamped(){val p=FloatArray(2*320*320){.1f};p[320*320+100*320+160]=Float.fromBits(1f.toRawBits()+1);val m=NeuralMasks.decode(Letterbox(320,180),p,p);assertEquals(255,m.lanes[30*320+160])}
    @Test(expected=IllegalArgumentException::class) fun invalidProbabilityIsStillRejected(){val p=FloatArray(2*320*320){.5f};p[0]=1.01f;NeuralMasks.decode(Letterbox(320,180),p,p)}
    @Test fun blankFramesCannotSupplyLaneEvidence(){assertFalse(NeuralImage.usefulVariation(IntArray(1024){0xff000000.toInt()}));assertFalse(NeuralImage.usefulVariation(IntArray(1024){0xff808080.toInt()}));assertTrue(NeuralImage.usefulVariation(IntArray(1024){i->if(i%2==0)0xff000000.toInt() else 0xff808080.toInt()}))}
    @Test fun vehicleAssociationRequiresRoadAtFoot(){
        val mask=NeuralMasks(100,100,IntArray(10000),BooleanArray(10000){i->i/100>70},0,2900)
        assertFalse(mask.roadUnder(Box(.4f,.1f,.6f,.4f)));assertTrue(mask.roadUnder(Box(.4f,.4f,.6f,.8f)))
    }
    @Test fun laneOnNonRoadRegionCannotActivateLdw(){
        val pixels=IntArray(320*180)
        for(y in 96..175){val t=(y/180f-.5f)/.48f;for(x in listOf(((-.2f*t+.45f)*320).toInt(),((.2f*t+.55f)*320).toInt()))for(dx in -1..1)pixels[y*320+x+dx]=230}
        val e=br.com.dashcamadas.AdasEngine();val mask=NeuralMasks(320,180,pixels,BooleanArray(pixels.size),1000,0)
        repeat(8){val r=e.processNeural(mask,emptyList(),it*300L,15f,Calibration(true));assertNull(r.lanes.left);assertNull(r.lanes.right);assertTrue(r.risk.risks.isEmpty())}
    }
    @Test fun neuralObservedLanesRequireRepeatedFramesAndRoadSupport(){
        val pixels=IntArray(320*180)
        for(y in 96..175){val t=(y/180f-.5f)/.48f;for(x in listOf(((-.2f*t+.45f)*320).toInt(),((.2f*t+.55f)*320).toInt()))for(dx in -1..1)pixels[y*320+x+dx]=230}
        val e=br.com.dashcamadas.AdasEngine();val mask=NeuralMasks(320,180,pixels,BooleanArray(pixels.size){true},1000,pixels.size)
        val first=e.processNeural(mask,emptyList(),0,15f,Calibration(true));assertTrue(first.lanes.left!!.confidence<.65f)
        val next=e.processNeural(mask,emptyList(),300,15f,Calibration(true));assertTrue(next.lanes.left!!.confidence>=.65f);assertTrue(next.lanes.right!!.confidence>=.65f)
    }
    @Test fun missingBottomDashDoesNotRemoveVisibleLeftLane(){
        val pixels=IntArray(320*180)
        for(y in 96..175){val t=(y/180f-.5f)/.48f
            if(y<164){val x=((- .2f*t+.45f)*320).toInt();for(dx in -1..1)pixels[y*320+x+dx]=230}
            val x=((.2f*t+.55f)*320).toInt();for(dx in -1..1)pixels[y*320+x+dx]=230
        }
        val e=br.com.dashcamadas.AdasEngine();val mask=NeuralMasks(320,180,pixels,BooleanArray(pixels.size){true},1000,pixels.size)
        e.processNeural(mask,emptyList(),0,null,Calibration())
        val observed=e.processNeural(mask,emptyList(),300,null,Calibration())
        assertNotNull(observed.lanes.left);assertNotNull(observed.lanes.right)
        assertTrue(observed.lanes.left!!.yEnd<.93f);assertTrue(observed.risk.risks.isEmpty())
    }
}
