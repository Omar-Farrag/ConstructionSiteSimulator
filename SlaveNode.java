import java.util.ArrayList;

/**
 * This class implements a SlaveNode, which encapsulates a uController and its connected devices
 */
public class SlaveNode extends SimulationObject{


    // A constant BLE transmission rate in Kbps between a SlaveNode and the MasterNode
    // This rate applies for all instances of a SlaveNode
    private int BLE_Transmission_Rate; //kbps
    
    // Round trip time between this slave node object and and the master node in the same zone
    protected int RTT_to_Master_Node; //ms

    // The uController of the node
    protected uController localController;

    // Reference to the masterNode of the zone
    private MasterNode masterNode;

    /**
     * Constructor
     * @param object_name Name of the slave node
     * @param RTT_to_Master_Node Round trip time between this node and the master node
     * @param BLE_Transmission_Rate transmission rate in Kbps for data sent by bluetooth from this node to master node
     * @param locaController uController of the node
     */
    public SlaveNode(String object_name, int RTT_to_Master_Node, int BLE_Transmission_Rate, uController localController){
        super(object_name);
        this.RTT_to_Master_Node = RTT_to_Master_Node;
        this.localController = localController;
        this.BLE_Transmission_Rate = BLE_Transmission_Rate;
        // Set this node as the parent or encapsulator of the local uController
        localController.setParentNode(this);
    }

    /**
     * Function to initialize fields of the slave node
     * Slave node does not have any fields of its own.
     * However, localController may be connected to devices that have fields
     */
    public void initFields(){

        // Ask local controller to initialize its fields
        localController.initFields();
    }
    
    /**
     * Function to set the master node of this slave node
     * All slave nodes in the same zone share the same master node
     * @param masterNode Reference to the zone's master node
     */
    public void setMasterNode(MasterNode masterNode){
        this.masterNode = masterNode;
    }

    /**
     * Function to send one or more packets from this slave node to the master node
     * @param packets packets to send to master node
     */
    public void publishPacket(DataPacket... packets){
        // Write a log message to output log file
        exportState(String.format("Started Publishing (%d) packets to Control Node [%s]",packets.length, masterNode.getObject_name()));
        
        // Send the packet to the master node
        masterNode.update(this,packets);

        // Write a log message to output log file after message was sent
        exportState(String.format("Done Publishing (%d) packets to Control Node [%s]",packets.length, masterNode.getObject_name()));
    }

    /**
     * Getter to retrieve a certain field from one of the slave node's devices.
     * Usually there is only one uController connected to one device.
     * @param requester MasterNode that requested the field. This node should be the same as the masterNode reference in the class.
     *        However, it is passed as an argument to make it easier to spot a mismatch between the requester and the local masterNode reference in the class.
     *        The mismatch would be visible on the output log files
     * @param deviceName Name of the device that contains the field to be retrieved
     * @param field Name of the field whose value is to be retrieved
     * @return Execution result containing a data packet that encapsulates the field value
     */
    public ExecutionResult getField(MasterNode requester, String deviceName, String field){

        // Delay to simulate the propagation delay between the master node and this slave node
        int time_delay= RTT_to_Master_Node /2;
        SimulationClock.getInstance().waitFor(time_delay);

        // Add a log message to the output log file indicating that the request arrived at the slave node
        exportState(String.format("Node [%s] requested field [%s] in object [%s]",
            requester.getObject_name(),
            field, 
            deviceName));


        // Add a log message to indicate that this slave node has asked its controller to retrieve the field value
        exportState(String.format("Attempted Retrieving field [%s] in object [%s] from local controller",
            field, 
            deviceName));

        ExecutionResult result;
        synchronized(localController){
            // Ask the local controller to retrieve the value
            result = localController.getField(deviceName, field);
        }

        //Add a log message to indicate the status of the query to the local controller
        exportState(String.format("[%s] Retrieved field [%s] in object [%s] from local controller. Returned Value [%s]",
        result.isSuccess()? "SUCCESS" : "FAILURE",
        field, 
        deviceName,
        result.isSuccess() ? result.getReturnedPacket().getValue() : "null"));

        // Add a log message to indicate the status of the query at the node level
        exportState(String.format("[%s] Node [%s] requested field [%s] in object [%s]. Returned Value [%s]",
        result.isSuccess() ? "SUCCESS" : "FAILURE",
        requester.getObject_name(),
        field, 
        deviceName,
        result.isSuccess() ? result.getReturnedPacket().getValue() : "null"
        ));

        // Delay simulating the propagation and transmission delays to transmit data packet from slave node to master node 
        time_delay= RTT_to_Master_Node/2 + (result.isSuccess() ? result.getReturnedPacket().getSize() * 8 / (BLE_Transmission_Rate) : 0);
        SimulationClock.getInstance().waitFor(time_delay);


        // return the execution result
        return result;

    }
    
