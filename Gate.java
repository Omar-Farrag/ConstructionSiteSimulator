import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

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

        String name = getFullName("scanner");
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

        name = getFullName("relay");
        outputFile  = getOutputFileName(name);
        motorRelay = new Relay(name,outputFile,runTimeStep,1);

        name = getFullName("motor");
        outputFile  = getOutputFileName(name);
        motor = new HighPowerDevice(name,outputFile,runTimeStep);

        localController.connectTo(scanner);
        
        motorRelay.connectTo(motor, 0);
        localController.connectTo(motorRelay);

    }

    private String getInputFileName(String name){
        return "inputs/" + name + "_input.csv"; 

    }

    private String getOutputFileName(String name){
        return "logs/" + name + "_output.csv"; 

    }

    private String getFullName(String localName){
        return this.object_name + "_"+localName;
    }    
   
}
