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
 * 24.11.2011: Wenn nicht kaempfen, aside statt away gehen
 * 24.11.2011: Fehler gefixt, unseen war wichtiger als enemyHillAttack
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
public class MyBot extends Bot {
	
	private final static boolean LOGGING = true;
	public int ALMOST_TIMEOUT = 20;
	
	public int VIEWRADIUS_STEPS = 12;	// ca. 12 steps entsprechen akt. viewradius2 von 77
	
    private Map<Tile, Tile> longTermDestination = new HashMap<Tile, Tile>();	// ant --> destination
    private Ants ants;
    private Set<Tile> blocked = new HashSet<Tile>();
    private Set<Tile> sent = new HashSet<Tile>();								// keep track of ants, already sent
    private int turn = 0;
    private Map<Tile, Tile> hillDefenders = new HashMap<Tile, Tile>();			// Ant->Hill; werden bei bestDirection beruecksichtigt
    private Set<Tile> noGos = new HashSet<Tile>();								// virtually blocked tiles. I should not go there 
    private boolean keepWatch = false;
    
	private Map<Tile, Integer> antOwners = new HashMap<Tile, Integer>();
	private Set<Tile> enemyHillsThisTurn = new HashSet<Tile>();
	private int extendedAttackRadius2;		// extended weil wir 1 Step vorausblicken
	private int maxStepsToAttackHill;		// wenn mehr steps als diese bis Hill, dann nicht explizit angreifen
	
    /**
     * Main method executed by the game engine for starting the bot.
     * 
     * @param args command line arguments
     * 
     * @throws IOException if an I/O error occurs
     */
    public static void main(String[] args) throws IOException {
    	MyBot myBot = new MyBot();
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
    	maxStepsToAttackHill = (getAnts().getRows()+getAnts().getCols()) / 2;
    }
    
    @Override
    public void beforeUpdate() {
    	super.beforeUpdate();
    	antOwners.clear();
    	enemyHillsThisTurn.clear();
    }
    
    @Override
    public void addHill(int row, int col, int owner) {
    	super.addHill(row, col, owner);
    	if (owner != 0) {
    		enemyHillsThisTurn.add(new Tile(row, col));
    	}
    }
    
	/**
	 * override to be able to tell which enemy an ant belongs to
	 */
	@Override
	public void addAnt(int row, int col, int owner) {
		super.addAnt(row, col, owner);
		Tile t = new Tile(row, col);
		antOwners.put(new Tile(row, col), owner);
		// nicht mehr vorhandene enemy-hills entfernen
		for (Iterator<Tile> iterator = ants.getEnemyHills().iterator(); iterator.hasNext(); ) {
			Tile hill = iterator.next();
			if (ants.getDistance(t, hill) <= ants.getViewRadius2() && !enemyHillsThisTurn.contains(hill)) {
				log("enemy hill at "+hill+" has been razed");
				iterator.remove();
			}
		}
	}
	
    @Override
    public void removeAnt(int row, int col, int owner) {
    	super.removeAnt(row, col, owner);
    	if (owner == 0) { 
    		longTermDestination.remove(new Tile(row, col));
    	}
    }
    
