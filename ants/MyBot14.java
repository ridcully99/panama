import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Version 14 (online set 7.11.)
 *
 * changed (since Version 13)
 * - fixed and improved hill defense (less sophisticated, better working)
 * - improved attacking algorithm (hopefully) but still not perfect; moved out of traceback loop -> much better 
 * 
 * changed (since Version 12)
 * - implemented correct attacking algorithm 
 * 
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
public class MyBot14 extends Bot {
	
	private final static boolean LOGGING = false;
	private final static int ALMOST_TIMEOUT = 20;
	//*DEBUG ONLY!!!*/private final static int ALMOST_TIMEOUT = Integer.MIN_VALUE;

	
	private int extendedAttackRadius2;		// extended weil wir 1 Step vorausblicken
	
    /**
     * Main method executed by the game engine for starting the bot.
     * 
     * @param args command line arguments
     * 
     * @throws IOException if an I/O error occurs
     */
    public static void main(String[] args) throws IOException {
        new MyBot14().readSystemInput();
    }

    @Override
    public void setup(int loadTime, int turnTime, int rows, int cols, int turns, int viewRadius2, int attackRadius2, int spawnRadius2) {
    	super.setup(loadTime, turnTime, rows, cols, turns, viewRadius2, attackRadius2, spawnRadius2);
    	extendedAttackRadius2 = 13; //9;//attackRadius2;//2*(int)(1d+(attackRadius+2d) * (attackRadius+2d));
    	log("extendedAttackRadius2="+extendedAttackRadius2);
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
    private Map<Tile, Tile> hilldefenders = new HashMap<Tile, Tile>();	// Ant->Hill; werden bei bestDirection beruecksichtigt
    
    /**
     */
    @Override
    public void doTurn() {
    	ants = getAnts();
    	foodEaten += foodEatenLastTurn();
    	// hilldefenders. 1 so schnell wie moeglich, dann ab 20 2 ab 30 3 usw.
    	hilldefenders.clear();
    	if (ants.getMyHills().size() > 0 && (ants.getMyAnts().size() >= 2 * ants.getMyHills().size())) {
    		int defenderCountPerHill = 1;
    		if (ants.getMyAnts().size() >= 20 * ants.getMyHills().size()) {
    			defenderCountPerHill = (ants.getMyAnts().size()/ants.getMyHills().size())/15;
    			defenderCountPerHill = Math.min(defenderCountPerHill, 3);
    		}
	    	for (Tile hill : ants.getMyHills()) {
	    		findHillDefenders(hill, defenderCountPerHill);
	    	}
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
     * find num ants next to hill
     */
    private void findHillDefenders(Tile hill, int num) {
    	floodFillQueue.clear();
    	floodFillQueue.add(hill);
    	traceBackData.clear();
    	traceBackData.put(hill, new QueueData(null, null, 0));
    	Tile currentTile = null;		// ausserhalb damit ich nachher das Tile das als letztes aus Queue genommen wurde, habe --> wenn kein Food dann in die Richtung gehen weil "am weitesten freie Bahn"
    	QueueData currentData = null;	// -""-
    	int found = 0;
    	while(!floodFillQueue.isEmpty() && found < num && ants.getTimeRemaining() >= ALMOST_TIMEOUT) {
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
        		if (metOwn) {
        			hilldefenders.put(dest, hill);
        			found++;
        			log("hilldefender: "+dest);
        		}
        		floodFillQueue.addLast(dest);
        		traceBackData.put(dest, new QueueData(currentTile, currentData, direction, metOwn, false));
     		}
    	}
    }
    
    /**
     * find best direction for ant
     */
    private Aim findBestDirection(Tile myAnt) {
    	Tile enemyInRange = enemyInRange(myAnt, extendedAttackRadius2); 
		if (enemyInRange != null && !enemyHillInRange(myAnt, extendedAttackRadius2)) {
			if (!wouldSurviveFight(myAnt) && !hilldefenders.containsKey(myAnt)) {	// hilldefender immer kaempfen!!
				log(myAnt + " avoid fight against "+enemyInRange);
				// weg von naehester enemyAnt (ist die die von enemyInRange gefunden wird)
				List<Aim> directions = ants.getDirections(myAnt, enemyInRange);
				if (directions.size() == 1) {
					return directions.get(0).behind();
				} else {
					Tile dest = ants.getTile(myAnt, directions.get(0).behind());
					Ilk i = ants.getIlk(dest);
					if (i.isPassable() && i.isUnoccupied() && !blocked.contains(dest)) {
						return directions.get(0).behind();
					} else {
						return directions.get(1).behind();
					}
				}
			} else {
				log(myAnt +" attack "+enemyInRange);
				// hin zu naehester enemyAnt (TODO besser waere in den Tracebackmechanismus einbauen aber dann muss ich ueberal pruefen ob attacking)
				List<Aim> directions = ants.getDirections(myAnt, enemyInRange);
				if (directions.size() == 1) {
					return directions.get(0);
				} else {
					Tile dest = ants.getTile(myAnt, directions.get(0));
					Ilk i = ants.getIlk(dest);
					if (i.isPassable() && i.isUnoccupied() && !blocked.contains(dest)) {
						return directions.get(0);
					} else {
						return directions.get(1);
					}
				}
			}
		}
    	
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
    		if (currentData.steps == 0 && currentTile.equals(longTermDestination.get(myAnt))) {	// ant has reached longterm destionation
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
        		
        		// hilldefender und dest mein hill?
        		if (hilldefenders.containsKey(myAnt) && dest.equals(hilldefenders.get(myAnt))) {
    				clearLongTermDestination(myAnt);
        			if (currentData.steps == 2) {		// bin schon in abstand 2 von meinem hill.
        				return null;	
        			} else if (currentData.steps > 2) {	// zu weit weg, naeher hin
                		traceBackData.put(dest, new QueueData(currentTile, currentData, direction, metOwn, metEnemy));	// required by trace back
        				return traceBack(dest, myAnt);
        			} else if (currentData.steps < 2) {	// zu nah, weiter weg gehen
        				
        			}
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

        		if (dest.equals(longTermDestination.get(myAnt))) {													// hit long term destination?
            		traceBackData.put(dest, new QueueData(currentTile, currentData, direction, metOwn, metEnemy));	// required by trace back
            		return traceBack(dest, myAnt);
        		}
        		
        		if (currentData.steps == 30) {
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
				
    	// entweder hatte ant noch kein longtermdestination oder hat es nicht schon vorher gefunden 
		// (was durch temporaere blockierungen passiert sein kann, oder seit version 12 auch durch "break queue" oder am wahrscheinlichsten durch terrain das beim festlegen nicht bekannt war.
		longTermDestination.remove(myAnt);	// raushau'n und neues suchen
		
		//Tile bestDest = findLongTermDestination();
		
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
		/*TEST-so kein Vorteil*/Tile randomizedDest = longTermTargets.get((int)(longTermTargets.size()*Math.random()));
		Tile firstDest = longTermTargets.get(0);
		longTermDestination.put(myAnt, firstDest);
		return traceBack(firstDest, myAnt);
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

	// ---- logging ---------------------------------------------------------------------------------
    
	private void log(Object s) {
		if (LOGGING) {
			System.err.println(turn+": "+s.toString());
		}
	}
	
	// ---- fighting stuff --------------------------------------------------------------------------
	
	private Map<Tile, Integer> antOwners = new HashMap<Tile, Integer>();
	
	/**
	 * override to be able to tell which enemy an ant belongs to
	 */
	@Override
	public void addAnt(int row, int col, int owner) {
		super.addAnt(row, col, owner);
		antOwners.put(new Tile(row, col), owner);
	}
	
	/** quick test, if any enemies in range */
	private Tile enemyInRange(Tile myAnt, int radius2) {
		for (Tile enemy : ants.getEnemyAnts()) {
			if (ants.getDistance(myAnt, enemy) <= radius2) {
				return enemy;
			}
		}
		return null;
	}
	
 	private boolean enemyHillInRange(Tile myAnt, int radius2) {
		for (Tile enemyHill : ants.getEnemyHills()) {
			if (ants.getDistance(myAnt, enemyHill) <= radius2) {
				return true;
			}
		}
		return false;
	}	
	
	private Set<Tile> getEnemiesInRange(Tile center, int owner, int radius2) {
		Set<Tile> inRange = new HashSet<Tile>();
		for (Map.Entry<Tile, Integer> a : antOwners.entrySet()) {
			if (a.getValue() != owner && ants.getDistance(center, a.getKey()) <= radius2) {
				inRange.add(a.getKey());
			}
		}
		return inRange;
	}
	
	/**
	 * checks if my ant would survive fight, according to rules of website
	 * "Then, if there is _any_ ant is next to an enemy with an equal or lesser number, it will die"
	 */
	private boolean wouldSurviveFight(Tile location) {
		Set<Tile> enemies = getEnemiesInRange(location, 0, extendedAttackRadius2);
		if (enemies.size() == 0) {
			return true;
		}
		for (Tile enemy : enemies) {
			Set<Tile> enemiesEnemies = getEnemiesInRange(enemy, antOwners.get(enemy), ants.getAttackRadius2());	// da den originalradius
			if (enemiesEnemies.size() <= enemies.size()) {
				return false;
			}
		}
		return true;
	}
}
