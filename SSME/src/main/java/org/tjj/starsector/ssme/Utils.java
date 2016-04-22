package org.tjj.starsector.ssme;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.EnumSet;

import javassist.CtBehavior;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.expr.FieldAccess;
import javassist.expr.MethodCall;

import org.tjj.starsector.ssme.javassist.AccessModifier;
import org.tjj.starsector.ssme.javassist.BehaviourModifier;
import org.tjj.starsector.ssme.javassist.MatchList;
import org.tjj.starsector.ssme.javassist.MethodPrototype;

public final class Utils {

	private Utils() {
	}
	
	public static final CtClass [] NO_ARGS = new CtClass[0];
	public static final CtClass [] NO_EXCEPTIONS = NO_ARGS; 
	
	/**
	 * class for encapsulating utility methods that operate on class names in the binary format
	 * Binary class names use the '.' separator.
	 * e.g.
	 * "java.lang.String" is a binary classname 
	 * 
	 * @author TehJumpingJawa
	 *
	 */
	public static class BinaryClassName {

		public static String toInternalName(String binaryName) {
			return binaryName.replace('.',  '/');
		}
		
		public static String toFilename(String binaryName) {
			return InternalClassName.toFilename(BinaryClassName.toInternalName(binaryName));
		}
		
		
	}
	
	/**
	 * class for encapsulating utility methods that operate on class names in the internal format.
	 * Interal class names use the '/' separator.
	 * e.g.
	 * "java/lang/String" is an internal classname.
	 * 
	 * @author TehJumpingJawa
	 *
	 */
	public static class InternalClassName {
		
		public static String toBinaryName(String internalName) {
			return internalName.replace('/', '.');
		}
		
		public static String toFilename(String internalName) {
			return internalName.concat(".class");
		}

		/**
		 * retrieves the package component from the specified internal class name.
		 * 
		 * "java/lang/String" will return "java/lang/"
		 * 
		 * @param className
		 * @return
		 */
		public static String getPackage(String internalName) {
			return internalName.substring(0, internalName.lastIndexOf('/')+1);
		}
		
