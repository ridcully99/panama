
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Starter bot implementation.
 */
public class MyBotWithAimers extends Bot {
	
	private final static boolean LOGGING = true;
	
    /**
     * Main method executed by the game engine for starting the bot.
     * 
     * @param args command line arguments
     * 
     * @throws IOException if an I/O error occurs
     */
    public static void main(String[] args) throws IOException {
        new MyBotWithAimers().readSystemInput();
    }
    
    private Ants ants;
    private Set<Tile> blocked = new HashSet<Tile>();
    private Map<Tile, List<Aimer>> aimingInfo = new HashMap<Tile, List<Aimer>>();
    private List<Quadrant> quadrants = new ArrayList<Quadrant>();
    private Map<Tile, Order> pendingOrders = new HashMap<Tile, Order>();
    
    @Override
    public void setup(int loadTime, int turnTime, int rows, int cols, int turns, int viewRadius2, int attackRadius2, int spawnRadius2) {
    	super.setup(loadTime, turnTime, rows, cols, turns, viewRadius2, attackRadius2, spawnRadius2);
    	// additional setup stuff, e.g. calculate quadrants ...

    	quadrants.add(new Quadrant(     0,      0, rows/2-1, cols/2-1));
    	quadrants.add(new Quadrant(     0, cols/2, rows/2-1, cols-1));
    	quadrants.add(new Quadrant(rows/2,      0,   rows-1, cols/2-1));
    	quadrants.add(new Quadrant(rows/2, cols/2,   rows-1, cols-1));
    }

    // overridden to count ant for the quadrants
    @Override
    public void addAnt(int row, int col, int owner) {
    	super.addAnt(row, col, owner);
    	if (owner == 0) {
	    	for (Quadrant q : quadrants) {
	    		if (q.contains(row, col)) {
	    			q.myAntsCount++;
	    			log(turnCount+": "+q);
	    		}
	    	}
    	}
    }
    
    private int turnCount = 0;
    /**
     */
    @Override
    public void doTurn() {
    	// reset before getAnts()
    	for (Quadrant q : quadrants) {
    		q.myAntsCount = 0;
    	}
        ants = getAnts();
        pendingOrders.clear();
        blocked.clear();
        aimingInfo.clear();
        
        Collections.sort(quadrants, new Comparator<Quadrant>() {
			@Override
			public int compare(Quadrant a, Quadrant b) {
				return a.myAntsCount - b.myAntsCount;
			}
        });
        
        for (int tries = 0; tries < 2; tries++) {	// 2 versuche um bei erster runde removte nochmal anderweitig zu probieren
	        for (Tile myAnt : ants.getMyAnts()) {
	        	if (isNextToFood(myAnt)) {
	        		log(myAnt+" is next to food");
	        		continue;	// fressen lassen
	        	}
	        	if (tries > 0 && pendingOrders.containsKey(myAnt)) {	// hat schon gueltigen Befehl
	        		continue;
	        	}
	        	Aim direction = findBestDirection(myAnt);
	        	if (direction != null) {
	            	Tile dest = ants.getTile(myAnt, direction);
	            	if (!blocked.contains(dest) && ants.getIlk(myAnt, direction).isUnoccupied()) {
	            		log(tries+": place order "+new Order(myAnt, direction));
	            		pendingOrders.put(myAnt, new Order(myAnt, direction));
	            		blocked.add(dest);
	            	} else {
	            		log(tries+": ant "+myAnt+" blocked; wanted to go to "+dest);
	            	}
	        	} else {
	        		log(tries+": no direction for ant "+myAnt);
	        	}
	        }
        }
        
        // finally issue orders
        for (Order order : pendingOrders.values()) {
        	log(order);
            System.out.println(order);
            System.out.flush();
        }
        
        turnCount++;
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
    		int depth = traceBackData.get(current).steps;
     		for (Aim direction : Aim.values()) {
        		Tile dest = ants.getTile(current, direction);
        		if (!ants.getIlk(dest).isPassable()) {
        			continue;	// da gehts nicht weiter
        		}
        		if (traceBackData.containsKey(dest)) {
        			continue;	// got covered already
        		}
        		if (ants.getEnemyHills().contains(dest)) {	// enemy-hills auch angreifen
        			Tile replaced = addAimer(myAnt, dest, depth+1, 1); // 5 is max dorthin
        			if (myAnt != replaced) {
        				log("attack hill");
                		traceBackData.put(dest, new QueueData(current, direction, depth+1));	// required by trace back
                		if (replaced != null) {
                			removeOrder(replaced);
                		}
                		return traceBack(dest, myAnt);
        			} else {
        				log("schon genug bessere dahin unterwegs");
        			}
        		}
        		if (ants.getFoodTiles().contains(dest)) {
        			Tile replaced = addAimer(myAnt, dest, depth+1, 3); // 5 is max dorthin
        			if (myAnt != replaced) {
        				log("get food");
        				if (replaced != null) {
        					removeOrder(replaced);
        				}
                		return traceBack(current, myAnt);
        			} else {
        				log("schon genug bessere dahin unterwegs");
        			}        			
        		}
        		floodFillQueue.addLast(dest);
        		traceBackData.put(dest, new QueueData(current, direction, depth+1));
     		}
    	}
    	if (current != null) {	// wenn nichts anderes angezielt, dann ist current als letztes in Queue gewesen und somit am weitesten vom Ausgangspunkt weg == weitester freier Weg
			log("geh sonst wo hin");
    		return traceBack(current, myAnt);
    	} else {
    		return null;
    	}
    }

    private void removeOrder(Tile replaced) {
		Order removedOrder = pendingOrders.remove(replaced);
		log("removed order: "+removedOrder);
		if (removedOrder != null) {
			blocked.remove(ants.getTile(new Tile(removedOrder.row, removedOrder.col), Aim.fromSymbol(removedOrder.direction)));
		}
	}

	/** returns, which tile was replaced by start; or null if less than maxNeeded or null if start was worse than what we got */
 	private Tile addAimer(Tile start, Tile dest, int depth, int maxNeeded) {
		if (!aimingInfo.containsKey(dest)) {
			aimingInfo.put(dest, new ArrayList<Aimer>());
		}
		List<Aimer> list = aimingInfo.get(dest);
		if (list.size() < maxNeeded) {
			list.add(new Aimer(start, depth));
			Collections.sort(aimingInfo.get(dest));
			return null;
		} else {
			if (list.get(list.size()-1).steps < depth) {
				return start;	// new one is farther away than what we got
			} else {
				Aimer removed = list.remove(list.size()-1);
				list.add(new Aimer(start, depth));
				Collections.sort(aimingInfo.get(dest));
				return removed.tile;	// new one is better, remove worse one
			}
		}
 	}

	private Aim traceBack(Tile current, Tile start) {
		QueueData back = traceBackData.get(current);
		if (back == null) {
			log("traceback is null");
			return null;
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
    
	private static void log(Object s) {
		if (LOGGING) {
			System.err.println(s.toString());
		}
	}	
}
