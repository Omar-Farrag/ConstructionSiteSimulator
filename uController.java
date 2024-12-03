import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;

public class uController extends SimulationObject{

    private SlaveNode parentSlaveNode;
    private MasterNode parentControlNode;
    private ProcessingAlgorithm setup;
    private ProcessingAlgorithm loop;
    private ArrayList<Device> devices;
    private ArrayList<String> globalOfferedFields;
    private ArrayList<String> localOfferedFields;
    
    private HashSet<String> permittedIDs;
    private boolean hasSetUp;

    private Gateway gateway;

    public uController(String name, int runTimeStep, ProcessingAlgorithm loop) {
        super(name,runTimeStep);
        this.loop = loop;
        this.devices = new ArrayList<>();
        globalOfferedFields = new ArrayList<>();
        localOfferedFields = new ArrayList<>();
        permittedIDs = new HashSet<>();
        hasSetUp = true;
    }

    public uController(String name, int runTimeStep, ProcessingAlgorithm loop, ProcessingAlgorithm setup) {
        super(name,runTimeStep);
        this.loop = loop;
        this.setup = setup;
        this.devices = new ArrayList<>();
        globalOfferedFields = new ArrayList<>();
        localOfferedFields = new ArrayList<>();
        permittedIDs = new HashSet<>();
        hasSetUp = false;
    }

    public void initFields(){
        for (Device dev : devices){
            dev.initFields();
            for(String field : dev.getFieldNames()){
                globalOfferedFields.add(dev.getObject_name()+"_"+field);
                localOfferedFields.add(field);
            }
        }         
    }

    public void setParentNode(SlaveNode parentNode) {
        this.parentSlaveNode = parentNode;
        this.parentControlNode = null;

    }
    
    public void setParentNode(MasterNode parentNode) {
        this.parentControlNode = parentNode;
        this.parentSlaveNode = null;
    }

    public void setGateway(Gateway gateway) {
        this.gateway = gateway;
    }

    public void connectTo(LowPowerDevice... devicesToConnect){
        synchronized(devices){
            for(LowPowerDevice device : devicesToConnect) {
                devices.add(device);
            }
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
    
    public void publishPacket(DataPacket... packets){
        exportState(String.format("Sent [%d] packets to parent node to publish them",packets.length));
        parentSlaveNode.publishPacket(packets);
    }

    public void receiveDataPacket(Gateway source, Gateway previous, BulkDataPacket packet){
        exportState(String.format("[SUCCESS] Received packet from Gateway [%s]. Last Forwarded By Gateway [%s]", source.getObject_name(), previous.getObject_name()));
        parentControlNode.receiveForwardedPacket(source.getParentNode(), packet);
    }
    
    public boolean forwardToZones(BulkDataPacket packet, String... peripheralZoneRoute){
        
        for (int i = 0; i<peripheralZoneRoute.length; i++) peripheralZoneRoute[i] += "_MasterNode_gateway";

        return forward(packet, String.join(",", peripheralZoneRoute));
    }

    public boolean forward(BulkDataPacket packet, String route){
        
        route = this.gateway.getObject_name()+","+route;
        String[] hops = route.split(",");
        
        
        String nextHop, destination;
        if(hops.length == 1) nextHop = destination = this.gateway.getObject_name();
        else{
            nextHop = hops[1];
            destination = hops[hops.length-1];
        }

        this.parentControlNode.exportState(String.format("[STARTED] Sent Packet Through Connected Gateway. Next Hop [%s]. Target Destination [%s]", nextHop,destination));
        exportState(String.format("[STARTED] Sent Packet Through Connected Gateway. Next Hop [%s]. Target Destination [%s]", nextHop,destination));
        boolean sent =  gateway.forward(gateway,gateway, packet, route, 0);
        exportState(String.format("[%s] Sent Packet Through Connected Gateway. Next Hop [%s]. Target Destination [%s]", sent ? "SUCCESS" : "FAILURE", nextHop, destination));
        this.parentControlNode.exportState(String.format("[%s] Sent Packet Through Connected Gateway. Next Hop [%s]. Target Destination [%s]", sent ? "SUCCESS" : "FAILURE", nextHop, destination));
        return sent;
    }

    public void addPermittedId(String id){
        permittedIDs.add(id);
    }

    public void removePermittedId(String id){
        permittedIDs.remove(id);
    }

    public boolean isPermittedToEnter(String id){
        boolean permitted = permittedIDs.contains(id);
        exportState(String.format("Parent Node [%s] asked if ID [%s] is permitted to enter zone. Permission [%s]", parentControlNode.getObject_name(),id, permitted ? "ALLOWED" : "DENIED"));
        return permitted;
    }
    
    public Queue<DataPacket> getBufferedDataPackets() {
        if(parentControlNode != null) return parentControlNode.getBufferedDataPackets();
        else return new LinkedList<>();
    }
    
    public void clearBufferedDataPackets(){
        if(parentControlNode != null) parentControlNode.clearBufferedDataPackets();

    }

    

    public Queue<BulkDataPacket> getReceivedBulkDataPackets(boolean consume) {
        return parentControlNode.getReceivedBulkDataPackets(consume);
    }

    public ExecutionResult setFieldIn(String targetNodeName, String targetObjectName, String field, String value,
            int size) {
        exportState(String.format("Asked parent node to set field [%s] in object [%s] in slave node [%s]", field, targetObjectName, targetNodeName));
        ExecutionResult result =  parentControlNode.setFieldIn(targetNodeName, targetObjectName, field, value, size);
        String successStatus = result.isSuccess() ? "SUCCESS" : "FAILURE";
        exportState(String.format("[%s] Parent set field [%s] in object [%s] in slave node [%s]", successStatus, field, targetObjectName, targetNodeName));
        return result;                
    }

    public ExecutionResult updateSwitchIn(String targetNodeName, String targetObjectName, String position,
            String switchStatus) {
        exportState(String.format("Asked parent node to set switch position [%s] in object [%s] in slave node [%s]", position, targetObjectName, targetNodeName));
        ExecutionResult result = parentControlNode.updateSwitchIn(targetNodeName, targetObjectName, position, switchStatus);
        String successStatus = result.isSuccess() ? "SUCCESS" : "FAILURE";
        exportState(String.format("[%s] Parent node set switch position [%s] in object [%s] in slave node [%s]", successStatus, position, targetObjectName, targetNodeName));
        return result;  
    }

    @Override
    protected void runTimeFunction() {
        if(!hasSetUp) {
            setup.process(this);
            hasSetUp = true;
        };
        loop.process(this);
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
    
    public SlaveNode getParentSlaveNode() {
        return parentSlaveNode;
    }
    
    public MasterNode getParentControlNode() {
        return parentControlNode;
    }

    public ArrayList<String> getGlobalOfferedFields() {
        return globalOfferedFields;
    }

    public ArrayList<String> getLocalOfferedFields() {
        return localOfferedFields;
    }

    public String getCurrentValue(String deviceName, String fieldName){
        if(parentControlNode != null){
            return parentControlNode.getCurrentValue(deviceName, fieldName);
        }
        else return null;
    }

    @Override
    public void terminate() {
        for(Device dev : devices) dev.terminate();
        if(gateway != null) gateway.terminate();
        super.terminate();
    }

    @Override
    public void start() {
        for(Device dev : devices) dev.start();
        if(gateway != null) gateway.start();
        super.start();
    }
}
