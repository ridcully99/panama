import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Version 12 (online auch als Version 12)
 * changed (since Version 11)
 * 
 * - sophisticated hill defense
 * - niemals stehen bleiben
 * - bessere verteilung von longterm
 * - zaehlen von eigenen und gegnern die unterwegs getroffen wurden
 * - ants in hive wieder entfernt als defense
 * - offense auf basis known territory entfernt
 * - queue abbruch bei tiefe n, statt immer ganz bis zum ende zu fuellen
 * 
 * added (since Version 9):
 * - Timeout Prevention
 * - Defense (including calculation of ants in hive)
 * - Offense (including calculation of % of known territory [ignoring viewradius, only counting tiles like ants, food, hills, water])
 * - Let nearest get the food
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
    private Map<Tile, Set<Tile>> hillsdefenders = new HashMap<Tile, Set<Tile>>();	// werden bei bestDirection beruecksichtigt
    
    /**
     */
    @Override
    public void doTurn() {
    	ants = getAnts();
    	foodEaten += foodEatenLastTurn();
    	hillsdefenders.clear();
    	for (Tile hill : ants.getMyHills()) {
    		hillsdefenders.put(hill, requiredHillDefenders(hill, ants.getViewRadius2()));
    	}
    	
    	blocked.clear();
        for (Tile myAnt : ants.getMyAnts()) {
        	// da wir uns enemy-hills jetzt fuer immer merken, muessen wir eroberte selbst aus liste entfernen, sonst werden sie unnoetig weiter angegriffen
        	if (ants.getEnemyHills().contains(myAnt)) {
        		ants.getEnemyHills().remove(myAnt);
        	}
        	Aim direction = findBestDirection(myAnt);
        	if (direction != null) {
            	Tile dest = ants.getTile(myAnt, direction);
            	if (blocked.contains(dest) || !ants.getIlk(myAnt, direction).isUnoccupied()) {
            		log("bestDirection cannot be used, finding replacement.");
            		direction = anyWhere(myAnt);
            	}
            	if (direction != null) {
            		ants.issueOrder(myAnt, direction);
            		blocked.add(dest);
            		// update movement in longTermDestination map
            		if (longTermDestination.containsKey(myAnt)) {
            			longTermDestination.put(dest, longTermDestination.get(myAnt));
            			longTermDestination.remove(myAnt);
            		}
            	}
        	} else {
        		log("no move for ant "+myAnt);
        	}
        	if (ants.getTimeRemaining() < ALMOST_TIMEOUT) {
        		break;
        	}
        }
        turn++;
    }

	private LinkedList<Tile> floodFillQueue = new LinkedList<Tile>();
    private Map<Tile, QueueData> traceBackData = new HashMap<Tile, QueueData>();
    
    /**
     * check hill safety and return list of ants needed for defense.
     * this will be an amount of own ants closest to the hill
     */
    private Set<Tile> requiredHillDefenders(Tile hill, int radius2) {
    	floodFillQueue.clear();
    	traceBackData.clear();
    	traceBackData.put(hill, new QueueData(null, null, 0));
    	Tile currentTile = null;		// ausserhalb damit ich nachher das Tile das als letztes aus Queue genommen wurde, habe --> wenn kein Food dann in die Richtung gehen weil "am weitesten freie Bahn"
    	QueueData currentData = null;	// -""-
    	int balance = 0;
    	boolean inDanger = true;
    	Set<Tile> requiredHelpers = new HashSet<Tile>();
    	queue:
    	while(!floodFillQueue.isEmpty() && ants.getTimeRemaining() >= ALMOST_TIMEOUT) {
    		currentTile = floodFillQueue.pollFirst();
    		currentData = traceBackData.get(currentTile);
     		for (Aim direction : Aim.values()) {
        		Tile dest = ants.getTile(currentTile, direction);
        		if (!ants.getIlk(dest).isPassable()) {
        			continue;	// da gehts nicht weiter
        		}
        		if (traceBackData.containsKey(dest)) {
        			continue;	// got covered already
        		}
        		boolean metOwn = ants.getMyAnts().contains(dest);
        		boolean metEnemy = ants.getEnemyAnts().contains(dest);
        		if (currentData.steps <= radius2) {	// balance nur innerhalb radius berechnen
	        		if (metOwn)   { 
	        			balance++;
	        			requiredHelpers.add(dest);	// verteidiger
	        		}
	        		if (metEnemy) { balance--; }
	        		if (balance < 0) { 		// more enemies closer to hill than own
	        			inDanger = true;	// und bleibt true
	        		}
        		} else {
        			if (!inDanger) {
        				requiredHelpers.clear();
        				return requiredHelpers;
        			} else {
        				if (metOwn) {
        					requiredHelpers.add(dest);	// verstaerkung
        					balance++;
        				}
    	        		if (metEnemy) { balance--; }
    	        		if (balance > 0) {
    	        			break queue;				// mehr verteidger als angreifer erreicht
    	        		}
        			}
        		}
        		floodFillQueue.addLast(dest);
        		traceBackData.put(dest, new QueueData(currentTile, currentData, direction, metOwn, metEnemy));
     		}
    	}
    	return requiredHelpers;
    }
    
    /**
     * find best direction for ant
     */
    private Aim findBestDirection(Tile myAnt) {
    	floodFillQueue.clear();
    	traceBackData.clear();
    	floodFillQueue.add(myAnt);
    	traceBackData.put(myAnt, new QueueData(null, null, 0));
    	Tile currentTile = null;		// ausserhalb damit ich nachher das Tile das als letztes aus Queue genommen wurde, habe --> wenn kein Food dann in die Richtung gehen weil "am weitesten freie Bahn"
    	QueueData currentData = null;	// -""-
    	queue:
    	while(!floodFillQueue.isEmpty() && ants.getTimeRemaining() >= ALMOST_TIMEOUT) {
    		currentTile = floodFillQueue.pollFirst();
    		currentData = traceBackData.get(currentTile);
    		if (currentData.steps == 0 && currentTile.equals(longTermDestination.get(myAnt))) {
    			//log(myAnt+" has reached longterm destination; removing it.");
    			clearLongTermDestination(myAnt);
    		}
     		for (Aim direction : Aim.values()) {
        		Tile dest = ants.getTile(currentTile, direction);
        		if (!ants.getIlk(dest).isPassable()) {
        			continue;	// da gehts nicht weiter
        		}
        		if (currentData.steps == 0 && blocked.contains(dest)) {
        			continue;	// von anderem Befehl blockiert
        		}
        		if (traceBackData.containsKey(dest)) {
        			continue;	// got covered already
        		}
        		boolean metOwn = ants.getMyAnts().contains(dest);
        		boolean metEnemy = ants.getEnemyAnts().contains(dest);
        		
        		// eigene hills verteidigen (wenn dest == hill der myAnt als defender wuenscht)
        		if (hillsdefenders.containsKey(dest) && hillsdefenders.get(dest).contains(myAnt)) {
        			log(myAnt+" is going to defend hill "+dest);
        			clearLongTermDestination(myAnt);
            		return traceBack(currentTile, myAnt);	// zu currentTile, nicht dest damit wir nicht am hill landen
        		}
        		
        		// enemy hills angreifen
        		if (ants.getEnemyHills().contains(dest)) {
        			clearLongTermDestination(myAnt);
            		traceBackData.put(dest, new QueueData(currentTile, currentData, direction, metOwn, metEnemy));	// required by trace back
            		return traceBack(dest, myAnt);
        		}
        		
        		if (ants.getFoodTiles().contains(dest) && currentData.ownMet == 0) {	// food und myAnt am naehesten
        			clearLongTermDestination(myAnt);
        			return traceBack(currentTile, myAnt);
        		}
//        		if (metEnemy) {	// TODO Kampf anders machen -- nicht mit zukuenftig, sondern wenn ich konkret
//        			if (currentData.ownMet > currentData.enemiesMet) {
//	        			clearLongTermDestination(myAnt);
//	            		traceBackData.put(dest, new QueueData(currentTile, currentData, direction, metOwn, metEnemy));	// required by trace back
//	            		return traceBack(dest, myAnt);
//	        		}
//        		}
        		if (dest.equals(longTermDestination.get(myAnt))) {										// hit long term destination?
            		traceBackData.put(dest, new QueueData(currentTile, currentData, direction, metOwn, metEnemy));	// required by trace back
            		//log("longterm-reached: "+myAnt+" -> "+dest);
            		return traceBack(dest, myAnt);
        		}
        		
        		if (currentData.steps == 20) {
        			break queue;
        		}
        		
        		floodFillQueue.addLast(dest);
        		traceBackData.put(dest, new QueueData(currentTile, currentData, direction, metOwn, metEnemy));
     		}
    	}
    	// nichts besonderes gefunden.

		if (currentData.steps < 2) {						// hm, nicht weit gekommen --> warten
			return anyWhere(myAnt);
		}
    	
    	// entweder hatte ant noch kein longtermdestination oder hat es nicht schon vorher gefunden (was eigentlich nur durch temporaere blockierungen passiert sein kann, oder seit version 12 auch durch "break queue")
		if (!longTermDestination.containsKey(myAnt)) {			// noch kein longtermdest --> eines setzen

    		// alle mit Tiefe currentData.steps suchen.
    		int ownMetMinimum = 9999;
    		List<Tile> longTermTargets = new ArrayList<Tile>();
    		for (QueueData d : traceBackData.values()) {
    			if (d.steps >= currentData.steps) {
    				if (d.ownMet < ownMetMinimum) {
        				ownMetMinimum = d.ownMet;
        				longTermTargets.clear();
        				longTermTargets.add(d.origin);			// ist halt das vorige, aber auch ok. ganz korrekt waere der key zum value.
    				} else if (d.ownMet == ownMetMinimum) {
    					longTermTargets.add(d.origin);			// ist halt das vorige, aber auch ok. ganz korrekt waere der key zum value.
    			
    				}
    			}
    		}
			longTermDestination.put(myAnt, longTermTargets.get(0));
			log("longterm - one of "+longTermTargets.size()+" options");
			return traceBack(longTermTargets.get(0), myAnt);
		} else {
			// hat schon eines, hat's aber via floodfill nicht erreicht...
			return anyWhere(myAnt);
		}
    }

 	private Aim traceBack(Tile current, Tile start) {
		QueueData back = traceBackData.get(current);
		if (back.origin == null) {
			return anyWhere(start);	// totally blocked?, nowhere to go right now? try to find any way to go.
		}
		while (!back.origin.equals(start)) {
			back = traceBackData.get(back.origin);
		}
		return back.originAimed;
	}

 	private Aim anyWhere(Tile ant) {
 		for (Aim direction : Aim.values()) {
 			Tile dest = ants.getTile(ant, direction);
 			if (ants.getIlk(dest).isPassable() && 
 				ants.getIlk(dest).isUnoccupied() &&
 				!blocked.contains(dest) &&
 				!ants.getMyHills().contains(dest)) {
 				return direction;
 			}
 		}
 		log("not even anyWhere found a way.");
 		return null;
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
