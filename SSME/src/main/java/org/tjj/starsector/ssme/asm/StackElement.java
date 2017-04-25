package org.tjj.starsector.ssme.asm;

import org.objectweb.asm.Opcodes;

public final class StackElement {

	/**
	 * The stack element's type. Primitive types are represented by
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
	 * The name of the field from which the stack element's value originated.
	 * If the source is unknown, the value of {@link StackElement#sourceField} will be null. 
	 * 
	 * Obviously there's no guarantee that the actual stack element's value equals the field's value.
	 * The sourceField is primarily of use for identification of method calls by their parameters.
	 * 
	 * e.g.
	 * 
	 * foo(this.bar);
	 * 
	 * Examining the stack element's {@link StackElement#sourceField} would give the value "bar".
	 */
	public final String sourceField;

	private StackElement(Object type, Object literalValue, String sourceField) {
		this.type = type;
		this.literalValue = literalValue;
		this.sourceField = sourceField;
	}

	public StackElement(Object type) {
		this.type = type;
		this.literalValue = Literal.UNKNOWN;
		this.sourceField = null;
	}
	
	public StackElement(Object type, String sourceField) {
		this.type = type;
		this.literalValue = Literal.UNKNOWN;
		this.sourceField = sourceField;
	}

	public static StackElement create(Integer literalValue) {
		return new StackElement(Opcodes.INTEGER, literalValue,null);
	}

	public static StackElement create(Long literalValue) {
		return new StackElement(Opcodes.LONG, literalValue,null);
	}

	public static StackElement create(Float literalValue) {
		return new StackElement(Opcodes.FLOAT, literalValue,null);
	}

	public static StackElement create(Double literalValue) {
		return new StackElement(Opcodes.DOUBLE, literalValue,null);
	}

	public static StackElement create(String literalValue) {
		return new StackElement("java/lang/String", literalValue,null);
	}
	
	/**
	 * returns the literal.
	 * Note there's no safety net here; if it isn't a known literal of the correct type, you'll get an Exception 
	 * @return
	 */
	public double getDouble() {
		return (Double) literalValue;
	}

	/**
	 * returns the literal.
	 * Note there's no safety net here; if it isn't a known literal of the correct type, you'll get an Exception 
	 * @return
	 */
	public float getFloat() {
		return (Float) literalValue;
	}

	/**
	 * returns the literal.
	 * Note there's no safety net here; if it isn't a known literal of the correct type, you'll get an Exception 
	 * @return
	 */
	public int getInt() {
		return (Integer) literalValue;
	}

	/**
	 * returns the literal.
	 * Note there's no safety net here; if it isn't a known literal of the correct type, you'll get an Exception 
	 * @return
	 */
	public long getLong() {
		return (Long) literalValue;
	}

	/**
	 * True only if the literal value of this stack element could be resolved. 
	 * @return
	 */
	public boolean isLiteral() {
		return literalValue != Literal.UNKNOWN;
	}

	public static final StackElement UNKNOWN_INTEGER = new StackElement(Opcodes.INTEGER);
	public static final StackElement UNKNOWN_FLOAT = new StackElement(Opcodes.FLOAT);
	public static final StackElement UNKNOWN_LONG = new StackElement(Opcodes.LONG);
	public static final StackElement UNKNOWN_DOUBLE = new StackElement(Opcodes.DOUBLE);
	public static final StackElement UNKNOWN_TOP = new StackElement(Opcodes.TOP);
	public static final StackElement UNKNOWN_UNINITIALIZED_THIS = new StackElement(Opcodes.UNINITIALIZED_THIS);
	public static final StackElement UNKNOWN_OBJECT = new StackElement("java/lang/Object");
	public static final StackElement UNKNOWN_CLASS = new StackElement("java/lang/Class");
	public static final StackElement UNKNOWN_METHOD = new StackElement("java/lang/invoke/MethodType");
	public static final StackElement UNKNOWN_HANDLE = new StackElement("java/lang/invoke/MethodHandle");
	public static final StackElement NULL = new StackElement(Opcodes.NULL, Literal.NULL,null);
	public static final StackElement I_M1 = create(-1);
	public static final StackElement I_0 = create(0);
	public static final StackElement I_1 = create(1);
	public static final StackElement I_2 = create(2);
	public static final StackElement I_3 = create(3);
	public static final StackElement I_4 = create(4);
	public static final StackElement I_5 = create(5);

	public static final StackElement L_0 = create((long)0);
	public static final StackElement L_1 = create((long)1);

	public static final StackElement F_0 = create((float)0);
	public static final StackElement F_1 = create((float)1);
	public static final StackElement F_2 = create((float)2);

	public static final StackElement D_0 = create((double)0);
	public static final StackElement D_1 = create((double)1);

	public String toString() {
		return "source=" + sourceField + ",type=" + type + ",value=" + literalValue;
	}
	
	private static class Literal {

		static final Literal UNKNOWN = new Literal("Unknown");
		static final Literal NULL = new Literal("null");
		
		public final String s;
		private Literal(String s) {
			this.s = s;
		}
		
		public String toString() {
			return s;
		}
	}
	

}
