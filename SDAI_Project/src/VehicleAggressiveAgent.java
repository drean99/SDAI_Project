import java.util.List;
import Utility.Coordinate;
import Utility.Environment;
import Utility.Intersection;
import Utility.SumoConnector;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;

public class VehicleAggressiveAgent extends Agent {
    private String vehicleID;
    // Flag per evitare richieste ripetute
    private boolean requestSent = false;
    // Stato: se il veicolo può procedere o deve rallentare/fermarsi
    private boolean canCross = false;
    // Flag per indicare che il messaggio PASSED è già stato inviato per questo attraversamento
    private boolean passedMessageSent = false;
    
    // Contatore per la priorità; se il veicolo riceve END più volte, aumenta la priorità per la richiesta successiva
    private int priorityLevel = 0;

    // Soglie (in unità della rete SUMO)
    private static final double MAX_THRESHOLD = 30.0;     // oltre questo, resetto lo stato
    private static final double APPROACH_THRESHOLD = 22.0;  // area in cui si invia la richiesta
    private static final double STOP_THRESHOLD = 12.0;      // area critica: se non autorizzato, rallento
    private static final double EXIT_THRESHOLD = 13.0;

    @Override
    protected void setup() {
        vehicleID = getLocalName();
        System.out.println(vehicleID + " - setup aggressivo completato.");
        addBehaviour(new CheckIntersectionBehaviour(this, 200));
        addBehaviour(new IntersectionResponseBehaviour());
    }
    
    private class CheckIntersectionBehaviour extends TickerBehaviour {
        public CheckIntersectionBehaviour(Agent a, long period) {
            super(a, period);
        }
        
        @Override
        protected void onTick() {
            // Ottieni la posizione attuale dal SUMO
            Coordinate pos = SumoConnector.getVehiclePosition(vehicleID);
            // Se troviamo un incrocio (o nodo rilevante) entro MAX_THRESHOLD
            Intersection inter = Environment.findNearbyIntersection(pos, MAX_THRESHOLD);
            if (inter != null) {
                double distance = pos.distance(new Coordinate((int) inter.getX(), (int) inter.getY()));
                System.out.println(vehicleID + " distanza dall'incrocio " + inter.getId() + ": " + distance);
                
                // Se il veicolo si avvicina (entro APPROACH_THRESHOLD) e non ha già inviato la richiesta
                if (distance <= APPROACH_THRESHOLD && !requestSent && !passedMessageSent) {
                    System.out.println(vehicleID + " è nell'area di approccio aggressiva dell'incrocio " + inter.getId() +
                        ". Invio richiesta a " + inter.getAgentName());
                    // Imposta una velocità aggressiva
                    SumoConnector.changeSpeed(vehicleID, 10.0);
                    
                    String message = inferVehicleIntent();
                    inviaRichiestaPassaggio(inter.getAgentName(), message);
                    
                    requestSent = true;
                }
                // Se il veicolo si avvicina troppo (entro STOP_THRESHOLD) e non ha ricevuto autorizzazione
                if (distance <= STOP_THRESHOLD && !canCross) {
                    SumoConnector.changeSpeed(vehicleID, 7.0);
                    System.out.println(vehicleID + " si è avvicinato troppo senza autorizzazione: rallento a 7.0.");
                }
                // Se il veicolo ha attraversato l'incrocio (oltre NEW_EXIT_THRESHOLD), invio PASSED anche se non ha ricevuto GO
                if (distance > EXIT_THRESHOLD && !passedMessageSent && requestSent) {
                    System.out.println(vehicleID + " ha attraversato l'incrocio - invio PASSED.");
                    inviaMessaggioPassato(inter.getAgentName());
                    passedMessageSent = true;
                }
                // Se il veicolo è lontano (oltre MAX_THRESHOLD), resetto tutti i flag per il prossimo ciclo
                if (distance > MAX_THRESHOLD) {
                    requestSent = false;
                    canCross = false;
                    passedMessageSent = false;
                }
            }
        }
    }
    
    // Comportamento per ascoltare le risposte dall'agente Intersection
    private class IntersectionResponseBehaviour extends CyclicBehaviour  {
        @Override
        public void action() {
            ACLMessage msg = receive();
            if (msg != null) {
                String content = msg.getContent().trim();
                System.out.println(vehicleID + " riceve risposta: " + content);
                if (content.equalsIgnoreCase("GO")) {
                    canCross = true;
                    // L'aggressivo mantiene una velocità elevata: ad esempio, 16.9 (massima per aggressiveCar)
                    SumoConnector.changeSpeed(vehicleID, 16.9);
                    System.out.println(vehicleID + " può attraversare, imposto velocità a 16.9.");
                } else if (content.equalsIgnoreCase("END")) {
                    // Se riceve END, anche se non è autorizzato, incrementa il livello di priorità per la prossima richiesta
                    // E resetta lo stato per future richieste.
                    if (!canCross)
                        requestSent = false;
                    priorityLevel++;
                    System.out.println(vehicleID + " ha ricevuto END, incremento priorità a " + priorityLevel +
                        " e resetto lo stato per future richieste.");
                }
            } else {
                block();
            }
        }
    }
    
