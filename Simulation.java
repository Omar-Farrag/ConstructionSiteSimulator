import java.util.ArrayList;
import java.util.Random;

public class Simulation {

    public static void main(String[] args) {
        
        int runTimeStep = 1000; //ms
        
        LowPowerDevice ultrasonicSensor1 = new LowPowerDevice("Ultrasonic1", "logs/Ultrasonic1.csv", runTimeStep, "inputs/ultrasonic1_values.csv",1);
        LowPowerDevice ultrasonicSensor2 = new LowPowerDevice("Ultrasonic2", "logs/Ultrasonic2.csv", runTimeStep, "inputs/ultrasonic2_values.csv",1);
        
        ProcessingAlgorithm controller1Algo = (uController controller)->{
            ExecutionResult result = controller.getField("Ultrasonic1","Distance_gusgsd");
            if(result.isSuccess()){
                DataPacket distance1 = result.getReturnedPacket();
                controller.publishPacket(distance1);
            } 
            
            // DataPacket distance2 = controller.getField("Ultrasonic2","Distasdfance").getReturnedPacket();
            // controller.publishPacket(distance2);
        };

        uController controller1 = new uController("LocalController1", "logs/LocalController1.csv", runTimeStep, controller1Algo);
        controller1.connectTo(ultrasonicSensor1,ultrasonicSensor2);
        
        Node node1 = new Node("Node1", "logs/Node1.csv", runTimeStep,20,controller1);

        
        HighPowerDevice fan = new HighPowerDevice("Fan", "logs/Fan.csv", runTimeStep);
        Relay actuator = new Relay("Actuator", "logs/Actuator.csv", runTimeStep, 3);
        actuator.connectTo(fan, 1);

        ProcessingAlgorithm controller2Algo = (uController controller)->{
            int temp = new Random().nextInt(99);
            if(temp > 40) controller.updateSwitch("Actuator", "5", "ON");
            else controller.updateSwitch("Actuator", "1", "OFF");
        };

        uController controller2 = new uController("LocalController2", "logs/LocalController2.csv", runTimeStep, controller2Algo);
        controller2.connectTo(actuator);

        Node node2 = new Node("Node2", "logs/Node2.csv", runTimeStep,30,controller2);
        

        LowPowerDevice camera = new LowPowerDevice("Camera", "logs/Camera.csv", runTimeStep, "inputs/camera_values.csv", 3500);
        ProcessingAlgorithm controller3Algo = (uController controller)->{
            Node node = controller.getParentNode();
            controller.exportState("Welcome to the playground");
            node.getFieldFrom("Node1", "Ultrasonic1", "Distanceasdfasdf");
            node.setFieldIn("Node1", "Ultrasonic2", "Distance","911");
        };

        uController controller3 = new uController("LocalController3", "logs/LocalController3.csv", runTimeStep, controller3Algo);
        controller3.connectTo(camera);

        Node node3 = new Node("Node3","logs/Node3.csv",runTimeStep,20,controller3);
        node3.connectTo(node1, node2);


        ArrayList<SimulationObject> simulationObjects = new ArrayList<>(); 
        simulationObjects.add(ultrasonicSensor1);
        simulationObjects.add(ultrasonicSensor2);
        simulationObjects.add(controller1);
        simulationObjects.add(node1);
        simulationObjects.add(fan);
        simulationObjects.add(actuator);
        simulationObjects.add(controller2);
        simulationObjects.add(node2);
        simulationObjects.add(camera);
        simulationObjects.add(controller3);
        simulationObjects.add(node3);

        for(SimulationObject object : simulationObjects) object.start();
        
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        
        for(SimulationObject object : simulationObjects) object.terminate();

    
    }
}
