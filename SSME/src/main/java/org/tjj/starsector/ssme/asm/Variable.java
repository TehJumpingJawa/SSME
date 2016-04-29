package org.tjj.starsector.ssme.asm;

import org.objectweb.asm.Opcodes;

public final class Variable {

	public final Object type, literalValue;
	
	public Variable(Object type, Object literalValue) {
		this.type = type;
		this.literalValue = literalValue;
	}
	
	public Variable(Object type) {
		this.type = type;
		this.literalValue = Literal.UNKNOWN; 
	}
	
	public static final Variable UNKNOWN_INTEGER = new Variable(Opcodes.INTEGER);
	public static final Variable UNKNOWN_FLOAT = new Variable(Opcodes.FLOAT);
	public static final Variable UNKNOWN_LONG = new Variable(Opcodes.LONG);
	public static final Variable UNKNOWN_DOUBLE = new Variable(Opcodes.DOUBLE);
	public static final Variable UNKNOWN_TOP = new Variable(Opcodes.TOP);
	public static final Variable UNKNOWN_UNINITIALIZED_THIS = new Variable(Opcodes.UNINITIALIZED_THIS);
	public static final Variable UNKNOWN_OBJECT = new Variable("java/lang/Object");
	public static final Variable UNKNOWN_CLASS = new Variable("java/lang/Class");
	public static final Variable UNKNOWN_METHOD = new Variable("java/lang/invoke/MethodType");
	public static final Variable UNKNOWN_HANDLE = new Variable("java/lang/invoke/MethodHandle");
	public static final Variable NULL = new Variable(Opcodes.NULL, null);
	public static final Variable M1 = new Variable(Opcodes.INTEGER, -1);
	public static final Variable I0 = new Variable(Opcodes.INTEGER, 0);
	public static final Variable I1 = new Variable(Opcodes.INTEGER, 1);
	public static final Variable I2 = new Variable(Opcodes.INTEGER, 2);
	public static final Variable I3 = new Variable(Opcodes.INTEGER, 3);
	public static final Variable I4 = new Variable(Opcodes.INTEGER, 4);
	public static final Variable I5 = new Variable(Opcodes.INTEGER, 5);
	
	public static final Variable L0 = new Variable(Opcodes.LONG, (long)0);
	public static final Variable L1 = new Variable(Opcodes.LONG, (long)1);
	
	public static final Variable F0 = new Variable(Opcodes.FLOAT, (float)0);
	public static final Variable F1 = new Variable(Opcodes.FLOAT, (float)1);
	public static final Variable F2 = new Variable(Opcodes.FLOAT, (float)2);
	
	public static final Variable D0 = new Variable(Opcodes.DOUBLE, (double)0);
	public static final Variable D1 = new Variable(Opcodes.DOUBLE, (double)1);
	
	public String toString() {
		return type + " : " + literalValue;
	}
	
}
