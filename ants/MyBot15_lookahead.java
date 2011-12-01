import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Version 15 (...)
 * 
 * changed since version 14
 * - unseen tiles richtig berechnen und als ziel fuer ausbreitung verwenden -- aber nicht nur.
 * - Bug in fighting-Berechnung gefixt (antOwners wurde nie resettet)
 * - fight-Berechnung in Ueberarbeitung
 * - dynamic-defense
 * 
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
public class MyBot15_lookahead extends Bot {
	
	private final static boolean LOGGING = true;
	public int ALMOST_TIMEOUT = 20;
	
	public int VIEWRADIUS_STEPS = 12;	// ca. 12 steps entsprechen akt. viewradius2 von 77
	
	private final static int I_WOULD_DIE = -1;
	private final static int BOTH_WOULD_DIE = 0;
	private final static int I_WOULD_SURVIVE = 1;

    private Map<Tile, Tile> longTermDestination = new HashMap<Tile, Tile>();	// ant --> destination
    private Ants ants;
    private Set<Tile> blocked = new HashSet<Tile>();
    private Map<Tile, Tile> sent = new HashMap<Tile, Tile>();						// keep track of ants already sent and their new position
    private int turn = 0;
    private int antsDied = 0;
    private int foodEaten = 0;
    private Set<Tile> foodLastTurn = new HashSet<Tile>();
    private Map<Tile, Tile> hillDefenders = new HashMap<Tile, Tile>();				// Ant->Hill; werden bei bestDirection beruecksichtigt
    
	private Map<Tile, Integer> antOwners = new HashMap<Tile, Integer>();
    private int antsInHive;
	private int extendedAttackRadius2;		// extended weil wir 1 Step vorausblicken
	
    /**
     * Main method executed by the game engine for starting the bot.
     * 
     * @param args command line arguments
     * 
     * @throws IOException if an I/O error occurs
     */
    public static void main(String[] args) throws IOException {
    	MyBot15_lookahead myBot = new MyBot15_lookahead();
        if (args.length > 0 && "debug".equals(args[0])) {
        	myBot.ALMOST_TIMEOUT = Integer.MIN_VALUE;
        }
        if (args.length > 0 && "tcp".equals(args[0])) {
        	myBot.ALMOST_TIMEOUT = 1000;
        }
        myBot.readSystemInput();
    }

    @Override
    public void setup(int loadTime, int turnTime, int rows, int cols, int turns, int viewRadius2, int attackRadius2, int spawnRadius2) {
    	super.setup(loadTime, turnTime, rows, cols, turns, viewRadius2, attackRadius2, spawnRadius2);
    	extendedAttackRadius2 = 17; //10; //9;//attackRadius2;//2*(int)(1d+(attackRadius+2d) * (attackRadius+2d));
    }
    
    @Override
    public void beforeUpdate() {
    	super.beforeUpdate();
    	antOwners.clear();
    }
    
	/**
	 * override to be able to tell which enemy an ant belongs to
	 */
	@Override
	public void addAnt(int row, int col, int owner) {
		super.addAnt(row, col, owner);
		antOwners.put(new Tile(row, col), owner);
	}
	
    @Override
    public void removeAnt(int row, int col, int owner) {
    	super.removeAnt(row, col, owner);
    	if (owner == 0) { 
    		antsDied++;
    		longTermDestination.remove(new Tile(row, col));
    	}
    }
    
    /**
     */
    @Override
    public void doTurn() {
    	try {
	    	ants = getAnts();
	    	sent.clear();
	    	// ants in hive bestimmen
	    	foodEaten += foodEatenLastTurn();
	    	antsInHive = (1+foodEaten-antsDied)-ants.getMyAnts().size();	// wieviele es sein sollten - wieviele es sind
	
	    	findHillDefenders();
	    	//findEnemyHillAttackers();
	    	
	    	blocked.clear();
	    	
	        for (Tile myAnt : ants.getMyAnts()) {
	        	if (sent.keySet().contains(myAnt)) {
	        		continue;
	        	}
	        	// da wir uns enemy-hills jetzt fuer immer merken, muessen wir eroberte selbst aus liste entfernen, sonst werden sie unnoetig weiter angegriffen
	        	if (ants.getEnemyHills().contains(myAnt)) {
	        		ants.getEnemyHills().remove(myAnt);
	        	}
	        	Aim direction = findBestDirection(myAnt);
	        	sendAnt(myAnt, direction);
	        	timeoutCheck();
	        }
    	} catch (RuntimeException e) {
    		// exceptions are thrown to prevent timeouts.
    		log("almost-timeout exception");
    		e.printStackTrace(System.err);
    	} finally {
	        log("time-spent: "+(ants.getTurnTime()-ants.getTimeRemaining())+" ; #ants: "+ants.getMyAnts().size()+" #unseen: "+ants.unseen.size());
    		turn++;
    	}
    }


