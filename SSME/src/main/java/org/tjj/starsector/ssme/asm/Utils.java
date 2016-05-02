package org.tjj.starsector.ssme.asm;

import org.objectweb.asm.Type;

public class Utils {

	/**
	 * Compares 2 arrays of Types.
	 * 1st compares length, then elements.
	 * null elements in the desiredTypes are not compared. (they always match)
	 * 
	 * @param types
	 * @param desiredTypes
	 * @return
	 */
	public static boolean typesMatch(Type[] types, Type[] desiredTypes) {
		if(types.length != desiredTypes.length) return false;
		
		for(int i = 0;i < desiredTypes.length;i++) {
			if(desiredTypes[i]==null) continue; //null elements are not compared
			if(desiredTypes[i].equals(types[i])) continue;
			
			return false;
		}
		return true;
	}
}