		/**
		 * retrieves the simple classname from the specified internal class name.
		 * 
		 * @param className
		 * @return
		 */
		public static String getSimpleName(String internalName) {
			return internalName.substring(internalName.lastIndexOf('/')+1);
		}
		
	}


	
	public static String toString(MethodCall m) {
		try {
			return m.getMethod().getReturnType().getName() + " " + m.getMethodName() + "(" +Utils.toString(m.getMethod().getParameterTypes()) + ")";
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
	
	/**
	 * Converts a standard java field descriptor to its internal form.
	 * It's quite tolerant.
	 * e.g.
	 * 
	 * "int[]" becomes "[I"
	 * "int[] a" becomes "[I"
	 * "int a[]" becomes "[I"
	 * "int [] a []" becomes "[[I"
	 * "java.lang.String s" becomes "Ljava/lang/String;"
	 * "java.lang.String []" becomes "[Ljava/lang/String;"
	 *  etc etc
	 *  
	 *  
	 * Note, It's hideously inefficient though :)
	 *  
	 * @param javaFieldType
	 * @return
	 * @deprecated
	 */
	public static String toFieldDescriptor(String javaFieldType) {
		return javaTypeToInternalType(javaFieldType);
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
		
		if(matchCriteria.includedBehaviourModifiers!=null || matchCriteria.excludedBehaviourModifiers!=null) {
			EnumSet<BehaviourModifier> behaviourModifiers = BehaviourModifier.fromJvmModifiers(ctBehaviour.getModifiers());
			
			if(matchCriteria.includedBehaviourModifiers!=null) {
				if(!behaviourModifiers.containsAll(matchCriteria.includedBehaviourModifiers)) {
					// method's behaviour modifiers aren't acceptable.
//					System.out.println(behaviourModifiers + " !containsAll " + matchCriteria.includedBehaviourModifiers);
					return false;
				}
			}
			
			if(matchCriteria.excludedBehaviourModifiers!=null) {
				if(!Collections.disjoint(matchCriteria.excludedBehaviourModifiers, behaviourModifiers)) {
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
			if(!Utils.matches(ctBehaviour.getParameterTypes(), matchCriteria.parameters)) {
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
			if(!Utils.matches(ctBehaviour.getExceptionTypes(), matchCriteria.thrownExceptions)) {
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
	
	/**
	 * Converts a standard java method descriptor to its internal form.
	 * Note *all* types (including those in the "java.lang" package) must be fully qualified.
	 *  
	 * e.g.
	 * 
	 * "java.lang.String toMethodDescriptor(java.lang.String javaMethodType)"
	 * will be converted to:
	 * "(Ljava/lang/String;)Ljava/lang/String;"
	 * 
	 * It's also quite tolerant of wonky input, so stuff like this is ok too:
	 * 
	 * "int[] cheese(byte[] a[],int [])"
	 * will be converted to:
	 * "([[B[I)[I"
	 * 
	 * "void(java.awt.event.ActionEvent)"
	 * will be converted to:
	 * "(Ljava/awt/event/ActionEvent;)V"
	 * 
	 * This tolerance does mean that some illegal input will get through (such as using 'void' as a parameter type). Just don't do it, ok!
	 * 
	 * Note, It's hideously inefficient though :)
	 *  
	 * @param javaMethodType
	 * @return
	 * @deprecated
	 */
	public static String toMethodDescriptor(String javaMethodType) {
		javaMethodType = javaMethodType.trim();
		StringBuilder sb = new StringBuilder();
		
		sb.append('(');

		final int returnTypeEndIndex = javaMethodType.indexOf(' '); 

		final int openParenthesisIndex = javaMethodType.indexOf('(');
		final int closeParenthesisIndex = javaMethodType.indexOf(')', openParenthesisIndex+1);
		
		String parameters = javaMethodType.substring(openParenthesisIndex+1, closeParenthesisIndex);
		String [] params = parameters.split(",");
		for (String param : params) {
			if(param.length()>0) {
				sb.append(javaTypeToInternalType(param.trim()));
			}
		}
		
		sb.append(')');

		final String returnType = javaMethodType.substring(0, Math.min(returnTypeEndIndex, openParenthesisIndex));
		
		sb.append(javaTypeToInternalType(returnType));
		
		return sb.toString();
	}

	private static String javaTypeToInternalType(String javaType) {
		StringBuilder result = null;
		for(int i = 0;i < javaType.length();i++) {
			final char c = javaType.charAt(i);
			if(result==null && (c==' ' || c=='[')) {
				result = new StringBuilder(toInternalType(javaType.substring(0, i)));
			}
			if(c=='[') {
				result.insert(0, '[');
			}
		}
		return result==null?toInternalType(javaType):result.toString();
	}
	
	
	private static String toInternalType(String type) {
		switch(type) {
		case "byte":
			return "B";
		case "char":
			return "C";
		case "double":
			return "D";
		case "float":
			return "F";
		case "int":
			return "I";
		case "long":
			return "J";
		case "short":
			return "S";
		case "boolean":
			return "Z";
		case "void":
			return "V";
		default:
			return 'L' + BinaryClassName.toInternalName(type) + ";";
		}
	}

	public static void removeRecursive(Path path) throws IOException
	{
	    Files.walkFileTree(path, new SimpleFileVisitor<Path>()
	    {
	        @Override
	        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
	                throws IOException
	        {
	            Files.delete(file);
	            return FileVisitResult.CONTINUE;
	        }

	        @Override
	        public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException
	        {
	            // try to delete the file anyway, even if its attributes
	            // could not be read, since delete-only access is
	            // theoretically possible
	            Files.delete(file);
	            return FileVisitResult.CONTINUE;
	        }

	        @Override
	        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException
	        {
	            if (exc == null)
	            {
	                Files.delete(dir);
	                return FileVisitResult.CONTINUE;
	            }
	            else
	            {
	                // directory iteration failed; propagate exception
	                throw exc;
	            }
	        }
	    });
	}	
	
}
