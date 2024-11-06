import java.util.ArrayList;
import java.util.Random;

public class Simulation {

    public static void main(String[] args) {
        
        int runTimeStep = 1000; //ms
        
        UltrasonicSensor ultrasonicSensor1 = new UltrasonicSensor("Ultrasonic1", "logs/Ultrasonic1.csv", runTimeStep, "inputs/ultrasonic1_values.csv");
        UltrasonicSensor ultrasonicSensor2 = new UltrasonicSensor("Ultrasonic2", "logs/Ultrasonic2.csv", runTimeStep, "inputs/ultrasonic2_values.csv");
        
        ProcessingAlgorithm controller1Algo = (uController controller)->{
            DataPacket distance1 = controller.queryField("Ultrasonic1","Distance");
            DataPacket distance2 = controller.queryField("Ultrasonic2","Distance");

            controller.publishPacket(distance1);
            controller.publishPacket(distance2);
        };

        uController controller1 = new uController("LocalController1", "logs/LocalController1.csv", runTimeStep, controller1Algo);
        controller1.connectTo(ultrasonicSensor1,ultrasonicSensor2);
        
        Node node1 = new Node("Node1", "logs/Node1.csv", runTimeStep,20,controller1);

        
        HighPowerDevice fan = new HighPowerDevice("Fan", "logs/Fan.csv", runTimeStep);
        Actuator actuator = new Actuator("Actuator", "logs/Actuator.csv", runTimeStep, 3);
        actuator.connectTo(fan, 1);

        ProcessingAlgorithm controller2Algo = (uController controller)->{
            int temp = new Random().nextInt(99);
            if(temp > 40) controller.updateSwitch("Actuator", "1", "ON");
            else controller.updateSwitch("Actuator", "1", "OFF");
        };

        uController controller2 = new uController("LocalController2", "logs/LocalController2.csv", runTimeStep, controller2Algo);
        controller2.connectTo(actuator);

        Node node2 = new Node("Node2", "logs/Node2.csv", runTimeStep,30,controller2);
        
        ArrayList<SimulationObject> simulationObjects = new ArrayList<>(); 
        simulationObjects.add(ultrasonicSensor1);
        simulationObjects.add(ultrasonicSensor2);
        simulationObjects.add(controller1);
        simulationObjects.add(node1);
        simulationObjects.add(fan);
        simulationObjects.add(actuator);
        simulationObjects.add(controller2);
        simulationObjects.add(node2);

        for(SimulationObject object : simulationObjects) object.start();
        
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        
        for(SimulationObject object : simulationObjects) object.terminate();

    
    }
}
