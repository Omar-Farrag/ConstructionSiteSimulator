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
    protected SimulationObject(String name, String outputLogFileName, int runTimeStep){
        this.runTimeThread = new Thread(this);
        this.hasAddedHeader = false;
        this.alive = false;
        this.object_name = name;
        this.outputLogFileName = outputLogFileName;
        this.runTimeStep = runTimeStep;
        initWriter();
    }

    public void terminate(){
        synchronized(alive){
            alive = false;
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

    public String getObject_name() {
        return object_name;
    }

    public abstract void exportState();
    public abstract void runTimeFunction();

}
