import java.util.ArrayList;

public class Node extends SimulationObject{

    private ArrayList<ZoneController> subscribers;
    private uController localController;
    private int timeToZoneController;

    public Node(String object_name, String outputFileName, int runTimeStep, int timeToZoneController, uController locaController){
        super(object_name, outputFileName, runTimeStep);
        this.timeToZoneController = timeToZoneController;
        this.localController = locaController;
        subscribers = new ArrayList<>();
        localController.setParentNode(this);
    }

    public void subscribe(ZoneController zoneController){
        subscribers.add(zoneController);
    }

    public void publishPacket(DataPacket packet){
        for (ZoneController sub : subscribers) {
            try {
                Thread.sleep(timeToZoneController);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            sub.update(packet);
        }
    }

    public DataPacket queryField(String objectName, String field){
        try {
            Thread.sleep(timeToZoneController);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        synchronized(localController){
            return localController.queryField(objectName, field);
        }
    }
    public Boolean setField(String objectName, String field, Float value){
        try {
            Thread.sleep(timeToZoneController);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        synchronized(localController){
            return localController.setField(objectName, field, value);
        }
    }

    @Override
    public synchronized void exportState(String... event) {
        synchronized(writer){

            if(!hasAddedHeader){
                writer.println("Timestamp, Object Name, Event");
                hasAddedHeader = true;
            }
            
            writer.println(getCurrentTimestamp()+"," + object_name + "," + event[0]);
        }
    }

    @Override
    public void runTimeFunction() {
        // Do Nothing
    }
    
}
