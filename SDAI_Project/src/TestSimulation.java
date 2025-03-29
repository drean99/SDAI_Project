import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import Utility.SumoConnector;
import Utility.VehicleHelper;
import Utility.Environment;
import Utility.Intersection;
import java.util.List;

public class TestSimulation {

    public static void main(String[] args) {
        // Avvia il runtime JADE e crea il main container
        Runtime rt = Runtime.instance();
        rt.setCloseVM(true);
        Profile p = new ProfileImpl();
        AgentContainer mainContainer = rt.createMainContainer(p);
        
        try {

            // 0. Carica la mappa e i veicoli dai file XML di configurazione
            Environment.loadMap("SDAI_Project\\src\\SUMO_config\\cross.net.xml");
            List<VehicleHelper.VehicleDefinition> vehicles = VehicleHelper.loadVehicles("SDAI_Project\\src\\SUMO_config\\cross.rou.xml");

            // 1. Avvia la connessione a SUMO
            SumoConnector.connect("SDAI_Project\\src\\SUMO_config\\cross.sumocfg", 12345);

            // 2. Crea un agente IntersectionAgent per ogni incrocio definito in Environment
            List<Intersection> incroci = Environment.getIntersections();
            for (Intersection inc : incroci) {
                // L'agentName è inc.getAgentName(), ad es. "IntersectionAgent_C2"
                AgentController intersectionAgent = mainContainer.createNewAgent(
                    inc.getAgentName(),
                    "IntersectionAgent",
                    null
                );
                intersectionAgent.start();
            }
            
            // 3. Crea un agente SimStepAgent per gestire i passi di simulazione
            mainContainer.createNewAgent("SimStepAgent", "SimStepAgent", null).start();

            // 4. Crea un agente VehicleAgent per ogni veicolo definito in vehicles
            // Se il tipo è aggressiveCar, crea un VehicleAggressiveAgent, altrimenti un VehicleAgent

            for (VehicleHelper.VehicleDefinition vDef : vehicles) {
                String agentClass = vDef.getType().equalsIgnoreCase("aggressiveCar") ? "VehicleAggressiveAgent" : "VehicleAgent"; 
                AgentController agent = mainContainer.createNewAgent(vDef.getId(), agentClass, null); 
                agent.start(); 
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
