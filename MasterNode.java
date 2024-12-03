import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Queue;

public class MasterNode extends SimulationObject {

    final int BLE_transmission_rate = 100; //kbps
    private HashMap<String, SlaveNode> connectedSlaveNodes;
    private Queue<DataPacket> bufferedDataPackets;
    private Queue<BulkDataPacket> receivedBulkDataPackets;
    private HashMap<String, String> fieldValues;
    protected uController localController;

    public MasterNode(String object_name, int runTimeStep, uController localController){
        super(object_name, runTimeStep);
        this.localController = localController;
        localController.setParentNode(this);
        connectedSlaveNodes = new HashMap<>();
        fieldValues = new HashMap<>();
        receivedBulkDataPackets = new LinkedList<>();
        bufferedDataPackets = new LinkedList<>();
    }

    public void subscribeTo(SlaveNode... nodesToConnect){
        for(SlaveNode node : nodesToConnect){
            node.setMasterNode(this);
            connectedSlaveNodes.put(node.getObject_name(), node);
            for(String field : node.getOfferedFields()) fieldValues.put(field, "Uninitialized");
        }
    }

    public void initFields(){
        localController.initFields();
        for(Entry<String, SlaveNode> entry : connectedSlaveNodes.entrySet()){
            ArrayList<String> fields = entry.getValue().getOfferedFields();
            for (String field : fields){
                fieldValues.put(field,"Uninitialized");
            }
        }   
        
    }
    
