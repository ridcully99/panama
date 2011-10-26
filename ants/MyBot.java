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
 * Version 10: Multiseed Fill
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

    private Map<Aim, Aim[]> avoidingDirections = new HashMap<Aim, Aim[]>();
    private Aim[][] seekOrders = new Aim[][] {{Aim.NORTH, Aim.EAST, Aim.SOUTH, Aim.WEST},
    										  {Aim.EAST, Aim.SOUTH, Aim.WEST, Aim.NORTH},
    										  {Aim.SOUTH, Aim.WEST, Aim.NORTH, Aim.EAST},
    										  {Aim.WEST, Aim.NORTH, Aim.EAST, Aim.SOUTH}};
    private List<Quadrant> quadrants = new ArrayList<Quadrant>();
    
    @Override
    public void setup(int loadTime, int turnTime, int rows, int cols,
    		int turns, int viewRadius2, int attackRadius2, int spawnRadius2) {
    	super.setup(loadTime, turnTime, rows, cols, turns, viewRadius2, attackRadius2,
    			spawnRadius2);
    	
    	avoidingDirections.put(Aim.EAST, new Aim[] {Aim.WEST, Aim.NORTH, Aim.SOUTH});
    	avoidingDirections.put(Aim.NORTH, new Aim[] {Aim.SOUTH, Aim.EAST, Aim.WEST});
    	avoidingDirections.put(Aim.WEST, new Aim[] {Aim.EAST, Aim.NORTH, Aim.SOUTH});
    	avoidingDirections.put(Aim.SOUTH, new Aim[] {Aim.NORTH, Aim.EAST, Aim.WEST});
    	
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
	    		}
	    	}
    	}
    } 
    
    private Ants ants;
    private Set<Tile> blocked = new HashSet<Tile>();
    private LinkedList<Tile> floodFillQueue = new LinkedList<Tile>();
    private Map<Tile, QueueData> traceBackData = new HashMap<Tile, QueueData>();
	private Set<Tile> foodEaten = new HashSet<Tile>();
	private Set<Tile> antsWithOrder = new HashSet<Tile>();
	
    /**
     */
    @Override
    public void doTurn() {
    	
    	// reset quadrants before getAnts()
    	for (Quadrant q : quadrants) {
    		q.myAntsCount = 0;
    	}
    	
        ants = getAnts();
        
        Collections.sort(quadrants, new Comparator<Quadrant>() {	// sort by # of ants
			@Override
			public int compare(Quadrant a, Quadrant b) {
				return a.myAntsCount - b.myAntsCount;
			}
        });
        
        blocked.clear();
    	antsWithOrder.clear();
    	
    	findTargetsForAnts();
    	findAntsForTargets();
    }
    
    private void findTargetsForAnts() {
    	floodFillQueue.clear();
    	traceBackData.clear();
    	foodEaten.clear();
    	
    	// seed all an once (except ants next to food)
        for (Tile myAnt : ants.getMyAnts()) {
        	Tile food = isNextToFood(myAnt);
        	if (food != null && !foodEaten.contains(food)) {	// food next to the ant --> let it eat and remember for others not to eat it
        		foodEaten.add(food);
        		antsWithOrder.add(myAnt);
        		continue;	// fressen lassen
        	}
        	if (!antsWithOrder.contains(myAnt)) {
	        	floodFillQueue.add(myAnt);
	        	traceBackData.put(myAnt, new QueueData(myAnt, myAnt, null, 0));
        	}
        }
        
    	while(!floodFillQueue.isEmpty()) {
    		Tile current = floodFillQueue.pollFirst();
    		QueueData currentData = traceBackData.get(current);
    		Tile ant = currentData.origin;
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
//        			if (traceBackData.get(dest).origin != ant) {	// covered by different ant --> remember that ant, and if we find no other place to go for us, we'll that ant's target
//        				
//        			}
        			continue;	// got covered already
        		}
        		if (ants.getEnemyHills().contains(dest)) {		// enemy-hills auch angreifen
            		traceBackData.put(dest, new QueueData(ant, current, direction, currentDepth+1));	// required by trace back
            		QueueData move = traceBack(dest);			// move move.cameFrom nach move.direction
            		order(move, "move towards enemy hill at "+dest);
            		continue;
        		}
        		if (ants.getFoodTiles().contains(dest)) {
        			QueueData move = traceBack(current);		// move move.cameFrom nach move.direction
        			order(move, "move near food at "+dest);
        			continue;
        		}
        		if (ants.getEnemyAnts().contains(dest)) {
            		traceBackData.put(dest, new QueueData(ant, current, direction, currentDepth+1));	// required by trace back
        			QueueData moveTowardsEnemy = traceBack(dest);
        			boolean attack = ants.getMyAnts().size() > ants.getEnemyAnts().size() * 2;	// TODO erster Versuch, testen/verbessern
        			if (attack) {
        				order(moveTowardsEnemy, "move towards enemy at "+dest);
        			} else {
        				QueueData moveAway = avoidDirection(moveTowardsEnemy);
        				order(moveAway, "avoid enemy at "+dest);
        			}
        			continue;
        		}
        		
        		if (currentDepth > 30 && quadrants.get(0).contains(dest)) {
            		traceBackData.put(dest, new QueueData(ant, current, direction, currentDepth+1));	// required by trace back
            		QueueData move = traceBack(dest);			// move move.cameFrom nach move.direction
            		order(move, "move towards most empty quadrant to "+dest);
            		continue;
        		}
        		
//        		// avoid own --> spread ... hoffentlich <-- das ist nix
        		if (ants.getMyAnts().contains(dest)) {
            		traceBackData.put(dest, new QueueData(currentData.origin, current, direction, currentDepth+1));	// required by trace back
        			QueueData move = traceBack(dest);
        			QueueData moveAway = avoidDirection(move);
        			order(moveAway, "move away from own at "+dest);
        			continue;
        		}

        		// TODO keep some ants near own hill(s) 
        		
        		floodFillQueue.addLast(dest);
        		QueueData qd = new QueueData(ant, current, direction, currentDepth+1);
        		traceBackData.put(dest, qd);
     		}
    	}
    	// TODO
        // am Ende die die noch keinen befehlt haben, entweder zu enemy hills oder enemies schicken (wenn welche entdeckt wurden)
    	// oder sonst dorthin wo noch keine eigenen sind.
    	// und zwar die naehesten meiner ants die noch keinen befehl haben und wieder mit traceback ---> erneutes multiseed floodfill
    	// aber mit diesen zielen
    	
    	log("Ants ohne Order: "+(ants.getMyAnts().size() - antsWithOrder.size()));
    }

    private void findAntsForTargets() {
		int seekStart = 0;
    	for (Tile ant : ants.getMyAnts()) {
    		if (antsWithOrder.contains(ant)) {
    			continue;
    		}
    		for (Aim direction : seekOrders[seekStart]) {
     			Tile dest = ants.getTile(ant, direction);
            	if (!blocked.contains(dest) && ants.getIlk(ant, direction).isUnoccupied()) {
            		order(ant, direction, "Notloesung");
            	}
    		}
    		seekStart += 1;
    		seekStart %= 4;
    	}
    }
    
	private QueueData traceBack(Tile tile) {
		QueueData data = traceBackData.get(tile);
		if (data.steps == 0) {	// if tile is the startpoint
			return data;		// hat direction = null
		}
		while (data.steps > 1) {
			data = traceBackData.get(data.cameFrom);
		}
		return data;
	}

 	private QueueData avoidDirection(QueueData moveTowardsEnemy) {
 		QueueData move = new QueueData(moveTowardsEnemy.cameFrom, null, moveTowardsEnemy.steps);
 		Tile ant = move.cameFrom;
 		Aim origDirection = moveTowardsEnemy.direction;
 		for (Aim newDirection : avoidingDirections.get(origDirection)) {
 			Tile dest = ants.getTile(ant, newDirection);
        	if (!blocked.contains(dest) && ants.getIlk(ant, newDirection).isUnoccupied()) {
        		move.direction = newDirection;
        		return move;
        	}
 		}
 		return move; /* with null direction, so no move -- best we can do here */
	}
 	
 	private void order(QueueData move, String msg) {
    	if (move != null && move.direction != null) {
    		Tile ant = move.cameFrom;
    		order(ant, move.direction, msg);
    	}
 	}
 		
 	private void order(Tile ant, Aim direction, String msg) {	
		if (antsWithOrder.contains(ant)) {
			return;
		}
    	Tile dest = ants.getTile(ant, direction);
    	if (!blocked.contains(dest) && ants.getIlk(ant, direction).isUnoccupied()) {
    		ants.issueOrder(ant, direction);
    		blocked.add(dest);
    		antsWithOrder.add(ant);
    		log("sent "+ant+" to "+direction+": "+msg);
    	}
 	}
 	
	/* nicht diagonal */
    private Tile isNextToFood(Tile ant) {
    	for (Aim direction : Aim.values()) {
    		if (ants.getIlk(ant, direction) == Ilk.FOOD) {
    			return ants.getTile(ant, direction);
    		}
    	}
    	return null;
    }
    
	private static void log(Object s) {
		if (LOGGING) {
			System.err.println(s.toString());
		}
	}	
}
