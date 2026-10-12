import br.com.dashcamadas.vision.*;
import br.com.dashcamadas.AdasEngine;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Real NN execution and the same preprocessing, masks and risk core as the Android app. */
public class NeuralVisionProbe {
    public static void main(String[] args) throws Exception {
        var input=new DataInputStream(new BufferedInputStream(System.in));
        var engine=new AdasEngine();var calibration=new Calibration(false,.5f,1.8f,3.5f);
        int frames=0;
        System.out.println("frame,time_ms,lane_pixels,road_pixels,left_confidence,right_confidence,vehicles,nn_pipeline_ms,left_phase,right_phase,fcw_phase,critical_risks");
        try(var network=new RoadNeuralSession(Files.readAllBytes(Path.of(args[0])))) {
            while(true){
                int index;try{index=input.readInt();}catch(EOFException done){break;}
                long time=input.readLong();byte[] bytes=input.readNBytes(320*180*3);
                if(bytes.length!=320*180*3)throw new EOFException("Incomplete RGB frame");
                int[] pixels=new int[320*180];
                for(int i=0;i<pixels.length;i++)pixels[i]=0xff000000|(bytes[i*3]&255)<<16|(bytes[i*3+1]&255)<<8|(bytes[i*3+2]&255);
                int count=input.readInt();var vehicles=new ArrayList<Vehicle>();
                for(int i=0;i<count;i++)vehicles.add(new Vehicle(new Box(input.readFloat(),input.readFloat(),input.readFloat(),input.readFloat()),input.readFloat(),input.readInt()));
                long start=System.nanoTime();var mask=network.detect(pixels,320,180);
                var outcome=engine.processNeural(mask,vehicles,time,null,calibration);double ms=(System.nanoTime()-start)/1e6;
                var lanes=outcome.getLanes();var risk=outcome.getRisk();
                if(!risk.getRisks().isEmpty())throw new IllegalStateException("Critical risk without GPS/calibration");
                System.out.printf(Locale.US,"%d,%d,%d,%d,%s,%s,%d,%.3f,%s,%s,%s,%d%n",index,time,mask.getLanePixels(),mask.getRoadPixels(),
                    lanes.getLeft()==null?"":Float.toString(lanes.getLeft().getConfidence()),lanes.getRight()==null?"":Float.toString(lanes.getRight().getConfidence()),
                    vehicles.size(),ms,risk.getPhases().get(Risk.LEFT_DEPARTURE),risk.getPhases().get(Risk.RIGHT_DEPARTURE),risk.getPhases().get(Risk.COLLISION),risk.getRisks().size());
                frames++;
            }
        }
        if(frames==0)throw new IllegalStateException("No real frames processed");
        System.err.printf("Actual pretrained neural executions: %d; no GPS/calibration supplied.%n",frames);
    }
}
