import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class MasterZone extends Zone {

    MasterZone(String object_name, int runTimeStep, ProcessingAlgorithm masterNodeLoop){
        super(object_name, runTimeStep, masterNodeLoop, null);
        createDatabaseFile();
        this.masterNode = createMasterNode();
    }
    
    MasterZone(String object_name, int runTimeStep, ProcessingAlgorithm masterNodeLoop, ProcessingAlgorithm masterNodeSetup){
        super(object_name, runTimeStep, masterNodeLoop, masterNodeSetup);
        createDatabaseFile();
        this.masterNode = createMasterNode();
    }
    
    private void createDatabaseFile(){
        try {
            File file = new File("logs/Database.csv"); // Change the path as needed
            if (file.exists()) file.delete();

            FileWriter fileWriter = new FileWriter("logs/Database.csv", true);
            PrintWriter writer = new PrintWriter(fileWriter,true);
            writer.println("Time of Arrival,"+DataPacket.getHeader());
            writer.close();

        } catch (IOException e) {
            System.out.println("An error occurred while appending to the file.");
            e.printStackTrace();
        }
    }

    @Override
    protected MasterNode createMasterNode(){

        String nodeName = getFullName("MasterNode");

        String controllerName = nodeName + "_controller";
        String gatewayName = nodeName + "_gateway";

        gateway = new Gateway(gatewayName, runTimeStep);

        uController controller;
        if(masterNodeSetup!= null) controller = new uController(controllerName, runTimeStep, masterNodeLoop, masterNodeSetup);
        else controller = new uController(controllerName, runTimeStep, masterNodeLoop);

        controller.connectTo(gateway);

        MasterNode node = new MasterNode(nodeName, runTimeStep, controller);

        return node;
    }
}
