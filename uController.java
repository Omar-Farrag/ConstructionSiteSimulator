import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

/**
 * This class represents a real-life microcontroller, which is present in every master and slave node in a zone
 */
public class uController extends SimulationObject{

    // Slave node containing this uController. Could be null if the uController is in a master node
    private SlaveNode parentSlaveNode;
    
    // Master node containing this uController. Could be null if the uController is in a slave node
    private MasterNode parentMasterNode;

    // Setup algorithm that is executed once on the uController before shifting to the loop algorithm
    private ProcessingAlgorithm setup;

    // Loop algorithm that executes continuously on the uController at the simulation's timestep
    private ProcessingAlgorithm loop;

    // List of devices connected to this uController
    private ArrayList<Device> devices;

    // List of all fields in the devices connected to this uController. The field names have the device name appended to them to distinguish them
    private ArrayList<String> globalOfferedFields;

    // Set of all field in the devices connected to this uController.
    private Set<String> localOfferedFields;
    
    // Set of all ID's permitted to enter a zone. Typically, the uController in a master node would 
    // have entries in this set
    private Set<String> permittedIDs;

    // Flag indicating whether the uController has run the set up function or not
    private boolean hasSetUp;

    // Gateway object connected to this uController. Typically only the uController of a master 
    // node would be connected to a gateway
    private Gateway gateway;

    /**
     * Constructor
     * @param name Name of this uController object
     * @param runTimeStep Timestep in ms of the uController's runtime thread
     * @param loop Looping algorithm running on the uController
     */
    public uController(String name, int runTimeStep, ProcessingAlgorithm loop) {
        super(name);
        this.loop = loop;
        this.devices = new ArrayList<>();
        this.runTimeStep = runTimeStep;
        globalOfferedFields = new ArrayList<>();
        localOfferedFields = new HashSet<>();
        permittedIDs = new HashSet<>();

        // Since no setup algorithm is provided, assume that the setup algorithm has already been executed
        hasSetUp = true;
    }

    
    /**
     * Constructor
     * @param name Name of this uController object
     * @param runTimeStep Timestep in ms of the uController's runtime thread
     * @param loop Looping algorithm running on the uController
     * @param setup Setup algorithm executed once on the uController before the loop algorithm
     */
    public uController(String name, int runTimeStep, ProcessingAlgorithm loop, ProcessingAlgorithm setup) {
        super(name);
        this.loop = loop;
        this.setup = setup;
        this.devices = new ArrayList<>();
        globalOfferedFields = new ArrayList<>();
        localOfferedFields = new HashSet<>();
        permittedIDs = new HashSet<>();

        // A setup algorithm was provided so the uController is not set up until the setup algorithm is executed
        hasSetUp = false;
    }

    /**
     * Function to initialize all the fields in the connected fields
     */
    public void initFields(){
        for (Device dev : devices){
            // initialize the device's fields
            dev.initFields();
            for(String field : dev.getFieldNames()){
                // Add the global and local field name to their respective lists
                globalOfferedFields.add(dev.getObject_name()+"_"+field);
                localOfferedFields.add(field);
            }
        }         
    }

    /**
     * Function to set the slave node where this uController is located. This function is likely 
     * called by the node containing this uController. 
     * 
     * uController can have either a parent slave node or a parent master node, but not both
     *  
     * @param parentNode Slave node containing this uController
     */
    public void setParentNode(SlaveNode parentNode) {

        // Set the parent slave node
        this.parentSlaveNode = parentNode;

        // Nullify the parent master node to ensure only one parent node
        this.parentMasterNode = null;

    }
    
    /**
     * Function to set the master node where this uController is located. This function is likely 
     * called by the node containing this uController. 
     * 
     * uController can have either a parent slave node or a parent master node, but not both
     *  
     * @param parentNode Master node containing this uController
     */
    public void setParentNode(MasterNode parentNode) {
        
        // Set the parent slave node
        this.parentMasterNode = parentNode;
        
        // Nullify the parent slave node to ensure only one parent node
        this.parentSlaveNode = null;
    }

