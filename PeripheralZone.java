import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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

    public PeripheralZone(String object_name, int runTimeStep, ProcessingAlgorithm algorithm){
        super(object_name, runTimeStep);
        exportState("Started");

        this.runTimeStep = runTimeStep;
        this.algorithm = algorithm;
        
        otherSlaveNodes = new ArrayList<>();

        controlNode = createControlNode();
        gateNode = createGateNode();
        actuationNode = createActuationNode();
        buzzerNode = createBuzzerNode();
        speakerNode = createSpeakerNode();

        controlNode.exportState("Started");
        controlNode.localController.exportState("Started");

        gateNode.exportState("Started");
        gateNode.localController.exportState("Started");
        
        actuationNode.exportState("Started");
        actuationNode.localController.exportState("Started");
        
        buzzerNode.exportState("Started");
        buzzerNode.localController.exportState("Started");
        
        speakerNode.exportState("Started");
        speakerNode.localController.exportState("Started");

        
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

    public void addPermittedId(String id){
        controlNode.addPermittedId(id);
    }
    
    private SlaveNode createGateNode(){
        String nodeName = getFullName("GateNode");
        
        SlaveNode gateNode = new Gate(nodeName, runTimeStep, RTT_to_Zone_Controller); 
        gateNode.setControlNode(controlNode);
        
        return gateNode;
        
    }

    private SlaveNode createActuationNode(){
        String nodeName = getFullName("ActuatorNode");

        String actuatorName = nodeName + "_actuator";
        String controllerName = nodeName + "_controller";

        actuator = new Relay(actuatorName, runTimeStep, 4);

        uController controller = new uController(controllerName, runTimeStep, (uController cont)->{});
        controller.connectTo(actuator);

        SlaveNode node = new SlaveNode(nodeName, runTimeStep, RTT_to_Zone_Controller, controller);
        gateNode.setControlNode(controlNode);

        return node;

    }
    
    private SlaveNode createBuzzerNode(){

        String nodeName = getFullName("BuzzerNode");

        String actuatorName = nodeName + "_actuator";
        String controllerName = nodeName + "_controller";
        String buzzerName = nodeName + "_buzzer";

        Relay actuator = new Relay(actuatorName, runTimeStep, 1);
        HighPowerDevice buzzer = new HighPowerDevice(buzzerName, runTimeStep);
        actuator.connectTo(buzzer, 0);

        uController controller = new uController(controllerName, runTimeStep, (uController cont)->{});
        controller.connectTo(actuator);

        SlaveNode node = new SlaveNode(nodeName,runTimeStep, RTT_to_Zone_Controller, controller);
        gateNode.setControlNode(controlNode);

        return node;

    }
    
    private SlaveNode createSpeakerNode(){
        String nodeName = getFullName("SpeakerNode");

        String controllerName = nodeName + "_controller";
        String speakerName = nodeName + "_speaker";

        if(!new File(getInputFileName(speakerName)).exists())
        { 
            try (FileWriter fileWriter = new FileWriter(getInputFileName(speakerName));
                BufferedWriter bufferedWriter = new BufferedWriter(fileWriter)) {
                bufferedWriter.write("Played Message");
                bufferedWriter.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        LowPowerDevice speaker = new LowPowerDevice(speakerName, runTimeStep,100);

        uController controller = new uController(controllerName, runTimeStep, (uController cont)->{});
        controller.connectTo(speaker);

        SlaveNode node = new SlaveNode(nodeName, runTimeStep, RTT_to_Zone_Controller, controller);
        gateNode.setControlNode(controlNode);

        return node;

    }

    private ControlNode createControlNode(){
        String nodeName = getFullName("ControlNode");

        String controllerName = nodeName + "_controller";
        String cameraName = nodeName + "_camera";
        String gatewayName = nodeName + "_gateway";

        if(!new File(getInputFileName(cameraName)).exists())
        { 
            try (FileWriter fileWriter = new FileWriter(getInputFileName(cameraName));
                BufferedWriter bufferedWriter = new BufferedWriter(fileWriter)) {
                bufferedWriter.write("Video");
                bufferedWriter.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        LowPowerDevice camera = new LowPowerDevice(cameraName, runTimeStep,3500000);
        gateway = new Gateway(gatewayName, runTimeStep);

        uController controller = new uController(controllerName, runTimeStep, algorithm);
        controller.connectTo(camera);
        controller.connectTo(gateway);

        ControlNode node = new ControlNode(nodeName, runTimeStep, controller);

        return node;
    }

    private String getFullName(String localName){
        return this.object_name + "_"+localName;
    }

    
    public void initFields(){
       controlNode.initFields();
       gateNode.initFields();
       actuationNode.initFields();
       buzzerNode.initFields();
       speakerNode.initFields();

       for(SlaveNode node : otherSlaveNodes) node.initFields();
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
