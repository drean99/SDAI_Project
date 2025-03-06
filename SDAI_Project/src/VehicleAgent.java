import Utility.Coordinate;
import Utility.Environment;
import Utility.Intersection;
import Utility.SumoConnector;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;

public class VehicleAgent extends Agent {
    private String vehicleID;
    // Flag per evitare richieste ripetute
    private boolean requestSent = false;
    // Stato: se il veicolo può procedere o deve fermarsi
    private boolean canCross = false;
    
    // Soglie (in unità della rete SUMO)
    private static final double APPROACH_THRESHOLD = 22.0;
    private static final double STOP_THRESHOLD = 12.0;
    private static final double EXIT_THRESHOLD = 30.0;
    
    @Override
    protected void setup() {
        vehicleID = getLocalName();
        System.out.println(vehicleID + " - setup completato.");
        addBehaviour(new CheckIntersectionBehaviour(this, 500));
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
            //System.out.println(vehicleID + " posizione SUMO: " + pos);
            
            // Cerca l'incrocio usando una soglia più ampia per permettere anche il reset
            Intersection inter = Environment.findNearbyIntersection(pos, EXIT_THRESHOLD);
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
                }
                // Se il veicolo si è avvicinato troppo (entro STOP_THRESHOLD) e non ha ricevuto GO, forzalo a fermarsi
                if (distance <= STOP_THRESHOLD && !canCross) {
                    SumoConnector.changeSpeed(vehicleID, 0.0);
                    System.out.println(vehicleID + " si è avvicinato troppo all'incrocio senza GO: mi fermo");
                }
                // Se il veicolo si è allontanato dall'incrocio, resetta il flag
                else if (distance > EXIT_THRESHOLD && requestSent) {
                    System.out.println(vehicleID + " ha superato l'incrocio, resetto il flag per future richieste.");
                    requestSent = false;
                }
            } else {
                System.out.println(vehicleID + " non è vicino ad alcun incrocio.");
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
                    canCross = false;
                    requestSent = false;
                    System.out.println(vehicleID + " ha ricevuto END, resetto lo stato per future richieste.");
                }
            } else {
                block();
            }
        }
    }
    /**
     * Invia un messaggio di richiesta di passaggio all'agente semaforico corrispondente.
     * Il formato del messaggio è: "REQUEST_PASS,<vehicleID>,unknown,straight,<timestamp>"
     */
    private void inviaRichiestaPassaggio(String intersectionAgentName) {
        ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
        msg.addReceiver(new jade.core.AID(intersectionAgentName, jade.core.AID.ISLOCALNAME));
        long ts = System.currentTimeMillis();
        String content = "REQUEST_PASS," + vehicleID + ",unknown,straight," + ts;
        msg.setContent(content);
        send(msg);
        System.out.println(vehicleID + " invia richiesta di passaggio a " + intersectionAgentName);
    }
}
