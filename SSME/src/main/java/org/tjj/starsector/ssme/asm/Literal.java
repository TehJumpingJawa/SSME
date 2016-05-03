package org.tjj.starsector.ssme.asm;

public class Literal {

	public static final Literal UNKNOWN = new Literal("Unknown");
	public static final Literal NULL = new Literal("null");
	
	public final String s;
	private Literal(String s) {
		this.s = s;
	}
	
	public String toString() {
		return s;
	}
}
