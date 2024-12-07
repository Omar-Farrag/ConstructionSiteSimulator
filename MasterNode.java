import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Queue;

/**
 * This class represents a master node that controls all other slave nodes within a zone 
 */
public class MasterNode extends SimulationObject {

    // BLE transmission rate in kbps of data sent between the master and slave node
    private int BLE_Transmission_Rate;

    // Map containing an entry for every slave node in the zone. The key of each entry is the 
    // slave node's name while the value is the SlaveNode object itself
    private HashMap<String, SlaveNode> connectedSlaveNodes;

    // This is a FIFO queue that stores packets automatically sent by slave nodes.
    // A master node typically subscribes to slave nodes such that the slave nodes
    // automatically send their newly created data packets to the master node.
    // These packets are buffered until the uController aggregates them into a bulk data packet
    // to send to the master zone.
    private Queue<DataPacket> bufferedDataPackets;

    // This is a FIFO queue that stores bulk data packets received by this node.
    // Most likely, the master node of a Master zone is the entity that receives 
    // bulk data packets from slave zones. These bulk data packets can then be 
    // unpacked to read their constituent data packets
    private Queue<BulkDataPacket> receivedBulkDataPackets;
    
    // Map storing entries for all field names and their current values. The fields represent all
    // data types (temp, distance, etc. ) generated within a zone by its devices. Whenever these devices 
    // create new data packets and share them from their respective slave nodes to this master node,
    // the value in the map is updated.
    // To distinguish between identical field names in different devices of the same type, the field name keys
    // in the map are stored as [object name]_[field name]
    private HashMap<String, String> fieldValues;

    // uController for the node
    protected uController localController;

    /**
     * Constructor
     * @param object_name Name of the object
     * @param BLE_Transmission_Rate BLE transmission rate in kbps for data sent from master node to slave nodes
     * @param localController Local uController for the node
     */
    public MasterNode(String object_name,int BLE_Transmission_Rate, uController localController){
        super(object_name);
        this.localController = localController;
        this.BLE_Transmission_Rate = BLE_Transmission_Rate;
        
        localController.setParentNode(this);
        connectedSlaveNodes = new HashMap<>();
        fieldValues = new HashMap<>();
        receivedBulkDataPackets = new LinkedList<>();
        bufferedDataPackets = new LinkedList<>();
    }

    /**
     * This function subscribes this master node to a set of slave nodes. This means that any new data packets generated
     * by those slave nodes will automatically be sent to this master node once they are created
     * @param nodesToConnect Slave nodes to subscribe to
     */
    public void subscribeTo(SlaveNode... nodesToConnect){
        for(SlaveNode node : nodesToConnect){
            node.setMasterNode(this);
            connectedSlaveNodes.put(node.getObject_name(), node);
            for(String field : node.getGlobalOfferedFields()) fieldValues.put(field, "Uninitialized");
        }
    }

    /**
     * Function to set initial values for all fields in devices connected to the master nodes uController.
     * Also creates new entries in the master node for all fields received from the connected slave nodes. Initial value
     * value for these entries is "Unitialialized" until the slave nodes begin sending data packets 
     */
    public void initFields(){
        // Initialize the uController's fields
        localController.initFields();
    
        // For every connected slave node
        for(Entry<String, SlaveNode> entry : connectedSlaveNodes.entrySet()){
            // Get the node's field names in the form [object name]_[field name]
            ArrayList<String> fields = entry.getValue().getGlobalOfferedFields();

            // For every field in the slave node's fields
            for (String field : fields){
                // Create a new entry in the values map with the initial value set to "Unitialized"
                fieldValues.put(field,"Uninitialized");
            }
        }   
        
    }
    
