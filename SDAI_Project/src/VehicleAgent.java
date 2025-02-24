import java.util.List;
import java.util.ArrayList;
import java.util.Random;

import Utility.Coordinate;
import Utility.Trajectory;
import Utility.MatrixMap;
import Utility.MatrixCell;
import Utility.SumoConnector;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;

public class VehicleAgent extends Agent {
    private String vehicleID;
    private Coordinate posizioneCorrente;
    private double velocità; // simulazione della velocità
    private List<Coordinate> rotta;
    private Trajectory currentTrajectory;
    private Random random = new Random();
    
    // Riferimento alla mappa condivisa
    private MatrixMap matrixMap;
    
    protected void setup() {
        vehicleID = getLocalName();
        matrixMap = EnvironmentAgent.sharedMap;
        if(matrixMap == null) {
            System.out.println(vehicleID + " non ha trovato la mappa condivisa!");
            doDelete();
            return;
        }
        
        // Usa la lista dei punti START, non più ENTRY_EXIT
        List<MatrixCell> startingPoints = matrixMap.getStartingPoints();
        
        // Sceglie casualmente un punto di partenza tra quelli START
        MatrixCell startCell = startingPoints.get(random.nextInt(startingPoints.size()));
        posizioneCorrente = new Coordinate(startCell.getRow(), startCell.getCol());
        
        // Pianifica la rotta: partenza -> centro -> destinazione (corrispondente ARRIVAL opposto)
        pianificaRotta(startCell, startingPoints);
        
        addBehaviour(new UpdateStateBehaviour());
        addBehaviour(new ReservationRequestBehaviour());
        
        System.out.println(vehicleID + " partito da: " + posizioneCorrente + " e rotta: " + rotta);
    }
    
    

        /**
         * Pianifica la rotta in base al punto di partenza.
         * La logica:
         * - Se il veicolo parte da un bordo orizzontale (col == 0 o col == cols-1):
         *      • Se parte da OVEST (col==0), deve andare verso EST: la rotta sarà:
         *           [partenza, (stessa riga, cols/2), (stessa riga, cols-1)] 
         *           (dove (stessa riga, cols-1) è un punto ARRIVAL)
         *      • Se parte da EST (col==cols-1), deve andare verso OVEST:
         *           [partenza, (stessa riga, cols/2), (stessa riga, 0)]
         * - Se il veicolo parte da un bordo verticale (row == 0 o row == rows-1):
         *      • Se parte da NORD (row==0), deve andare verso SUD:
         *           [partenza, (rows/2, stessa colonna), (rows-1, stessa colonna)]
         *      • Se parte da SUD (row==rows-1), deve andare verso NORD:
         *           [partenza, (rows/2, stessa colonna), (0, stessa colonna)]
         */
        private void pianificaRotta(MatrixCell startCell, List<MatrixCell> startingPoints) {
            rotta = new ArrayList<>();
            int rows = matrixMap.getRows();
            int cols = matrixMap.getCols();
            rotta.add(new Coordinate(startCell.getRow(), startCell.getCol()));
            Coordinate center = null;
            Coordinate destination = null;
            int r = startCell.getRow();
            int c = startCell.getCol();
            
            if(c == 0) { // Partenza da OVEST
                center = new Coordinate(r, cols/2);
                destination = new Coordinate(r, 0 + (cols - 1)); // destinazione: lato EST
                // Tuttavia, per garantire coerenza scegliamo la cella ARRIVAL corrispondente: 
                // se start è (h2,0) allora destination sarà (h2,cols-1)
                destination = new Coordinate(r, cols - 1);
            } else if(c == cols - 1) { // Partenza da EST
                center = new Coordinate(r, cols/2);
                destination = new Coordinate(r, 0);
            } else if(r == 0) { // Partenza da NORD
                center = new Coordinate(rows/2, c);
                destination = new Coordinate(rows - 1, c);
            } else if(r == rows - 1) { // Partenza da SUD
                center = new Coordinate(rows/2, c);
                destination = new Coordinate(0, c);
            } else {
                center = new Coordinate(rows/2, cols/2);
                destination = new Coordinate(0, 0);
            }
            rotta.add(center);
            rotta.add(destination);
        }

    
    /**
     * Invia una richiesta di prenotazione all'IntersectionAgent relativo al centro dell'incrocio.
     * Il nome dell'agente incrocio è formato come "IntersectionAgent_5_5" (senza parentesi).
     */
    public void inviaRichiestaPrenotazione(Coordinate prossimoIncrocio) {
        ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
        String agentName = "IntersectionAgent_" + prossimoIncrocio.getX() + "_" + prossimoIncrocio.getY();
        msg.addReceiver(new AID(agentName, AID.ISLOCALNAME));
        // Formato del messaggio: "vehicleID,priority,arrivalLane,turningIntention,startTime,endTime,cellRow,cellCol"
        String content = vehicleID + ",0,unknown,straight,0,10," + prossimoIncrocio.getX() + "," + prossimoIncrocio.getY();
        msg.setContent(content);
        send(msg);
        System.out.println(vehicleID + " invia richiesta di prenotazione per incrocio in " + prossimoIncrocio);
    }
    
    /**
     * Comportamento per aggiornare lo stato del veicolo, simulando il movimento lungo la rotta.
     * Dopo aver aggiornato la posizione interna, viene chiamato SumoConnector per aggiornare SUMO.
     */
    private class UpdateStateBehaviour extends TickerBehaviour {
        public UpdateStateBehaviour() { super(VehicleAgent.this, 100); }
        protected void onTick() {
            if(rotta != null && !rotta.isEmpty()){
                Coordinate prossimoWaypoint = rotta.get(0);
                if(posizioneCorrente.equals(prossimoWaypoint)) {
                    rotta.remove(0);
                    if(isIncrocio(prossimoWaypoint)) {
                        inviaRichiestaPrenotazione(prossimoWaypoint);
                    }
                } else {
                    posizioneCorrente = moveTowards(posizioneCorrente, prossimoWaypoint);
                    System.out.println(vehicleID + " si muove verso " + prossimoWaypoint + " attuale: " + posizioneCorrente);
                    // Aggiorna la posizione in SUMO
                    SumoConnector.updateVehiclePosition(vehicleID, posizioneCorrente);
                }
            }
        }
    }
    
    private boolean isIncrocio(Coordinate coord) {
        int centerRow = matrixMap.getRows() / 2;
        int centerCol = matrixMap.getCols() / 2;
        return (coord.getX() == centerRow && coord.getY() == centerCol);
    }
    
    /**
     * Movimento semplificato: il veicolo si sposta di un'unità verso il waypoint target.
     */
    private Coordinate moveTowards(Coordinate current, Coordinate target) {
        int x = current.getX();
        int y = current.getY();
        if(x < target.getX()) x++;
        else if(x > target.getX()) x--;
        if(y < target.getY()) y++;
        else if(y > target.getY()) y--;
        return new Coordinate(x, y);
    }
    
    private class ReservationRequestBehaviour extends CyclicBehaviour {
        public void action() {
            ACLMessage msg = receive();
            if(msg != null) {
                String content = msg.getContent();
                System.out.println(vehicleID + " riceve risposta: " + content);
            } else {
                block();
            }
        }
    }
}
