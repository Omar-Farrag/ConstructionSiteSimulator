import java.util.ArrayList;

public class uController extends SimulationObject{

    private ProcessingAlgorithm algorithm;
    private ArrayList<Device> devices;
    private String lastExecutedCommand;
    private ArrayList<ZoneController> subscribers;

    public uController(String name, String outputLogFileName, int runTimeStep, ProcessingAlgorithm algorithm) {
        super(name, outputLogFileName, runTimeStep);
        this.algorithm = algorithm;
        this.devices = new ArrayList<>();
        this.subscribers = new ArrayList<>();
        lastExecutedCommand = "";

    }

    public void connectTo(Device... devicesToConnect){
        synchronized(devices){
            for(Device device : devicesToConnect) devices.add(device);
        }
    }

    public void addSubscriber(ZoneController controller){
        subscribers.add(controller);
    }

    public void publishPacket(DataPacket packet){
        for(ZoneController subscriber : subscribers) subscriber.update(packet);
    }

    @Override
    public void exportState() {
        if(!hasAddedHeader){
            writer.println("Timestamp, Object Name, Event");
            hasAddedHeader = true;
        }
        synchronized(lastExecutedCommand){
            writer.println(getCurrentTimestamp()+"," + object_name + "," + lastExecutedCommand);
        }
    }

    @Override
    public void runTimeFunction() {
        algorithm.process(this);
    }


    public void setLastExecutedCommand(String lastExecutedCommand) {
        synchronized(lastExecutedCommand){
            this.lastExecutedCommand = lastExecutedCommand;
        }
    }

    public ArrayList<Device> getDevices() {
        return devices;
    }

    public DataPacket queryField(String objectName, String field){
        synchronized(devices){
            for(Device dev : devices){
                if(dev.getObject_name().equalsIgnoreCase(objectName)){
                    ExecutionResult result = dev.execute("GET", field);
                    return result.getReturnedPacket();
                }
            }
        }
        return null;
    }
    public Boolean setField(String objectName, String field, Float value){

        synchronized(devices){
            for(Device dev : devices){
                if(dev.getObject_name().equalsIgnoreCase(objectName)){
                    ExecutionResult result = dev.execute("SET", field, value.toString());
                    return result.isSuccess();
                }
            }
        }
        return false;

    }
    
}
