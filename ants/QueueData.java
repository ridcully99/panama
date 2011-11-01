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
	public int unknown;
	
	public QueueData(Tile origin, Aim originAimed, int steps) {
		this.origin = origin;
		this.originAimed = originAimed;
		this.steps = steps;
	}

	public QueueData(Tile origin, Aim originAimed, int steps, int unknown) {
		this.origin = origin;
		this.originAimed = originAimed;
		this.steps = steps;
		this.unknown = unknown;
	}

}
