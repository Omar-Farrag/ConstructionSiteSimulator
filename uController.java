import java.util.ArrayList;

public class uController extends SimulationObject{

    private Node parentNode;
    private ProcessingAlgorithm algorithm;
    private ArrayList<Device> devices;

    public uController(String name, String outputLogFileName, int runTimeStep, ProcessingAlgorithm algorithm) {
        super(name, outputLogFileName, runTimeStep);
        this.algorithm = algorithm;
        this.devices = new ArrayList<>();

    }

    public void setParentNode(Node parentNode) {
        this.parentNode = parentNode;
    }

    public void connectTo(Device... devicesToConnect){
        synchronized(devices){
            for(Device device : devicesToConnect) devices.add(device);
        }
    }

    public void publishPacket(DataPacket packet){
        parentNode.publishPacket(packet);
    }

    @Override
    public synchronized void exportState(String... event) {
        if(!hasAddedHeader){
            writer.println("Timestamp, Object Name, Event");
            hasAddedHeader = true;
        }
        writer.println(getCurrentTimestamp()+"," + object_name + "," + event[0]);
    }

    @Override
    public void runTimeFunction() {
        algorithm.process(this);
    }

    public DataPacket queryField(String objectName, String field){
        synchronized(devices){
            for(Device dev : devices){
                if(dev.getObject_name().equalsIgnoreCase(objectName)){
                    ExecutionResult result = dev.execute("GET", field);
                    String status = result.isSuccess() ? "Success" : "Fail";
                    exportState(String.format("%s querying object %s field %s",status, objectName, field));
                    return result.getReturnedPacket();
                }
            }
        }
        exportState(String.format("Failed querying object %s field %s", objectName, field));

        return null;
    }
    public Boolean setField(String objectName, String field, Float value){

        synchronized(devices){
            for(Device dev : devices){
                if(dev.getObject_name().equalsIgnoreCase(objectName)){
                    ExecutionResult result = dev.execute("SET", field, value.toString());
                    String status = result.isSuccess() ? "Success" : "Fail";
                    exportState(String.format("%s setting object %s field %s to %s",status, objectName, field, value.toString()));
                    return result.isSuccess();
                }
            }
        }
        exportState(String.format("Failed setting object %s field %s to %s", objectName, field, value.toString()));
        return false;

    }
    public boolean updateSwitch(String objectName, String position, String switchStatus){

        synchronized(devices){
            for(Device dev : devices){
                if(dev.getObject_name().equalsIgnoreCase(objectName)){
                    ExecutionResult result = dev.execute("Switch", position, switchStatus);
                    String successStatus = result.isSuccess() ? "Success" : "Fail";
                    exportState(String.format("%s switching object %s position %s to %s",successStatus, objectName, position, switchStatus));
                    return result.isSuccess();
                }
            }
        }
        exportState(String.format("Failed switching object %s position %f to %s", objectName, position, switchStatus));
        return false;

    }
    
}
