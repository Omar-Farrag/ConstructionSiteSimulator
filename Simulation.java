import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Scanner;

public class Simulation {

    // Initialize scanner for user input
    static Scanner scanner = new Scanner(System.in);
    // A map to hold all simulation objects (zones and nodes)
    static HashMap<String, Zone> simulationObjects = new HashMap<>(); 

    public static void main(String[] args) {
        
        // Simulation parameters
        int runTimeStep = 20; //ms
        int RTT_to_Master_Node = 20; //ms
        int RTT_between_gateways = 100; //ms 
        int BLE_Transmission_Rate = 100; //kbps
        int WIFI_Transmission_Rate = 2000; // kbps

        // Set the simulation clock scale factor
        SimulationClock.getInstance().setScaleFactor(1);

        // List to store extra slave nodes
        ArrayList<SlaveNode> extraSlaveNodes = new ArrayList<>();
        
        // Create additional slave nodes
        extraSlaveNodes.add(create_general_slave_node("RoofZone_UltrasonicNode1","UltrasonicSensor", "Distance",4, runTimeStep,RTT_to_Master_Node, BLE_Transmission_Rate));
        extraSlaveNodes.add(create_general_slave_node("RoofZone_UltrasonicNode2","UltrasonicSensor", "Distance",4, runTimeStep,RTT_to_Master_Node, BLE_Transmission_Rate));
        extraSlaveNodes.add(create_general_slave_node("RoofZone_UltrasonicNode3","UltrasonicSensor", "Distance",4, runTimeStep,RTT_to_Master_Node, BLE_Transmission_Rate));
        extraSlaveNodes.add(create_general_slave_node("RoofZone_UltrasonicNode4","UltrasonicSensor", "Distance",4, runTimeStep,RTT_to_Master_Node, BLE_Transmission_Rate));
        extraSlaveNodes.add(create_general_slave_node("RoofZone_UltrasonicNode5","UltrasonicSensor", "Distance",4, runTimeStep,RTT_to_Master_Node, BLE_Transmission_Rate));
       
        // Create Smart Rope Nodes
        extraSlaveNodes.add(create_general_slave_node("RoofZone_SmartRopeNode1","SmartRope", "Is Attached",1, runTimeStep,RTT_to_Master_Node, BLE_Transmission_Rate));
        extraSlaveNodes.add(create_general_slave_node("RoofZone_SmartRopeNode2","SmartRope", "Is Attached",1, runTimeStep,RTT_to_Master_Node, BLE_Transmission_Rate));
        extraSlaveNodes.add(create_general_slave_node("RoofZone_SmartRopeNode3","SmartRope", "Is Attached",1, runTimeStep,RTT_to_Master_Node, BLE_Transmission_Rate));
        
        // Create Wind Sensing Node
        extraSlaveNodes.add(create_general_slave_node("RoofZone_WindNode","WindSensor", "Wind Speed",4, runTimeStep,RTT_to_Master_Node, BLE_Transmission_Rate));
        
        // Create a pulley lift to be attached to actuator
        HighPowerDevice pulleyLift = new HighPowerDevice("RoofZone_PulleyLift");

        // Define the processing algorithm for the roof zone master node uController
        ProcessingAlgorithm roofZoneAlgo = (uController controller)->{
            
            // Get current values from various sensors
            String distance1 = controller.getCurrentValue( "RoofZone_UltrasonicNode1_UltrasonicSensor", "Distance");
            String distance2 = controller.getCurrentValue( "RoofZone_UltrasonicNode2_UltrasonicSensor", "Distance");
            String distance3 = controller.getCurrentValue( "RoofZone_UltrasonicNode3_UltrasonicSensor", "Distance");
            String distance4 = controller.getCurrentValue( "RoofZone_UltrasonicNode4_UltrasonicSensor", "Distance");
            String distance5 = controller.getCurrentValue( "RoofZone_UltrasonicNode5_UltrasonicSensor", "Distance");
            
            String windSpeed = controller.getCurrentValue("RoofZone_WindNode_WindSensor", "Wind Speed");
            
            String ropeAttached1 = controller.getCurrentValue("RoofZone_SmartRopeNode1_SmartRope", "Is Attached");
            String ropeAttached2 = controller.getCurrentValue("RoofZone_SmartRopeNode2_SmartRope", "Is Attached");
            String ropeAttached3 = controller.getCurrentValue("RoofZone_SmartRopeNode3_SmartRope", "Is Attached");         

            // Convert the string values to float for processing
            Float distance1_float;
            Float distance2_float;
            Float distance3_float;
            Float distance4_float;
            Float distance5_float;

            Float windSpeed_float;

            Float ropeAttached1_float;
            Float ropeAttached2_float;
            Float ropeAttached3_float;

            try{
                distance1_float = Float.parseFloat(distance1);
                distance2_float = Float.parseFloat(distance2);
                distance3_float = Float.parseFloat(distance3);
                distance4_float = Float.parseFloat(distance4);
                distance5_float = Float.parseFloat(distance5);

                windSpeed_float =  Float.parseFloat(windSpeed);

                ropeAttached1_float = Float.parseFloat(ropeAttached1);
                ropeAttached2_float = Float.parseFloat(ropeAttached2);
                ropeAttached3_float = Float.parseFloat(ropeAttached3);
            }catch(Exception e){
                return; // Exit if there is a parsing error
            }
            
            // Safety check: if any rope is not attached, trigger buzzer and message
            if(ropeAttached1_float == 0 || ropeAttached2_float == 0 || ropeAttached3_float == 0){
                controller.updateSwitchIn("RoofZone_BuzzerNode", "RoofZone_BuzzerNode_relay", "0","true");
                controller.setFieldIn("RoofZone_SpeakerNode", "RoofZone_SpeakerNode_speaker", "Played Message", "One or more safety ropes not attached!!!!",32);
            }
            else{
                controller.updateSwitchIn("RoofZone_BuzzerNode", "RoofZone_BuzzerNode_relay", "0","false");
                controller.setFieldIn("RoofZone_SpeakerNode", "RoofZone_SpeakerNode_speaker", "Played Message", "",0);
            }
            
            // Safety check: if the worker is too close to the edge, trigger buzzer and message
            if(distance1_float<100 || distance2_float < 100 || distance3_float<100 || distance4_float<100 || distance4_float<100 || distance5_float<100){
                controller.updateSwitchIn("RoofZone_BuzzerNode", "RoofZone_BuzzerNode_relay", "0","true");
                controller.setFieldIn("RoofZone_SpeakerNode", "RoofZone_SpeakerNode_speaker", "Played Message", "Worker too close to edge",32);
            }else{
                controller.updateSwitchIn("RoofZone_BuzzerNode", "RoofZone_BuzzerNode_relay", "0","false");
                controller.setFieldIn("RoofZone_SpeakerNode", "RoofZone_SpeakerNode_speaker", "Played Message", "",0);
            }

            // Control the actuator node based on wind speed
            if(windSpeed_float < 20){
                controller.updateSwitchIn("RoofZone_ActuatorNode", "RoofZone_ActuatorNode_relay", "0", "true");
            }else{
                controller.updateSwitchIn("RoofZone_ActuatorNode", "RoofZone_ActuatorNode_relay", "0","false");
            }

            // If more than 20 packets are buffered, aggregate and forward them
            Queue<DataPacket> packets = controller.getBufferedDataPackets();
            if(packets.size() > 20){
                controller.clearBufferedDataPackets();
                BulkDataPacket bigBoi = new BulkDataPacket(controller.getParentMasterNode().getObject_name(), controller.getCurrentTimestamp());
                bigBoi.addPackets(packets);
                controller.exportState(String.format("Aggregated (%d) data packets to forward to control zone",packets.size()));
                controller.forwardToZones(bigBoi, "Zone2","Zone3","MasterZone");
            }
        };
        
        // Create the RoofZone with the defined algorithm
        SlaveZone roofZone = new SlaveZone("RoofZone", runTimeStep, roofZoneAlgo, RTT_to_Master_Node, BLE_Transmission_Rate, WIFI_Transmission_Rate);
        
        // Add permitted users to the zone
        roofZone.addPermittedId("Omar");
        roofZone.addPermittedId("Farrag");
        roofZone.addPermittedId("Mohsen");

        // Add slave nodes to the zone
        roofZone.addAllSlaveNodes(extraSlaveNodes);
        // Connect zone to an actuator node
        roofZone.connectToActuationNode(pulleyLift, 0);

        // Create other zones
        SlaveZone zone2 = new SlaveZone("Zone2", runTimeStep, (uController cont)->{}, RTT_to_Master_Node, BLE_Transmission_Rate,  WIFI_Transmission_Rate);
        SlaveZone zone3 = new SlaveZone("Zone3", runTimeStep, (uController cont)->{}, RTT_to_Master_Node, BLE_Transmission_Rate,  WIFI_Transmission_Rate);

        MasterZone masterZone = new MasterZone("MasterZone", runTimeStep, (uController cont)->{

            // Retrieve any bulk data packets received by the master zone
            Queue<BulkDataPacket> bulkDataPackets = cont.getReceivedBulkDataPackets(true);
        
            // If there are any bulk data packets, write them to a CSV log
            if(!bulkDataPackets.isEmpty()){
                try{
                    // Open a file writer to append data to the CSV file
                    FileWriter fileWriter = new FileWriter("logs/Database.csv", true);
                    PrintWriter writer = new PrintWriter(fileWriter);
                        
                    // Loop through all bulk packets and their individual data packets
                    for(BulkDataPacket bulkPacket : bulkDataPackets)
                        for(DataPacket packet : bulkPacket.getPackets())
                            writer.println(cont.getCurrentTimestamp() + "," + packet.toString());
                
                    // Flush and close the writer to save data
                    writer.flush();
                    writer.close();
                    
                } catch (IOException e) {
                    e.printStackTrace(); // Print stack trace in case of IO exception
                }
            }
        }, RTT_to_Master_Node, BLE_Transmission_Rate, WIFI_Transmission_Rate);
        
        // Connect the roof zone to zone2, and zone2 to zone3, and zone3 to master zone
        roofZone.connectToZone(zone2, RTT_between_gateways);
        zone2.connectToZone(zone3, RTT_between_gateways);
        zone3.connectToZone(masterZone, RTT_between_gateways);
        
        // Add all zones and master zone to the simulation objects map
        simulationObjects.put(roofZone.getObject_name(), roofZone);
        simulationObjects.put(zone2.getObject_name(), zone2);
        simulationObjects.put(zone3.getObject_name(), zone3);
        simulationObjects.put(masterZone.getObject_name(), masterZone);
        
        // Uncomment the line below to activate the menu, if needed
        // while(menu());
        
        // Initialize fields, start the simulation, and then end it after 10 seconds
        initFields();
        startSimulation();
        SimulationClock.getInstance().waitFor(10000); // Wait for 10 seconds
        endSimulation();
    }
        