    /**
     * Function to set a certain field in one of the slave node's objects. 
     * Usually there is only one uController connected to only one device
     * @param setter MasterNode that is attemoting to set the field. This node should be the same as the masterNode reference in the class.
     *        However, it is passed as an argument to make it easier to spot a mismatch between the requester and the local masterNode reference in the class.
     *        The mismatch would be visible on the output log files
     * @param deviceName Name of the device whose field is to be updated 
     * @param field Name of the field to be updated
     * @param value New value of the field
     * @param size Size in bytes of the new value
     * @return ExecutionResult encapsulating the success state of setting the field and a copy of the passed values as a DataPacket
     */
    public ExecutionResult setField(MasterNode setter, String deviceName, String field, String value, int size){

        // Delay simulating the propagation and tranmission delays of the new value from the master node to this slave node
        int time_delay= RTT_to_Master_Node /2 + size * 8 / BLE_Transmission_Rate;
        SimulationClock.getInstance().waitFor(time_delay);
    

        // Add a log message indicating that the request to set the field was received at the slave node
        exportState(String.format("Node [%s] attempted updating field [%s] in object [%s]",
            setter.getObject_name(),
            field, 
            deviceName));

        // Add a log message indicating that this slave node has asked its local uController to set the value of the field 
        exportState(String.format("Attempted setting field [%s] in object [%s] through local controller",
            field, 
            deviceName));

        ExecutionResult result;
        synchronized(localController){
            // Ask the localuController to set the field
            result = localController.setField(deviceName, field, value);
        }
        
        // Add a log message indicating the success of the setting attempt by the local uController
        exportState(String.format("[%s] Set field [%s] in object [%s] through local controller. New Value [%s]",
        result.isSuccess()? "SUCCESS" : "FAILURE",
        field, 
        deviceName,
        result.isSuccess() ? result.getReturnedPacket().getValue() : "old value"));

        // Add a log message indicating the success of the setting attempt at the node level
        exportState(String.format("[%s] Node [%s] attempted updating field [%s] in object [%s]. New Value [%s]",
            result.isSuccess() ? "SUCCESS" : "FAILURE",
            setter.getObject_name(),
            field, 
            deviceName,
            result.isSuccess() ? result.getReturnedPacket().getValue() : "old value"
            ));

        // Delay simulating the propagation delay of the acknowledgement back to the master node that attempted to set the field.
        // It is assumed that the transmission delay of that acknowledgement is negligible 
         time_delay= RTT_to_Master_Node/2;
         SimulationClock.getInstance().waitFor(time_delay);

        // Return result to the master node
        return result;
    }

