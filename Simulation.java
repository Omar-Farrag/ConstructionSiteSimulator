import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Scanner;

public class Simulation {

    static Scanner scanner = new Scanner(System.in);
    static HashMap<String, PeripheralZone> simulationObjects = new HashMap<>(); 

    public static void main(String[] args) {
        
        int runTimeStep = 1000; //ms

        ProcessingAlgorithm zoneAlgo = (uController controller)->{
            ControlNode node = controller.getParentControlNode();
            BulkDataPacket packet = new BulkDataPacket(node.getObject_name(),controller.getCurrentTimestamp());
            packet.addPackets(node.getBufferedDataPackets());
            controller.exportState(String.format("Aggregated %s Data Packets", node.getBufferedDataPackets().size()));
            controller.forwardToZones(packet, "Zone2");
            controller.exportState(String.format("Current Buffer Size  = %s Data Packets", node.getBufferedDataPackets().size()));
        };
        PeripheralZone zone = new PeripheralZone("Zone1", runTimeStep, zoneAlgo);
        zone.addPermittedId("Omar");
        zone.addPermittedId("Farrag");
        
        
        LowPowerDevice dev = new LowPowerDevice("someDevice", runTimeStep, 1);
        uController cont = new uController("someCont", runTimeStep, (uController contr)->{});
        cont.connectTo(dev);
        SlaveNode node = new SlaveNode("someNode", runTimeStep, 20, cont);

        
        zone.addSlaveNode(node);

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
        System.out.println("1) Init Fields");
        System.out.println("2) Start Simulation");
        System.out.println("3) Deactivate Simulation Object");
        System.out.println("4) Update Field");
        System.out.println("5) End Simulation");
        System.out.print("Your Option (type number only): ");
        
        String userInput = scanner.nextLine();
        try{
            int option = Integer.parseInt(userInput);
            switch(option){
                case 1: initFields(); break;
                case 2: startSimulation(); break;
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

    static void initFields(){
        for(Entry<String,PeripheralZone> entry : simulationObjects.entrySet()) 
            if (!entry.getValue().isAlive()) entry.getValue().initFields();

        System.out.println("[Fields Initialized]");
    }
    static void startSimulation(){
        for(Entry<String,PeripheralZone> entry : simulationObjects.entrySet()) 
            if (!entry.getValue().isAlive()) entry.getValue().start();

        System.out.println("[Simulation Started]");
        
        
    }
    static void switchObject(boolean ON){
        
    }
    static void updateField(){
        
    }
    static void endSimulation(){
        for(Entry<String,PeripheralZone> entry : simulationObjects.entrySet()) 
            if (entry.getValue().isAlive()) entry.getValue().terminate();
        
        System.out.println("[Simulation Ended]");
    }
}
