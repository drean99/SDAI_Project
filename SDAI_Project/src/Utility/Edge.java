package Utility;

public class Edge {
    private String id;
    private Coordinate start;
    private Coordinate end;
    private double length;

    public Edge(String id, Coordinate start, Coordinate end, double length) {
        this.id = id;
        this.start = start;
        this.end = end;
        this.length = length;
    }

    public String getId() {
        return id;
    }

    public Coordinate getStart() {
        return start;
    }

    public Coordinate getEnd() {
        return end;
    }

    public double getLength() {
        return length;
    }

    @Override
    public String toString() {
        return "Edge[id=" + id + ", start=" + start + ", end=" + end + ", length=" + length + "]";
    }
}
