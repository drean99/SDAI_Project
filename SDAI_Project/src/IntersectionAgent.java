import Utility.MatrixCell;
import Utility.CellType;
import Utility.Reservation;
import Utility.ReservationTable;
import Utility.TimeSlot;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;

public class IntersectionAgent extends Agent {
    private String intersectionID;
    private ReservationTable reservationTable;
    // La cella dell'incrocio può essere determinata in base all'ID o impostata separatamente
    private MatrixCell cella;
    
    protected void setup() {
        // Inizializzazione dell'incrocio
        intersectionID = getLocalName(); // per esempio "IntersectionAgent_3_4"
        reservationTable = new ReservationTable();
        
        // In una versione più completa, potremmo estrarre la cella dall'ID oppure
        // riceverla dall'ambiente. Qui la impostiamo come dummy.
        cella = new MatrixCell(5, 5, CellType.INTERSECTION); // esempio
        
        addBehaviour(new ProcessReservationBehaviour());
    }
    
    private class ProcessReservationBehaviour extends CyclicBehaviour {
        public void action() {
            ACLMessage msg = receive();
            if (msg != null) {
                // Estrai la richiesta e verifica la disponibilità applicando le policy di precedenza
                Reservation richiesta = parseReservation(msg.getContent());
                boolean disponibile = verificaDisponibilità(richiesta);
                ACLMessage reply = msg.createReply();
                if (disponibile) {
                    reservationTable.add(richiesta);
                    reply.setPerformative(ACLMessage.CONFIRM);
                    reply.setContent("Prenotazione confermata per " + richiesta.getVehicleID());
                } else {
                    reply.setPerformative(ACLMessage.REFUSE);
                    reply.setContent("Prenotazione rifiutata per " + richiesta.getVehicleID());
                }
                send(reply);
            } else {
                block();
            }
        }
    }
    
    /**
     * Verifica la disponibilità della richiesta confrontandola con le prenotazioni esistenti.
     * La logica qui dovrebbe includere:
     *   1. Policy basata sulla priorità: una richiesta con priorità maggiore (ad esempio, CnR)
     *      deve essere considerata prima.
     *   2. Policy basata sulla corsia: se nella stessa corsia esiste già un veicolo in attesa,
     *      la richiesta viene rifiutata.
     *   3. Policy FCFS: se le richieste hanno la stessa priorità e sono in corsie diverse,
     *      viene servita quella arrivata per prima.
     * La logica specifica è delegata a ReservationTable.checkAvailability().
     */
    public boolean verificaDisponibilità(Reservation richiesta) {
        return reservationTable.checkAvailability(richiesta);
    }
    
    /**
     * Interpreta il contenuto del messaggio per creare un oggetto Reservation.
     * Formato atteso: "vehicleID,priority,arrivalLane,turningIntention,startTime,endTime,cellRow,cellCol"
     */
    private Reservation parseReservation(String content) {
        String[] parts = content.split(",");
        if (parts.length < 8) {
            // Fallback: se il formato non è corretto, creiamo una Reservation di default usando:
            // - vehicleID: il contenuto intero come identificativo,
            // - cell: la cella corrente dell'incrocio (cella dummy impostata in setup),
            // - timeSlot: default [0,0],
            // - priority: 0, arrivalLane: "unknown", turningIntention: "straight".
            return new Reservation(
                content,
                cella,
                new TimeSlot(0, 0),
                0,
                "unknown",
                "straight"
            );
        } else {
            String vehicleID = parts[0].trim();
            int priority = Integer.parseInt(parts[1].trim());
            String arrivalLane = parts[2].trim();
            String turningIntention = parts[3].trim();
            double startTime = Double.parseDouble(parts[4].trim());
            double endTime = Double.parseDouble(parts[5].trim());
            int cellRow = Integer.parseInt(parts[6].trim());
            int cellCol = Integer.parseInt(parts[7].trim());
            
            TimeSlot timeSlot = new TimeSlot(startTime, endTime);
            MatrixCell cell = new MatrixCell(cellRow, cellCol, CellType.ROAD); // oppure il tipo appropriato
            
            // Costruisce la Reservation con tutte le informazioni necessarie
            return new Reservation(vehicleID, cell, timeSlot, priority, arrivalLane, turningIntention);
        }
    }
}
