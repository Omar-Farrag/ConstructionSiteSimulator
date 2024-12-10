import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

public class SimulationClock {

    // Singleton instance of the SimulationClock
    private static SimulationClock instance;
    
    // StartTime of the Simulation
    private long startTime;
    
    // Factor representing the relationship between in simulation delays and real-life delays
    // For example, if scale factor is 100, then a 1 second delay in real-life is represented 
    // by 1/100 seconds in the simulation 
    private int scaleFactor = 100;

    private SimulationClock(){
        this.startTime = System.nanoTime();
    }

    public static SimulationClock getInstance() {
        return (instance == null) ? instance = new SimulationClock() : instance;
    }

    public String getCurrentTimeString(){
        double seconds_duration = getCurrentTime();
        return String.format("%.3f",seconds_duration); // Up to 9 decimal places
    }

    public double getCurrentTime(){
        long duration = (System.nanoTime() - startTime) * scaleFactor;                  
        double seconds_duration = duration * 1.0 / 1e9;
        return seconds_duration; // Up to 9 decimal places
    }
    
    public void waitFor(int milliseconds) {
        
        double scaledTimeToWait = milliseconds * 1.0 / scaleFactor;
        long targetWaitTimeInNanos = (long) (scaledTimeToWait * 1_000_000); // Convert to nanoseconds
        
        long startTime = System.nanoTime();
        long endTime = startTime + targetWaitTimeInNanos;

        while (System.nanoTime() < endTime);

        // System.out.println(System.nanoTime()-startTime);

    }

    public void reset(){
        startTime = System.nanoTime();
    }

    public static void main(String[] args) throws IOException, InterruptedException {

        SimulationClock.getInstance();
        String outputLogFileName = "test.txt";
        File file = new File(outputLogFileName); 
        
        // Delete the file if it already exists
        if (file.exists()) file.delete();
        
        // Start the writer
        FileWriter fileWriter = new FileWriter(outputLogFileName, true);
        
        PrintWriter writer = new PrintWriter(fileWriter);
        

        double prev = 0;
        for(int i = 0; i < 100000; i++){
            SimulationClock.getInstance().waitFor(10);
            double now = SimulationClock.getInstance().getCurrentTime();
            writer.println(String.format("%.3f",now-prev));
            prev = now;
            // timestamps.add(SimulationClock.getInstance().getCurrentTime());
        }
        writer.flush();
        writer.close();
    }

}
