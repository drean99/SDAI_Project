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
                System.out.println(vehicleID + " distanza dall'incrocio " + inter.getId() + ": " + distance);
                
                // Se il veicolo è entro la soglia di approccio e non ha ancora inviato la richiesta
                if (distance <= APPROACH_THRESHOLD && !requestSent) {
                    System.out.println(vehicleID + " è nell'area di approccio dell'incrocio " + inter.getId() +
                        ". Invio richiesta a " + inter.getAgentName());
                    // Riduci la velocità del veicolo in quanto ti avvicini ad un incrocio
                    SumoConnector.changeSpeed(vehicleID, 5.0);
                    inviaRichiestaPassaggio(inter.getAgentName());
                    requestSent = true;
                    hasPassed = false;
                }
                // Se il veicolo si è avvicinato troppo (entro STOP_THRESHOLD) e non ha ricevuto GO, forzalo a fermarsi
                if (distance <= STOP_THRESHOLD && !canCross) {
                    SumoConnector.changeSpeed(vehicleID, 0.0);
                    System.out.println(vehicleID + " si è avvicinato troppo all'incrocio senza GO: mi fermo");
                }
                // Se il veicolo si è allontanato dall'incrocio, resetta il flag
                else if (distance > STOP_THRESHOLD && requestSent && canCross && !hasPassed) {
                    System.out.println(vehicleID + " ha superato l'incrocio, resetto il flag per future richieste.");
                    inviaMessaggioPassato(inter.getAgentName());
                    requestSent = false;
                    canCross = false;
                }
            } else {
                //System.out.println(vehicleID + " non è vicino ad alcun incrocio.");
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
                } else if (content.equalsIgnoreCase("STOP")) {
                    canCross = false;
                    SumoConnector.changeSpeed(vehicleID, 0.0);
                    System.out.println(vehicleID + " deve fermarsi, imposto velocità a 0.");
                } else if (content.equalsIgnoreCase("END")) {
                    
                    // Se riceve END e non ha attraversato, resetta il flag per permettere una nuova richiesta
                    if(!canCross)
                        requestSent = false; //Corner case in qui il veicolo è fermo, ha ricevuto go ma riceve end mentre sta attraversando e quindi si ferma in mezzo all'incrocio
                    
                    priorityLevel++;

                    System.out.println(vehicleID + " ha ricevuto END, resetto lo stato per future richieste.");
                }
            } else {
                block();
            }
        }
    }
    /**
     * Invia una richiesta di passaggio all'agente semaforico.
     * Il formato del messaggio è: 
     * "REQUEST_PASS,<vehicleID>,unknown,straight,<timestamp>,<priority>"
     */
    private void inviaRichiestaPassaggio(String intersectionAgentName) {
        ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
        msg.addReceiver(new jade.core.AID(intersectionAgentName, jade.core.AID.ISLOCALNAME));
        long ts = System.currentTimeMillis();
        String content = "REQUEST_PASS," + vehicleID + ",unknown,straight," + ts + "," + priorityLevel;
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

    //     public static String inferVehicleIntent(String vehicleID) {
    //     List<Edge> route = SumoConnector.getVehicleRoute(vehicleID);
    //     if (route != null && route.size() >= 2) {
    //         Edge e1 = route.get(0);
    //         Edge e2 = route.get(1);
    //         e1.getID(); 
    //         Coordinate end1 = e1.getEndCoordinate();   // ipotetico metodo di Edge
    //         Coordinate start2 = e2.getStartCoordinate(); // ipotetico metodo di Edge
            
    //         int dx = start2.getX() - end1.getX();
    //         int dy = start2.getY() - end1.getY();
    //         double angle = Math.atan2(dy, dx); // in radianti
            
    //         // Normalizza l'angolo in gradi per una lettura più intuitiva
    //         double angleDegrees = Math.toDegrees(angle);
            
    //         // Esempio di soglie (da regolare in base alla scala della tua rete)
    //         if (Math.abs(angleDegrees) < 20) {
    //             return "east";
    //         } else if (Math.abs(angleDegrees - 180) < 20 || Math.abs(angleDegrees + 180) < 20) {
    //             return "west";
    //         } else if (Math.abs(angleDegrees - 90) < 20) {
    //             return "south";
    //         } else if (Math.abs(angleDegrees + 90) < 20) {
    //             return "north";
    //         } else {
    //             return "turning"; // indica che il veicolo sta eseguendo una svolta
    //         }
    //     }
    //     return "straight";
    // }
}
