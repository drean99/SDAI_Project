package Utility;
public class TimeSlot {
    private double startTime;
    private double endTime;
    
    public TimeSlot(double startTime, double endTime) {
        this.startTime = startTime;
        this.endTime = endTime;
    }
    
    public double getStartTime() { return startTime; }
    public double getEndTime() { return endTime; }
    
    /**
     * Due TimeSlot si sovrappongono se il tempo d'inizio di uno è inferiore al tempo di fine dell'altro
     * e viceversa.
     */
    public boolean overlaps(TimeSlot other) {
        return (this.startTime < other.endTime) && (this.endTime > other.startTime);
    }
    
    @Override
    public String toString() {
        return "[" + startTime + ", " + endTime + "]";
    }
}
