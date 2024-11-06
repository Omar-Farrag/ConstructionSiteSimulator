
public class UltrasonicSensor extends Sensor {

    public UltrasonicSensor(String name, String outputFileName, int runTimeStep, String inputFileName){
        super(name, outputFileName, runTimeStep,inputFileName);
    }
    
    @Override
    protected void initFields() {
        fields.put("Distance",Float.valueOf(0));
    }

    @Override
    public void runTimeFunction() {
        synchronized(fields){
            // Set a new value for fields
            String distance_string = simulationValues.get(valueIndex).get(0);
            if(!distance_string.equals("MAINTAIN")){
                Float distance = Float.parseFloat(simulationValues.get(valueIndex).get(0));
                fields.put("Distance", distance);
            }

            valueIndex = (valueIndex + 1) % simulationValues.size();
        }
        exportState();
    }

}
