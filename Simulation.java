import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Scanner;

public class Simulation {

    static Scanner scanner = new Scanner(System.in);
    static HashMap<String, Zone> simulationObjects = new HashMap<>(); 

    public static void main(String[] args) {
        
        int runTimeStep = 500; //ms
        int RTT_to_Master_Node = 20; //ms
        int RTT_between_gateways = 100; //ms 
        int BLE_Transmission_Rate = 100; //kbps
        int WIFI_Transmission_Rate = 2000; // kbps
        ArrayList<SlaveNode> extraSlaveNodes = new ArrayList<>();
        
        //Create additional slave nodes
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
        
        //Create a pulley lift to be attached to actuator
        HighPowerDevice pulleyLift = new HighPowerDevice("RoofZone_PulleyLift", runTimeStep);


        ProcessingAlgorithm zoneAlgo = (uController controller)->{
            
            String distance1 = controller.getCurrentValue( "RoofZone_UltrasonicNode1_UltrasonicSensor", "Distance");
            String distance2 = controller.getCurrentValue( "RoofZone_UltrasonicNode2_UltrasonicSensor", "Distance");
            String distance3 = controller.getCurrentValue( "RoofZone_UltrasonicNode3_UltrasonicSensor", "Distance");
            String distance4 = controller.getCurrentValue( "RoofZone_UltrasonicNode4_UltrasonicSensor", "Distance");
            String distance5 = controller.getCurrentValue( "RoofZone_UltrasonicNode5_UltrasonicSensor", "Distance");
            
            String windSpeed = controller.getCurrentValue("RoofZone_WindNode_WindSensor", "Wind Speed");
            
            String ropeAttached1 = controller.getCurrentValue("RoofZone_SmartRopeNode1_SmartRope", "Is Attached");
            String ropeAttached2 = controller.getCurrentValue("RoofZone_SmartRopeNode2_SmartRope", "Is Attached");
            String ropeAttached3 = controller.getCurrentValue("RoofZone_SmartRopeNode3_SmartRope", "Is Attached");
            
            

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
                return;
            }
            
            if(ropeAttached1_float == 0 || ropeAttached2_float == 0 || ropeAttached3_float == 0){
                controller.updateSwitchIn("RoofZone_BuzzerNode", "RoofZone_BuzzerNode_relay", "0","true");
                controller.setFieldIn("RoofZone_SpeakerNode", "RoofZone_SpeakerNode_speaker", "Played Message", "One or more safety ropes not attached!!!!",32);
            }
            else{
                controller.updateSwitchIn("RoofZone_BuzzerNode", "RoofZone_BuzzerNode_relay", "0","false");
                 controller.setFieldIn("RoofZone_SpeakerNode", "RoofZone_SpeakerNode_speaker", "Played Message", "",0);

            }
            
            if(distance1_float<100 || distance2_float < 100 || distance3_float<100 || distance4_float<100 || distance4_float<100 || distance5_float<100){
                controller.updateSwitchIn("RoofZone_BuzzerNode", "RoofZone_BuzzerNode_relay", "0","true");
                controller.setFieldIn("RoofZone_SpeakerNode", "RoofZone_SpeakerNode_speaker", "Played Message", "Worker too close to edge",32);

            }else{
                controller.updateSwitchIn("RoofZone_BuzzerNode", "RoofZone_BuzzerNode_relay", "0","false");
                controller.setFieldIn("RoofZone_SpeakerNode", "RoofZone_SpeakerNode_speaker", "Played Message", "",0);
            }

            if(windSpeed_float < 20){
                controller.updateSwitchIn("RoofZone_ActuatorNode", "RoofZone_ActuatorNode_relay", "0", "true");
            }else{
                controller.updateSwitchIn("RoofZone_ActuatorNode", "RoofZone_ActuatorNode_relay", "0","false");
            }

            Queue<DataPacket> packets = controller.getBufferedDataPackets();
            if(packets.size() > 20){
                controller.clearBufferedDataPackets();
                BulkDataPacket bigBoi = new BulkDataPacket(controller.getParentControlNode().getObject_name(), controller.getCurrentTimestamp());
                bigBoi.addPackets(packets);
                controller.exportState(String.format("Aggregated (%d) data packets to forward to control zone",packets.size()));
                controller.forwardToZones(bigBoi, "Zone2","Zone3","MasterZone");
            }


        };
        
