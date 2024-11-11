import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Scanner;

public class Simulation {

    static Scanner scanner = new Scanner(System.in);
    static HashMap<String, SimulationObject> simulationObjects = new HashMap<>(); 

    public static void main(String[] args) {
        
        int runTimeStep = 1000; //ms
        
        LowPowerDevice ultrasonicSensor1 = new LowPowerDevice("Ultrasonic1", "logs/Ultrasonic1.csv", runTimeStep, "inputs/ultrasonic1_values.csv",1);
        LowPowerDevice ultrasonicSensor2 = new LowPowerDevice("Ultrasonic2", "logs/Ultrasonic2.csv", runTimeStep, "inputs/ultrasonic2_values.csv",1);
        
        ProcessingAlgorithm controller1Algo = (uController controller)->{
            ExecutionResult result1 = controller.getField("Ultrasonic1","Distance");
            ExecutionResult result2 = controller.getField("Ultrasonic1","Hala");
            ExecutionResult result3 = controller.getField("Ultrasonic1","Hala2");
            if(result1.isSuccess()){
                DataPacket distance1 = result1.getReturnedPacket();
                controller.publishPacket(distance1);
            } 
            if(result2.isSuccess()){
                DataPacket hala = result2.getReturnedPacket();
                controller.publishPacket(hala);

            } 
            if(result3.isSuccess()){
                DataPacket hala2 = result3.getReturnedPacket();
                controller.publishPacket(hala2);

            } 
            
            // DataPacket distance2 = controller.getField("Ultrasonic2","Distasdfance").getReturnedPacket();
            // controller.publishPacket(distance2);
        };

        // Gateway gateway1 = new Gateway("Gateway1", "logs/gateway1.csv", runTimeStep);

        uController controller1 = new uController("LocalController1", "logs/LocalController1.csv", runTimeStep, controller1Algo);
        controller1.connectTo(ultrasonicSensor1,ultrasonicSensor2);
        // controller1.connectTo(gateway1);

        SlaveNode node1 = new SlaveNode("Node1", "logs/Node1.csv", runTimeStep,20,controller1);

        
        HighPowerDevice fan = new HighPowerDevice("Fan", "logs/Fan.csv", runTimeStep);
        Relay actuator = new Relay("Actuator", "logs/Actuator.csv", runTimeStep, 3);
        actuator.connectTo(fan, 1);

        ProcessingAlgorithm controller2Algo = (uController controller)->{
            int temp = new Random().nextInt(99);
            if(temp > 40) controller.updateSwitch("Actuator", "1", "ON");
            else controller.updateSwitch("Actuator", "1", "OFF");
            String[] route = {"Gateway3"};
            controller.forward(new DataPacket("TempSensor", "Temp", String.valueOf(temp), 8, ""),String.join(",", route));
        };
        Gateway gateway2 = new Gateway("Gateway2", "logs/gateway2.csv", runTimeStep);
        uController controller2 = new uController("LocalController2", "logs/LocalController2.csv", runTimeStep, controller2Algo);
        controller2.connectTo(actuator);
        controller2.connectTo(gateway2);

        ControlNode node2 = new ControlNode("Node2", "logs/Node2.csv", runTimeStep,controller2);
        

        LowPowerDevice camera = new LowPowerDevice("Camera", "logs/Camera.csv", runTimeStep, "inputs/camera_values.csv", 3500);
        ProcessingAlgorithm controller3Algo = (uController controller)->{
            ControlNode node = controller.getParentControlNode();
            controller.exportState("Welcome to the playground");
            // node.getFieldFrom("Node1", "Ultrasonic1", "Distance");
            // node.setFieldIn("Node1", "Ultrasonic2", "Distance","911");S
        };

        Gateway gateway3 = new Gateway("Gateway3", "logs/gateway3.csv", runTimeStep);
        
        uController controller3 = new uController("LocalController3", "logs/LocalController3.csv", runTimeStep, controller3Algo);
        controller3.connectTo(camera);
        controller3.connectTo(gateway3);

        ControlNode node3 = new ControlNode("Node3","logs/Node3.csv",runTimeStep,controller3);
        node3.subscribeTo(node1);


        // gateway1.connectTo(gateway2,gateway3);
        gateway2.connectTo(gateway3, 300);
        gateway3.connectTo(gateway2, 300);

        simulationObjects.put(ultrasonicSensor1.getObject_name(),ultrasonicSensor1);
        simulationObjects.put(ultrasonicSensor2.getObject_name(),ultrasonicSensor2); 
        // simulationObjects.put(gateway1.getObject_name(),gateway1); 
        simulationObjects.put(controller1.getObject_name(),controller1);
        simulationObjects.put(node1.getObject_name(),node1);
        simulationObjects.put(fan.getObject_name(),fan);
        simulationObjects.put(actuator.getObject_name(),actuator);
        simulationObjects.put(gateway2.getObject_name(),gateway2); 
        simulationObjects.put(controller2.getObject_name(),controller2);
        simulationObjects.put(node2.getObject_name(),node2);
        simulationObjects.put(camera.getObject_name(),camera);
        simulationObjects.put(gateway3.getObject_name(),gateway3); 
        simulationObjects.put(controller3.getObject_name(),controller3);
        simulationObjects.put(node3.getObject_name(),node3);

        

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
            System.out.print("Invalid option");
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
