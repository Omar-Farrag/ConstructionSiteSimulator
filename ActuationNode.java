public class ActuationNode extends Node {
    
    private Actuator actuator;

    public ActuationNode( String object_name, String  outputFileName, int runTimeStep, Actuator actuator){
        super(object_name, outputFileName, runTimeStep);
        this.actuator = actuator;
    }

}
