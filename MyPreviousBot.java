import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Starter bot implementation.
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
        		log("mampf");
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

    private Aim findBestDirection(Tile myAnt) {
    	Aim foodAim = findBestDirectionToFood(myAnt);
    	if (foodAim != null) {
    		return foodAim;
    	} else {
    		return findOpenSpace(myAnt);
    	}
    }
    
    private LinkedList<Tile> floodFillQueue = new LinkedList<Tile>();
    private Map<Tile, QueueData> traceBackData = new HashMap<Tile, QueueData>();
    
    /* also detects if myAnt is directly next to food already - return is null so cannot be distinquished --> use isNextToFood() before */
    private Aim findBestDirectionToFood(Tile myAnt) {
    	floodFillQueue.clear();
    	traceBackData.clear();
    	int seq = 0;
    	floodFillQueue.add(myAnt);
    	traceBackData.put(myAnt, null);
    	Tile current = null;	// ausserhalb damit ich nachher das Tile das als letztes aus Queue genommen wurde, habe --> wenn kein Food dann in die Richtung gehen weil "am weitesten freie Bahn"
    	while(!floodFillQueue.isEmpty()) {
    		current = floodFillQueue.pollFirst();
     		for (Aim direction : Aim.values()) {
        		Tile dest = ants.getTile(current, direction);
        		if (traceBackData.containsKey(dest)) {
        			continue;	// got covered already
        		}
        		if (ants.getEnemyHills().contains(dest)) {	// enemy-hills auch angreifen
        			return traceBack(current, myAnt);
        		}
        		if (ants.getFoodTiles().contains(dest)) {
        			return traceBack(current, myAnt);
        		} else if (ants.getIlk(dest).isPassable()) {
        			floodFillQueue.addLast(dest);
        			traceBackData.put(dest, new QueueData(current, direction, ++seq));
        		}
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
		if (back == null) {
			return null;
		}
		while (!back.origin.equals(start)) {
			back = traceBackData.get(back.origin);
		}
		return back.originAimed;
	}

	private List<Aim> getPassableDirections(Tile myAnt) {
 		List<Aim> res = new ArrayList<Aim>();
 		for (Aim direction : Aim.values()) {
    		Tile dest = ants.getTile(myAnt, direction);
    		if (ants.getIlk(myAnt, direction).isPassable()) {
    			res.add(direction);
    		}
 		}
 		return res;
	}

	// dahin von wo am meisten wege weitergehen
    private Aim findOpenSpace(Tile myAnt) {	
    	Aim bestAim = null;
    	int bestCount = 0;
    	for (Aim direction : Aim.values()) {
    		Tile dest = ants.getTile(myAnt, direction);
    		if (!ants.getIlk(dest).isPassable()) {
    			continue;	// dahin gehts gar nicht
            }
    		int count = 0;
    		for (Aim lookAhead : Aim.values()) {
        		Tile lookAheadDest = ants.getTile(dest, lookAhead);
        		if (ants.getFoodTiles().contains(lookAheadDest)) {
        			count += 2;	// zwar nicht frei, aber Food, was wir als noch besser bewerten
        		}
        		if (ants.getIlk(lookAheadDest).isPassable()) {	// for vorausschau reicht isPassable()
        			count++;	// freies Feld
        		}
        	}
    		if (count > bestCount || (count == bestCount && Math.random() > 0.5)) {
    			bestCount = count;
    			bestAim = direction;
    		}
    	}
    	return bestAim;
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
