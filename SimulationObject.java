import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * The base class of most objects in the simulation
 */
public abstract class SimulationObject implements Runnable {

    // Timestep in ms for the object's lifetime thread. The thread continuously 
    // calls a runtime function representing the object's behavior throughout
    // the simulation. After executing the function, the thread waits for a
    // short duration, runTimeStep, before re-executing the runtime function. Not all
    // simulation objects start a runtime thread
    protected int runTimeStep;

    // Name of the simulation object
    protected String object_name;

    // Name of the output CSV file where all the logs for this object will be stored
    private String outputLogFileName;

    // Writer object responsible for writing to the output log file
    protected PrintWriter writer;

    // Flag indicating whether the header of the CSV file has been added or not
    protected boolean hasAddedHeader;

    // Flag indicating whether the object's thread is alive and running or has been
    // stopped
    private Boolean alive;
        
    /**
     * Constructor
     * @param name Name of the object
     */
    protected SimulationObject(String name){
        this.hasAddedHeader = false;
        this.alive = false;
        this.object_name = name;
        this.outputLogFileName = getOutputFileName(name);
        initWriter();
    }

    /**
     * Function to terminate the object's thread
     */
    public void terminate(){
        synchronized(alive){
            // Update the alive flag so that the runtime thread terminates in its next iteration
            alive = false;
        }
        synchronized(writer){
            // flush any remaining content
            writer.flush();
            
            // close the writer
            writer.close();
        }
    }
    
    /**
     * Getter
     * @return true if object's runtime thread is still running, false otherwise
     */
    public boolean isAlive(){
        synchronized(alive){
            return alive;
        }
    }
    
    /**
     * Function to create an output log file and open the writer object
     */
    private void initWriter(){
        try {
            File file = new File(outputLogFileName); 

            // Delete the file if it already exists
            if (file.exists()) file.delete();

            // Start the writer
            FileWriter fileWriter = new FileWriter(outputLogFileName, true);
            writer = new PrintWriter(fileWriter);
            hasAddedHeader = false;
        } catch (IOException e) {
            System.out.println("An error occurred while appending to the file.");
            e.printStackTrace();
        }
    }
    
    /**
     * Function to start the object to be implemented by all children
     */
    public abstract void start();

    /**
     * Function to start the object. Starting an already started object has no effect
     * @param startThread flag indicating whether a runtime thread should be started or not.
     * @param runTimeStep timestep for the runtime thread's execution. The runtime thread continuously
     * calls the runtime function with a delay of runTimeStep ms between successive executions
     */
    protected void start(boolean startThread, int runTimeStep){
        synchronized(alive){

            // Return if object is already started
            if(alive) return;

            this.alive = true;
            
            // Start thread
            if (startThread) {
                this.runTimeStep = runTimeStep;
                Thread runTimeThread = new Thread(this);
                runTimeThread.start();
            }
        }
    }

    /**
     * Run function called by the newly created thread
     */
    @Override
    public void run(){
        // While thread is alive
        while(isAlive()){

            // Call the runtime function specified for the object
            runTimeFunction();
            
            // Wait the timestep
            // Thread.sleep(runTimeStep);
            SimulationClock.getInstance().waitFor(runTimeStep);
        }
    }

    /**
     * Utility function to generate the output log file name using the object's name
     * @param name object's name for which the output log file name is to be generated
     * @return output file name
     */
    protected static String getOutputFileName(String name){
        return "logs/"+ name+ "_output.csv";
        
    }

    /**
     * Function to write a log record to the output log file. Default implementation
     * @param event optional parameter to specify a certain event message in the log record 
     */
    public void exportState(String... event) {

        synchronized(writer){
            // Add header if not added already
            if(!hasAddedHeader){
                writer.println("Timestamp,Object Name,Event");
                hasAddedHeader = true;
            }

            // Write the log record
            writer.println(getCurrentTimestamp()+"," + object_name + "," + event[0]);
        }
        
    }

    /**
     * Getter
     * @return object name
     */
    public String getObject_name() {
        return object_name;
    }

    /**
     * Get current timestamp
     * @return current timestamp
     */
    public String getCurrentTimestamp(){
        return SimulationClock.getInstance().getCurrentTimeString();
        // return LocalDateTime.now().toString();
    }
    
    /**
     * Abstract function to be implemented by every child of this class.
     * This function is the function continuously called by the runTimeThread
     * in runTimeStep intervals
     */
    protected abstract void runTimeFunction();

}
