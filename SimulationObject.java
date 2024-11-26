import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.io.PrintWriter;

public abstract class SimulationObject implements Runnable {

    protected int runTimeStep; // in milliseconds
    protected String object_name;
    private Thread runTimeThread;
    private String outputLogFileName;
    protected PrintWriter writer;
    protected boolean hasAddedHeader;
    private Boolean alive;
    
    public String getCurrentTimestamp(){
        return LocalDateTime.now().toString();
    }
    
    protected SimulationObject(String name, int runTimeStep){
        this.runTimeThread = new Thread(this);
        this.hasAddedHeader = false;
        this.alive = false;
        this.object_name = name;
        this.outputLogFileName = getOutputFileName(name);
        this.runTimeStep = runTimeStep;
        initWriter();
    }

    public void terminate(){
        synchronized(alive){
            alive = false;
        }
        synchronized(writer){
            writer.close();
        }
    }
    
    public boolean isAlive(){
        synchronized(alive){
            return alive;
        }
    }
    
    private void initWriter(){
        try {
            File file = new File(outputLogFileName); // Change the path as needed
            if (file.exists()) file.delete();

            FileWriter fileWriter = new FileWriter(outputLogFileName, true);
            writer = new PrintWriter(fileWriter,true);
            hasAddedHeader = false;
        } catch (IOException e) {
            System.out.println("An error occurred while appending to the file.");
            e.printStackTrace();
        }
    }
    
    public void start(){
        this.alive = true;
        runTimeThread.start();
    }

    @Override
    public void run(){
        while(isAlive()){
            runTimeFunction();
            try {
                Thread.sleep(runTimeStep);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    protected static String getOutputFileName(String name){
        return "logs/"+ name+ "_output.csv";
        
    }

    protected static String getInputFileName(String name) {
        return "inputs/"+ name+ "_input.csv";
    }

    public String getObject_name() {
        return object_name;
    }

    public void exportState(String... event) {

        synchronized(writer){

            if(!hasAddedHeader){
                writer.println("Timestamp,Object Name,Event");
                hasAddedHeader = true;
            }
            writer.println(getCurrentTimestamp()+"," + object_name + "," + event[0]);
        }
        
    }
    
    protected abstract void runTimeFunction();

}
