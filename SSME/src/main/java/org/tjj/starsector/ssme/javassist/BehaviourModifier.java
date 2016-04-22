package org.tjj.starsector.ssme.javassist;

import java.lang.reflect.Modifier;
import java.util.EnumSet;

public enum BehaviourModifier {
	STATIC("static"),
	FINAL("final"),
	SYNCHRONIZED("synchronized"),
	VOLATILE("volatile"),
	TRANSIENT("transient"),
	NATIVE("native"),
	INTERFACE("interface"),
	ABSTRACT("abstract"),
	STRICT("strictfp");
	

	private String name;
	
	BehaviourModifier(String name) {
		this.name = name;
	}
	
	public static BehaviourModifier fromString(String string) {
		BehaviourModifier [] values = BehaviourModifier.values();
		for (BehaviourModifier behaviourModifier : values) {
			if(string.equals(behaviourModifier.name)) {
				return behaviourModifier;
			}
		}
		throw new IllegalArgumentException(string + " not a valid BehaviourModifier");
	}
	
	public static EnumSet<BehaviourModifier> fromStringList(String string) {
		String [] modifier = string.trim().split(" +");
		EnumSet<BehaviourModifier> set = EnumSet.noneOf(BehaviourModifier.class);
		for (String string2 : modifier) {
			set.add(fromString(string2));
		}
		return set;
	}	
	
	
	public static EnumSet<BehaviourModifier> fromJvmModifiers(int bits) {
		final int BEHAVIOUR_MODIFIERS_MASK = Modifier.STATIC | Modifier.FINAL | Modifier.SYNCHRONIZED | Modifier.VOLATILE | Modifier.TRANSIENT | Modifier.NATIVE | Modifier.INTERFACE | Modifier.ABSTRACT | Modifier.STRICT;
		bits&=BEHAVIOUR_MODIFIERS_MASK;
		// the STATIC bit is now in the right-most('1') position.
		bits>>>=3;
		
		EnumSet<BehaviourModifier> set = EnumSet.noneOf(BehaviourModifier.class);
		BehaviourModifier [] behaviours = BehaviourModifier.values(); 

		int ordinal = 0;
		while(bits!=0) {
			
			if((bits&1)!=0) {
				set.add(behaviours[ordinal]);
			}
			ordinal++;
			bits>>>=1;
		}
		return set;
	}
}