    public void update(SlaveNode sender, DataPacket... receivedDataPackets){
        int totalDataSize = 0;
        for(DataPacket packet : receivedDataPackets) totalDataSize += packet.getSize();
        int time_delay = sender.getRTT_to_Master_Node()/2 + totalDataSize / BLE_transmission_rate;
        try {
            Thread.sleep(time_delay);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        exportState(String.format("Received (%d) new packets from slave node [%s] created @ [%s]",receivedDataPackets.length, sender.getObject_name(),receivedDataPackets[0].getTime_of_creation())); 
        
        synchronized(bufferedDataPackets){
            for(DataPacket packet : receivedDataPackets) bufferedDataPackets.add(packet);
        }

        synchronized(fieldValues){
            for(DataPacket packet : receivedDataPackets)
            fieldValues.put(
                packet.getSourceObjectName()+"_"+packet.getFieldName(),
                packet.getValue());
                
        }
        time_delay = sender.getRTT_to_Master_Node()/2;
        try {
            Thread.sleep(time_delay);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void receiveForwardedPacket(MasterNode sender, BulkDataPacket receivedBulkDataPacket){
        synchronized(receivedBulkDataPackets){
            receivedBulkDataPackets.add(receivedBulkDataPacket);
            exportState(String.format("Received new bulk packet from control node [%s]", sender.getObject_name()));
        }
    }

    public ExecutionResult getFieldFrom(String targetNodeName, String targetObjectName, String field){
        ExecutionResult result;
         
        SlaveNode targetNode = connectedSlaveNodes.get(targetNodeName);
        if(targetNode!= null) {
            
            exportState(String.format("Queried field [%s] from object [%s] in slave node [%s]", field, targetObjectName, targetNodeName));
            result = targetNode.getField(this, targetObjectName, field);
            String successStatus = result.isSuccess() ? "SUCCESS" : "FAILURE";
            exportState(String.format("[%s] Received field [%s] from object [%s] in slave node [%s]. Value [%s]",successStatus, field, targetObjectName, targetNodeName, result.isSuccess()? result.getReturnedPacket().getValue():"Null"));
            return result;
        }
        else{
            exportState(String.format("[FAILURE] Received field [%s] from object [%s] in slave node [%s]. Value [Null]",field, targetObjectName, targetNodeName));
            return new ExecutionResult(false, null);
        }
    }

    public ExecutionResult setFieldIn(String targetNodeName, String targetObjectName, String field, String value, int size){
        
        SlaveNode targetNode = connectedSlaveNodes.get(targetNodeName);
        
        if(targetNode != null){
            exportState(String.format("Attempted setting field [%s] in object [%s] in slave node [%s]", field, targetObjectName, targetNodeName));
            ExecutionResult result = targetNode.setField(this, targetObjectName, field, value, size);
            String successStatus = result.isSuccess() ? "SUCCESS" : "FAILURE";
            exportState(String.format("[%s] Set field [%s] in object [%s] in slave node [%s]. New Value [%s]", successStatus, field, targetObjectName, targetNodeName, result.isSuccess() ? result.getReturnedPacket().getValue() : "old value"));
            return result;
        }       
        else{
            exportState(String.format("[FAILURE] Set field [%s] in object [%s] in slave node [%s]. New Value [old value]",field, targetObjectName, targetNodeName));
            return new ExecutionResult(false, null);
        }
        
    }

    public ExecutionResult updateSwitchIn(String targetNodeName, String targetObjectName, String position, String switchStatus){
        SlaveNode targetNode = connectedSlaveNodes.get(targetNodeName);
        
        if(targetNode != null){
            exportState(String.format("Attempted setting switch position [%s] in object [%s] in slave node [%s]", position, targetObjectName, targetNodeName));
            ExecutionResult result = targetNode.updateSwitch(this, targetObjectName, position,switchStatus);
            String successStatus = result.isSuccess() ? "SUCCESS" : "FAILURE";
            exportState(String.format("[%s] Set switch position [%s] in object [%s] in slave node [%s]. New State [%s]", successStatus, position, targetObjectName, targetNodeName, result.isSuccess() ? result.getReturnedPacket().getValue() : "old state"));
            return result;
        }       
        else{
            exportState(String.format("[FAILURE] Set switch position [%s] in object [%s] in slave node [%s]. New State [old state]",position, targetObjectName, targetNodeName));
            return new ExecutionResult(false, null);
        }
    }

    public void addPermittedId(String id){
        localController.addPermittedId(id);
    }

    public Queue<DataPacket> getBufferedDataPackets() {
        synchronized(bufferedDataPackets){
            Queue<DataPacket> copy = new LinkedList<>(bufferedDataPackets);
            return copy;

        }
    }
    
    public void clearBufferedDataPackets(){
        synchronized(bufferedDataPackets){
            bufferedDataPackets.clear();
        }
    }

    public Queue<BulkDataPacket> getReceivedBulkDataPackets(boolean consume) {
        synchronized(receivedBulkDataPackets){
            Queue<BulkDataPacket> copy = new LinkedList<>(receivedBulkDataPackets);
            if(consume) receivedBulkDataPackets.clear();
            return copy;

        }
    }
    
    @Override
    protected void runTimeFunction() {
        //Do Nothing. Controller does all the functionality
    }

    public boolean isPermittedToEnter(SlaveNode gate, String id){
        try {
            Thread.sleep(gate.getRTT_to_Master_Node()/2);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        synchronized(fieldValues){
            fieldValues.put(gate.getOfferedFields().get(0),id);
        }
        exportState(String.format("Gate [%s] queried permission status for ID [%s]", gate.getObject_name(), id));
        boolean isPermitted =  localController.isPermittedToEnter(id);
        exportState(String.format("Gate [%s] queried permission status for ID [%s]. Permission [%s]", gate.getObject_name(), id, isPermitted ? "ALLOWED" : "DENIED"));
        
        try {
            Thread.sleep(gate.getRTT_to_Master_Node()/2);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return isPermitted;
    
    }

    public String getCurrentValue(String deviceName, String fieldName){
        synchronized(fieldValues){
            return fieldValues.get(deviceName+"_"+ fieldName);
        }
    }
    
    @Override
    public void exportState(String... event) {
        if(!hasAddedHeader){
            ArrayList<String> columns = new ArrayList<>();
            columns.add("Timestamp");
            columns.add("Object Name");
            columns.add("Event");
            columns.addAll(fieldValues.keySet());
            writer.println(String.join(",",columns));
            hasAddedHeader = true;
        }
        
        ArrayList<String> values = new ArrayList<>();
        values.add(getCurrentTimestamp());
        values.add(object_name);
        values.add(event[0]);

        synchronized(fieldValues){
            for(Entry<String, String> entry : fieldValues.entrySet()) values.add(entry.getValue());
        } 

        synchronized(writer){
            writer.println(String.join(",",values));  
        }

    }

    @Override
    public void start() {
        localController.start();
        super.start();
    }

    @Override
    public void terminate() {
        localController.terminate();
        super.terminate();
    }
}
