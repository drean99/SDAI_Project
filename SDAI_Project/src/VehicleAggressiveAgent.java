import Utility.Coordinate;
import Utility.Environment;
import Utility.Intersection;
import Utility.SumoConnector;
import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;

public class VehicleAggressiveAgent extends Agent {
    private String vehicleID;
    // Flag per evitare richieste ripetute
    private boolean requestSent = false;
    // Stato: se il veicolo può procedere o deve rallentare/fermarsi
    private boolean canCross = false;
    // Flag per evitare invii multipli del messaggio PASSED
    private boolean hasPassed = false;
    
    // Contatore per la priorità; se il veicolo riceve END più volte, aumenta la priorità per la richiesta successiva
    private int priorityLevel = 0;

    // Soglie aggressive (in unità della rete SUMO)
    private static final double MAX_THRESHOLD = 30.0;
    private static final double APPROACH_THRESHOLD = 22.0;
    private static final double STOP_THRESHOLD = 12.0;

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
            //System.out.println(vehicleID + " posizione SUMO: " + pos);
            
            // Cerca l'incrocio usando MAX_THRESHOLD per permettere il reset dei flag
            Intersection inter = Environment.findNearbyIntersection(pos, MAX_THRESHOLD);
            if (inter != null) {
                double distance = pos.distance(new Coordinate((int) inter.getX(), (int) inter.getY()));
                System.out.println(vehicleID + " distanza dall'incrocio " + inter.getId() + ": " + distance);
                
                // Se il veicolo è entro la soglia di approccio e non ha ancora inviato la richiesta, invia la richiesta.
                // L'aggressivo non rallenta, quindi non modifica la velocità.
                if (distance <= APPROACH_THRESHOLD && !requestSent) {
                    System.out.println(vehicleID + " è nell'area di approccio aggressiva dell'incrocio " + inter.getId() +
                        ". Invio richiesta a " + inter.getAgentName());
                    SumoConnector.changeSpeed(vehicleID,10.0);
                    inviaRichiestaPassaggio(inter.getAgentName());
                    requestSent = true;
                    hasPassed = false;
                }
                // Se il veicolo si avvicina troppo (entro STOP_THRESHOLD) e non ha ricevuto GO, rallenta moderatamente (ma non si ferma completamente)
                if (distance <= STOP_THRESHOLD && !canCross) {
                    SumoConnector.changeSpeed(vehicleID, 7.0);
                    System.out.println(vehicleID + " si è avvicinato troppo senza autorizzazione: rallento a 7.0.");
                }
                // Se il veicolo si è allontanato dall'incrocio, resetta il flag
                else if (distance > STOP_THRESHOLD && requestSent) {
                    if(!hasPassed){
                        System.out.println(vehicleID + " ha superato l'incrocio, resetto il flag per future richieste.");
                        inviaMessaggioPassato(inter.getAgentName());
                    }
                    requestSent = false;
                    canCross = false;
                }
            } else {
                //System.out.println(vehicleID + " non è vicino ad alcun incrocio.");
            }
        }
    }
    
    // Comportamento per ascoltare le risposte dall'agente Intersection
    private class IntersectionResponseBehaviour extends TickerBehaviour {
        public IntersectionResponseBehaviour() {
            super(VehicleAggressiveAgent.this, 500);
        }
        
        @Override
        protected void onTick() {
            ACLMessage msg = receive();
            if (msg != null) {
                String content = msg.getContent().trim();
                System.out.println(vehicleID + " riceve risposta: " + content);
                if (content.equalsIgnoreCase("GO")) {
                    canCross = true;
                    // L'aggressivo mantiene una velocità elevata: ad esempio, 16.9 (massima per aggressiveCar)
                    SumoConnector.changeSpeed(vehicleID, 16.9);
                    System.out.println(vehicleID + " può attraversare, imposto velocità a 16.9.");
                } else if (content.equalsIgnoreCase("STOP")) {
                    canCross = false;
                    // Se riceve STOP, l'aggressivo rallenta leggermente ma non si ferma completamente
                    SumoConnector.changeSpeed(vehicleID, 2.0);
                    System.out.println(vehicleID + " riceve STOP, imposto velocità a 2.0.");
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
}
