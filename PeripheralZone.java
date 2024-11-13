import java.util.ArrayList;

public class PeripheralZone extends SimulationObject {


    protected ControlNode controlNode;
    protected SlaveNode gateNode;
    protected SlaveNode actuationNode;
    protected SlaveNode buzzerNode;
    protected SlaveNode speakerNode;
    protected ProcessingAlgorithm algorithm;

    private Relay actuator;
    private Gateway gateway;

    private final int RTT_to_Zone_Controller = 20;
    private int runTimeStep;

    protected ArrayList<SlaveNode> otherSlaveNodes;

    public PeripheralZone(String object_name, String outputLogFileName, int runTimeStep, ProcessingAlgorithm algorithm){
        super(object_name, outputLogFileName, runTimeStep);
        this.runTimeStep = runTimeStep;

        this.algorithm = algorithm;

        controlNode = createControlNode();
        gateNode = createGateNode();
        actuationNode = createActuationNode();
        buzzerNode = createBuzzerNode();
        speakerNode = createSpeakerNode();
        
    }

    public void connectToActuationNode(HighPowerDevice dev, int position){
        actuator.connectTo(dev, position);
    }

    public void connectToZone(PeripheralZone zone, int RTT_to_gateway){
        this.gateway.connectTo(zone.gateway, RTT_to_gateway);
    }

    public void addSlaveNode(SlaveNode otherSlaveNode){
        otherSlaveNodes.add(otherSlaveNode);
        controlNode.subscribeTo(otherSlaveNode);

    }

    private SlaveNode createGateNode(){
        String nodeName = getFullName("GateNode");
        String outputFileName = getOutputFileName(nodeName);
        
        SlaveNode gateNode = new Gate(nodeName, outputFileName, runTimeStep, RTT_to_Zone_Controller); 
        gateNode.setControlNode(controlNode);
        
        return gateNode;
        
    }

    private SlaveNode createActuationNode(){
        String nodeName = getFullName("ActuatorNode");
        String outputFileName = getOutputFileName(nodeName);

        String actuatorName = nodeName + "_actuator";
        String controllerName = nodeName + "_controller";

        actuator = new Relay(nodeName + actuatorName, getOutputFileName(actuatorName), runTimeStep, 4);

        uController controller = new uController(controllerName, getOutputFileName(controllerName), runTimeStep, (uController cont)->{});
        controller.connectTo(actuator);

        SlaveNode node = new SlaveNode(nodeName, outputFileName, runTimeStep, RTT_to_Zone_Controller, controller);
        gateNode.setControlNode(controlNode);

        return node;

    }
    
    private SlaveNode createBuzzerNode(){

        String nodeName = getFullName("BuzzerNode");
        String outputFileName = getOutputFileName(nodeName);

        String actuatorName = nodeName + "_actuator";
        String controllerName = nodeName + "_controller";
        String buzzerName = nodeName + "_buzzer";

        Relay actuator = new Relay(nodeName + actuatorName, getOutputFileName(actuatorName), runTimeStep, 1);
        HighPowerDevice buzzer = new HighPowerDevice(buzzerName, getOutputFileName(buzzerName), runTimeStep);
        actuator.connectTo(buzzer, 0);

        uController controller = new uController(controllerName, getOutputFileName(controllerName), runTimeStep, (uController cont)->{});
        controller.connectTo(actuator);

        SlaveNode node = new SlaveNode(nodeName, outputFileName, runTimeStep, RTT_to_Zone_Controller, controller);
        gateNode.setControlNode(controlNode);

        return node;

    }
    
    private SlaveNode createSpeakerNode(){
        String nodeName = getFullName("SpeakerNode");
        String outputFileName = getOutputFileName(nodeName);

        String controllerName = nodeName + "_controller";
        String speakerName = nodeName + "_speaker";

        LowPowerDevice speaker = new LowPowerDevice(speakerName, getOutputFileName(speakerName), runTimeStep,getInputFileName(speakerName),100);

        uController controller = new uController(controllerName, getOutputFileName(controllerName), runTimeStep, (uController cont)->{});
        controller.connectTo(speaker);

        SlaveNode node = new SlaveNode(nodeName, outputFileName, runTimeStep, RTT_to_Zone_Controller, controller);
        gateNode.setControlNode(controlNode);

        return node;

    }

    private ControlNode createControlNode(){
        String nodeName = getFullName("ControlNode");
        String outputFileName = getOutputFileName(nodeName);

        String controllerName = nodeName + "_controller";
        String cameraName = nodeName + "_camera";
        String gatewayName = nodeName + "_gateway";

        LowPowerDevice camera = new LowPowerDevice(cameraName, getOutputFileName(cameraName), runTimeStep,getInputFileName(cameraName),3500000);
        gateway = new Gateway(gatewayName, getOutputFileName(gatewayName), runTimeStep);

        uController controller = new uController(controllerName, getOutputFileName(controllerName), runTimeStep, algorithm);
        controller.connectTo(camera);
        controller.connectTo(gateway);

        ControlNode node = new ControlNode(nodeName, outputFileName, runTimeStep, controller);

        return node;
    }

    private String getOutputFileName(String name){
        return "logs/"+ name+ ".csv";
        
    }

    private String getInputFileName(String name) {
        return "inputs/"+ name+ ".csv";
    }

    private String getFullName(String localName){
        return this.object_name + "_"+localName;
    }

    @Override
    protected void runTimeFunction() {
        // Do Nothing
    }

    @Override
    public void start() {
        controlNode.start();
        gateNode.start();
        actuationNode.start();
        buzzerNode.start();
        speakerNode.start();

        for(SlaveNode node : otherSlaveNodes) node.start();

        super.start();
    }
    
    @Override
    public void terminate() {
        controlNode.terminate();
        gateNode.terminate();
        actuationNode.terminate();
        buzzerNode.terminate();
        speakerNode.terminate();

        for(SlaveNode node : otherSlaveNodes) node.terminate();
        super.terminate();
    }

}