    /**
     * Function to set the gateway object connected to this uController
     */
    public void setGateway(Gateway gateway) {
        this.gateway = gateway;
    }

    /**
     * Function to connect one or more low power devices to this uController
     * @param devicesToConnect Low power devices to connect to this uController
     */
    public void connectTo(LowPowerDevice... devicesToConnect){
        synchronized(devices){
            for(LowPowerDevice device : devicesToConnect) {
                devices.add(device);
            }
        }
    }

    /**
     * Function to connect the uController to one or more relaus
     * @param devicesToConnect relays to connect to 
     */
    public void connectTo(Relay... devicesToConnect){
        synchronized(devices){
            for(Device device : devicesToConnect) devices.add(device);
        }
    }

    /**
     * Function to connect the uController to a gateway
     * @param gateway Gateway to connect to
     */
    public void connectTo(Gateway gateway){
        this.gateway = gateway;
        
        // Let the gateway know that this is the parent controller of the gateway
        gateway.setParentController(this);
    }
    
    /**
     * Function to send one or more packets to the master node. Typically called in the loop algorithms of slave node uControllers
     * @param packets Packets to send to master node
     */
    public void publishPacket(DataPacket... packets){
        // Add a log message indiciating that the packets have been sent upwards to the parent node
        exportState(String.format("Sent [%d] packets to parent node to publish them",packets.length));
        
        // Ask the parent node to publish the packeets
        parentSlaveNode.publishPacket(packets);
    }

    /**
     * Function to receive a bulk data packet sent from a different zone
     * @param source Gateway that initiated the transfer of the bulk data packet
     * @param previous Last gateway to forward the packet to this uController's connected gateway
     * @param packet Bulk data packet being shared
     */
    public void receiveBulkDataPacket(Gateway source, Gateway previous, BulkDataPacket packet){
        // Add a log message indiciating the receival of the packet
        exportState(String.format("[SUCCESS] Received packet from Gateway [%s]. Last Forwarded By Gateway [%s]", source.getObject_name(), previous.getObject_name()));
        
        // Forward the received bulk packet to the parent master node of the uController
        parentMasterNode.receiveForwardedPacket(source.getParentNode(), packet);
    }
    
    /**
     * Function to forward a bulk data packet along a certain route. Typically called by the loop algorithms 
     * of uControllers in master node in slave zone to upload the packets to the master zone
     * @param packet BulkDataPacket to be uploaded 
     * @param peripheralZoneRoute Route along which the packet should traverse. The arguments are the zone names, which must be provided 
     * in the order they should be traversed
     * @return true if the packet was forwarded successfully to its target destination, false otherwise
     */
    public boolean forwardToZones(BulkDataPacket packet, String... peripheralZoneRoute){
        
        // Actual data sending takes place between the gateways of the zones
        // Therefore route involving the names of the zones must be converted to the equivalent gateway names
        for (int i = 0; i<peripheralZoneRoute.length; i++) peripheralZoneRoute[i] += "_MasterNode_gateway";

        // Forward the packet along the route
        return forward(packet, String.join(",", peripheralZoneRoute));
    }

    /**
     * Lower level forward function to perform the actual forwarding
     * @param packet Packet to forward
     * @param route The names of the gateways along which the packet should be forwarded separated by ","
     * @return true if the packet was forwarded successfully to its target destination, false otherwise
     */
    private boolean forward(BulkDataPacket packet, String route){
        
        // Append the name of this zone's gateway as the first element in the route
        route = this.gateway.getObject_name()+","+route;

        // Extract the individual gateway names
        String[] hops = route.split(",");
                
        String nextHop, destination;

        // if there is only one gateway in the route, then it is the next hop and the desintation too
        // which is this uController's connected gateway
        if(hops.length == 1) nextHop = destination = this.gateway.getObject_name();
        else{
            // otherwise, next hop is the next element in the route
            nextHop = hops[1];

            // destination is the last element in the route
            destination = hops[hops.length-1];
        }

        // Add a log message to parent master node indicating the start of the forwarding
        this.parentMasterNode.exportState(String.format("[STARTED] Sent Packet Through Connected Gateway. Next Hop [%s]. Target Destination [%s]", nextHop,destination));
        
        // Add a log message indicating the start of the forwarding
        exportState(String.format("[STARTED] Sent Packet Through Connected Gateway. Next Hop [%s]. Target Destination [%s]", nextHop,destination));
        
        // Forwardd the packet
        boolean sent =  gateway.forward(gateway,gateway, packet, route, 0);
        
        // Add a log message indicating the success or failure of the forwarding
        exportState(String.format("[%s] Sent Packet Through Connected Gateway. Next Hop [%s]. Target Destination [%s]", sent ? "SUCCESS" : "FAILURE", nextHop, destination));
        
        // Add a log message to the parent master node indicating the success or failure of the forwarding
        this.parentMasterNode.exportState(String.format("[%s] Sent Packet Through Connected Gateway. Next Hop [%s]. Target Destination [%s]", sent ? "SUCCESS" : "FAILURE", nextHop, destination));
        
        return sent;
    }

