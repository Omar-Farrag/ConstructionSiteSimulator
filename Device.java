public abstract class Device extends SimulationObject{
    
    public Device(String name, int runTimeStep){
        super(name, runTimeStep);
    }

    public void initFields(){}
    public abstract ExecutionResult execute(String command, String... arguments);


}
