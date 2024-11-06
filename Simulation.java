public class Simulation {

    public static void main(String[] args) {
        
        HighPowerDevice dev1 = new HighPowerDevice("Bombaster1", "logs/Bombaster1.csv", 500);
        HighPowerDevice dev2 = new HighPowerDevice("Bombaster2", "logs/Bombaster2.csv", 500);
        HighPowerDevice dev3 = new HighPowerDevice("Bombaster3", "logs/Bombaster3.csv", 500);

        Actuator node = new Actuator("Actuation_Node", "logs/Actuation_Node.csv", 100, 3);

        node.connectTo(dev1, 0);
        node.connectTo(dev2, 1);
        node.connectTo(dev3, 2);


        ProcessingAlgorithm algo = (uController controller) -> {
            Device actuator = controller.getDevices().get(0);
            String status = "ON";

            actuator.execute("Switch", "0", status);
            controller.setLastExecutedCommand("Turned " + status + " Switch 0");
            controller.exportState();
            
            actuator.execute("Switch", "1", status);
            controller.setLastExecutedCommand("Turned " + status + " Switch 1");
            controller.exportState();
            
            actuator.execute("Switch", "2", status);
            controller.setLastExecutedCommand("Turned " + status + " Switch 2");
            controller.exportState();

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            status = "OFF";

            actuator.execute("Switch", "0", status);
            controller.setLastExecutedCommand("Turned " + status + " Switch 0");
            controller.exportState();
            
            actuator.execute("Switch", "1", status);
            controller.setLastExecutedCommand("Turned " + status + " Switch 1");
            controller.exportState();
            
            actuator.execute("Switch", "2", status);
            controller.setLastExecutedCommand("Turned " + status + " Switch 2");
            controller.exportState();
            
        };

        uController controller = new uController("uCont1", "logs/uCont1.csv", 1000,algo);
        controller.connectTo(node);

        dev1.start();
        dev2.start();
        dev3.start();
        node.start();
        controller.start();
        
        try {
    //         Thread.sleep(750);
    //         node.execute("Switch","0", "ON");
    //         Thread.sleep(1000);
    //         node.execute("Switch","1", "ON");
    //         Thread.sleep(1000);
    //         node.execute("Switch","2", "ON");

    //         Thread.sleep(1000);
    //         node.execute("Switch", "0", "OFF");
    //         Thread.sleep(1000);
    //         node.execute("Switch", "1", "OFF");
    //         Thread.sleep(1000);
    //         node.execute("Switch", "2", "OFF");

    //         Thread.sleep(1000);
    //         node.execute("Switch","0", "ON");
    //         Thread.sleep(1000);
    //         node.execute("Switch", "1", "OFF");
    //         Thread.sleep(1000);
    //         node.execute("Switch","2", "ON");
            
    //         Thread.sleep(1000);
    //         node.disconnect(2);

    //         Thread.sleep(3000);
            Thread.sleep(10000);
            


        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        dev1.terminate();
        dev2.terminate();
        dev3.terminate();
        node.terminate();
        controller.terminate();

    
    }
}
