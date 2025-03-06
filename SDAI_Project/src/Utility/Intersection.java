package Utility;

public class Intersection {
    private String id;
    private double x;
    private double y;
    private String agentName; // Nome dell'agente semaforico responsabile

    public Intersection(String id, double x, double y, String agentName) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.agentName = agentName;
    }

    public String getId() {
        return id;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public String getAgentName() {
        return agentName;
    }

    @Override
    public String toString() {
        return "Intersection{" +
               "id='" + id + '\'' +
               ", x=" + x +
               ", y=" + y +
               ", agentName='" + agentName + '\'' +
               '}';
    }
}