    /** send ant in a direction */
	private void sendAnt(Tile myAnt, Aim direction) {
		Tile dest = null;
    	if (direction != null) {
        	dest = ants.getTile(myAnt, direction);
        	if (blocked.contains(dest) || !(ants.getIlk(dest).isUnoccupied() || ants.getMyAnts().contains(dest))) {	// wenn occupied von eigener ant dann trotzdem okay, weil ich sie eh wegbewege (ausser in extremen ausnahmefaellen)
        		log("bestDirection cannot be used, finding replacement.");
        		direction = anyWhere(myAnt);
        		dest = ants.getTile(myAnt, direction);
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
    		blocked.add(myAnt);
    		dest = myAnt;
    	}
    	sent.put(myAnt, dest);
	}

	private LinkedList<Tile> floodFillQueue = new LinkedList<Tile>();
    private Map<Tile, QueueData> traceBackData = new HashMap<Tile, QueueData>();
    
    /**
     */
    private void findHillDefenders() {
    	
    	hillDefenders.clear();
    	for (Tile hill : ants.getMyHills()) {
    		findHillDefenders(hill);
    	}
    }
    
    /** 
     */
    private void findHillDefenders(Tile hill) {
    	List<Tile> antsMet = new ArrayList<Tile>();	// Liste der getroffenen Ameisen, geordnet anhand Naehe zum Hill
    	int balance = 0;
    	floodFillQueue.clear();
    	traceBackData.clear();
    	floodFillQueue.add(hill);
    	traceBackData.put(hill, new QueueData(null, null, 0));
    	Tile currentTile = null;		// ausserhalb damit ich nachher das Tile das als letztes aus Queue genommen wurde, habe --> wenn kein Food dann in die Richtung gehen weil "am weitesten freie Bahn"
    	QueueData currentData = null;	// -""-
    	queue:
    	while(!floodFillQueue.isEmpty()) {
    		timeoutCheck();
    		currentTile = floodFillQueue.pollFirst();
    		currentData = traceBackData.get(currentTile);
    		
    		// fuer eigene currentTile nehmen statt dest, damit die eines naeher sein muessen als die gegner
    		if (ants.getMyAnts().contains(currentTile)) {
    			antsMet.add(currentTile);
    			balance++;
    		}
    		
     		for (Aim direction : Aim.values()) {
        		Tile dest = ants.getTile(currentTile, direction);
        		if (!ants.getIlk(dest).isPassable()) {
        			continue;	// da gehts nicht weiter
        		}
        		
        		if (traceBackData.containsKey(dest)) {
        			continue;	// got covered already
        		}
        		
        		boolean metOwn = ants.getMyAnts().contains(dest);		// nur fuer queuedata
        		boolean metEnemy = ants.getEnemyAnts().contains(dest);	
        		if (metEnemy && currentData.steps <= VIEWRADIUS_STEPS) { // enemies nur so weit beruecksichtigen wie viewradius
           			antsMet.add(dest);
           		    balance--; 
        		}
        		if (balance > 0 && currentData.steps > VIEWRADIUS_STEPS) {
        			break queue;
        		}
        		floodFillQueue.addLast(dest);
        		QueueData qd = new QueueData(currentTile, currentData, direction, metOwn, metEnemy, false, false);
        		traceBackData.put(dest, qd);
     		}
    	}
    	
    	balance = 0;
    	if (ants.getMyAnts().size() > 10) {	// wenn ich ein paar ameisen habe, dann balance so, dass mindest. 1 beim hill bleibt
    		balance = -1;
    	}
    	for (Tile ant : antsMet) {
    		if (ants.getMyAnts().contains(ant)) {
    			if (balance < 0) {	// mehr gegner als eigene zw. ant und hill --> zurueck zum hill!
    				hillDefenders.put(ant, hill);
    			}
    			balance++;
    		} else {
    			balance--;
    		}
    	}
    }
    
    
    /**
     * find best direction for ant
     */
    private Aim findBestDirection(Tile myAnt) {

//    	Tile enemyInRange = enemyInRange(myAnt, extendedAttackRadius2);
//		boolean hillDefender = hillDefenders.containsKey(myAnt);
//    	if (enemyInRange != null && !hillDefender) {
//    		boolean wouldSurvive = (I_WOULD_SURVIVE == wouldSurviveFight(myAnt));
//    		if (wouldSurvive) {
//				log(myAnt +" attack "+enemyInRange);
//				// hin zu naehester enemyAnt
//				return findMoveTowardsEnemy(myAnt, enemyInRange);
//    		} else {
//				log(myAnt + " avoid fight against "+enemyInRange);
//				// weg von naehester enemyAnt (ist die die von enemyInRange gefunden wird)
//				return findMoveAwayFromEnemey(myAnt, enemyInRange);
//			}
//		}
    	
    	floodFillQueue.clear();
    	traceBackData.clear();
    	floodFillQueue.add(myAnt);
    	traceBackData.put(myAnt, new QueueData(null, null, 0));
    	
    	Tile currentTile = null;		// ausserhalb damit ich nachher das Tile das als letztes aus Queue genommen wurde, habe --> wenn kein Food dann in die Richtung gehen weil "am weitesten freie Bahn"
    	QueueData currentData = null;	// -""-

    	queue:
    	while(!floodFillQueue.isEmpty()) {
    		timeoutCheck();
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
        		
        		if (currentData.steps == 0 && wouldSurvive(myAnt, direction) != I_WOULD_SURVIVE) {
        			continue;
        		}
        		
        		boolean metOwn = ants.getMyAnts().contains(dest);
        		boolean metEnemy = ants.getEnemyAnts().contains(dest);
        		boolean hitUnseen = ants.isUnseen(dest);
        		
        		// hilldefender und dest mein hill? naeher zum hill gehen aber nicht drauf (daher currentTile, nicht dest)
        		if (hillDefenders.containsKey(myAnt) && dest.equals(hillDefenders.get(myAnt))) {
    				clearLongTermDestination(myAnt);
        			return traceBack(currentTile, myAnt);
        		}
        		
        		// enemy hills angreifen
        		if (ants.getEnemyHills().contains(dest)) {
        			clearLongTermDestination(myAnt);
            		traceBackData.put(dest, new QueueData(currentTile, currentData, direction, metOwn, metEnemy, false, hitUnseen));	// required by trace back
            		log(myAnt + " heading for enemy hill at "+dest);
            		return traceBack(dest, myAnt);
        		}
        		
        		if (ants.getFoodTiles().contains(dest) && currentData.ownMet == 0) {								// food und myAnt am naehesten
        			clearLongTermDestination(myAnt);
        			return traceBack(currentTile, myAnt);
        		}

        		if (dest.equals(longTermDestination.get(myAnt))) {													// hit long term destination?
            		traceBackData.put(dest, new QueueData(currentTile, currentData, direction, metOwn, metEnemy, false, hitUnseen));	// required by trace back
            		//log(myAnt + " continues on path to longtermdest "+dest);
            		return traceBack(dest, myAnt);
        		}
        		
        		if (currentData.unseenMet > 0/* || currentData.steps >= ants.getRows()/2*/) {
        			log(myAnt+" is heading for unseen at "+dest);
        			break queue;
        		}
        		
        		floodFillQueue.addLast(dest);
        		traceBackData.put(dest, new QueueData(currentTile, currentData, direction, metOwn, metEnemy, false, hitUnseen));
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
		
		// alle mit Tiefe >= currentData.steps suchen.
		int ownMetMinimum = 9999;
		int maxUnseen = -1;
		List<Tile> longTermTargets = new ArrayList<Tile>();
		for (QueueData d : traceBackData.values()) {
			if (d.steps >= currentData.steps) {
				if (d.ownMet < ownMetMinimum || d.unseenMet > maxUnseen) {
    				ownMetMinimum = Math.min(ownMetMinimum, d.ownMet);
    				maxUnseen = Math.max(maxUnseen, d.unseenMet);
    				longTermTargets.clear();
    				longTermTargets.add(d.origin);			// ist halt das vorige, aber auch ok. ganz korrekt waere der key zum value.
				} else if (d.ownMet == ownMetMinimum) {
					longTermTargets.add(d.origin);			// ist halt das vorige, aber auch ok. ganz korrekt waere der key zum value.
				}
			}
		}
		Tile firstDest = longTermTargets.get(0);	// TODO eventuell das nehmen mit den meisten getroffenen enemies? oder zumindest unter gewissen umstaenden?
		
		longTermDestination.put(myAnt, firstDest);
		log(myAnt + " got new longtermdest "+firstDest);
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

	// ---- logging && timeout-stuff -----------------------------------------------------------------
    
	private void log(Object s) {
		if (LOGGING) {
			System.err.println(turn+": "+s.toString());
		}
	}
	
    private void timeoutCheck() {
    	if (ants.getTimeRemaining() < ALMOST_TIMEOUT) {
    		throw new RuntimeException();
    	}
	}

	// ---- fighting stuff --------------------------------------------------------------------------
	
	
	private Aim findMoveTowardsEnemy(Tile me, Tile enemy) {
		List<Aim> directions = ants.getDirections(me, enemy);
		for (Aim direction : directions) {
			Tile dest = ants.getTile(me, direction);
			Ilk ilk = ants.getIlk(dest);
			if (ilk.isPassable() && 
				!blocked.contains(dest) &&
				(ilk.isUnoccupied() || ants.getMyAnts().contains(dest))) {
				return direction;
			}
		}
		return anyWhere(me);	// towards not possible, at least move anywhere, otherwise might crash with own ant!
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
				!blocked.contains(dest) &&
				(ilk.isUnoccupied() || ants.getMyAnts().contains(dest))) {
				return direction;
			}
		}
		return anyWhere(me);	// away not possible, at least move anywhere, otherwise might crash with own ant!
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
	
	private Set<Tile> getMineInRange(Tile center, int radius2) {
		Set<Tile> inRange = new HashSet<Tile>();
		for (Tile a : ants.getMyAnts()) {
			if (ants.getDistance(center, a) <= radius2) {
				inRange.add(a);
			}
		}
		return inRange;
	}
	
	/**
	 * checks if my ant would survive fight, according to rules of website
	 * "Then, if there is _any_ ant is next to an enemy with an equal or lesser number, it will die"
	 */
	private int wouldSurviveFight(Tile location) {
		Set<Tile> enemies = getEnemiesInRange(location, 0, extendedAttackRadius2);
		if (enemies.size() == 0) {
			return I_WOULD_SURVIVE;
		}
		int result = I_WOULD_SURVIVE;
		for (Tile enemy : enemies) {
			timeoutCheck();
			Set<Tile> enemiesEnemies = getEnemiesInRange(enemy, antOwners.get(enemy), extendedAttackRadius2);
			if (enemiesEnemies.size() < enemies.size()) {
				result = I_WOULD_DIE;
				break;
			}
			if (enemiesEnemies.size() == enemies.size()) {
				result = BOTH_WOULD_DIE;
			}
		}
		return result;
	}
	
	
	private int wouldSurvive(Tile myAnt, Aim direction) {
		Tile center = ants.getTile(myAnt, direction);
		Set<Tile> mine = getMineInRange(center, extendedAttackRadius2);
		Set<Tile> others = getEnemiesInRange(center, 0, extendedAttackRadius2);
		
		if (others.size() == 0) {
			return I_WOULD_SURVIVE;	// no enemies
		}

		log("wouldSurvive "+myAnt+" ; "+direction);
		
		Set<Tile> mineMoved = new HashSet<Tile>();
		Set<Tile> othersMoved = new HashSet<Tile>();
		Map<Tile, Integer> movedAntOwners = new HashMap<Tile, Integer>();

		// move mine
		for (Iterator<Tile> it = mine.iterator(); it.hasNext();) {
			Tile a = it.next();
			// wenn diese ant schon bewegt wurde, dann fix deren neue position nehmen
			if (sent.keySet().contains(a)) {
				Tile dest = sent.get(a);
				mineMoved.add(dest);
				movedAntOwners.put(dest, 0/*antOwners.get(a)*/);
				
				it.remove();
				continue;
			}
			List<Aim> directions = ants.getDirections(a, center);
			// wenn 2 directions, die finden die mich in kampfnaehe zu mehr der enemies bringt...
			Tile betterDest = null;
			int maxEnemiesInFightingRange = -1;
			for (Aim dir : directions) {
				timeoutCheck();
				Tile dest = ants.getTile(a, dir);
				Ilk ilk = ants.getIlk(dest);
				if (ilk == Ilk.MY_ANT || ilk == Ilk.ENEMY_ANT) {	// anpassen wg. lookahead
					ilk = Ilk.LAND;
				}
				if (ilk.isPassable() && 
				  !mineMoved.contains(dest) &&
				  !othersMoved.contains(dest) &&
				  (ilk.isUnoccupied() || mine.contains(dest))) {
					int enemiesInFightingRange = 0;
					for (Tile e : others) {
						if (ants.getDistance(dest, e) <= ants.getAttackRadius2()) {
							enemiesInFightingRange++;
						}
					}
					if (enemiesInFightingRange > maxEnemiesInFightingRange) {
						maxEnemiesInFightingRange = enemiesInFightingRange;
						betterDest = dest;
					}
				}
			}
			if (betterDest != null) {
				mineMoved.add(betterDest);
				movedAntOwners.put(betterDest, 0/*antOwners.get(a)*/);
			} else {	// keine Bewegung moeglich, originalpos uebernehmen
				mineMoved.add(a);
				movedAntOwners.put(a, 0/*antOwners.get(a)*/);
			}
			it.remove();

		}
		
		// move others
		for (Iterator<Tile> it = others.iterator(); it.hasNext();) {
			timeoutCheck();
			Tile a = it.next();
			List<Aim> directions = ants.getDirections(a, center);
			boolean added = false;
			for (Aim dir : directions) {
				timeoutCheck();
				Tile dest = ants.getTile(a, dir);
				Ilk ilk = ants.getIlk(dest);
				if (ilk == Ilk.MY_ANT || ilk == Ilk.ENEMY_ANT) {	// anpassen wg. lookahead
					ilk = Ilk.LAND;
				}
				if (ilk.isPassable() && 
				  !mineMoved.contains(dest) &&
				  !othersMoved.contains(dest) &&
				  (ilk.isUnoccupied() || others.contains(dest))) {
					othersMoved.add(dest);
					movedAntOwners.put(dest, antOwners.get(a));
					added = true;
					break;
				}
			}
			if (!added) {	// kein move moeglich, dann orignalpos uebernehmen
				othersMoved.add(a);
				movedAntOwners.put(a, antOwners.get(a));
			}
			it.remove();
		}

		// evaluate fighting of moved ants
		Set<Tile> enemiesInFightingRange = new HashSet<Tile>();
		for (Tile e : othersMoved) {
			timeoutCheck();
			// find moved enemies of mine, in actual fighting range of center
			if (ants.getDistance(center, e) <= ants.getAttackRadius2()) {
				enemiesInFightingRange.add(e);
			}
		}
		
		int result = I_WOULD_SURVIVE;
		for (Tile enemy : enemiesInFightingRange) {
			timeoutCheck();
			// find moved enemies of enemy in actual fighting range of the moved enemy
			Set<Tile> enemiesEnemiesInFightingRange = new HashSet<Tile>();
			int owner = movedAntOwners.get(enemy);
			for (Map.Entry<Tile, Integer> a : movedAntOwners.entrySet()) {
				log("distance:"+enemy+","+a.getKey());
				if (a.getValue() != owner && ants.getDistance(enemy, a.getKey()) <= ants.getAttackRadius2()) {
					enemiesEnemiesInFightingRange.add(a.getKey());
				}
			}
			if (enemiesEnemiesInFightingRange.size() < enemiesInFightingRange.size()) {
				result = I_WOULD_DIE;
				break;
			}
			if (enemiesEnemiesInFightingRange.size() == enemiesInFightingRange.size()) {
				result = BOTH_WOULD_DIE;
			}
		}
		log("returning "+result);
		return result;
	}
}
