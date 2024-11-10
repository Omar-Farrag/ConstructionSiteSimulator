import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

public class ControlNode extends SimulationObject {

    private HashMap<String, Node> connectedSlaveNodes;
    private Queue<DataPacket> receivedDataPackets;

    public ControlNode(String object_name, String outputFileName, int runTimeStep, uController localController){
        super(object_name, outputFileName, runTimeStep);
        localController.setParentNode(this);
        receivedDataPackets = new LinkedList<DataPacket>();
        connectedSlaveNodes = new HashMap<>();
    }

    public void subscribeTo(Node... nodesToConnect){
        for(Node node : nodesToConnect){
            node.addSubscriber(this);
            connectedSlaveNodes.put(node.getObject_name(), node);
        }
    }
    
    public void update(Node sender, DataPacket receivedDataPacket){
        synchronized(receivedDataPacket){
            receivedDataPackets.add(receivedDataPacket);
            exportState(String.format("Received new packet from Node [%s]", sender.getObject_name()));
        }
    }

    public void update(ControlNode sender, DataPacket receivedDataPacket){
        synchronized(receivedDataPacket){
            receivedDataPackets.add(receivedDataPacket);
            exportState(String.format("Received new packet from Control Node [%s]", sender.getObject_name()));
        }
    }

    public ExecutionResult getFieldFrom(String targetNodeName, String targetObjectName, String field){
        ExecutionResult result;
         
        Node targetNode = connectedSlaveNodes.get(targetNodeName);
        if(targetNode!= null) {
            result = targetNode.getField(this, targetObjectName, field);
            String successStatus = result.isSuccess() ? "SUCCESS" : "FAILURE";
            exportState(String.format("[%s] Requested field [%s] from object [%s] in node [%s]. Value [%s]",successStatus, field, targetObjectName, targetNodeName, result.isSuccess()? result.getReturnedPacket().getValue():"Null"));
            return result;
        }
        else{
            exportState(String.format("[FAILURE] Requested field [%s] from object [%s] in node [%s]. Value [Null]",field, targetObjectName, targetNodeName));
            return new ExecutionResult(false, null);
        }
    }

    public ExecutionResult setFieldIn(String targetNodeName, String targetObjectName, String field, String value){
        Node targetNode = connectedSlaveNodes.get(targetNodeName);
        if(targetNode != null){
            ExecutionResult result = targetNode.setField(this, targetObjectName, field, value);
            String successStatus = result.isSuccess() ? "SUCCESS" : "FAILURE";
            exportState(String.format("[%s] Set field [%s] in object [%s] in node [%s]", successStatus, field, targetObjectName, targetNodeName));
            return result;
        }       
        else{
            exportState(String.format("[FAILURE] Set field [%s] in object [%s] in node [%s]",field, targetObjectName, targetNodeName));
            return new ExecutionResult(false, null);
        }
        
    }

    @Override
    protected void runTimeFunction() {
        // Do Nothing
    }
}