        // Method to create a general slave node with a given set of parameters
        static SlaveNode create_general_slave_node(String nodeName, String deviceName, String fieldName, int fieldSize, int runTimeStep, int RTT_to_Zone_Controller, int BLE_transmission_rate) {
            String fullName = nodeName + "_" + deviceName; // Full name of the node
            LowPowerDevice dev = new LowPowerDevice(fullName, fieldSize); // Create a low power device with specified field size
            
            // Define the processing algorithm for the slave node uController
            ProcessingAlgorithm algo = (uController controller)->{
                // Get the field value from the device and publish the packet
                ExecutionResult result = controller.getField(fullName, fieldName);
                if(result.isSuccess()) controller.publishPacket(result.getReturnedPacket());
            };
        
            // Create a controller for the node with the defined algorithm
            uController controller = new uController(nodeName + "_controller", runTimeStep, algo);
            
            // Connect the controller to the device
            controller.connectTo(dev);
            return new SlaveNode(nodeName, RTT_to_Zone_Controller, BLE_transmission_rate, controller); // Return the created slave node
        }
        
        // Method for the menu, allowing user to interact with the simulation
        static boolean menu(){
            // Display menu options
            System.out.println("***************Options*************** ");
            System.out.println("1) Init Fields");
            System.out.println("2) Start Simulation");
            System.out.println("3) End Simulation");
            System.out.print("Your Option (type number only): ");
            
            String userInput = scanner.nextLine(); // Get user input
            try{
                int option = Integer.parseInt(userInput); // Try parsing the input to an integer
                switch(option){
                    case 1: initFields(); break; // Option to initialize fields
                    case 2: startSimulation(); break; // Option to start simulation
                    case 3: endSimulation(); break; // Option to end simulation
                    default: System.out.println("Invalid option"); // Handle invalid option
                }
                return option != 3; // Keep showing the menu until the user selects option 3 (exit)
        
            }catch(NumberFormatException e){
                System.out.println("Invalid option"); // Handle invalid input format
                return true; // Keep showing the menu
            }
        }
        
        // Method to initialize fields for all simulation objects
        static void initFields(){
            // Loop through all simulation objects and initialize fields for inactive zones
            for(Entry<String, Zone> entry : simulationObjects.entrySet()) 
                if (!entry.getValue().isAlive()) entry.getValue().initFields();
        
            System.out.println("[Fields Initialized]"); // Notify that fields have been initialized
        }
        
        // Method to start the simulation, resetting the simulation clock and starting all zones
        static void startSimulation(){
            SimulationClock.getInstance().reset(); // Reset the simulation clock
            
            // Loop through all simulation objects and start any inactive zones
            for(Entry<String, Zone> entry : simulationObjects.entrySet()) 
                if (!entry.getValue().isAlive()) entry.getValue().start();
        
            System.out.println("[Simulation Started]"); // Notify that the simulation has started
        }
        
        // Method to end the simulation and terminate all active zones
        static void endSimulation(){
            // Loop through all simulation objects and terminate any active zones
            for(Entry<String, Zone> entry : simulationObjects.entrySet()) 
                if (entry.getValue().isAlive()) entry.getValue().terminate();
            
            System.out.println("[Simulation Ended]"); // Notify that the simulation has ended
        }
    }        