    /**
     * Function to add a worker's ID to the list of IDs permitted to enter a zone. Typically called on uControllers
     * of master nodes
     * @param id Id of the worker to be granted access to the zone
     */
    public void addPermittedId(String id){
        permittedIDs.add(id);
    }

    
    /**
     * Function to remove a worker's ID from the list of IDs permitted to enter a zone. Typically called on uControllers
     * of master nodes
     * @param id Id of the worker whose access to the zone is to be revoked
     */
    public void removePermittedId(String id){
        permittedIDs.remove(id);
    }

    /**
     * Function to check whether a worker is permitted to enter a zone or not. Typically called on the uController of a master node
     * @param id ID of the worker whose entry permission status is being checked
     * @return true if the worker is permitted to enter the zone, false otherwise
     */
    public boolean isPermittedToEnter(String id){
        boolean permitted = permittedIDs.contains(id);
        exportState(String.format("Parent Node [%s] asked if ID [%s] is permitted to enter zone. Permission [%s]", parentMasterNode.getObject_name(),id, permitted ? "ALLOWED" : "DENIED"));
        return permitted;
    }
    
    /**
     * Function to retrieve the data packets buffered in the parent master node
     * @return A copy of the buffered packets
     */
    public Queue<DataPacket> getBufferedDataPackets() {
        if(parentMasterNode != null) return parentMasterNode.getBufferedDataPackets();
        else return new LinkedList<>();
    }
    
    /**
     * Function to clear the data packets buffered in the parent master node
     */
    public void clearBufferedDataPackets(){
        if(parentMasterNode != null) parentMasterNode.clearBufferedDataPackets();

    }

    /**
     * Function to retrieve the bulk data packets buffered in the parent master node
     * @param consume true if the buffered bulk data packets should be cleared from the node's buffer
     * after reading them or if they remain in storage
     * @return Copy of all the bulk data packetst received at the parent master node
     */
    public Queue<BulkDataPacket> getReceivedBulkDataPackets(boolean consume) {
        return parentMasterNode.getReceivedBulkDataPackets(consume);
    }

    /**
     * Function to update the value of a certain field in a certain device in a certain slave node. Typically called from within the loop algorithm of 
     * uController of a master node
     * @param targetNodeName Name of the slave node containing the device containing the field whose value is to be updated
     * @param targetObjectName Name of the device containing the field to be updated
     * @param field Name of the field to be updated
     * @param value New value for the field
     * @param size Size of the new value in bytes
     * @return Execution result encapsulating the success of the query and the returned data packet containing the user provided parameters
     */
    public ExecutionResult setFieldIn(String targetNodeName, String targetObjectName, String field, String value,
            int size) {
        // Add a log message indicating that the parent master node  was asked to set field in target object
        exportState(String.format("Asked parent node to set field [%s] in object [%s] in slave node [%s]", field, targetObjectName, targetNodeName));
        
        // Ask parent master node to set the field in the target device
        ExecutionResult result =  parentMasterNode.setFieldIn(targetNodeName, targetObjectName, field, value, size);
        
        // Check whether update attempt was successful or not 
        String successStatus = result.isSuccess() ? "SUCCESS" : "FAILURE";
        
        // Add a log message indiciating the success of the setting attempt
        exportState(String.format("[%s] Parent set field [%s] in object [%s] in slave node [%s]", successStatus, field, targetObjectName, targetNodeName));
        
        // return the execution result
        return result;                
    }

