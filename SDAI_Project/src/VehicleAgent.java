import java.util.List;

import Utility.Coordinate;
import Utility.Environment;
import Utility.Intersection;
import Utility.SumoConnector;
import it.polito.appeal.traci.Edge;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;

public class VehicleAgent extends Agent {
    private String vehicleID;
    // Flag per evitare richieste ripetute
    private boolean requestSent = false;
    // Flag che indica se il veicolo può procedere o deve fermarsi
    private boolean canCross = false;
    // Flag che indica se il veicolo ha già attraversato l'incrocio
    private boolean hasPassed = false;
    
    // Contatore per la priorità; se il veicolo riceve END più volte, aumenta la priorità per la richiesta successiva
    private int priorityLevel = 0;

    // Soglie (in unità della rete SUMO)
    private static final double MAX_THRESHOLD = 30.0;
    private static final double APPROACH_THRESHOLD = 22.0;
    private static final double STOP_THRESHOLD = 12.0;
    // Nuova soglia per determinare se il veicolo ha superato l'incrocio
    private static final double EXIT_THRESHOLD = 16.0;

    
    @Override
    protected void setup() {
        vehicleID = getLocalName();
        System.out.println(vehicleID + " - setup completato.");
        addBehaviour(new CheckIntersectionBehaviour(this, 300));
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
            
            // Cerca l'incrocio usando una soglia più ampia per permettere anche il reset
            Intersection inter = Environment.findNearbyIntersection(pos, MAX_THRESHOLD);
            if (inter != null) {
                double distance = pos.distance(new Coordinate((int) inter.getX(), (int) inter.getY()));
                // Se il veicolo è entro la soglia di approccio e non ha ancora inviato la richiesta
                if (distance <= APPROACH_THRESHOLD && !requestSent) {
                    System.out.println(vehicleID + " è nell'area di approccio dell'incrocio " + inter.getId() +
                        ". Invio richiesta a " + inter.getAgentName());
                    // Riduci la velocità del veicolo in quanto ti avvicini ad un incrocio
                    SumoConnector.changeSpeed(vehicleID, 5.0);
                    
                    String message = inferVehicleIntent(); //TO TEST
                    inviaRichiestaPassaggio(inter.getAgentName(), message);
                    
                    System.out.println(vehicleID + " ha inviato richiesta a " + inter.getAgentName() + " con intento di: " + message);

                    requestSent = true;
                    hasPassed = false;
                }
                // Se il veicolo si è avvicinato troppo (entro STOP_THRESHOLD) e non ha ricevuto GO, forzalo a fermarsi
                if (distance <= STOP_THRESHOLD && !canCross) {
                    SumoConnector.changeSpeed(vehicleID, 0.0);
                    System.out.println(vehicleID + " si è avvicinato troppo all'incrocio senza GO: mi fermo");
                }
                // Se il veicolo si è allontanato sufficientemente dall'incrocio, resetta i flag e invia il messaggio PASSED
                else if (distance > EXIT_THRESHOLD && requestSent && canCross && !hasPassed) {
                    System.out.println(vehicleID + " ha superato l'incrocio, resetto lo stato per future richieste.");
                    inviaMessaggioPassato(inter.getAgentName());
                    requestSent = false;
                    canCross = false;
                }
            }
        }
    }

    private class IntersectionResponseBehaviour extends TickerBehaviour {
        public IntersectionResponseBehaviour() {
            // Verifica le risposte ogni 500ms
            super(VehicleAgent.this, 500);
        }
        
        @Override
        protected void onTick() {
            ACLMessage msg = receive();
            if (msg != null) {
                String content = msg.getContent().trim();
                System.out.println(vehicleID + " riceve risposta: " + content);
                if (content.equalsIgnoreCase("GO")) {
                    canCross = true;
                    // Imposta la velocità al valore desiderato (ad esempio, 13.9)
                    SumoConnector.changeSpeed(vehicleID, 13.9);
                    System.out.println(vehicleID + " può attraversare, imposto velocità a 13.9.");
                }
                else if (content.equalsIgnoreCase("END")) {
                    // Se riceve END e il veicolo non sta attraversando, resetta il flag
                    if(!canCross) {
                        requestSent = false;
                        System.out.println(vehicleID + " ha ricevuto END e non sta attraversando: resetto flag per nuove richieste.");
                    } else {
                        System.out.println(vehicleID + " ha ricevuto END ma sta attraversando: mantengo lo stato attuale.");
                    }
                    // Aumenta il livello di priorità per future richieste
                    priorityLevel++;
                    System.out.println(vehicleID + " ha aggiornato il livello di priorità a " + priorityLevel);
                }
            } 
            else {
                block();
            }
        }
    }
    /**
     * Invia una richiesta di passaggio all'agente semaforico.
     * Il formato del messaggio è: 
     * "REQUEST_PASS,<vehicleID>,<daDoveArrivo(north,south,est,west)>,<intenzione(straight,left,right)>,<timestamp>,<priority>"
     */
    private void inviaRichiestaPassaggio(String intersectionAgentName, String message) {
        ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
        msg.addReceiver(new jade.core.AID(intersectionAgentName, jade.core.AID.ISLOCALNAME));
        long ts = System.currentTimeMillis();
        String content = "REQUEST_PASS," + vehicleID + "," + message + ","+ ts + "," + priorityLevel;
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
        hasPassed = true;
        priorityLevel = 0;
        System.out.println(vehicleID + " invia messaggio PASSED a " + intersectionAgentName);
    }

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
