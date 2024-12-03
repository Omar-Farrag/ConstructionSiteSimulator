
/**
 * Abstract parent class representing a zone
 */
public abstract class Zone extends SimulationObject {

    // Master node of the zone
    protected MasterNode masterNode;
    
    // Control algorithm running continuously on master node's uController
    protected ProcessingAlgorithm masterNodeLoop;
    
    // Setup algorithm that runs once on master node's uController at the beginning
    protected ProcessingAlgorithm masterNodeSetup;

    // Reference to the master node's internal gateway
    protected Gateway gateway;

    /**
     * Constructor
     * @param objectName Name of the zone
     * @param runTimeStep Timestep in ms of the zone's simulation lifetime
     * @param loop Control algorithm running continuously on master node's uController
     * @param setup Setup algorithm that runs once on master node's uController at the beginning
     */
    public Zone(String objectName, int runTimeStep, ProcessingAlgorithm loop, ProcessingAlgorithm setup){
        super(objectName,runTimeStep);
        this.masterNodeLoop = loop;
        this.masterNodeSetup = setup;
    }

    /**
     * Function to initialize the fields of the devices in the zone. Default implementation
     */
    public void initFields(){
        masterNode.initFields();

    }

    /**
     * Function to connect two adjacent zones together via Wi-Fi
     * @param zone Zone to conenct to this zone
     * @param RTT_to_gateway Round trip time in ms between this zone's gateway and other zone's gateway
     */
    public void connectToZone(Zone zone, int RTT_to_gateway){
        this.gateway.connectTo(zone.gateway, RTT_to_gateway);
    }
    
    /**
     * Utility function to generate full name of a local object name
     * @param localName Name of the object whose full name is to be retrieved
     * @return Name of the object with the name of the zone added as a prefix
     */
    protected String getFullName(String localName){
        return this.object_name + "_"+localName;
    }
    
    /**
     * Function called continuously by the object's runtime thread
     */
    @Override
    protected void runTimeFunction() {
        // Do Nothing
    }
    
    /**
     * Function to start the zone's runtime thread. Zone is responsible for starting
     * its master node.
     */
    @Override
    public void start() {
        super.start();
        exportState("Started");
        
        masterNode.start();   
        
    }

    /**
     * Function to terminate the zone's runtime thread. Zone is responsible for terminating
     * its master node.
     */
    @Override
    public void terminate() {        
        masterNode.terminate();
        super.terminate();
    }
    
    /**
     * Abstract method to be implemented by children. This function sets up a master node for the zone
     * @return Fully created master node object
     */
    protected abstract MasterNode createMasterNode();
}