    /**
     * Function to update the state of a certain switch in a certain relay in a certain slave node. Typically called from within the loop algorithm of 
     * uController of a master node
     * @param targetNodeName Name of the slave node containing relay device whose switch state is to be updated
     * @param targetObjectName Name of the relay device whose switch state is to be update
     * @param position The switch number with the relay
     * @param switchStatus The new status of the switch. "true" or "false"
     * @return Execution result encapsulating the success of the query and the returned data packet containg the user provided parameters
     */
    public ExecutionResult updateSwitchIn(String targetNodeName, String targetObjectName, String position,
            String switchStatus) {
        // Add a log message indicating that the parent slave node was asked to update a switch position in target device
        exportState(String.format("Asked parent node to set switch position [%s] in object [%s] in slave node [%s]", position, targetObjectName, targetNodeName));
        
        // Ask parent master node to update the switch status
        ExecutionResult result = parentMasterNode.updateSwitchIn(targetNodeName, targetObjectName, position, switchStatus);
        
        // Check if update attempt was successful
        String successStatus = result.isSuccess() ? "SUCCESS" : "FAILURE";
        
        // Add a log message indicating that the success of the switch status update attempt
        exportState(String.format("[%s] Parent node set switch position [%s] in object [%s] in slave node [%s]", successStatus, position, targetObjectName, targetNodeName));
        
        // Return execution result
        return result;  
    }

    /**
     * Runtime function continuously called by the uController's runtime thread. Devices, Nodes, and Gateways do not run on their own threads.
     * Instead, they are updated by the uController's thread before the uController executes its loop algorithm.
     */
    @Override
    protected void runTimeFunction() {

        // Call the runtime function of all devices, nodes, and the gateway of the uController
        if (parentSlaveNode != null) parentSlaveNode.runTimeFunction();
        if (parentMasterNode != null) parentMasterNode.runTimeFunction();
        if(gateway != null) gateway.runTimeFunction();
        for(Device device : devices) device.runTimeFunction();

        // Execute setup algorithm if uController hasn't set up
        if(!hasSetUp) {
            setup.process(this);
            hasSetUp = true;
        };
        
        // Execute the looping algorithm
        loop.process(this);
    }

    /**
     * Function to retrieve the value of a certain field in one of the devices connected to this uController
     * @param objectName Name of the object whose field is to be queried
     * @param field Name of the field being queried
     * @return ExecutionResult encapsulating the success status of the query and the return data packet
     */
    public ExecutionResult getField(String objectName, String field){
        synchronized(devices){
            // For every device in the list of connected devices
            for(Device dev : devices){
                
                // If the device is the target device
                if(dev.getObject_name().equalsIgnoreCase(objectName)){
                    
                    // Execute a GET command on that device
                    ExecutionResult result = dev.execute("GET", field);

                    // Check  the status of the GET command
                    String status = result.isSuccess() ? "SUCCESS" : "FAILURE";
                    
                    // Extract data packet from result
                    DataPacket packet = result.getReturnedPacket();
                    
                    // Add a log message indicating the success of the command and the retrieved value
                    exportState(String.format("[%s] Received field [%s] from object [%s]. Value [%s]",status, field, objectName, packet != null ? packet.getValue(): "Null"));
                    
                    // Return the result
                    return result;
                }
            }
        }
        
        // If the target device is not among the list of devices connected to this uController, add an error log message
        exportState(String.format("[FAILURE] Received field [%s] from object [%s]. Value [Null]", field, objectName));

        // Return a blank ExecutionResult
        return new ExecutionResult(false, null);
    }
    
