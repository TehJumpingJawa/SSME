package org.tjj.starsector.ssme;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

public final class Utils {

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
			return Utils.InternalClassName.toFilename(BinaryClassName.toInternalName(binaryName));
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
			return '/' + internalName.concat(".class");
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

	private Utils() {
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

	public static String toInternalType(String type) {
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