       SlaveZone roofZone = new SlaveZone("RoofZone", runTimeStep, zoneAlgo, RTT_to_Master_Node, BLE_Transmission_Rate, WIFI_Transmission_Rate);
        
       roofZone.addPermittedId("Omar");
       roofZone.addPermittedId("Farrag");
       roofZone.addPermittedId("Mohsen");

       roofZone.addAllSlaveNodes(extraSlaveNodes);
       roofZone.connectToActuationNode(pulleyLift, 0);


        SlaveZone zone2 = new SlaveZone("Zone2", runTimeStep, (uController cont)->{}, RTT_to_Master_Node, BLE_Transmission_Rate,  WIFI_Transmission_Rate);
        SlaveZone zone3 = new SlaveZone("Zone3", runTimeStep, (uController cont)->{}, RTT_to_Master_Node, BLE_Transmission_Rate,  WIFI_Transmission_Rate);

        MasterZone masterZone = new MasterZone("MasterZone", runTimeStep, (uController cont)->{

            Queue<BulkDataPacket> bulkDataPackets = cont.getReceivedBulkDataPackets(true);

            if(!bulkDataPackets.isEmpty()){

                try{
                    FileWriter fileWriter = new FileWriter("logs/Database.csv", true);
                    PrintWriter writer = new PrintWriter(fileWriter,true);
                        
                    for(BulkDataPacket bulkPacket : bulkDataPackets)
                        for(DataPacket packet : bulkPacket.getPackets())
                            writer.println(cont.getCurrentTimestamp() + ","+packet.toString());
                    writer.close();
                    
                } catch (IOException e) {
                    e.printStackTrace();
                }
                        
            }
        }, RTT_to_Master_Node, BLE_Transmission_Rate, WIFI_Transmission_Rate);
                    

        roofZone.connectToZone(zone2, RTT_between_gateways);
        zone2.connectToZone(zone3, RTT_between_gateways);
        zone3.connectToZone(masterZone, RTT_between_gateways);

        simulationObjects.put(roofZone.getObject_name(),roofZone);
        simulationObjects.put(zone2.getObject_name(),zone2);
        simulationObjects.put(zone3.getObject_name(),zone3);
        simulationObjects.put(masterZone.getObject_name(),masterZone);

        while(menu());

    }

    static SlaveNode create_general_slave_node(String nodeName, String deviceName, String fieldName, int fieldSize, int runTimeStep, int RTT_to_Zone_Controller, int BLE_transmission_rate) {
        String fullName = nodeName + "_" + deviceName;
        LowPowerDevice dev = new LowPowerDevice(fullName, runTimeStep, fieldSize);
        
        ProcessingAlgorithm algo = (uController controller)->{
            ExecutionResult result = controller.getField(fullName, fieldName);
            if(result.isSuccess()) controller.publishPacket(result.getReturnedPacket());
        
        };
        uController controller = new uController(nodeName+"_controller", runTimeStep, algo);
        
        controller.connectTo(dev);
        return new SlaveNode(nodeName, runTimeStep, RTT_to_Zone_Controller,BLE_transmission_rate, controller);
    }

    static boolean menu(){

        System.out.println("***************Options*************** ");
        System.out.println("1) Init Fields");
        System.out.println("2) Start Simulation");
        System.out.println("3) End Simulation");
        System.out.print("Your Option (type number only): ");
        
        String userInput = scanner.nextLine();
        try{
            int option = Integer.parseInt(userInput);
            switch(option){
                case 1: initFields(); break;
                case 2: startSimulation(); break;
                case 3: endSimulation(); break;
                default: System.out.println("Invalid option");
            }
            return option != 3;

        }catch(NumberFormatException e){
            System.out.println("Invalid option");
            return true;
        }

    }

    static void initFields(){
        for(Entry<String,Zone> entry : simulationObjects.entrySet()) 
            if (!entry.getValue().isAlive()) entry.getValue().initFields();

        System.out.println("[Fields Initialized]");
    }
   
    static void startSimulation(){
        for(Entry<String,Zone> entry : simulationObjects.entrySet()) 
            if (!entry.getValue().isAlive()) entry.getValue().start();

        System.out.println("[Simulation Started]");   
    }

    static void endSimulation(){
        for(Entry<String,Zone> entry : simulationObjects.entrySet()) 
            if (entry.getValue().isAlive()) entry.getValue().terminate();
        
        System.out.println("[Simulation Ended]");
    }
}
