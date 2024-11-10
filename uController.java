import java.util.ArrayList;

public class uController extends SimulationObject{

    private Node parentSlaveNode;
    private ControlNode parentControlNode;
    private ProcessingAlgorithm algorithm;
    private ArrayList<Device> devices;
    private Gateway gateway;

    public uController(String name, String outputLogFileName, int runTimeStep, ProcessingAlgorithm algorithm) {
        super(name, outputLogFileName, runTimeStep);
        this.algorithm = algorithm;
        this.devices = new ArrayList<>();

    }

    public void setParentNode(Node parentNode) {
        this.parentSlaveNode = parentNode;
        this.parentControlNode = null;
    }
    
    public void setParentNode(ControlNode parentNode) {
        this.parentControlNode = parentNode;
        this.parentSlaveNode = null;
    }

    public void setGateway(Gateway gateway) {
        this.gateway = gateway;
    }

    public void connectTo(LowPowerDevice... devicesToConnect){
        synchronized(devices){
            for(Device device : devicesToConnect) devices.add(device);
        }
    }

    public void connectTo(Relay... devicesToConnect){
        synchronized(devices){
            for(Device device : devicesToConnect) devices.add(device);
        }
    }

    public void connectTo(Gateway gateway){
        this.gateway = gateway;
        gateway.setParentController(this);
    }
    
    public void publishPacket(DataPacket packet){
        parentSlaveNode.publishPacket(packet);
    }

    public void receiveDataPacket(Gateway source, DataPacket packet){
        parentControlNode.update(source.getParentNode(), packet);
    }
    
    public boolean forward(DataPacket packet, String route){
        route = this.gateway.getObject_name()+","+route;
        boolean sent =  gateway.forward(gateway,gateway, packet, route, 0);
        exportState(String.format("[%s] Forwarded Packet Through Connected Gateway", sent ? "SUCCESS" : "FAILURE"));
        this.parentControlNode.exportState(String.format("[%s] Forwarded Packet Through Connected Gateway", sent ? "SUCCESS" : "FAILURE"));
        return sent;
    }

    @Override
    protected void runTimeFunction() {
        algorithm.process(this);
    }

    public ExecutionResult getField(String objectName, String field){
        synchronized(devices){
            for(Device dev : devices){
                if(dev.getObject_name().equalsIgnoreCase(objectName)){
                    ExecutionResult result = dev.execute("GET", field);
                    String status = result.isSuccess() ? "SUCCESS" : "FAILURE";
                    DataPacket packet = result.getReturnedPacket();
                    exportState(String.format("[%s] Received field [%s] from object [%s]. Value [%s]",status, field, objectName, packet != null ? packet.getValue(): "Null"));
                    return result;
                }
            }
        }
        exportState(String.format("[FAILURE] Received field [%s] from object [%s]. Value [Null]", field, objectName));

        return new ExecutionResult(false, null);
    }
    
    public ExecutionResult setField(String objectName, String field, String value){       
        synchronized(devices){
            for(Device dev : devices){
                if(dev.getObject_name().equalsIgnoreCase(objectName)){
                    ExecutionResult result = dev.execute("SET", field, value);
                    String status = result.isSuccess() ? "SUCCESS" : "FAILURE";
                    exportState(String.format("[%s] Set field [%s] in object [%s]",status, field, objectName));
                    return result;
                }
            }
        }
        exportState(String.format("[FAILURE] Set field %s in object %s", field, objectName));
        return new ExecutionResult(false, null);

    }
    
    public ExecutionResult updateSwitch(String objectName, String position, String switchStatus){

        synchronized(devices){
            for(Device dev : devices){
                if(dev.getObject_name().equalsIgnoreCase(objectName)){
                    ExecutionResult result = dev.execute("Switch", position, switchStatus);
                    String successStatus = result.isSuccess() ? "SUCCESS" : "FAILURE";
                    exportState(String.format("[%s] Switched object [%s] position [%s] to [%s]",successStatus, objectName, position, switchStatus));
                    return result;
                }
            }
        }
        exportState(String.format("[FAILURE] Switched object [%s] position [%s] to [%s]", objectName, position, switchStatus));
        return new ExecutionResult(false, null);

    }
    
    public Node getParentSlaveNode() {
        return parentSlaveNode;
    }
    
    public ControlNode getParentControlNode() {
        return parentControlNode;
    }
}
