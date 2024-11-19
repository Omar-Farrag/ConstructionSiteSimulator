import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Queue;

public class ControlNode extends SimulationObject {

    final int BLE_transmission_rate = 100; //kbps
    private HashMap<String, SlaveNode> connectedSlaveNodes;
    private Queue<DataPacket> bufferedDataPackets;
    private Queue<BulkDataPacket> receivedBulkDataPackets;
    private HashMap<String, String> fieldValues;
    protected uController localController;

    public ControlNode(String object_name, int runTimeStep, uController localController){
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
            node.setControlNode(this);
            connectedSlaveNodes.put(node.getObject_name(), node);
            for(String field : node.getOfferedFields()) fieldValues.put(field, "Uninitialized");
        }
    }

    public void initFields(){
        localController.initFields();
    }
    
    public synchronized void update(SlaveNode sender, DataPacket receivedDataPacket){
        int time_delay = sender.getRTT_to_Control_Node()/2 + receivedDataPacket.getSize() / BLE_transmission_rate;
        try {
            Thread.sleep(time_delay);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        exportState(String.format("Received new packet from slave node [%s]", sender.getObject_name()));      
        
        synchronized(bufferedDataPackets){
            bufferedDataPackets.add(receivedDataPacket);
        }
        
        fieldValues.put(
            receivedDataPacket.getSourceObjectName()+"_"+receivedDataPacket.getFieldName(),
            receivedDataPacket.getValue());

        time_delay = sender.getRTT_to_Control_Node()/2;
        try {
            Thread.sleep(time_delay);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void receiveForwardedPacket(ControlNode sender, BulkDataPacket receivedBulkDataPacket){
        synchronized(receivedBulkDataPackets){
            receivedBulkDataPackets.add(receivedBulkDataPacket);
            exportState(String.format("Received new packet from control node [%s]", sender.getObject_name()));
        }
    }

    public ExecutionResult getFieldFrom(String targetNodeName, String targetObjectName, String field){
        ExecutionResult result;
         
        SlaveNode targetNode = connectedSlaveNodes.get(targetNodeName);
        if(targetNode!= null) {
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
            ExecutionResult result = targetNode.setField(this, targetObjectName, field, value, size);
            String successStatus = result.isSuccess() ? "SUCCESS" : "FAILURE";
            exportState(String.format("[%s] Set field [%s] in object [%s] in slave node [%s]", successStatus, field, targetObjectName, targetNodeName));
            return result;
        }       
        else{
            exportState(String.format("[FAILURE] Set field [%s] in object [%s] in slave node [%s]",field, targetObjectName, targetNodeName));
            return new ExecutionResult(false, null);
        }
        
    }

    public Queue<BulkDataPacket> getReceivedBulkDataPackets() {
        return receivedBulkDataPackets;
    }

    public void addPermittedId(String id){
        localController.addPermittedId(id);
    }

    public Queue<DataPacket> getBufferedDataPackets() {
        return bufferedDataPackets;
    }
    
    @Override
    protected void runTimeFunction() {
        //Do Nothing. Controller does all the functionality
    }

    public boolean isPermittedToEnter(SlaveNode gate, String id){
        try {
            Thread.sleep(gate.getRTT_to_Control_Node()/2);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        boolean isPermitted =  localController.isPermittedToEnter(id);
        exportState(String.format("Gate [%s] queried permission status for ID [%s]. Permission [%s]", gate.getObject_name(), id, isPermitted ? "ALLOWED" : "DENIED"));
        
        try {
            Thread.sleep(gate.getRTT_to_Control_Node()/2);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return isPermitted;
    
    }

    @Override
    public synchronized void exportState(String... event) {
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
        for(Entry<String, String> entry : fieldValues.entrySet()) values.add(entry.getValue());
        writer.println(String.join(",",values));  

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
