
public class Camera extends SimulationObject{

    
    public Camera(String name,String outputLogFileName, int runTimeStep){
        super(name, outputLogFileName,runTimeStep);
    }
    @Override
    public void exportState() {
        if(!hasAddedHeader){
                writer.println("Timestamp, Object Name, Event");
                hasAddedHeader = true;
            }
        writer.println(getCurrentTimestamp()+"," + object_name + ", Took a Video");
        
    }

    @Override
    public void runTimeFunction() {
        // Do Nothing. Camera is controlled by zone controller
    }

    public DataPacket getVideo(){

        DataPacket video = new DataPacket("Video", Float.parseFloat("-1"), 3500000, getCurrentTimestamp());
        
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        exportState();
        return video;

    }


}