    /**
     * Invia una richiesta di passaggio all'agente semaforico.
     * Il formato del messaggio è: 
     * "REQUEST_PASS,<vehicleID>,<intent>,<timestamp>,<priority>"
     */
    private void inviaRichiestaPassaggio(String intersectionAgentName, String message) {
        ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
        msg.addReceiver(new jade.core.AID(intersectionAgentName, jade.core.AID.ISLOCALNAME));
        long ts = System.currentTimeMillis();
        String content = "REQUEST_PASS," + vehicleID + "," + message + "," + ts + "," + priorityLevel;
        msg.setContent(content);
        send(msg);
        System.out.println(vehicleID + " invia richiesta di passaggio a " + intersectionAgentName + " con priorità " + priorityLevel);
    }
    
    /**
     * Invia un messaggio "PASSED" all'agente semaforico.
     */
    private void inviaMessaggioPassato(String intersectionAgentName) {
        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        msg.addReceiver(new jade.core.AID(intersectionAgentName, jade.core.AID.ISLOCALNAME));
        String content = "PASSED," + vehicleID;
        msg.setContent(content);
        send(msg);
        // Reset della priorità per il prossimo ciclo
        priorityLevel = 0;
        System.out.println(vehicleID + " invia messaggio PASSED a " + intersectionAgentName);
    }
    
    /**
     * Metodo che inferisce l'intento del veicolo (direzione di partenza e intenzione)
     * basandosi sulla route corrente. Restituisce una stringa del tipo "<startingDirection><intent>".
     */
    private String inferVehicleIntent() {
        // Recupera la route corrente del veicolo tramite l'API TraCI
        // Si supponga che SumoConnector abbia un metodo getVehicleRoute che restituisce una List<Edge>
        List<it.polito.appeal.traci.Edge> route = SumoConnector.getVehicleRoute(vehicleID);
        if (route == null || route.size() < 2) {
            System.out.println(vehicleID + " non dispone di una route sufficiente per inferire l'intento.");
            return "unknown,unknown";
        }
        
        // Per inferire l'intento, usiamo i primi due edge della route
        it.polito.appeal.traci.Edge currentEdge = route.get(0);
        it.polito.appeal.traci.Edge nextEdge = route.get(1);
        
        // Cerchiamo di ottenere le informazioni geometriche dall'Environment
        Utility.Edge envCurrentEdge = Environment.getEdgeByID(currentEdge.getID());
        Utility.Edge envNextEdge = Environment.getEdgeByID(nextEdge.getID());
        if (envCurrentEdge == null || envNextEdge == null) {
            System.out.println(vehicleID + " non ha trovato la geometria per gli edge della route.");
            return "unknown,unknown";
        }
        
        // Calcola il vettore del current edge
        double dx1 = envCurrentEdge.getEnd().getX() - envCurrentEdge.getStart().getX();
        double dy1 = envCurrentEdge.getEnd().getY() - envCurrentEdge.getStart().getY();
        double angle1 = Math.atan2(dy1, dx1);
        
        // Determina la direzione di partenza (cardinale) basata sul vettore del current edge
        String startingDirection = angleToCardinal(angle1);
        
        // Calcola il vettore del next edge
        double dx2 = envNextEdge.getEnd().getX() - envNextEdge.getStart().getX();
        double dy2 = envNextEdge.getEnd().getY() - envNextEdge.getStart().getY();
        double angle2 = Math.atan2(dy2, dx2);
        
        // Calcola la differenza angolare e normalizza in (-pi, pi)
        double angleDiff = angle2 - angle1;
        while (angleDiff > Math.PI) angleDiff -= 2 * Math.PI;
        while (angleDiff < -Math.PI) angleDiff += 2 * Math.PI;
        
        // Imposta una soglia (15° in radianti)
        double threshold = Math.toRadians(15);
        String intent;
        if (Math.abs(angleDiff) < threshold) {
            intent = "straight";
        } else if (angleDiff > 0) {
            intent = "left";
        } else {
            intent = "right";
        }
        
        // Restituisce il risultato nel formato "<startingDirection><intent>"
        return startingDirection + intent;
    }
    
    /**
     * Converte un angolo (in radianti) in una direzione cardinale.
     * L'angolo viene normalizzato in [0, 2π).
     * - [0, π/4) o [7π/4, 2π) → "west"
     * - [π/4, 3π/4) → "south"
     * - [3π/4, 5π/4) → "east"
     * - [5π/4, 7π/4) → "north"
     * Se l'angolo non rientra in nessuna di queste categorie, restituisce "unknown".
     */
    private String angleToCardinal(double angle) {
        // Normalizza l'angolo in [0, 2π)
        if (angle < 0) {
            angle += 2 * Math.PI;
        }
        if (angle < Math.PI / 4 || angle >= 7 * Math.PI / 4) {
            return "west,";
        } else if (angle >= Math.PI / 4 && angle < 3 * Math.PI / 4) {
            return "south,";
        } else if (angle >= 3 * Math.PI / 4 && angle < 5 * Math.PI / 4) {
            return "east,";
        } else if (angle >= 5 * Math.PI / 4 && angle < 7 * Math.PI / 4) {
            return "north,";
        }
        return "unknown,";
    }
}
