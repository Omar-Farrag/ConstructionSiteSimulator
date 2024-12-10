public class SimulationClock {

    // Singleton instance of the SimulationClock to ensure only one instance exists
    private static SimulationClock instance;
    
    // Start time of the simulation in nanoseconds (initialized when the clock is created)
    private long startTime;
    
    // Factor representing the relationship between simulation delays and real-life delays
    // For example, if scale factor is 100, a 1 second delay in real life is represented by 1/100 seconds in the simulation
    private int scaleFactor = 1;

    // Private constructor to initialize the start time with the current nano time
    private SimulationClock(){
        this.startTime = System.nanoTime();
    }

    // Public static method to get the singleton instance of the SimulationClock
    public static SimulationClock getInstance() {
        // Create the instance if it doesn't exist, ensuring only one instance is created (Singleton Pattern)
        return (instance == null) ? instance = new SimulationClock() : instance;
    }

    // Method to return the current time in the simulation as a formatted string with 3 decimal places
    public String getCurrentTimeString(){
        double seconds_duration = getCurrentTime();
        return String.format("%.3f",seconds_duration); // Format to 3 decimal places
    }

    // Method to return the current time in seconds in the simulation (scaled by the scaleFactor)
    public double getCurrentTime(){
        // Calculate the duration since startTime and scale it by the scaleFactor
        long duration = (System.nanoTime() - startTime) * scaleFactor; 
        // Convert the duration to seconds and return it
        double seconds_duration = duration * 1.0 / 1e9;
        return seconds_duration; // Returns time in seconds (scaled by factor)
    }
    
    // Method to wait for a specified number of milliseconds, scaled by the scaleFactor
    public void waitFor(int milliseconds) {
        
        // Scale the time to wait by the scaleFactor (making the simulation time faster/slower)
        double scaledTimeToWait = milliseconds * 1.0 / scaleFactor;
        // Convert the scaled time to wait from milliseconds to nanoseconds
        long targetWaitTimeInNanos = (long) (scaledTimeToWait * 1_000_000); 
        
        // Store the current time in nanoseconds
        long startTime = System.nanoTime();
        // Calculate the target time for when to stop waiting
        long endTime = startTime + targetWaitTimeInNanos;

        // Busy-wait loop to keep the thread alive until the target time is reached
        while (System.nanoTime() < endTime); // Do nothing, just wait
    }

    // Method to reset the start time of the simulation clock to the current time
    public void reset(){
        startTime = System.nanoTime();
    }

    public void setScaleFactor(int scaleFactor) {
        this.scaleFactor = scaleFactor;
    }
}
