import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Version 10
 */
public class MyBot10 extends Bot {
	
	private final static boolean LOGGING = true;
	private final static int ALMOST_TIMEOUT = 10;
	
    /**
     * Main method executed by the game engine for starting the bot.
     * 
     * @param args command line arguments
     * 
     * @throws IOException if an I/O error occurs
     */
    public static void main(String[] args) throws IOException {
        new MyBot10().readSystemInput();
    }

    // for leftyMove
    private Map<Tile, Aim> antStraight = new HashMap<Tile, Aim>();	// long-term tracking, dont clear each turn
    private Map<Tile, Aim> antLefty = new HashMap<Tile, Aim>();		// long-term tracking, dont clear each turn

    private Map<Tile, Tile> longTermDestination = new HashMap<Tile, Tile>();	// ant --> destination
    
    private Ants ants;
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
            		// update movement in longTermDestination map
            		if (longTermDestination.containsKey(myAnt)) {
            			longTermDestination.put(dest, longTermDestination.get(myAnt));
            			longTermDestination.remove(myAnt);
            		}
            	}
        	}
        	if (ants.getTimeRemaining() < ALMOST_TIMEOUT) {
        		break;
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
    	while(!floodFillQueue.isEmpty() && ants.getTimeRemaining() >= ALMOST_TIMEOUT) {
    		current = floodFillQueue.pollFirst();
    		int currentDepth = traceBackData.get(current).steps;
    		if (currentDepth == 0 && current.equals(longTermDestination.get(myAnt))) {
    			log(myAnt+" has reached longterm destination; removing it.");
    			clearLongTermDestination(myAnt);
    		}
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
        			clearLongTermDestination(myAnt);
            		traceBackData.put(dest, new QueueData(current, direction, currentDepth+1));	// required by trace back
            		return traceBack(dest, myAnt);
        		}
        		if (ants.getFoodTiles().contains(dest)) {
        			clearLongTermDestination(myAnt);
        			return traceBack(current, myAnt);
        		}
        		if (dest.equals(longTermDestination.get(myAnt))) {								// hit long term destination?
            		traceBackData.put(dest, new QueueData(current, direction, currentDepth+1));	// required by trace back
            		return traceBack(dest, myAnt);
        		}
        		floodFillQueue.addLast(dest);
        		traceBackData.put(dest, new QueueData(current, direction, currentDepth+1));
     		}
    	}
    	// nichts besonderes gefunden.
    	// Leftybot ist schlechter. return leftyMove(myAnt);
    	if (current != null) {	// current ist als letztes in Queue gewesen und somit am weitesten vom Ausgangspunkt weg == weitester freier Weg
			// entweder hatte ant noch kein longtermdestination oder hat es nicht schon vorher gefunden (was eigentlich nur durch temporaere blockierungen passiert sein kann)
    		if (!longTermDestination.containsKey(myAnt)) {	// noch kein longtermdest --> eines setzen
    			longTermDestination.put(myAnt, current);
    			return traceBack(current, myAnt);
    		} else {
    			// hat schon eines, hat's aber via floodfill nicht erreicht... dont move, vielleicht gehts naechstesmal besser.
    			return null;
    		}
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

 	private void clearLongTermDestination(Tile ant) {
 		longTermDestination.remove(ant);
		antStraight.remove(ant);
		antLefty.remove(ant);
 	}
 	
 	/** from LeftyBot example */
 	private Aim leftyMove(Tile location) {
		// send new ants in a straight line
		if (!antStraight.containsKey(location) && !antLefty.containsKey(location)) {
			Aim direction;
			if (location.row % 2 == 0) {
				if (location.col % 2 == 0) {
					direction = Aim.NORTH;
				} else {
					direction = Aim.SOUTH;
				}
			} else {
				if (location.col % 2 == 0) {
					direction = Aim.EAST;
				} else {
					direction = Aim.WEST;
				}
			}
			antStraight.put(location, direction);
		}
		// send ants going in a straight line in the same direction
		if (antStraight.containsKey(location)) {
			Aim direction = antStraight.get(location);
			Tile destination = ants.getTile(location, direction);
			if (ants.getIlk(destination).isPassable()) {
				if (ants.getIlk(destination).isUnoccupied() && !blocked.contains(destination)) {
					antStraight.remove(location);				// remember movement
					antStraight.put(destination, direction);	// -""-
					return direction;
				} else {
					// pause ant, turn and try again next turn
					antStraight.put(location, direction.left());
					blocked.add(location);
					return null;
				}
			} else {
				// hit a wall, start following it
				antStraight.remove(location);
				antLefty.put(location, direction.right());
			}
		}
		// send ants following a wall, keeping it on their left
		if (antLefty.containsKey(location)) {
			Aim direction = antLefty.get(location);
			List<Aim> directions = new ArrayList<Aim>();
			directions.add(direction.left());
			directions.add(direction);
			directions.add(direction.right());
			directions.add(direction.behind());
			// try 4 directions in order, attempting to turn left at corners
			for (Aim newDirection : directions) {
				Tile destination = ants.getTile(location, newDirection);
				if (ants.getIlk(destination).isPassable()) {
					if (ants.getIlk(destination).isUnoccupied() && !blocked.contains(destination)) {
						antLefty.remove(location);					// remember movement
						antLefty.put(destination, newDirection);	// -""-
						return newDirection;
					} else {
						// pause ant, turn and send straight
						antLefty.remove(location);
						antStraight.put(location, direction.right());
						blocked.add(location);
						return null;
					}
				}
			}
		}
		return null;
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
