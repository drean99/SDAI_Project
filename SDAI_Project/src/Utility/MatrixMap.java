package Utility;

import java.util.ArrayList;
import java.util.List;

public class MatrixMap {
    private MatrixCell[][] grid;
    private int rows;
    private int cols;
    // Definiamo le due righe per la strada orizzontale
    private int h1, h2; // h1 = corsia superiore, h2 = corsia inferiore
    // Definiamo le due colonne per la strada verticale
    private int v1, v2; // v1 = corsia sinistra, v2 = corsia destra
    
    public MatrixMap(int rows, int cols) {
        if(rows % 2 != 0 || cols % 2 != 0) {
            throw new IllegalArgumentException("Rows e cols devono essere numeri pari.");
        }
        this.rows = rows;
        this.cols = cols;
        grid = new MatrixCell[rows][cols];
        initMap();
    }
    
    private void initMap() {
        // Inizializza tutte le celle come WALL
        for (int i = 0; i < rows; i++){
            for (int j = 0; j < cols; j++){
                grid[i][j] = new MatrixCell(i, j, CellType.WALL);
            }
        }
        // Definisce le due righe centrali per la strada orizzontale
        h1 = rows / 2 - 1;
        h2 = rows / 2;
        // Definisce le due colonne centrali per la strada verticale
        v1 = cols / 2 - 1;
        v2 = cols / 2;
        
        // Imposta le righe h1 e h2 come ROAD
        for (int j = 0; j < cols; j++){
            grid[h1][j].setType(CellType.ROAD);
            grid[h2][j].setType(CellType.ROAD);
        }
        // Imposta le colonne v1 e v2 come ROAD
        for (int i = 0; i < rows; i++){
            grid[i][v1].setType(CellType.ROAD);
            grid[i][v2].setType(CellType.ROAD);
        }
        // Le celle in cui le corsie si intersecano diventano INTERSECTION
        grid[h1][v1].setType(CellType.INTERSECTION);
        grid[h1][v2].setType(CellType.INTERSECTION);
        grid[h2][v1].setType(CellType.INTERSECTION);
        grid[h2][v2].setType(CellType.INTERSECTION);
        
        // Imposta i punti di partenza e di arrivo (START e ARRIVAL)
        // Lato OVEST (col 0):
        grid[h1][0].setType(CellType.ARRIVAL);  // OVEST, corsia superiore → ARRIVAL
        grid[h2][0].setType(CellType.START);      // OVEST, corsia inferiore → START
        
        // Lato EST (col cols-1):
        grid[h1][cols-1].setType(CellType.START); // EST, corsia superiore → START
        grid[h2][cols-1].setType(CellType.ARRIVAL); // EST, corsia inferiore → ARRIVAL
        
        // Lato NORD (row 0):
        grid[0][v1].setType(CellType.START);      // NORD, corsia sinistra → START
        grid[0][v2].setType(CellType.ARRIVAL);      // NORD, corsia destra → ARRIVAL
        
        // Lato SUD (row rows-1):
        grid[rows-1][v1].setType(CellType.ARRIVAL); // SUD, corsia sinistra → ARRIVAL
        grid[rows-1][v2].setType(CellType.START);     // SUD, corsia destra → START
    }
    
    public MatrixCell getCell(int row, int col) {
        if (row >= 0 && row < rows && col >= 0 && col < cols) {
            return grid[row][col];
        }
        return null;
    }
    
    /**
     * Restituisce tutti i punti START.
     */
    public List<MatrixCell> getStartingPoints() {
        List<MatrixCell> points = new ArrayList<>();
        // Aggiunge i punti che sono START
        // Lato OVEST: (h2,0)
        points.add(grid[h2][0]);
        // Lato EST: (h1,cols-1)
        points.add(grid[h1][cols-1]);
        // Lato NORD: (0,v1)
        points.add(grid[0][v1]);
        // Lato SUD: (rows-1,v2)
        points.add(grid[rows-1][v2]);
        return points;
    }
    
    /**
     * Restituisce tutti i punti ARRIVAL.
     */
    public List<MatrixCell> getArrivalPoints() {
        List<MatrixCell> points = new ArrayList<>();
        // Lato OVEST: (h1,0)
        points.add(grid[h1][0]);
        // Lato EST: (h2,cols-1)
        points.add(grid[h2][cols-1]);
        // Lato NORD: (0,v2)
        points.add(grid[0][v2]);
        // Lato SUD: (rows-1,v1)
        points.add(grid[rows-1][v1]);
        return points;
    }
    
    public void printMap() {
        for (int i = 0; i < rows; i++){
            for (int j = 0; j < cols; j++){
                MatrixCell cell = grid[i][j];
                switch(cell.getType()){
                    case WALL: System.out.print(" X "); break;
                    case ROAD: System.out.print(" . "); break;
                    case INTERSECTION: System.out.print(" I "); break;
                    case START: System.out.print(" P "); break;
                    case ARRIVAL: System.out.print(" A "); break;
                }
            }
            System.out.println();
        }
    }
    
    public int getRows(){ return rows; }
    public int getCols(){ return cols; }
    public int getH1(){ return h1; }
    public int getH2(){ return h2; }
    public int getV1(){ return v1; }
    public int getV2(){ return v2; }
}
