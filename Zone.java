public abstract class Zone extends SimulationObject {

    protected MasterNode masterNode;
    protected ProcessingAlgorithm masterNodeLoop;
    protected ProcessingAlgorithm masterNodeSetup;
    protected Gateway gateway;

    Zone(String objectName, int runTimeStep, ProcessingAlgorithm loop, ProcessingAlgorithm setup){
        super(objectName,runTimeStep);
        this.masterNodeLoop = loop;
        this.masterNodeSetup = setup;
    }

    public void initFields(){
        masterNode.initFields();

    }
    protected abstract MasterNode createMasterNode();
    
    public void connectToZone(Zone zone, int RTT_to_gateway){
        this.gateway.connectTo(zone.gateway, RTT_to_gateway);
    }

    protected String getFullName(String localName){
        return this.object_name + "_"+localName;
    }

    @Override
    protected void runTimeFunction() {
        // Do Nothing
    }

    @Override
    public void start() {
        masterNode.start();
        super.start();

        exportState("Started");
        masterNode.exportState("Started");
        masterNode.localController.exportState("Started");

    }
    
    @Override
    public void terminate() {        
        masterNode.terminate();
        super.terminate();
    }

}
