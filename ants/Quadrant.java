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
public class Quadrant {

	public int minRow, minCol, maxRow, maxCol;
	public int myAntsCount = 0;
	
	public Quadrant(int minRow, int minCol, int maxRow, int maxCol) {
		this.minRow = minRow;
		this.minCol = minCol;
		this.maxRow = maxRow;
		this.maxCol = maxCol;
	}
	
	public boolean contains(int row, int col) {
		return minRow <= row &&
		  row <= maxRow &&
		  minCol <= col &&
		  col <= maxCol;
	}
	
	public boolean contains(Tile tile) {
		return contains(tile.getRow(), tile.getCol());
	}
	
	public String toString() {
		return "["+minRow+", "+minCol+"]-["+maxRow+", "+maxCol+"]:"+myAntsCount;
	}
}
