package org.tjj.starsector.ssme.javassist;

import java.lang.reflect.Modifier;
import java.util.EnumSet;

public enum AccessModifier {

	DEFAULT("default"), PUBLIC("public"), PRIVATE("private"), PROTECTED("protected");
	
	String name;
	private AccessModifier(String name) {
		this.name = name;
	}
	
	public static AccessModifier fromString(String string) {
		AccessModifier [] values = AccessModifier.values();
		for (AccessModifier accessModifier : values) {
			if(string.equals(accessModifier.name)) {
				return accessModifier;
			}
		}
		throw new IllegalArgumentException(string + " not a valid AccessModifier");
	}
	
	/**
	 * create an enum set of access modifiers from a String of space delimited modifiers
	 * e.g.
	 * 
	 * "default public private"
	 * 
	 * 
	 * @param accessModifiers
	 * @return
	 */
	public static EnumSet<AccessModifier> fromStringList(String string) {
		String [] modifier = string.trim().split(" +");
		EnumSet<AccessModifier> set = EnumSet.noneOf(AccessModifier.class);
		for (String string2 : modifier) {
			set.add(fromString(string2));
		}
		return set;
	}
	
	public static AccessModifier fromJvmModifiers(int bit) {
		final int ACCESS_MODIFIERS_MASK = Modifier.PUBLIC | Modifier.PRIVATE | Modifier.PROTECTED;
		bit&=ACCESS_MODIFIERS_MASK;
		
		switch(bit) {
		case 0:
			return DEFAULT;
		case Modifier.PUBLIC:
			return PUBLIC;
		case Modifier.PRIVATE:
			return PRIVATE;
		case Modifier.PROTECTED:
			return PROTECTED;
			default:
				throw new IllegalArgumentException(bit + " has more than one bit set");
		}
	}
}
