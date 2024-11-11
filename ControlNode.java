import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Queue;

public class ControlNode extends SimulationObject {

    private HashMap<String, SlaveNode> connectedSlaveNodes;
    private Queue<DataPacket> receivedDataPackets;
    private HashMap<String, String> fieldValues;

    public ControlNode(String object_name, String outputFileName, int runTimeStep, uController localController){
        super(object_name, outputFileName, runTimeStep);
        localController.setParentNode(this);
        receivedDataPackets = new LinkedList<DataPacket>();
        connectedSlaveNodes = new HashMap<>();
        fieldValues = new HashMap<>();
    }

    public void subscribeTo(SlaveNode... nodesToConnect){
        for(SlaveNode node : nodesToConnect){
            node.addSubscriber(this);
            connectedSlaveNodes.put(node.getObject_name(), node);
            for(String field : node.getOfferedFields()) fieldValues.put(field, "Uninitialized");
        }
    }
    
    public synchronized void update(SlaveNode sender, DataPacket receivedDataPacket){
        fieldValues.put(
            receivedDataPacket.getSourceObjectName()+"_"+receivedDataPacket.getFieldName(),
            receivedDataPacket.getValue());
        exportState(String.format("Received new packet from slave node [%s]", sender.getObject_name()));
    }

    public void receiveForwardedPacket(ControlNode sender, DataPacket receivedDataPacket){
        synchronized(receivedDataPacket){
            receivedDataPackets.add(receivedDataPacket);
            exportState(String.format("Received new packet from control node [%s]", sender.getObject_name()));
        }
    }

    public ExecutionResult getFieldFrom(String targetNodeName, String targetObjectName, String field){
        ExecutionResult result;
         
        SlaveNode targetNode = connectedSlaveNodes.get(targetNodeName);
        if(targetNode!= null) {
            result = targetNode.getField(this, targetObjectName, field);
            String successStatus = result.isSuccess() ? "SUCCESS" : "FAILURE";
            exportState(String.format("[%s] Requested field [%s] from object [%s] in slave node [%s]. Value [%s]",successStatus, field, targetObjectName, targetNodeName, result.isSuccess()? result.getReturnedPacket().getValue():"Null"));
            return result;
        }
        else{
            exportState(String.format("[FAILURE] Requested field [%s] from object [%s] in slave node [%s]. Value [Null]",field, targetObjectName, targetNodeName));
            return new ExecutionResult(false, null);
        }
    }

    public ExecutionResult setFieldIn(String targetNodeName, String targetObjectName, String field, String value){
        SlaveNode targetNode = connectedSlaveNodes.get(targetNodeName);
        if(targetNode != null){
            ExecutionResult result = targetNode.setField(this, targetObjectName, field, value);
            String successStatus = result.isSuccess() ? "SUCCESS" : "FAILURE";
            exportState(String.format("[%s] Set field [%s] in object [%s] in slave node [%s]", successStatus, field, targetObjectName, targetNodeName));
            return result;
        }       
        else{
            exportState(String.format("[FAILURE] Set field [%s] in object [%s] in slave node [%s]",field, targetObjectName, targetNodeName));
            return new ExecutionResult(false, null);
        }
        
    }

    @Override
    protected void runTimeFunction() {
        exportState("");
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

}
