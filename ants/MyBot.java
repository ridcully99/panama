import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

/**
 * Version 9
 */
public class MyBot extends Bot {
	
	private final static boolean LOGGING = true;
	
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
    LinkedList<Tile> myAnts = new LinkedList<Tile>();	// Queue -- wenn ich fuer FOOD wen naeheren finde, kommt der fruehere wieder in die Queue
    private Set<Tile> blocked = new HashSet<Tile>();
    private int turn = 0;
    
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
        turn++;
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
    	// nichts besonderes gefunden.
    	if (current != null) {	// current ist als letztes in Queue gewesen und somit am weitesten vom Ausgangspunkt weg == weitester freier Weg
			return traceBack(current, myAnt);
    	} else {
    		return null;
    	}
    }

 	private Aim traceBack(Tile current, Tile start) {
		QueueData back = traceBackData.get(current);
		if (back.origin == null) {
			return null;	// totally blocked, nowhere to go right now
		}
		while (!back.origin.equals(start)) {
			back = traceBackData.get(back.origin);
		}
		return back.originAimed;
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
    
	private void log(Object s) {
		if (LOGGING) {
			System.err.println(turn+": "+s.toString());
		}
	}	
}
