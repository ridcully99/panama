import java.io.IOException;

/**
 * Starter bot implementation.
 */
public class MyBot extends Bot {
    /**
     * Main method executed by the game engine for starting the bot.
     * 
     * @param args command line arguments
     * 
     * @throws IOException if an I/O error occurs
     */
    public static void main(String[] args) throws IOException {
        new MyBot().readSystemInput();
    }
    
    private Ants ants;
    
    /**
     * For every ant check every direction in fixed order (N, E, S, W) and move it if the tile is
     * passable.
     */
    @Override
    public void doTurn() {
        ants = getAnts();
        for (Tile myAnt : ants.getMyAnts()) {
        	if (isNextToFood(myAnt)) {
        		continue;	// fressen lassen
        	}
            for (Aim direction : Aim.values()) {
                if (ants.getIlk(myAnt, direction).isPassable()) {
                    ants.issueOrder(myAnt, direction);
                    break;
                }
            }
        }
    }
    
    /* nicht diagonal */
    private boolean isNextToFood(Tile ant) {
    	for (Tile food : ants.getFoodTiles()) {
    		if (ants.getDistance(ant, food) == 1) {
    			return true;
    		}
    	}
    	return false;
    }
}