    /**
     * Function similar to setField but written specifically for updating the switch status of a relay in the node
     * @param switcher MasterNode that is attempting to switch the relay at a certain position. This node should be the same as the masterNode reference in the class.
     *        However, it is passed as an argument to make it easier to spot a mismatch between the requester and the local masterNode reference in the class.
     *        The mismatch would be visible on the output log files
     * @param deviceName Name of the relay device to be switched
     * @param position Position to be switched in the relay
     * @param switchStatus New switch state at the target position: "true" or "false"
     * @return ExecutionResult encapsulating the success state of switching the specified position in the relay and a copy of the passed values as a DataPacket
     * 
     */
    public ExecutionResult updateSwitch(MasterNode switcher, String deviceName, String position, String switchStatus) {
        
        // Delay simulating the propagation and tranmission delays of the new switch state from the master node to this slave node
        int time_delay= RTT_to_Master_Node /2 + 8 / BLE_Transmission_Rate;
        SimulationClock.getInstance().waitFor(time_delay);

        // Add a log message indicating that the request to update the switch was received at the slave node
        exportState(String.format("Node [%s] attempted updating switch position [%s] in object [%s]",
            switcher.getObject_name(),
            position, 
            deviceName));

        // Add a log message indicating that this slave node has asked its local uController to switch the specified position in the connected relay
        exportState(String.format("Attempted switching position [%s] in object [%s] through local controller.",
           position, 
            deviceName
            ));

        ExecutionResult result;
        synchronized(localController){
            // Ask the localuController to update the switch state
            result = localController.updateSwitch(deviceName, position, switchStatus);
        }

        // Add a log message indicating the success of the switch update attempt by the local uController
        exportState(String.format("[%s] Switched position [%s] in object [%s] through local controller. New State [%s]",
        result.isSuccess()? "SUCCESS" : "FAILURE",
        position, 
        deviceName,
        result.isSuccess() ? result.getReturnedPacket().getValue() : "old state"));
        
        // Add a log message indicating the success of the switch update attempt at the node level
        exportState(String.format("[%s] Node [%s] attempted updating switch position [%s] in object [%s]. New State [%s]",
            result.isSuccess() ? "SUCCESS" : "FAILURE",
            switcher.getObject_name(),
            position, 
            deviceName,
            result.isSuccess() ? result.getReturnedPacket().getValue() : "old state"
            ));

        // Delay simulating the propagation delay of the acknowledgement back to the master node that attempted to update the switch state.
        // It is assumed that the transmission delay of that acknowledgement is negligible
        time_delay= RTT_to_Master_Node/2;
        SimulationClock.getInstance().waitFor(time_delay);

        // Return result to the master node
        return result;       
    }

    /**
     * Function to ask the master node whether a certain worker ID is permitted to enter the zone or not 
     * @param packet Data packet encapsulating worker ID whose entry permission is to be queried 
     * @return true if worker is permitted, false otherwise
     */
    public boolean isPermittedToEnter(DataPacket packet){
        String id = packet.getValue();

        // Add a log message indicating that this slave node has asked 
        // the master node about a certain worker's permission to enter zone
        exportState(String.format("Asked Control Node [%s] about ID [%s]'s permission",masterNode.getObject_name(),id));

        // Ask master node if the worker is permitted to enter or not
        boolean permitted = masterNode.isPermittedToEnter(this, packet);

        // Add a log message indicating whether the worker is permitted or not
        exportState(String.format("ID [%s]'s permission: [%s]", id, permitted? "ALLOWED": "DENIED"));
        
        // Return permission status
        return permitted;
    }

    /**
     * Getter
     * @return RTT_to_Master_Node
     */
    public int getRTT_to_Master_Node() {
        return RTT_to_Master_Node;
    }

    /**
     * Gets all fields available on all devices in this node. Gets the fields from the local uController
     * @return list of field names in the form [object name]_[field name]
     */
    public ArrayList<String> getGlobalOfferedFields(){
        return localController.getGlobalOfferedFields();
    }

    /**
     * Getter
     * @return reference to master node object
     */
    public MasterNode getMasterNode() {
        return masterNode;
    }

    /**
     * Function called continuously in the node's runtime thread assuming a thread was started for 
     * this node
     */
    @Override
    protected void runTimeFunction() {
        // Do Nothing
    }

    /**
     * Function to start the object without creating a thread. The SlaveNode is responsible for starting its local controller
     */
    @Override
    public void start() {
        super.start(false,0);
        exportState("Started");
        localController.start();
    }

    /**
     * Function to terminate the node's thread. The SlaveNode is responsible for terminating its local controller
     */
    @Override
    public void terminate() {
        localController.terminate();
        super.terminate();
    }
    
}
