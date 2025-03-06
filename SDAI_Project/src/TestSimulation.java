import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import Utility.SumoConnector;
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

            // 4. Scenario 1:
            AgentController vehicle1 = mainContainer.createNewAgent("v1", "VehicleAgent", null);
            vehicle1.start();
            AgentController vehicle2 = mainContainer.createNewAgent("v2", "VehicleAgent", null);
            vehicle2.start();
            AgentController vehicle3 = mainContainer.createNewAgent("v3", "VehicleAgent", null);
            vehicle3.start();
            // AgentController vehicle4 = mainContainer.createNewAgent("v4", "VehicleAgent", null);
            // vehicle4.start();
            // AgentController vehicle5 = mainContainer.createNewAgent("v5", "VehicleAgent", null);
            // vehicle5.start();
            // AgentController vehicle6 = mainContainer.createNewAgent("v6", "VehicleAgent", null);
            // vehicle6.start();
            // AgentController vehicle7 = mainContainer.createNewAgent("v7", "VehicleAgent", null);
            // vehicle7.start();

            // 5. Chiudi la connessione a SUMO
            //SumoConnector.close();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