    /**
     * Function to share a new data packet with the master node. This function is typically called by slave nodes
     * @param sender Slave node sending the new packets
     * @param receivedDataPackets New data packets shared by the slave node
     */
    public void update(SlaveNode sender, DataPacket... receivedDataPackets){
        
        int totalDataSize = 0;
        
        //Calculate total size of all the received packets' payloads
        for(DataPacket packet : receivedDataPackets) totalDataSize += packet.getSize();
        
        // Based on the total size of the payload being sent, calculate the propagation and transmission delay from the 
        // slave node to this master node: delay = RTT/2 + Size / Transmission Rate
        int time_delay = sender.getRTT_to_Master_Node()/2 + totalDataSize / BLE_Transmission_Rate;
        try {
            // Simulate the delay introduced by the transmission of the data by BLE
            Thread.sleep(time_delay);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Add log message indicating that the data packets have been received
        exportState(String.format("Received (%d) new packets from slave node [%s] created @ [%s]",receivedDataPackets.length, sender.getObject_name(),receivedDataPackets[0].getTime_of_creation())); 
        
        synchronized(bufferedDataPackets){
            // Add the data packets to thebuffer
            for(DataPacket packet : receivedDataPackets) bufferedDataPackets.add(packet);
        }

        synchronized(fieldValues){
            // For every packet in the received data packets
            for(DataPacket packet : receivedDataPackets)
            // Update the corresponding field entry in the values map
            fieldValues.put(
                //Keys of the map are the object name_field name
                packet.getSourceObjectName()+"_"+packet.getFieldName(),
                packet.getValue());
                
        }
        // Calculate the delay for the acknowledgement that has to travel back to the slave node that shared the packet
        // The delay is only RTT/2 assuming the acknowledgment size is negligible
        time_delay = sender.getRTT_to_Master_Node()/2;
        try {
            // Simulate the acknowledgement delay before returing back to the slave node that called this function
            Thread.sleep(time_delay);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Function to share a BulkDataPacket with this master node. The bulk data packet will have most likely originated from a 
     * master node in another zone
     * @param sender Master node that created and transmitted the bulk data packet
     * @param receivedBulkDataPacket The bulk data packet being shared
     */
    public void receiveForwardedPacket(MasterNode sender, BulkDataPacket receivedBulkDataPacket){
        synchronized(receivedBulkDataPackets){
            // Add the received bulk data packets to the buffer
            receivedBulkDataPackets.add(receivedBulkDataPacket);
            
            // Add a log message indicating the receival of the bulk data packet.
            exportState(String.format("Received new bulk packet from control node [%s]", sender.getObject_name()));
        }
    }

    /**
     * Function to retreive the value of a certain field from a certain device in a certain slave node in the same zone.
     * @param targetNodeName Name of the slave node containing the device that contains the field to be retrieved
     * @param targetObjectName Name of the device that contains the field to be retrieved
     * @param field Name of the field to be retrieved
     * @return Execution result encapsulating the success of the query and the returned data packet
     */
    public ExecutionResult getFieldFrom(String targetNodeName, String targetObjectName, String field){
        ExecutionResult result;
         
        // Find the slave node having the target node name in the list of connected slave nodes
        SlaveNode targetNode = connectedSlaveNodes.get(targetNodeName);

        // If target node exists
        if(targetNode!= null) {
            // Add a log message indicating that this master node is queryinig a field in another slave node
            exportState(String.format("Queried field [%s] from object [%s] in slave node [%s]", field, targetObjectName, targetNodeName));
            
            // Retrieve the field from the target node
            result = targetNode.getField(this, targetObjectName, field);
            
            // Check if the query was successful or not
            String successStatus = result.isSuccess() ? "SUCCESS" : "FAILURE";
            
            // Add a log message inidcating whether the query was successful or not
            exportState(String.format("[%s] Received field [%s] from object [%s] in slave node [%s]. Value [%s]",successStatus, field, targetObjectName, targetNodeName, result.isSuccess()? result.getReturnedPacket().getValue():"Null"));
            
            // Return the execution result
            return result;
        }
        // If the target node does not exist
        else{
            // Add an error log message
            exportState(String.format("[FAILURE] Received field [%s] from object [%s] in slave node [%s]. Value [Null]",field, targetObjectName, targetNodeName));
            
            // Return a blank execution result
            return new ExecutionResult(false, null);
        }
    }

    /**
     * Function to update the value of a certain field in a certain device in a certain slave node.
     * @param targetNodeName Name of the slave node containing the device containing the field whose value is to be updated
     * @param targetObjectName Name of the device containing the field to be updated
     * @param field Name of the field to be updated
     * @param value New value for the field
     * @param size Size of the new value in bytes
     * @return Execution result encapsulating the success of the query and the returned data packet containing the user provided parameters
     */
    public ExecutionResult setFieldIn(String targetNodeName, String targetObjectName, String field, String value, int size){
        
        // Find the slave node in the list of connected slave nodes having the target node name
        SlaveNode targetNode = connectedSlaveNodes.get(targetNodeName);
        
        // If the target node exists
        if(targetNode != null){

            // Add a log message indicating the attempt to update the field value
            exportState(String.format("Attempted setting field [%s] in object [%s] in slave node [%s]", field, targetObjectName, targetNodeName));
            
            // Attempt updating the field value
            ExecutionResult result = targetNode.setField(this, targetObjectName, field, value, size);
            
            // Check whether the new value was successfully set or not
            String successStatus = result.isSuccess() ? "SUCCESS" : "FAILURE";
            
            // Add a log message indicating the success of setting the new value
            exportState(String.format("[%s] Set field [%s] in object [%s] in slave node [%s]. New Value [%s]", successStatus, field, targetObjectName, targetNodeName, result.isSuccess() ? result.getReturnedPacket().getValue() : "old value"));
            return result;
        }       
        // If the target node does not exist
        else{
            // Add an error log message
            exportState(String.format("[FAILURE] Set field [%s] in object [%s] in slave node [%s]. New Value [old value]",field, targetObjectName, targetNodeName));
            
            // Return a blank execution result
            return new ExecutionResult(false, null);
        }
        
    }

    /**
     * Function to update the state of a certain switch in a certain relay in a certain slave node
     * @param targetNodeName Name of the slave node containing relay device whose switch state is to be updated
     * @param targetObjectName Name of the relay device whose switch state is to be update
     * @param position The switch number with the relay
     * @param switchStatus The new status of the switch. "true" or "false"
     * @return Execution result encapsulating the success of the query and the returned data packet containg the user provided parameters
     */
    public ExecutionResult updateSwitchIn(String targetNodeName, String targetObjectName, String position, String switchStatus){
        
        // Find the slave node in the list of connected slave nodes having the target node name
        SlaveNode targetNode = connectedSlaveNodes.get(targetNodeName);
        
        // If the target node exists
        if(targetNode != null){
            // Add a log message indicating the attempt to update the switch state
            exportState(String.format("Attempted setting switch position [%s] in object [%s] in slave node [%s]", position, targetObjectName, targetNodeName));
            
            // Attempt updating the switch state
            ExecutionResult result = targetNode.updateSwitch(this, targetObjectName, position,switchStatus);
            
            // Check whether the update attempt was successful or not
            String successStatus = result.isSuccess() ? "SUCCESS" : "FAILURE";
            
            // Add a log message indicating the success of the update attempt
            exportState(String.format("[%s] Set switch position [%s] in object [%s] in slave node [%s]. New State [%s]", successStatus, position, targetObjectName, targetNodeName, result.isSuccess() ? result.getReturnedPacket().getValue() : "old state"));
            
            // Return the execution result
            return result;
        }       
        // If the target node does not exist
        else{
            // Add an error log message
            exportState(String.format("[FAILURE] Set switch position [%s] in object [%s] in slave node [%s]. New State [old state]",position, targetObjectName, targetNodeName));
            
            // Return blank execution result
            return new ExecutionResult(false, null);
        }
    }

    /**
     * Function to add a the ID of a worker permitted to enter the zone where the master node is located
     * @param id ID of the worker
     */
    public void addPermittedId(String id){
        // Delegate call to the local controller
        localController.addPermittedId(id);
    }

    /**
     * Function to return a copy of all the buffered data packets received from slave nodes
     * @return Queue containing a copy of all packets buffered in the master node
     */
    public Queue<DataPacket> getBufferedDataPackets() {
        synchronized(bufferedDataPackets){
            // Copy all the packets to a new queue
            Queue<DataPacket> copy = new LinkedList<>(bufferedDataPackets);
            
            // Return the copy
            return copy;

        }
    }
    
    /**
     * Function to clear all the buffered data packets received from slave nodes
     */
    public void clearBufferedDataPackets(){
        synchronized(bufferedDataPackets){
            // Clear the buffered data packets queue
            bufferedDataPackets.clear();
        }
    }

    /**
     * Function to return a copy of all the buffered bulk data packets received from slave zones
     * @param consume if true, the stored bulk packets in this master node will be cleared after returning their copy. 
     * If false, they will not be clearedd 
     * @return Queue containing a copy of the buffered bulk data packets
     */
    public Queue<BulkDataPacket> getReceivedBulkDataPackets(boolean consume) {
        synchronized(receivedBulkDataPackets){
            // Copy the bulk data packets into a new containter
            Queue<BulkDataPacket> copy = new LinkedList<>(receivedBulkDataPackets);
            
            // Clear the bulk data packets buffer if the caller wants to consume the buffer
            if(consume) receivedBulkDataPackets.clear();

            // Return the copy of the bulk data packets buffer
            return copy;
        }
    }
    
    /**
     * Run time function called continuously by the object's runtime thread assuming a thread 
     * was started for this node.
     */
    @Override
    protected void runTimeFunction() {
        //Do Nothing. Controller does all the functionality
    }

    /**
     * Function to check whether or not a certain worker is permitted to enter the zone in which the master node is located
     * @param gate The gate slave node object inquiring about permission of a certain worker
     * @param packet DataPacket encapsulating the ID of worker whose entry permission is being queried
     * @return true if the worker is permitted to enter, false otherwise
     */
    public boolean isPermittedToEnter(SlaveNode gate, DataPacket packet){
        String id = packet.getValue();
        try {
            // Simulate the delay of RTT/2 + size/Transmission rate for the request to arrive from gate slave node to this master node
            Thread.sleep(gate.getRTT_to_Master_Node()/2 + packet.getSize() / BLE_Transmission_Rate);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        synchronized(bufferedDataPackets){
            // Add received packet to list of buffered packets
            bufferedDataPackets.add(packet);
        }

        // Since the gate queried about the worker with the given ID, this means 
        // that gate detected the worker at the gate. Hence, the field "ID" in the gate 
        // must be updated
        synchronized(fieldValues){
            // getGlobalOfferedFields returns an array of fields in the gate with object name appended at the beginning
            // The arraylist contains only one field "ID"
            // Update that entry in the field values map
            fieldValues.put(gate.getGlobalOfferedFields().get(0),id);
        }

        // Add a log message indicating that gate asked about the permission of a certain worker
        exportState(String.format("Gate [%s] queried permission status for ID [%s]", gate.getObject_name(), id));
        
        // Check if the worker is permitted to enter or not
        boolean isPermitted =  localController.isPermittedToEnter(id);
        
        // Add a log message indicating whether the worker is allowed or not
        exportState(String.format("Gate [%s] queried permission status for ID [%s]. Permission [%s]", gate.getObject_name(), id, isPermitted ? "ALLOWED" : "DENIED"));
        
        try {
            // Simulate a delay of RTT/2, which is the time needed for the acknowledgement to get back to caller
            Thread.sleep(gate.getRTT_to_Master_Node()/2);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Return permission status
        return isPermitted;
    
    }

    /**
     * Function to retrieve the current value of a field in a certain device based on the latest data packet
     * received from the device for the field. Likely called by the master node's uController
     * @param deviceName Device whose field is to be queried
     * @param fieldName Name of the field in the device whose value is to be retrieved.
     * @return String value of the field
     */
    public String getCurrentValue(String deviceName, String fieldName){
        synchronized(fieldValues){
            return fieldValues.get(deviceName+"_"+ fieldName);
        }
    }
    
    /**
     * Function to add logs the output CSV file
     * @param event argument to specify an event message in the output log
     */
    @Override
    public void exportState(String... events) {
        // Add header for CSV file if not done already
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
        
        // Prepare values for first three columns
        values.add(getCurrentTimestamp());
        values.add(object_name);
        values.add(events[0]);

        // Add values for for the remaining columns using current values map
        synchronized(fieldValues){
            for(Entry<String, String> entry : fieldValues.entrySet()) values.add(entry.getValue());
        } 

        synchronized(writer){
            // Write to output csv file
            writer.println(String.join(",",values));  
        }

    }

    /**
     * Function to start the object without starting a new thread. The master node is responsible for
     * starting its local controller, which in turn starts all devices connected to uController
     */
    @Override
    public void start() {
        super.start(false,0);
        exportState("Started");
        localController.start();
    }
    /**
     * Function to terminate the object's run time thread. The master node is responsbile for
     * terminating its local controller, which in turn terminates all devices connected to uController
     */
    @Override
    public void terminate() {
        localController.terminate();
        super.terminate();
    }
}