    /**
     * doTurn
     */
    @Override
    public void doTurn() {
    	try {
	    	ants = getAnts();
	    	sent.clear();
	
	    	findHillDefenders();
	    	
	    	blocked.clear();
	    	noGos.clear();

	    	//attacks();
	    	
	        for (Tile myAnt : ants.getMyAnts()) {
	        	if (sent.contains(myAnt)) {
	        		continue;
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
    		if (blocked.contains(myAnt)) {	// wurde schon jemand auf unsern platz geschickt!
    			direction = anyWhere(myAnt);
    			if (direction != null) {
    				sendAnt(myAnt, direction); // --> doch irgendwo hingehen
    			}
    		}
    		blocked.add(myAnt);
    	}
    	sent.add(myAnt);
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
        		if (metEnemy /*&& currentData.steps <= VIEWRADIUS_STEPS*/) { // enemies nur so weit beruecksichtigen wie viewradius
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
    	if (keepWatch) {	// balance so, dass mindest. 1 beim hill bleibt
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

    	Tile enemyInRange = enemyInRange(myAnt, extendedAttackRadius2);
    	if (enemyInRange != null) {
    		int fightPrediction = simulateFight(myAnt);
    		if (fightPrediction > 0) {
				log("attacking "+enemyInRange);
				for (Tile a : getMineInRange(enemyInRange, extendedAttackRadius2)) {
					longTermDestination.put(a, enemyInRange);
				}
    		//} else if (fightPrediction == 0) {	// stehen bleiben
    		//	return null;
    		} else { // I_WOULD_DIE
				log(myAnt + " avoid fight against "+enemyInRange+"; would need "+Math.abs(fightPrediction)+" verstaerkung");
				// weg von naehester enemyAnt (ist die die von enemyInRange gefunden wird)
				for (Tile a : getMineInRange(enemyInRange, extendedAttackRadius2)) {
					if (!hillDefenders.containsKey(a)) {	// hilldefender haben schon longtermdest hill
						longTermDestination.put(a, ants.getTile(a, findMoveAsideFromEnemey(a, enemyInRange)));
					}
				}
			}
		}
    	
    	Map<QueueData, Tile> enemyHillAttacks = new HashMap<QueueData, Tile>();
    	
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
        		if (noGos.contains(dest)) {	
        			continue;	// nicht Richtung enemy gehen wo ich sterben wuerde
        		}
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
        		boolean hitUnseen = ants.isUnseen(dest);
        		
        		if (metEnemy) {
        			keepWatch = true;
        		}
        		
        		// hilldefender und dest mein hill? naeher zum hill gehen aber nicht drauf (daher currentTile, nicht dest)
        		if (hillDefenders.containsKey(myAnt) && dest.equals(hillDefenders.get(myAnt))) {
    				clearLongTermDestination(myAnt);
        			return traceBack(currentTile, myAnt);
        		}
        		
        		// enemy hills angreifen
        		if (ants.getEnemyHills().contains(dest) && currentData.steps < maxStepsToAttackHill) {
        			clearLongTermDestination(myAnt);
        			enemyHillAttacks.put(new QueueData(currentTile, currentData, direction, metOwn, metEnemy, false, hitUnseen), dest);
        			continue;
        		}
        		
        		if (ants.getFoodTiles().contains(dest) && currentData.ownMet == 0 && enemyHillAttacks.isEmpty()) {		// food und myAnt am naehesten und nicht hill naeher als food
        			clearLongTermDestination(myAnt);
        			return traceBack(currentTile, myAnt);
        		}

        		if (dest.equals(longTermDestination.get(myAnt))) {											// hit long term destination?
            		traceBackData.put(dest, new QueueData(currentTile, currentData, direction, metOwn, metEnemy, false, hitUnseen));	// required by trace back
            		//log(myAnt + " continues on path to longtermdest "+dest);
            		return traceBack(dest, myAnt);
        		}
        		
        		if (hitUnseen && enemyHillAttacks.isEmpty()) {
            		traceBackData.put(dest, new QueueData(currentTile, currentData, direction, metOwn, metEnemy, false, hitUnseen));	// required by trace back
            		longTermDestination.put(myAnt, dest);
            		return traceBack(dest, myAnt);
        		}
        		
        		floodFillQueue.addLast(dest);
        		traceBackData.put(dest, new QueueData(currentTile, currentData, direction, metOwn, metEnemy, false, hitUnseen));
     		}
    	}
    	
    	if (!enemyHillAttacks.isEmpty()) {	// go to enemy-hill where least enemies are underway
    		clearLongTermDestination(myAnt);
    		QueueData best = null;
    		for (QueueData q : enemyHillAttacks.keySet()) {
    			if (best == null || best.enemiesMet > q.enemiesMet || (best.enemiesMet == q.enemiesMet && q.steps < best.steps)) {
    				best = q;
    			}
    		}
    		Tile dest = enemyHillAttacks.get(best);
    		traceBackData.put(dest, best);
    		return traceBack(dest, myAnt);
    	}
    	
    	// nichts besonderes gefunden.
		if (currentData.steps < 2) {						// hm, nicht weit gekommen --> irgendwohin gehen
			log(myAnt + " steps < 2");
			return anyWhere(myAnt);
		}
				
    	// entweder hatte ant noch kein longtermdestination oder hat es nicht schon vorher gefunden 
		// (was durch temporaere blockierungen passiert sein kann, oder seit version 12 auch durch "break queue" oder am wahrscheinlichsten durch terrain das beim festlegen nicht bekannt war.
		clearLongTermDestination(myAnt);
		
		// wenn kein unseen, dann aufschliessen zu Freunden.
		// alle mit Tiefe >= currentData.steps suchen.
		int ownMetMaximum = 0;
		List<Tile> longTermTargets = new ArrayList<Tile>();
		for (QueueData d : traceBackData.values()) {
			if (d.steps >= currentData.steps) {
				if (d.ownMet > ownMetMaximum) {
					ownMetMaximum = Math.max(ownMetMaximum, d.ownMet);
    				longTermTargets.clear();
    				longTermTargets.add(d.origin);			// ist halt das vorige, aber auch ok. ganz korrekt waere der key zum value.
				} else if (d.ownMet == ownMetMaximum) {
					longTermTargets.add(d.origin);			// ist halt das vorige, aber auch ok. ganz korrekt waere der key zum value.
				}
			}
		}
		Tile firstDest = longTermTargets.get(0);	// TODO eventuell das nehmen mit den meisten getroffenen enemies? oder zumindest unter gewissen umstaenden?
		
		longTermDestination.put(myAnt, firstDest);
		return traceBack(firstDest, myAnt);
    }


    /**
     * trackBack
     */
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

	
	/**
	 * anyWhere
	 */
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
	
	
	private Aim findMoveAsideFromEnemey(Tile me, Tile enemy) {
		List<Aim> directions = ants.getDirections(me, enemy);	// towards enemy
		List<Aim> aside = new ArrayList<Aim>();
		for (Aim direction : directions) {
			aside.add(direction.left());
			aside.add(direction.right());
		}
		for (Aim direction : aside) {
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

	private Set<Tile> getMineInRange(Tile center, int radius2) {
		Set<Tile> inRange = new HashSet<Tile>();
		for (Tile a : ants.getMyAnts()) {
			if (!sent.contains(a) && ants.getDistance(center, a) <= radius2) {
				inRange.add(a);
			}
		}
		return inRange;
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
	 * checks if ant at location would survive fight, according to rules of website
	 * "Then, if there is _any_ ant is next to an enemy with an equal or lesser number, it will die"
	 */
	private int simulateFight(Tile location) {
		int owner = antOwners.get(location);
		Set<Tile> enemies = getEnemiesInRange(location, owner, extendedAttackRadius2);
		int lowest = 999;
		for (Tile enemy : enemies) {
			timeoutCheck();
			Set<Tile> enemiesEnemies = getEnemiesInRange(enemy, antOwners.get(enemy), extendedAttackRadius2);	// reduced exendedAttackRadius2
			int diff = enemiesEnemies.size() - enemies.size();
			lowest = Math.min(diff, lowest);
		}
		return lowest;	// wenn negativ, dann wuerde ich verlieren - und braeuchte |lowest| verstaerkung
	}
	
	// --- new attacks ---
	
	private void attacks() {
		for (Tile enemy : ants.getEnemyAnts()) {
			attack(enemy);
		}
	}
	
	private void attack(Tile enemy) {
		Set<Tile> mine = getMineInRange(enemy, extendedAttackRadius2);
		if (mine.size() == 0) {
			return;
		}
		int outcome = simulateFight(enemy);
		if (outcome < 0) { // enemy would die...
			// move all of mine towards enemy
			for (Tile a : mine) {
				Aim direction = findMoveTowardsEnemy(a, enemy);
				if (direction != null) {
					sendAnt(a, direction);
				}
			}
		} else {
			// make sure we do not go towards enemy
			for (Tile a : mine) {
				List<Aim> directions = ants.getDirections(a, enemy);
				for (Aim direction : directions) {
					Tile dest = ants.getTile(a, direction);
					noGos.add(dest);
				}
			}
		}
	}
}
