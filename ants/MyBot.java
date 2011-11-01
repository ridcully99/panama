import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

/**
 * Version 11 (online als Version 10)
 */
public class MyBot extends Bot {
	
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
        new MyBot().readSystemInput();
    }

    @Override
    public void setup(int loadTime, int turnTime, int rows, int cols, int turns, int viewRadius2, int attackRadius2, int spawnRadius2) {
    	super.setup(loadTime, turnTime, rows, cols, turns, viewRadius2, attackRadius2, spawnRadius2);
    }
    
    @Override
    public void removeAnt(int row, int col, int owner) {
    	super.removeAnt(row, col, owner);
    	if (owner == 0) { 
    		antsDied++;
    	}
    }
    
    private Map<Tile, Tile> longTermDestination = new HashMap<Tile, Tile>();	// ant --> destination
    
    private Ants ants;
    private Set<Tile> blocked = new HashSet<Tile>();
    private int turn = 0;
    private int antsDied = 0;
    private int foodEaten = 0;
    private Set<Tile> foodLastTurn = new HashSet<Tile>();
    
    /**
     */
    @Override
    public void doTurn() {
    	ants = getAnts();
    	foodEaten += foodEatenLastTurn();
    	int antsInHive = (1+foodEaten-antsDied)-ants.getMyAnts().size();	// wieviele es sein sollten - wieviele es sind
    	int minAntsToKeepInHive = (int)Math.sqrt(ants.getMyAnts().size()) - 1;	// keep some ants in hive for safety (to defend hill)
    	blocked.clear();
        log("known territory: "+((ants.knownTerritory.size()*100f)/(ants.getCols()*ants.getRows()))+"%");
        for (Tile myAnt : ants.getMyAnts()) {
        	// defense
        	if (ants.getMyHills().contains(myAnt) && 				// ant sitting on own hill
        		antsInHive < minAntsToKeepInHive) {					// not enough ants in hive
        		log("keep ant in hive, as we want "+minAntsToKeepInHive);
        		continue;											// block hill until more ants in hive
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
    
    private Aim findBestDirection(Tile myAnt) {
    	Tile mostUnknown = null;
    	int maxUnknown = 0;
    	floodFillQueue.clear();
    	traceBackData.clear();
    	floodFillQueue.add(myAnt);
    	traceBackData.put(myAnt, new QueueData(null, null, 0));
    	Tile current = null;	// ausserhalb damit ich nachher das Tile das als letztes aus Queue genommen wurde, habe --> wenn kein Food dann in die Richtung gehen weil "am weitesten freie Bahn"
    	queue:
    	while(!floodFillQueue.isEmpty() && ants.getTimeRemaining() >= ALMOST_TIMEOUT) {
    		current = floodFillQueue.pollFirst();
    		int currentDepth = traceBackData.get(current).steps;
    		int currentUnknown = traceBackData.get(current).unknown;
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
        		int newUnknown = currentUnknown;
        		if (!ants.knownTerritory.contains(dest)) {
        			newUnknown += 1;
        			if (newUnknown > maxUnknown) {
        				maxUnknown = newUnknown;
        				mostUnknown = dest;
        			}
        		}
        		traceBackData.put(dest, new QueueData(current, direction, currentDepth+1, newUnknown));
     		}
    	}
    	// nichts besonderes gefunden.
    	if (current != null) {	// current ist als letztes in Queue gewesen und somit am weitesten vom Ausgangspunkt weg == weitester freier Weg
			// entweder hatte ant noch kein longtermdestination oder hat es nicht schon vorher gefunden (was eigentlich nur durch temporaere blockierungen passiert sein kann)
    		if (!longTermDestination.containsKey(myAnt)) {	// noch kein longtermdest --> eines setzen
    			if (mostUnknown != null) {
    				longTermDestination.put(myAnt, mostUnknown);
    			} else {
    				longTermDestination.put(myAnt, current);
    			}
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
 	}
 	
    private int foodEatenLastTurn() {
    	int count = 0;
    	foodLastTurn.removeAll(ants.getFoodTiles());	// was ueberbleibt war da und ist jetzt weg
    	for (Tile food : foodLastTurn) {
    		boolean mine = false;
    		for (Aim direction : Aim.values()) {
	    		if (ants.getIlk(food, direction) == Ilk.MY_ANT) {
	    			mine = true;
	    		}
	    		if (ants.getIlk(food, direction) == Ilk.ENEMY_ANT) {
	    			mine = false;
	    			break;
	    		}
	    	}
    		if (mine) {
    			count++;
    			log("I ate food at "+food+" last turn");
    		}
    	}
    	foodLastTurn.clear();
    	foodLastTurn.addAll(ants.getFoodTiles());
    	return count;
	}

	private void log(Object s) {
		if (LOGGING) {
			System.err.println(turn+": "+s.toString());
		}
	}	
}
