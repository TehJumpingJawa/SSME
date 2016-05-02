package org.tjj.starsector.ssme.asm;

public class Literal {

	public static final Literal UNKNOWN = new Literal("Value Unknown");
	public static final Literal NULL = new Literal("null");
	
	private String s;
	private Literal(String s) {
		this.s = s;
	}
	
	public String toString() {
		return s;
	}
}
