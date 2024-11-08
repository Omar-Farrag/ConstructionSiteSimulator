public abstract class Device extends SimulationObject{
    
    public Device(String name, String outputFileName, int runTimeStep){
        super(name, outputFileName, runTimeStep);
    }

    public abstract ExecutionResult execute(String command, String... arguments);


}
