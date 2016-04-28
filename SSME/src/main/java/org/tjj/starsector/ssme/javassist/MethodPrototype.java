package org.tjj.starsector.ssme.javassist;

import java.util.EnumSet;

import javassist.CtClass;

public class MethodPrototype {

	public final EnumSet<AccessModifier> allowedAccessModifiers;
	public final EnumSet<NonAccessModifier> includedNonAccessModifiers;
	public final EnumSet<NonAccessModifier> excludedNonAccessModifiers;
	public final String methodName;
	public final CtClass [] parameters;
	public final CtClass returnType;
	public final CtClass [] thrownExceptions;
	
	
	/**
	 * Creates a MethodPrototype that can be used by the findDeclaredMethod utility method.
	 * 
	 * null is valid for all parameters parameters, and will cause a 'match everything' behaviour.
	 * 
	 * @param allowedAccessModifiers Set of access modifiers that this method prototype will match
	 * @param includedAccessModifiers Set of behaviour modifiers that matched methods must contain
	 * @param excludedAccessModifiers Set of behaviour modifiers that matched methods must *not* contain.
	 * @param methodName
	 * @param parameters
	 * @param thrownExceptions 
	 */
	public MethodPrototype(EnumSet<AccessModifier> allowedAccessModifiers, EnumSet<NonAccessModifier> includedNonAccessModifiers, EnumSet<NonAccessModifier> excludedNonAccessModifiers, String methodName, CtClass [] parameters, CtClass returnType, CtClass [] thrownExceptions) {
		this.allowedAccessModifiers = allowedAccessModifiers;
		this.includedNonAccessModifiers = includedNonAccessModifiers;
		this.excludedNonAccessModifiers = excludedNonAccessModifiers;
		this.methodName = methodName;
		this.returnType = returnType;
		this.parameters = parameters;
		this.thrownExceptions = thrownExceptions;
	}
	
	public MethodPrototype(String methodName, CtClass [] parameters, CtClass returnType) {
		this(null, null, null, methodName, parameters, returnType, null);
	}
	
	public MethodPrototype(EnumSet<AccessModifier> allowedAccessModifiers, EnumSet<NonAccessModifier> includedBehaviourModifiers, String methodName, CtClass [] parameters, CtClass returnType) {
		this(allowedAccessModifiers, includedBehaviourModifiers, null, methodName, parameters, returnType, null);
	}

}
