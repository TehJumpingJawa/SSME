package org.tjj.starsector.ssme.javassist;

import java.util.Collections;
import java.util.EnumSet;

import javassist.CtBehavior;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.expr.FieldAccess;
import javassist.expr.MethodCall;

public final class JavassistUtils {

	private JavassistUtils() {
	}
	
	public static final CtClass [] NO_ARGS = new CtClass[0];
	public static final CtClass [] NO_EXCEPTIONS = NO_ARGS; 
	
	public static String toString(MethodCall m) {
		try {
			return m.getMethod().getReturnType().getName() + " " + m.getMethodName() + "(" +JavassistUtils.toString(m.getMethod().getParameterTypes()) + ")";
		} catch (NotFoundException e) {
			return "NotFoundException";
		}
	}
	
	public static String toString(FieldAccess f) {
		try {
			return f.getClassName() + "#" + f.getField().getType().getName() + " "+ f.getFieldName();
		} catch (NotFoundException e) {
			return "NotFoundException";
		}
	}
	
	public static String toString(CtClass [] types) {
		StringBuilder sb = new StringBuilder();
		for (CtClass ctClass : types) {
			sb.append(ctClass.getName()).append(",");
		}
		if(sb.length()>0) {
			sb.setLength(sb.length()-1);
		}
		return sb.toString();
	}
	
	public static boolean compare(CtBehavior ctBehaviour, MethodPrototype matchCriteria) throws NotFoundException {
		if(matchCriteria.allowedAccessModifiers!=null) {
			AccessModifier accessModifier = AccessModifier.fromJvmModifiers(ctBehaviour.getModifiers());
			
			if(!matchCriteria.allowedAccessModifiers.contains(accessModifier)) {
				// method's access modifier isn't acceptable.
//				System.out.println(accessModifier + " !contains " + matchCriteria.allowedAccessModifiers);
				return false; 
			}
		}
		
		if(matchCriteria.includedNonAccessModifiers!=null || matchCriteria.excludedNonAccessModifiers!=null) {
			EnumSet<NonAccessModifier> behaviourModifiers = NonAccessModifier.fromJvmModifiers(ctBehaviour.getModifiers());
			
			if(matchCriteria.includedNonAccessModifiers!=null) {
				if(!behaviourModifiers.containsAll(matchCriteria.includedNonAccessModifiers)) {
					// method's behaviour modifiers aren't acceptable.
//					System.out.println(behaviourModifiers + " !containsAll " + matchCriteria.includedBehaviourModifiers);
					return false;
				}
			}
			
			if(matchCriteria.excludedNonAccessModifiers!=null) {
				if(!Collections.disjoint(matchCriteria.excludedNonAccessModifiers, behaviourModifiers)) {
					// method's behaviour modifiers aren't acceptable.
//					System.out.println(behaviourModifiers + " !disjoint " + matchCriteria.excludedBehaviourModifiers);
					return false;
				}
			}
		}
		
		if(matchCriteria.methodName!=null && !ctBehaviour.getName().equals(matchCriteria.methodName)) {
			// method name doesn't match
//			System.out.println(ctBehaviour.getName() + " !equals " + matchCriteria.methodName);
			return false;
		}
		
		if(matchCriteria.parameters!=null) {
			if(!JavassistUtils.matches(ctBehaviour.getParameterTypes(), matchCriteria.parameters)) {
//				System.out.println(Arrays.toString(ctBehaviour.getParameterTypes()) + " !equals " + Arrays.toString(matchCriteria.parameters));
				//parameters don't match
				return false;
			}
		}
		
		if(matchCriteria.returnType!=null && ctBehaviour instanceof CtMethod) {
			CtMethod ctMethod = (CtMethod) ctBehaviour;
			if(!ctMethod.getReturnType().equals(matchCriteria.returnType)) {
				// return type doesn't match
//				System.out.println(ctMethod.getReturnType() + " !equals " + matchCriteria.returnType);
				return false;
			}
		}
		
		if(matchCriteria.thrownExceptions!=null) {
			if(!JavassistUtils.matches(ctBehaviour.getExceptionTypes(), matchCriteria.thrownExceptions)) {
//				System.out.println(Arrays.toString(ctBehaviour.getExceptionTypes()) + " !equals " + Arrays.toString(matchCriteria.thrownExceptions));
				// exception types don't match
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Compare lists of CtClass types (can be parameters, exceptions, etc)
	 * Null elements are treated as 'unknown', and so considered a match.
	 * 
	 * @param a
	 * @param b
	 * @return
	 */
	public static boolean matches(CtClass[] a, CtClass[] b) {
		if(a.length!=b.length) {
			return false;
		}
		
		for (int i = 0; i < a.length; i++) {
			if(a[i]==null || b[i]==null) continue;
			if(a[i].equals(b[i])) continue;
			return false;
		}
		return true;
	}
	
	/**
	 * 
	 * Method for finding CtMethods using partial attribute matches.
	 * 
	 * @param clazz 			The CtClass in which to search
	 * @param matchCriteria 	The method prototype to use as matching criteria.
	 * @return The set of methods found
	 * @throws NotFoundException  
	 */
	public static MatchList<CtMethod> findDeclaredMethods(CtClass clazz, MethodPrototype matchCriteria) throws NotFoundException {
		CtMethod[] methods = clazz.getDeclaredMethods();
		MatchList<CtMethod> results = new MatchList<>();
		for (CtMethod ctMethod : methods) {
			
			if(compare(ctMethod, matchCriteria)) {
				results.add(ctMethod);
			}
		}
		
		return results;
	}	
	
}
