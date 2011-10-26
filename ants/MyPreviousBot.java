import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

/**
 * Version 9
 */
public class MyPreviousBot extends Bot {
	
	private final static boolean LOGGING = true;
	
    /**
     * Main method executed by the game engine for starting the bot.
     * 
     * @param args command line arguments
     * 
     * @throws IOException if an I/O error occurs
     */
    public static void main(String[] args) throws IOException {
        new MyPreviousBot().readSystemInput();
    }
    
    private Ants ants;
    Set<Tile> blocked = new HashSet<Tile>();
    
    /**
     */
    @Override
    public void doTurn() {
        ants = getAnts();
        blocked.clear();
        for (Tile myAnt : ants.getMyAnts()) {
        	if (isNextToFood(myAnt)) {
        		continue;	// fressen lassen
        	}
        	Aim direction = findBestDirection(myAnt);
        	if (direction != null) {
            	Tile dest = ants.getTile(myAnt, direction);
            	if (!blocked.contains(dest) && ants.getIlk(myAnt, direction).isUnoccupied()) {
            		ants.issueOrder(myAnt, direction);
            		blocked.add(dest);
            	}
        	}
        }
    }

    private LinkedList<Tile> floodFillQueue = new LinkedList<Tile>();
    private Map<Tile, QueueData> traceBackData = new HashMap<Tile, QueueData>();
    
    /* also detects if myAnt is directly next to food already - return is null so cannot be distinquished --> use isNextToFood() before */
    private Aim findBestDirection(Tile myAnt) {
    	floodFillQueue.clear();
    	traceBackData.clear();
    	floodFillQueue.add(myAnt);
    	traceBackData.put(myAnt, new QueueData(null, null, 0));
    	Tile current = null;	// ausserhalb damit ich nachher das Tile das als letztes aus Queue genommen wurde, habe --> wenn kein Food dann in die Richtung gehen weil "am weitesten freie Bahn"
    	while(!floodFillQueue.isEmpty()) {
    		current = floodFillQueue.pollFirst();
    		int currentDepth = traceBackData.get(current).steps;
     		for (Aim direction : Aim.values()) {
        		Tile dest = ants.getTile(current, direction);
        		if (!ants.getIlk(dest).isPassable()) {
        			continue;	// da gehts nicht weiter
        		}
        		if (currentDepth == 0 && blocked.contains(dest)) {
        			continue;	// von anderem Befehl blockiert
        		}
        		if (traceBackData.containsKey(dest)) {
        			continue;	// got covered already
        		}
        		if (ants.getEnemyHills().contains(dest)) {	// enemy-hills auch angreifen
            		traceBackData.put(dest, new QueueData(current, direction, currentDepth+1));	// required by trace back
        			return traceBack(dest, myAnt);
        		}
        		if (ants.getFoodTiles().contains(dest)) {
        			return traceBack(current, myAnt);
        		} 
        		floodFillQueue.addLast(dest);
        		traceBackData.put(dest, new QueueData(current, direction, currentDepth+1));
     		}
    	}
    	if (current != null) {	// wenn no FOOD dann ist current als letztes in Queue gewesen und somit am weitesten vom Ausgangspunkt weg == weitester freier Weg
			return traceBack(current, myAnt);
    	} else {
    		return null;
    	}
    }

 	private Aim traceBack(Tile current, Tile start) {
		QueueData back = traceBackData.get(current);
		if (back.cameFrom == null) {
			return null;	// totally blocked, nowhere to go right now
		}
		while (!back.cameFrom.equals(start)) {
			back = traceBackData.get(back.cameFrom);
		}
		return back.direction;
	}

	/* nicht diagonal */
    private boolean isNextToFood(Tile ant) {
    	for (Aim direction : Aim.values()) {
    		if (ants.getIlk(ant, direction) == Ilk.FOOD) {
    			return true;
    		}
    	}
    	return false;
    }
    
	private static void log(Object s) {
		if (LOGGING) {
			System.err.println(s.toString());
		}
	}	
}