    /**
     * Function to update the value of a certain field in one of the devices connected to this uController
     * @param objectName Name of the object whose field value is to be updated
     * @param field Name of the field whose value is to be updated
     * @param value New value for the field
     * @return ExecutionResult encapsulating the success of the command and a packet containing the user provided parameters
     */
    public ExecutionResult setField(String objectName, String field, String value){       
        synchronized(devices){
            // For every device in the list of devices connected to this uController
            for(Device dev : devices){

                // If the device is the target device
                if(dev.getObject_name().equalsIgnoreCase(objectName)){
                    
                    // Execute the SET command on it
                    ExecutionResult result = dev.execute("SET", field, value);

                    // Check if the command was successful or not
                    String status = result.isSuccess() ? "SUCCESS" : "FAILURE";

                    // Add a log message indicating the success of the command
                    exportState(String.format("[%s] Set field [%s] in object [%s]",status, field, objectName));

                    // Return result
                    return result;
                }
            }
        }
        
        // If device is not in the list of devices connected to this uController, add an error log message
        exportState(String.format("[FAILURE] Set field [%s] in object [%s]", field, objectName));

        // Return a blank execution result
        return new ExecutionResult(false, null);

    }
    
    /**
     * Function to update the value of a certain switch position in a relay connected to this uController
     * @param objectName Name of the relay device whose switch status is to be updated
     * @param position An integer position of the switch in the relay 
     * @param switchStatus New state of the switch. "true" or "false"
     * @return ExecutionResult encapsulating the success of the update attempt and a data packet encapsulating the returned parameters
     */
    public ExecutionResult updateSwitch(String objectName, String position, String switchStatus){

        synchronized(devices){
            // For every device in the list of devices connected to this uController
            for(Device dev : devices){
                // If the device is the target device
                if(dev.getObject_name().equalsIgnoreCase(objectName)){
                    // Execute Switch command on device
                    ExecutionResult result = dev.execute("Switch", position, switchStatus);
                    
                    // Check whether the command was successful or not
                    String successStatus = result.isSuccess() ? "SUCCESS" : "FAILURE";
                    
                    // Add a log message indicating the success of he switch update attemp
                    exportState(String.format("[%s] Switched object [%s] position [%s] to [%s]",successStatus, objectName, position, switchStatus));
                    
                    // Return the result
                    return result;
                }
            }
        }
        
        // If device is is not in list of devices connected to this uController, add an error log message
        exportState(String.format("[FAILURE] Switched object [%s] position [%s] to [%s]", objectName, position, switchStatus));
        
        // Return blank execution result
        return new ExecutionResult(false, null);

    }
    
    /**
     * Getter
     * @return parent slave node of this uController
     */
    public SlaveNode getParentSlaveNode() {
        return parentSlaveNode;
    }
    
    /**
     * Getter
     * @return parent master node of this uController
     */
    public MasterNode getParentMasterNode() {
        return parentMasterNode;
    }

    /**
     * Getter
     * @return list of global offered fields containing all field names with the device names appended to them
     */
    public ArrayList<String> getGlobalOfferedFields() {
        return globalOfferedFields;
    }

    /**
     * Getter
     * @return set of fields in all devices connected to this uController
     */
    public Set<String> getLocalOfferedFields() {
        return localOfferedFields;
    }

    /**
     * Function to retrieve the current value of a certain field in any node in the zone. Typicall called by the loop algorithm of a uController of a master node
     * @param deviceName Name of the device containing the field
     * @param fieldName Field whose value is to be retrieved
     * @return The value of the field
     */
    public String getCurrentValue(String deviceName, String fieldName){
        // Function only works if this uController is in a master node
        if(parentMasterNode != null){
            // get the current value from the parent master node
            return parentMasterNode.getCurrentValue(deviceName, fieldName);
        }

        // Otherwise, return null
        else return null;
    }

    /**
     * Function to terminate the uController. uController is responsible for terminating its connected devices and gateway
     */
    @Override
    public void terminate() {
        for(Device dev : devices) dev.terminate();
        if(gateway != null) gateway.terminate();
        super.terminate();
    }

    /**
     * Function to start the uController's runtime thread. uController is responsible for starting its conencted devices and gateway
     */
    @Override
    public void start() {
        for(Device dev : devices) dev.start();
        if(gateway != null) gateway.start();

        // start the uController with a thread
        super.start(true, runTimeStep);
        
        exportState("Started");
    }
}
