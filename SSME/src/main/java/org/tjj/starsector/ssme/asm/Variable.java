package org.tjj.starsector.ssme.asm;

import org.objectweb.asm.Opcodes;

public final class Variable {

	/**
	 * The variable's type. Primitive types are represented by
	 * {@link Opcodes#TOP}, {@link Opcodes#INTEGER}, {@link Opcodes#FLOAT},
	 * {@link Opcodes#LONG}, {@link Opcodes#DOUBLE},{@link Opcodes#NULL} or
	 * {@link Opcodes#UNINITIALIZED_THIS} (long and double are represented by a
	 * single element). Reference types are represented by String objects
	 * (representing internal names), and uninitialized types by Label objects
	 * (this label designates the NEW instruction that created this
	 * uninitialized value).
	 */
	public final Object type;
	/**
	 * The literal value.
	 * Will be one of:
	 * Long, Integer, Float, Double, String, Literal.NULL, or Literal.UNKNOWN.
	 */
	public final Object literalValue;
	
	/**
	 * The field from which the variable originated.
	 */
	public final String sourceField;

	private Variable(Object type, Object literalValue, String sourceField) {
		this.type = type;
		this.literalValue = literalValue;
		this.sourceField = sourceField;
	}

	public Variable(Object type) {
		this.type = type;
		this.literalValue = Literal.UNKNOWN;
		this.sourceField = null;
	}
	
	public Variable(Object type, String sourceField) {
		this.type = type;
		this.literalValue = Literal.UNKNOWN;
		this.sourceField = sourceField;
	}

	public static Variable create(Integer literalValue) {
		return new Variable(Opcodes.INTEGER, literalValue,null);
	}

	public static Variable create(Long literalValue) {
		return new Variable(Opcodes.LONG, literalValue,null);
	}

	public static Variable create(Float literalValue) {
		return new Variable(Opcodes.FLOAT, literalValue,null);
	}

	public static Variable create(Double literalValue) {
		return new Variable(Opcodes.DOUBLE, literalValue,null);
	}

	public static Variable create(String literalValue) {
		return new Variable("java/lang/String", literalValue,null);
	}
	
	public double getDouble() {
		return (Double) literalValue;
	}

	public float getFloat() {
		return (Float) literalValue;
	}

	public int getInt() {
		return (Integer) literalValue;
	}

	public long getLong() {
		return (Long) literalValue;
	}

	public boolean isLiteral() {
		return literalValue != Literal.UNKNOWN;
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
	public static final Variable NULL = new Variable(Opcodes.NULL, Literal.NULL,null);
	public static final Variable M1 = create(-1);
	public static final Variable I0 = create(0);
	public static final Variable I1 = create(1);
	public static final Variable I2 = create(2);
	public static final Variable I3 = create(3);
	public static final Variable I4 = create(4);
	public static final Variable I5 = create(5);

	public static final Variable L0 = create((long)0);
	public static final Variable L1 = create((long)1);

	public static final Variable F0 = create((float)0);
	public static final Variable F1 = create((float)1);
	public static final Variable F2 = create((float)2);

	public static final Variable D0 = create((double)0);
	public static final Variable D1 = create((double)1);

	public String toString() {
		return "source=" + sourceField + ",type=" + type + ",value=" + literalValue;
	}

}
