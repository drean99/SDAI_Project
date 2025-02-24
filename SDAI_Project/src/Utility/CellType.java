package Utility;
public enum CellType {
    WALL,         // Area non navigabile (muri, marciapiedi, ecc.)
    ROAD,         // Strada (una corsia per senso di marcia)
    INTERSECTION, // Incrocio centrale
    START,    // Punti di ingresso
    ARRIVAL // Punti di uscita
}
