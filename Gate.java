import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class Gate extends SlaveNode{

    private LowPowerDevice scanner;
    private Relay motorRelay;
    private HighPowerDevice motor;

    public Gate(String nodeName, String outputFileName, int runTimeStep, int RTT_to_Zone_Controller){
        super(nodeName, outputFileName, runTimeStep, RTT_to_Zone_Controller, 
                new uController(
                    getFullName(nodeName, "controller") 
                    ,getOutputFileName(getFullName(nodeName, "controller"))
                    , runTimeStep
                    , (uController controller)->{gateControllerFunction(controller,nodeName);}
                    )
            );

        String name = getFullName(nodeName,"scanner");
        String outputFile  = getOutputFileName(name);
        String inputFile = getInputFileName(name);

        if(!new File(inputFile).exists())
        {        
            try (FileWriter fileWriter = new FileWriter(inputFile);
                BufferedWriter bufferedWriter = new BufferedWriter(fileWriter)) {
                bufferedWriter.write("ID");
                bufferedWriter.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        scanner = new LowPowerDevice(name, outputFile, runTimeStep, inputFile, 12); 

        name = getFullName(nodeName,"relay");
        outputFile  = getOutputFileName(name);
        motorRelay = new Relay(name,outputFile,runTimeStep,1);

        name = getFullName(nodeName,"motor");
        outputFile  = getOutputFileName(name);
        motor = new HighPowerDevice(name,outputFile,runTimeStep);

        localController.connectTo(scanner);
        
        motorRelay.connectTo(motor, 0);
        localController.connectTo(motorRelay);

    }

    private static String getInputFileName(String name){
        return "inputs/" + name + "_input.csv"; 

    }

    private static String getOutputFileName(String name){
        return "logs/" + name + "_output.csv"; 

    }

    private static void gateControllerFunction(uController controller, String gateObjectName){
        ExecutionResult result = controller.getField(getFullName(gateObjectName, "scanner"), "ID");
                    if(result.isSuccess()){
                        String value = result.getReturnedPacket().getValue();
                        SlaveNode parent = controller.getParentSlaveNode();
                        controller.exportState(String.format("Asked Parent Node [%s] about ID [%s]'s permission",parent.getObject_name(),value));
                        boolean permitted = parent.isPermittedToEnter(value);
                        controller.exportState(String.format("ID [%s]'s permission: [%s]", value, permitted? "ALLOWED": "DENIED"));
                        if(permitted) controller.updateSwitch(getFullName(gateObjectName, "relay"), "0","ON");
                        else controller.updateSwitch(getFullName(gateObjectName, "relay"), "0","OFF");
                    }
    }

    private static String getFullName(String nodeName, String localObjectName){
        return nodeName + "_"+ localObjectName;
    }    
   
}
