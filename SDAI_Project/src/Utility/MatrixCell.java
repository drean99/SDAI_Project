package Utility;
public class MatrixCell {
    private int row;
    private int col;
    private CellType type;
    
    public MatrixCell(int row, int col, CellType type) {
        this.row = row;
        this.col = col;
        this.type = type;
    }
    
    public int getRow() { return row; }
    public int getCol() { return col; }
    public CellType getType() { return type; }
    public void setType(CellType type) { this.type = type; }
    
    @Override
    public String toString() {
        return "(" + row + "," + col + "):" + type;
    }
}
