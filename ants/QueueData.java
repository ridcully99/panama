/*
 *  Copyright 2004-2010 Robert Brandner (robert.brandner@gmail.com) 
 *  
 *  Licensed under the Apache License, Version 2.0 (the "License"); 
 *  you may not use this file except in compliance with the License. 
 *  You may obtain a copy of the License at 
 *  
 *  http://www.apache.org/licenses/LICENSE-2.0 
 *  
 *  Unless required by applicable law or agreed to in writing, software 
 *  distributed under the License is distributed on an "AS IS" BASIS, 
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 *  See the License for the specific language governing permissions and 
 *  limitations under the License. 
 */

/**
 * @author ridcully
 *
 */
public class QueueData {

	public Tile origin;
	public Aim originAimed;
	public int steps;
	public int ownMet;
	public int enemiesMet;
	public int waterMet;	// used for finding hill defenders
	public int unseenMet;
	
	public QueueData(Tile origin, QueueData previous, Aim direction, boolean metOwn, boolean metEnemy, boolean hitWater, boolean hitUnseen) {
		this(origin, previous, direction, metOwn, metEnemy);
		waterMet = previous.waterMet + (hitWater ? 1 : 0);
		unseenMet = previous.unseenMet + (hitUnseen ? 1 : 0);
	}
	
	public QueueData(Tile origin, QueueData previous, Aim direction, boolean metOwn, boolean metEnemy) {
		this(origin, direction, previous.steps+1);
		ownMet = previous.ownMet + (metOwn ? 1 : 0);
		enemiesMet = previous.enemiesMet + (metEnemy ? 1: 0);
	}
	
	public QueueData(Tile origin, Aim originAimed, int steps) {
		this.origin = origin;
		this.originAimed = originAimed;
		this.steps = steps;
	}

}
