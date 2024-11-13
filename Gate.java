
public class Gate extends SlaveNode{

    private LowPowerDevice scanner;
    private Relay motorRelay;
    private HighPowerDevice motor;

    public Gate(String object_name, String outputFileName, int runTimeStep, int RTT_to_Zone_Controller){
        super(object_name, outputFileName, runTimeStep, RTT_to_Zone_Controller, 
            new uController(
                object_name + "_" + "controller" 
                ,"logs/"+object_name + "_" + "controller.csv" 
                , runTimeStep
                , (uController controller)->{
                    ExecutionResult result = controller.getField(object_name + "_" + "scanner", "ID");
                    if(result.isSuccess()){
                        String value = result.getReturnedPacket().getValue();
                        SlaveNode parent = controller.getParentSlaveNode();
                        controller.exportState(String.format("Asked Parent Node [%s] about ID [%s]'s permission",parent.getObject_name(),value));
                        boolean permitted = parent.isPermittedToEnter(value);
                        controller.exportState(String.format("ID [%s]'s permission: [%s]", value, permitted? "ALLOWED": "DENIED"));
                        if(permitted) controller.updateSwitch(object_name + "_" + "relay", "0","ON");
                        else controller.updateSwitch(object_name + "_" + "relay", "0","OFF");
                    }
                }));

        scanner = new LowPowerDevice(
            object_name + "_" + "scanner" 
            , "logs/"+ object_name + "_" + "scanner.csv"
            , runTimeStep
            , "inputs/"+object_name + "_" + "scanner_values.csv"
            , 12);

        motorRelay = new Relay(
            object_name + "_" + "relay" 
            , "logs/"+object_name + "_" + "relay.csv" 
            , runTimeStep
            , 1);

        motor = new HighPowerDevice(
            object_name + "_" + "motor" 
            , "logs/"+object_name + "_" + "motor.csv" 
            , runTimeStep);

        localController.connectTo(scanner);
        
        motorRelay.connectTo(motor, 0);
        localController.connectTo(motorRelay);

    }

    @Override
    public void start() {
        scanner.start();
        motorRelay.start();
        motor.start();
        this.localController.start();
        super.start();
    }

    @Override
    public void terminate() {
        scanner.terminate();
        motorRelay.terminate();
        motor.terminate();
        this.localController.terminate();
        super.terminate();
    }

    
   
}
