/*
 KylGis POS Monitor de Cocina
 Modifications Copyright (c) 2026 KylGis
 Portions Copyright (c) 2015 John Lewis / Chromis

 Based on Chromis Kitchen Screen. Upstream attribution is retained under the
 GNU General Public License, version 3 or (at your option) any later version.

 KylGis POS Monitor de Cocina is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by the
 Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 KylGis POS Monitor de Cocina is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with KylGis POS Monitor de Cocina. If not, see <http://www.gnu.org/licenses/>.
 */

package com.mx.kylgis.kitchenscr.utils;


import java.util.Stack;




public class FixedStack<T> extends Stack<T> {

	private final int stackSize;

	public FixedStack(int size) {
		super();
		this.stackSize = size;
	}

	 @Override
	 public Object push(Object object) {
		// If we have reached the maximum size of the stack, remove the lowest element
		while (this.size() >= stackSize) {
			 this.remove(0);
		}
		return super.push((T) object);
	}
	 
}