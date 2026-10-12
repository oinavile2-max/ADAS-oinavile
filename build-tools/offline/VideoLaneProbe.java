import br.com.dashcamadas.LaneGeometry;
import java.io.*;
import java.util.Locale;

/** Feeds decoded REAL video luminance to the exact candidate detector used by Android. */
public class VideoLaneProbe {
    public static void main(String[] args) throws Exception {
        int width=Integer.parseInt(args[0]),height=Integer.parseInt(args[1]);
        byte[] bytes=new byte[width*height];int[] gray=new int[bytes.length];
        int index=0,candidates=0,pairs=0;
        var input=new BufferedInputStream(System.in);
        System.out.println("frame,timestamp_s,left_candidate,right_candidate,left_top_x,left_top_y,left_bottom_x,left_bottom_y,right_top_x,right_top_y,right_bottom_x,right_bottom_y");
        while(true) {
            int count=input.readNBytes(bytes,0,bytes.length);
            if(count==0) break;
            if(count!=bytes.length) throw new IOException("Incomplete frame: "+count);
            for(int i=0;i<bytes.length;i++) gray[i]=bytes[i]&255;
            var result=LaneGeometry.INSTANCE.detect(gray,width,height);
            System.out.printf(Locale.US,"%d,%.6f,%s,%s",index,index/3.0,result!=null && result.getLeft()!=null,result!=null && result.getRight()!=null);
            if(result!=null) {
                candidates++;
                if(result.getLeft()!=null && result.getRight()!=null) pairs++;
                for(float[] line:new float[][] {result.getLeft(),result.getRight()}) {
                    if(line==null) {System.out.print(",,,,");continue;}
                    for(float coordinate:line) {
                        if(!Float.isFinite(coordinate)||coordinate<0||coordinate>1) throw new IllegalStateException("Invalid geometry");
                        System.out.printf(Locale.US,",%.6f",coordinate);
                    }
                }
            } else System.out.print(",,,,,,,,");
            System.out.println();index++;
        }
        if(index==0) throw new IllegalStateException("No frames processed");
        System.err.println("Frames="+index+" candidate_frames="+candidates+" candidate_pairs="+pairs+"; no annotated accuracy or LDW validation");
    }
}
