import br.com.dashcamadas.vision.*;
import br.com.dashcamadas.AdasEngine;
import java.io.*;
import java.util.*;

/** Same quadratic detector and risk engine as the APK; video has no measured GPS/calibration. */
public class ServiceVisionProbe {
    public static void main(String[] args) throws Exception {
        var input=new DataInputStream(new BufferedInputStream(System.in));
        var engine=new AdasEngine();
        var calibration=new Calibration(false,.5f,1.8f,3.5f);
        int frames=0,lanes=0,vehicleFrames=0;
        System.out.println("frame,time_ms,left_confidence,right_confidence,vehicles,ttc_estimate_s,left_phase,right_phase,fcw_phase,critical_risks");
        while(true) {
            int index;try{index=input.readInt();}catch(EOFException done){break;}
            long time=input.readLong();byte[] bytes=input.readNBytes(320*180);
            if(bytes.length!=320*180)throw new EOFException("Truncated real frame");
            int[] gray=new int[bytes.length];for(int i=0;i<gray.length;i++)gray[i]=bytes[i]&255;
            int count=input.readInt();var vehicles=new ArrayList<Vehicle>();
            for(int i=0;i<count;i++)vehicles.add(new Vehicle(new Box(input.readFloat(),input.readFloat(),input.readFloat(),input.readFloat()),input.readFloat(),input.readInt()));
            var outcome=engine.process(gray,320,180,vehicles,time,null,calibration);
            var observed=outcome.getLanes();var result=outcome.getRisk();
            if(observed.getLeft()!=null || observed.getRight()!=null)lanes++;
            if(!vehicles.isEmpty())vehicleFrames++;
            System.out.printf(Locale.US,"%d,%d,%s,%s,%d,%s,%s,%s,%s,%d%n",index,time,
                observed.getLeft()==null?"":Float.toString(observed.getLeft().getConfidence()),
                observed.getRight()==null?"":Float.toString(observed.getRight().getConfidence()),vehicles.size(),
                result.getTtcSeconds()==null?"":result.getTtcSeconds(),result.getPhases().get(Risk.LEFT_DEPARTURE),
                result.getPhases().get(Risk.RIGHT_DEPARTURE),result.getPhases().get(Risk.COLLISION),result.getRisks().size());
            if(!result.getRisks().isEmpty())throw new IllegalStateException("Critical risk without real GPS/calibration");
            frames++;
        }
        if(frames==0)throw new IllegalStateException("No frames");
        System.err.printf("Real frames=%d lane_candidate_frames=%d vehicle_candidate_frames=%d; GPS/calibration absent, critical risks unavailable%n",frames,lanes,vehicleFrames);
    }
}
