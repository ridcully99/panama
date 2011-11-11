/**
 * Represents a tile of the game map.
 */
public class Tile {
    public int row;
    public int col;
    private int hash;
    
    /**
     * Creates new {@link Tile} object.
     * 
     * @param row row index
     * @param col column index
     */
    public Tile(int row, int col) {
        this.row = row;
        this.col = col;
        this.hash = row * Ants.MAX_MAP_SIZE + col;
    }
    
    public void recalcHash() {
        this.hash = row * Ants.MAX_MAP_SIZE + col;
    }
    
    /**
     * Returns row index.
     * 
     * @return row index
     */
    public int getRow() {
        return row;
    }
    
    /**
     * Returns column index.
     * 
     * @return column index
     */
    public int getCol() {
        return col;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return hash;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        boolean result = false;
        if (o != null && o instanceof Tile) {
            Tile tile = (Tile)o;
            result = row == tile.row && col == tile.col;
        }
        return result;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return row + " " + col;
    }
}
