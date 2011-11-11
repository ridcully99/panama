import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Version 15 (...)
 * 
 * FAIL: fighting zentriert auf enemy Ants nicht gut genug da Move des Enemies nicht beruecksichtigt.
 * 
 * changed since version 14
 * - unseen tiles richtig berechnen und als ziel fuer ausbreitung verwenden
 * - Abbruch nach n steps entfernt
 * - Bestimmung longtermdest nach queue-Abarbeitung nicht mehr via ownMet sondern ganz simpel currentData.origin (wie am Anfang).
 * - fighting aus Sicht der enemy Ants berechnen
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
public class _MyBot15Failure1 extends Bot {
	
	private final static boolean LOGGING = true;
	private final static int ALMOST_TIMEOUT = 20;
	//*DEBUG ONLY!!!*/private final static int ALMOST_TIMEOUT = Integer.MIN_VALUE;

    /**
     * Main method executed by the game engine for starting the bot.
     * 
     * @param args command line arguments
     * 
     * @throws IOException if an I/O error occurs
     */
    public static void main(String[] args) throws IOException {
        new _MyBot15Failure1().readSystemInput();
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
    		longTermDestination.remove(new Tile(row, col));
    	}
    }
    
    private Map<Tile, Tile> longTermDestination = new HashMap<Tile, Tile>();	// ant --> destination
    
    private Ants ants;
    private Set<Tile> blocked = new HashSet<Tile>();
    private Set<Tile> attackers = new HashSet<Tile>();
    private int turn = 0;
    private int antsDied = 0;
    private int foodEaten = 0;
    private Set<Tile> foodLastTurn = new HashSet<Tile>();
    private Map<Tile, Tile> hilldefenders = new HashMap<Tile, Tile>();	// Ant->Hill; werden bei bestDirection beruecksichtigt
    private int antsInHive;
    
    /**
     */
    @Override
    public void doTurn() {
    	ants = getAnts();

    	// ants in hive bestimmen
    	foodEaten += foodEatenLastTurn();
    	antsInHive = (1+foodEaten-antsDied)-ants.getMyAnts().size();	// wieviele es sein sollten - wieviele es sind

    	// hilldefenders:
    	// 1 so schnell wie moeglich, dann ab 20 2 ab 30 3 usw.
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
    	attackers.clear();
    	
    	// check and try attacks on enemy ants
    	for (Tile enemyAnt : ants.getEnemyAnts()) {
    		tryAttack(enemyAnt);
    	}
    	
        for (Tile myAnt : ants.getMyAnts()) {
        	if (attackers.contains(myAnt)) {
        		continue;	// already moved by tryAttack
        	}
        	// da wir uns enemy-hills jetzt fuer immer merken, muessen wir eroberte selbst aus liste entfernen, sonst werden sie unnoetig weiter angegriffen
        	if (ants.getEnemyHills().contains(myAnt)) {
        		ants.getEnemyHills().remove(myAnt);
        	}
        	Aim direction = findBestDirection(myAnt);
        	sendAnt(myAnt, direction);

        	if (ants.getTimeRemaining() < ALMOST_TIMEOUT) {
        		break;
        	}
        }
        log("#unseen: "+ants.unseen.size());
        turn++;
    }

    /** send ant in a direction */
	private void sendAnt(Tile myAnt, Aim direction) {
    	if (direction != null) {
        	Tile dest = ants.getTile(myAnt, direction);
        	if (blocked.contains(dest) || !(ants.getIlk(dest).isUnoccupied() || ants.getMyAnts().contains(dest))) {	// wenn occupied von eigener ant dann trotzdem okay, weil ich sie eh wegbewege (ausser in extremen ausnahmefaellen)
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
	}

	private LinkedList<Tile> floodFillQueue = new LinkedList<Tile>();
    private Map<Tile, QueueData> traceBackData = new HashMap<Tile, QueueData>();
    
    /**
     * find ants next to hill
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
        			if (currentData.steps < 2) {		// zu nah, weiter weg
                		traceBackData.put(dest, new QueueData(currentTile, currentData, direction.behind(), metOwn, metEnemy));	// required by trace back
        				return traceBack(dest, myAnt);
        			} else if (currentData.steps >= 3) {	// zu weit weg, naeher hin
                		traceBackData.put(dest, new QueueData(currentTile, currentData, direction, metOwn, metEnemy));	// required by trace back
        				return traceBack(dest, myAnt);
        			}
        		}
        		
        		// enemy hills angreifen
        		if (ants.getEnemyHills().contains(dest)) {
        			clearLongTermDestination(myAnt);
            		traceBackData.put(dest, new QueueData(currentTile, currentData, direction, metOwn, metEnemy));	// required by trace back
            		log(myAnt + " attacks enemy hill "+dest);
            		return traceBack(dest, myAnt);
        		}
        		
        		// enemy in der naehe, dann weggehen - offenbar hat tryAttack Angriff nicht durchgefuehrt.
        		if (metEnemy) {
        			int dist = ants.getDistance(myAnt, dest);
        			if (dist <= 10) {
        				clearLongTermDestination(myAnt);
        				log(myAnt+" avoids enemy "+dest);
        				return findMoveAwayFromEnemey(myAnt, dest);
        			}
        		}
        		
        		if (ants.getFoodTiles().contains(dest) && currentData.ownMet == 0) {	// food und myAnt am naehesten
        			clearLongTermDestination(myAnt);
            		//log(myAnt + " found food at "+dest);
        			return traceBack(currentTile, myAnt);
        		}

        		if (dest.equals(longTermDestination.get(myAnt))) {													// hit long term destination?
            		traceBackData.put(dest, new QueueData(currentTile, currentData, direction, metOwn, metEnemy));	// required by trace back
            		log(myAnt + " reached longtermdest "+dest);
            		return traceBack(dest, myAnt);
        		}
        		
        		// unseen und noch kein anderer dorthin unterwegs?
        		if (ants.isUnseen(dest) && !longTermDestination.values().contains(dest)) {
        			//log(myAnt+" heads for unseen spot at "+dest);
            		traceBackData.put(dest, new QueueData(currentTile, currentData, direction, metOwn, metEnemy));	// required by trace back
            		//log(myAnt + " goes for unseen at "+dest);
            		return traceBack(dest, myAnt);
        		}
        		
        		floodFillQueue.addLast(dest);
        		traceBackData.put(dest, new QueueData(currentTile, currentData, direction, metOwn, metEnemy));
     		}
    	}
    	// nichts besonderes gefunden.
		if (currentData.steps < 2) {						// hm, nicht weit gekommen --> irgendwohin gehen
			log(myAnt + " steps < 2");
			return anyWhere(myAnt);
		}
				
    	// entweder hatte ant noch kein longtermdestination oder hat es nicht schon vorher gefunden 
		// (was durch temporaere blockierungen passiert sein kann, oder seit version 12 auch durch "break queue" oder am wahrscheinlichsten durch terrain das beim festlegen nicht bekannt war.
		longTermDestination.remove(myAnt);	// raushau'n und neues suchen
		
		// wie am Anfang einfach currentData.origin als longtermdest nehmen
		longTermDestination.put(myAnt, currentData.origin);
		log(myAnt + " got new longtermdestination " + currentData.origin);
		return traceBack(currentData.origin, myAnt);
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
 				(ants.getIlk(dest).isUnoccupied() || ants.getMyAnts().contains(dest)) &&
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
	
	Map<Tile, Aim> movesTowardsEnemy = new HashMap<Tile, Aim>();
	Set<Tile> tempBlocked = new HashSet<Tile>();
	
	private void tryAttack(Tile enemyAnt) {
		int enemy = antOwners.get(enemyAnt);
		movesTowardsEnemy.clear();
		tempBlocked.clear();
		for (Tile ant : ants.getMyAnts()) {
			if (attackers.contains(ant)) {	// not available anymore
				continue;
			}
			int dist = ants.getDistance(ant, enemyAnt);
			if (dist <= 10) {											// can attack with zero or one step
				Aim direction = findMoveTowardsEnemy(ant, enemyAnt);	// aufpassen das nicht 2 eigene kollidieren
				if (direction != null) {
					movesTowardsEnemy.put(ant, direction);
					tempBlocked.add(ants.getTile(ant, direction));
// TODO wenns prinzipiell geht, dann hier Optimieren mit vorzeitigem Abbruch
//					if (movesTowardsEnemy.size() >= 4) {				// das wird wohl reichen, erspart weitere Ants zu testen
//						break;
//					}
				}
			}
		}
		//enemies zaehlen
		int enemies = 0; 												// der im zentrum wird bei loop mitgezaehlt
		int enemiesEnemies = 0;											// Ants die weder mir noch angegriffenem enemy gehoeren
		for (Tile ant : ants.getEnemyAnts()) {
			int dist = ants.getDistance(ant, enemyAnt);
			if (dist > 10) {
				continue;
			}
			if (antOwners.get(ant) == enemy) {
				enemies++;
			} else {
				enemiesEnemies++;
			}
		}
		
		// angriff wagen?
		if (movesTowardsEnemy.size() > enemies) {
			log("attacking ant at "+enemyAnt+" with "+movesTowardsEnemy.size()+" ants.");
			for (Map.Entry<Tile, Aim> entry : movesTowardsEnemy.entrySet()) {
				sendAnt(entry.getKey(), entry.getValue());
				attackers.add(entry.getKey());
			}
		}
	}

	
	private Aim findMoveTowardsEnemy(Tile me, Tile enemy) {
		List<Aim> directions = ants.getDirections(me, enemy);
		for (Aim direction : directions) {
			Tile dest = ants.getTile(me, direction);
			Ilk ilk = ants.getIlk(dest);
			if (ilk.isPassable() && 
				(ilk.isUnoccupied() || ants.getMyAnts().contains(dest)) &&
				!tempBlocked.contains(dest)) {
				return direction;
			}
		}
		return null;
	}
	
	
	private Aim findMoveAwayFromEnemey(Tile me, Tile enemy) {
		List<Aim> directions = ants.getDirections(me, enemy);	// towards enemy
		for (Aim direction : Aim.values()) {
			if (directions.contains(direction)) {				// don't go towards enemy
				continue;
			}
			Tile dest = ants.getTile(me, direction);
			Ilk ilk = ants.getIlk(dest);
			if (ilk.isPassable() && 
				(ilk.isUnoccupied() || ants.getMyAnts().contains(dest))) {
				return direction;
			}
		}
		return null;
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
		Set<Tile> enemies = getEnemiesInRange(location, 0, ants.getAttackRadius2());
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
