package Utility;

public class Reservation {
    private String vehicleID;
    private MatrixCell cell; // La cella (ad es. l'incrocio) interessata
    private TimeSlot timeSlot;
    private int priority; // 0 = normale, 1 = CnR (priorità maggiore)
    private String arrivalLane;
    private String turningIntention;
    
    public Reservation(String vehicleID, MatrixCell cell, TimeSlot timeSlot, int priority, String arrivalLane, String turningIntention) {
        this.vehicleID = vehicleID;
        this.cell = cell;
        this.timeSlot = timeSlot;
        this.priority = priority;
        this.arrivalLane = arrivalLane;
        this.turningIntention = turningIntention;
    }
    
    public String getVehicleID() { return vehicleID; }
    public MatrixCell getCell() { return cell; }
    public TimeSlot getTimeSlot() { return timeSlot; }
    public int getPriority() { return priority; }
    public String getArrivalLane() { return arrivalLane; }
    public String getTurningIntention() { return turningIntention; }
    
    /**
     * Verifica se questa prenotazione confligge con un'altra.
     * Il conflitto avviene se si tratta della stessa cella e gli intervalli temporali si sovrappongono.
     */
    public boolean conflictsWith(Reservation other) {
        if (!this.cell.equals(other.cell)) return false;
        return this.timeSlot.overlaps(other.timeSlot);
    }
}
