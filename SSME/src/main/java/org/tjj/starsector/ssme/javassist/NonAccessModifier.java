package org.tjj.starsector.ssme.javassist;

import java.lang.reflect.Modifier;
import java.util.EnumSet;

public enum NonAccessModifier {
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
	
	NonAccessModifier(String name) {
		this.name = name;
	}
	
	public static NonAccessModifier fromString(String string) {
		NonAccessModifier [] values = NonAccessModifier.values();
		for (NonAccessModifier behaviourModifier : values) {
			if(string.equals(behaviourModifier.name)) {
				return behaviourModifier;
			}
		}
		throw new IllegalArgumentException(string + " not a valid BehaviourModifier");
	}
	
	public static EnumSet<NonAccessModifier> fromStringList(String string) {
		String [] modifier = string.trim().split(" +");
		EnumSet<NonAccessModifier> set = EnumSet.noneOf(NonAccessModifier.class);
		for (String string2 : modifier) {
			set.add(fromString(string2));
		}
		return set;
	}	
	
	
	public static EnumSet<NonAccessModifier> fromJvmModifiers(int bits) {
		final int BEHAVIOUR_MODIFIERS_MASK = Modifier.STATIC | Modifier.FINAL | Modifier.SYNCHRONIZED | Modifier.VOLATILE | Modifier.TRANSIENT | Modifier.NATIVE | Modifier.INTERFACE | Modifier.ABSTRACT | Modifier.STRICT;
		bits&=BEHAVIOUR_MODIFIERS_MASK;
		// the STATIC bit is now in the right-most('1') position.
		bits>>>=3;
		
		EnumSet<NonAccessModifier> set = EnumSet.noneOf(NonAccessModifier.class);
		NonAccessModifier [] behaviours = NonAccessModifier.values(); 

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
