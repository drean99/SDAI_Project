import Utility.MatrixCell;
import Utility.TimeSlot;
import Utility.Trajectory;

public class TrajectoryPlanner {
    /**
     * Calcola la traiettoria in Default Mode, basandosi su velocità, accelerazione e distanza dall'incrocio.
     */
    public static Trajectory calculateDefaultTrajectory(VehicleAgent vehicle, MatrixCell intersectionCell) {
        // Implementa la logica secondo il paper.
        return new Trajectory();
    }
    
    /**
     * Calcola la traiettoria in MAS Mode (Maximum Arrival Speed) per il veicolo,
     * utilizzando lo slot temporale assegnato.
     */
    public static Trajectory calculateMASTrajectory(VehicleAgent vehicle, MatrixCell intersectionCell, TimeSlot slot) {
        // Implementa la logica MAS mode.
        return new Trajectory();
    }
}
