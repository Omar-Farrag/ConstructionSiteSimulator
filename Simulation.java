import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Scanner;

public class Simulation {

    static Scanner scanner = new Scanner(System.in);
    static HashMap<String, SimulationObject> simulationObjects = new HashMap<>(); 

    public static void main(String[] args) {
        
        int runTimeStep = 1000; //ms

        ProcessingAlgorithm zoneAlgo = (uController controller)->{
            ControlNode node = controller.getParentControlNode();
            BulkDataPacket packet = new BulkDataPacket(node.getObject_name(),controller.getCurrentTimestamp());
            packet.addPackets(node.getBufferedDataPackets());
            controller.exportState(String.format("Aggregated %s Data Packets", node.getBufferedDataPackets().size()));
            controller.forwardToZones(packet, "Zone2");
            node.clearBufferedPackets();
            controller.exportState(String.format("Current Buffer Size  = %s Data Packets", node.getBufferedDataPackets().size()));
        };
        PeripheralZone zone = new PeripheralZone("Zone1", runTimeStep, zoneAlgo);
        zone.addPermittedId("Omar");
        zone.addPermittedId("Farrag");

        ProcessingAlgorithm zoneAlgo2 = (uController controller)->{            
        };
        PeripheralZone zone2 = new PeripheralZone("Zone2", runTimeStep, zoneAlgo2);
        zone.addPermittedId("Omar");
        zone.addPermittedId("Farrag");

        zone.connectToZone(zone2, 100);

        simulationObjects.put(zone.getObject_name(),zone);
        simulationObjects.put(zone2.getObject_name(),zone2);

        

        while(menu());
            
    }

    static boolean menu(){

        System.out.println("***************Options*************** ");
        System.out.println("1) Start Simulation");
        System.out.println("2) Activate Simulation Object");
        System.out.println("3) Deactivate Simulation Object");
        System.out.println("4) Update Field");
        System.out.println("5) End Simulation");
        System.out.print("Your Option (type number only): ");
        
        String userInput = scanner.nextLine();
        try{
            int option = Integer.parseInt(userInput);
            switch(option){
                case 1: startSimulation(); break;
                case 2: switchObject(true); break;
                case 3: switchObject(false); break;
                case 4: updateField(); break;
                case 5: endSimulation(); break;
                default: System.out.println("Invalid option");
            }
            return option != 5;

        }catch(NumberFormatException e){
            System.out.println("Invalid option");
            return true;
        }

    }

    static void startSimulation(){
        for(Entry<String,SimulationObject> entry : simulationObjects.entrySet()) 
            if (!entry.getValue().isAlive()) entry.getValue().start();

        System.out.println("[Simulation Started]");
        
        
    }
    static void switchObject(boolean ON){
        
    }
    static void updateField(){
        
    }
    static void endSimulation(){
        for(Entry<String,SimulationObject> entry : simulationObjects.entrySet()) 
            if (entry.getValue().isAlive()) entry.getValue().terminate();
        
        System.out.println("[Simulation Ended]");
    }
}
