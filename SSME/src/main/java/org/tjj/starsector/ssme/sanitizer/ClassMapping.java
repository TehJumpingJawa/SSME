package org.tjj.starsector.ssme.sanitizer;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InnerClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.tjj.starsector.ssme.Utils;
import org.tjj.starsector.ssme.Utils.InternalClassName;
import org.tjj.starsector.ssme.asm.KeyWords;

public class ClassMapping implements Opcodes {

	public static final int ACC_DEFAULT = 0;
	
	private final SanitizerContext context;
	
	private String newName;
	
	/**
	 * indicates that this class is no-longer an innerclass, so any references to it as such should be discarded by the SanitizingVisitor.
	 */
	private boolean promotedToTopTier = false;
	
	private int deobfuscatedMethodCount;
	private int deobfuscatedFieldCount;
	
	private int enumFieldCount = 0;
	
	private boolean superClassSet = false;
	private ClassMapping superClass;

	public final ClassNode classNode;
	
	private Map<String,String> tempSyntheticMap; 

//	private Map<String,String> bridgeMethodTarget;
	
	private Map<Method, String> methodMap;
	private Map<String, String> fieldMap;
	
	private ArrayList<String> enumFieldNames;
	
	public String getNewName() {
		if(newName==null) {
			newName = makeName();
		}
		return newName;
	}
	
	private String makeName() {
		
		final String oldName = classNode.name;
		final String newName;
		
		if(context.inWorkingSet(oldName)) {
			
			String deobfuscatedName = context.getDeobfuscatedName(oldName);
			
			if(deobfuscatedName!=null) {
				// this class has been registered as desiring a meaningful deobfuscated name
				
				// record the package structure of the provided meaningful name.
				// Note there's a bug, and limitation here.
				// if any package element of the meaningful name triggers the deobfuscation logic, it'll get deobfuscated.
				// also, meaningful names for classes inside obfuscated classes won't work atm.
				// TODO
				final String deobfuscatedPackagePath = deobfuscatePackage(Utils.InternalClassName.getPackage(deobfuscatedName));

				newName = deobfuscatedName;
			}
			else if(classNode.outerClass!=null) {
				// 1st check if this is an anonymous or local nested class.
				
				
				// outerClass is determined by using the class file's EnclosingMethod Attribute
				// https://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.7.7
				
				// Thus this means we're dealing with a local or anonymous class
				
				ClassMapping outerClass = context.get(classNode.outerClass);
				// simple name of the this inner class.
				String simpleClassname = oldName.substring(outerClass.classNode.name.length()+1, oldName.length());
				
				final String outerClassNewName = outerClass.getNewName();
				
				String proposedNewName = outerClassNewName  + "$" + simpleClassname;
				
				// all synthetic classes will need their names deobfuscated.
				if(isObfuscatedClass(context, proposedNewName, simpleClassname, true) || !context.registerOutputName(proposedNewName)) {

					newName = outerClassNewName + "$" + makeDeobfuscatedName();
					if(!context.registerOutputName(newName)) {
						throw new RuntimeException("unexpected class naming collision: " + newName);
					}
				}
				else {
					newName = proposedNewName; 
				}
			}
			else {

				// now we check for inner, or static nested classes 

				// https://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.7.6
				// "if C is a top-level class or interface (JLS §7.6) or a local class (JLS §14.3) or an anonymous class (JLS §15.9.5), the value of the outer_class_info_index item must be zero."
				
				@SuppressWarnings("unchecked")
				List<InnerClassNode> innerClasses = classNode.innerClasses;
				InnerClassNode found = null;
				for (InnerClassNode innerClassNode : innerClasses) {
					if(innerClassNode.name.equals(oldName)) {
						found = innerClassNode;
						break;
					}
				}
				
				if(found!=null) {
					if(checkAccess(ACC_STATIC, found.access)) {
						// static nested interface, we want to promote this to a top-tier class.
						ClassMapping outerClass = context.get(found.outerName);

						// we don't need to deobfuscate the package, it's already been done for the outer class, so we just use that.
						final String deobfuscatedPackagePath = Utils.InternalClassName.getPackage(outerClass.getNewName());

						newName = deobfuscatedPackagePath + makeDeobfuscatedName() + "from" + Utils.InternalClassName.getSimpleName(outerClass.getNewName()).replace('$', '_');
						if(!context.registerOutputName(newName)) {
							throw new RuntimeException("unexpected class naming collision: " + newName);
						}

						promotedToTopTier = true;

					}
					else {
						ClassMapping outerClass = context.get(found.outerName);
						String simpleClassname = oldName.substring(outerClass.classNode.name.length()+1, oldName.length());
						
						final String outerClassNewName = outerClass.getNewName();
						
						String proposedNewName = outerClassNewName + "$" + simpleClassname;
	
						if(isObfuscatedClass(context, proposedNewName, simpleClassname, false) || !context.registerOutputName(proposedNewName)) {
							newName = outerClassNewName + "$" + makeDeobfuscatedName();
							if(!context.registerOutputName(newName)) {
								throw new RuntimeException("unexpected class naming collision: " + newName);
							}							
						}
						else {
							newName = proposedNewName; 
						}
					}
					
				}
				else {
					// must be a top-level class 
					String simpleClassname =  Utils.InternalClassName.getSimpleName(oldName);
					
					final String deobfuscatedPackagePath = deobfuscatePackage(Utils.InternalClassName.getPackage(oldName));
					
					String proposedNewName = deobfuscatedPackagePath + simpleClassname;
					if(isObfuscatedClass(context, proposedNewName, simpleClassname, false) || !context.registerOutputName(proposedNewName)) {
						newName = deobfuscatedPackagePath + makeDeobfuscatedName();
						if(!context.registerOutputName(newName)) {
							throw new RuntimeException("unexpected class naming collision: " + newName);
						}						
					}
					else {
						newName = proposedNewName;
					}
				}
			}

		}
		else {

			newName = oldName;
		}
		
		return newName;
	}
	
	private String makeDeobfuscatedName() {
		final boolean isInterface = checkAccess(ACC_INTERFACE, classNode.access); 
		final boolean isEnum = checkAccess(ACC_ENUM, classNode.access);

		final String typeString;
		if(isInterface) {
			typeString = "Interface" + context.incrementInterfaceCount();
		}
		else if(isEnum) {
			typeString = "Enum" + context.incrementEnumCount();
		}
		else {
			typeString = "Class" + context.incrementClassCount();
		}
		return typeString;		
		
	}
	
	public Map<Method,String> getMethodMap() {
		if(methodMap==null) {
			methodMap = makeMethodMap();
		}
		return methodMap;
	}
	
	private Map<Method, String> makeMethodMap() {
		
		final HashMap<Method,String> methodMap = new HashMap<>();
		
		collectMethods(context, this, methodMap);
		
		ClassMapping parent = getSuper();
		if(parent!=null) {
			parent.getMethodMap();
			deobfuscatedMethodCount = parent.deobfuscatedMethodCount;
		}
		
		final String oldName = classNode.name;
		
		if(context.inWorkingSet(oldName)) {
			// For handling synthetic enum fields, we need to do the methods first.
			@SuppressWarnings("unchecked")
			List<MethodNode> methods = classNode.methods;

			for (MethodNode methodNode : methods) {
	
				final Method currentMethod = new Method(methodNode.name, methodNode.desc, methodNode);
	
				String newName = methodMap.remove(currentMethod);
	
				if(newName==null) {
					// this method isn't overriding a non-private super class implementation
					// so we might need to make a new name for it.
					
					String syntheticName = handleSyntheticMethod(methodNode);
					
					if(syntheticName!=null) {
						newName = syntheticName;
					}else if(methodNode.name.equals("<init>") || methodNode.name.equals("<clinit>") || !isObfuscatedMethod(methodNode.name)) {
						newName = methodNode.name;
					}
					else {
						newName = generateDeobfuscatedMethodName(currentMethod);
					}
				}
				checkForBridgeMethod(methodNode, newName, methodMap);
				
				methodMap.put(currentMethod, newName);
			}
		}
		else {
			@SuppressWarnings("unchecked")
			List<MethodNode> methods = classNode.methods;
			for (MethodNode methodNode : methods) {
				Method currentMethod = new Method(methodNode.name, methodNode.desc, methodNode);
				methodMap.remove(currentMethod);
				methodMap.put(currentMethod, methodNode.name);
			}
		}
		
		return methodMap;
	}
	
	static final int[] bridgeMethodInstructions = new int[] {
	     ALOAD,
	     ALOAD,
	     CHECKCAST,
	     ALOAD,
	     CHECKCAST,
	     INVOKEVIRTUAL,
	};

	private void checkForBridgeMethod(MethodNode methodNode, String newName, HashMap<Method, String> methodMap) {
		if (checkAccess(ACC_BRIDGE, methodNode.access)) {
			InsnList instructions = methodNode.instructions;
			
			AbstractInsnNode instruction = instructions.getFirst();

			while(instruction!=null && instruction.getOpcode()!=INVOKEVIRTUAL) {
				instruction = instruction.getNext();
			}
			
			if(instruction==null) {
				System.out.println("no invokevirtual found in bridge method!");
			}
			else {
				MethodInsnNode invokeVirtual = (MethodInsnNode) instruction;
				
				//Map.replace(...) is Java 1.8+ :( 
//				String oldName = methodMap.replace(new Method(invokeVirtual.name, invokeVirtual.desc, null), newName);
				
				Method method = new Method(invokeVirtual.name, invokeVirtual.desc, null);
				if (methodMap.containsKey(method)) {
				     String oldName = methodMap.put(method, newName);
//						System.out.println(getNewName() + ": Bridge rename. \"" + oldName  + " " + invokeVirtual.desc + "\" now called \"" + newName + "\" " + methodNode.desc);
				}
			}
		}
	}

	/**
	 * the sequence of Opcodes used to identify a method as being a synthetic
	 * enum "$SWITCH_TABLE$" method.
	 */
	static final int[] syntheticEnumSwitchTableInstructions = new int[] {
			GETSTATIC,
			DUP,
			IFNULL,
			ARETURN,
			POP,
			INVOKESTATIC
	};	
	
	private String handleSyntheticMethod(MethodNode methodNode) {
		if(checkModifiers(ACC_DEFAULT,ACC_STATIC|ACC_SYNTHETIC, methodNode.access)) {
			if(methodNode.desc.equals("()[I")) {
				// investigate the bytecode.
				
				noMatch: {
					
					InsnList instructions = methodNode.instructions;
					
					AbstractInsnNode instruction = instructions.getFirst();
					
					String associatedFieldName = null;
					String enumClass = null;
					
					
					for (int i : syntheticEnumSwitchTableInstructions) {
						int instructionOpCode = instruction.getOpcode();
						while(instructionOpCode==-1) {
							// no idea why ASM sticks a -1 in the opcode stream.
							instruction = instruction.getNext();
							instructionOpCode = instruction.getOpcode();
						}

						if(instructionOpCode!=i) {
							break noMatch;
						}
						else {
							switch(i) {
							case GETSTATIC:
								FieldInsnNode getStatic = (FieldInsnNode)instruction;
								associatedFieldName = getStatic.name;
								break;
							case INVOKESTATIC:
								MethodInsnNode invokeStatic = (MethodInsnNode)instruction;
								if(!invokeStatic.name.equals("values")) {
									break noMatch;
								}
								enumClass = invokeStatic.owner;
								break;
							}
							instruction = instruction.getNext();
						}
					}

					ClassMapping targetEnum = context.get(enumClass);
					
					String newName = "$SWITCH_TABLE$" + targetEnum.getNewName().replace('/', '$');

					// completed match
					if(tempSyntheticMap==null) {
						tempSyntheticMap = new HashMap<>();
					}
					tempSyntheticMap.put(associatedFieldName, newName);
					
					return newName;
				}
				System.out.println("A synthetic method that looks like it's enum related, but isn't!");
			}
		}
		
		
		return null;
	}	
	
	public Map<String,String> getFieldMap() {
		if(fieldMap==null) {
			fieldMap = makeFieldMap();
		}
		return fieldMap;
	}
	
	private Map<String,String> makeFieldMap() {

		Map<String, String> fieldMap = new HashMap<>();
		
		ClassMapping parent = getSuper();
		
		if(parent!=null) {
			parent.getFieldMap();
			deobfuscatedFieldCount = parent.deobfuscatedFieldCount;
		}
		
		if(context.inWorkingSet(classNode.name)) {
			@SuppressWarnings("unchecked")
			List<FieldNode> fields = classNode.fields;
			for (FieldNode fieldNode : fields) {
				String oldName = fieldNode.name;
				String newName = oldName;
				
				String syntheticName = handleSyntheticField(fieldNode);

				if(syntheticName!=null) {
					newName = syntheticName;
				}
				else if(isObfuscatedField(oldName)) {
					if(checkAccess(ACC_ENUM, classNode.access) && checkModifiers(ACC_PUBLIC, ACC_STATIC|ACC_FINAL|ACC_ENUM, fieldNode.access) && !fieldNode.desc.startsWith("[")) {
						// take the name of the enum constant from the String parameter passed into it as the 1st arg.
						newName = getNextEnumFieldName();
					}
					else {
						newName = modifierToString(fieldNode.access) + descToString(fieldNode.desc) + deobfuscatedFieldCount++;
					}
				}
				fieldMap.put(oldName, newName);
			}
		}
		else {
			@SuppressWarnings("unchecked")
			List<FieldNode> fields = classNode.fields;
			for (FieldNode fieldNode : fields) {
				fieldMap.put(fieldNode.name, fieldNode.name);
			}
		}		
		
		return fieldMap;
	}
	
	/**
	 * This method assumes the order the enums are initialised is the same as the order they appear in the class file.
	 * A safe assumption for any class file generated by a compiler, though an obfuscator might purposefully screw this up.
	 * @return
	 */
	private String getNextEnumFieldName() {
		if(enumFieldNames==null) {
			enumFieldNames = new ArrayList<>();
			
			@SuppressWarnings("unchecked")
			List<MethodNode> methods = classNode.methods;
			
			complete:
			{
				for (MethodNode methodNode : methods) {
					if(methodNode.name.equals("<clinit>")) {
						
						InsnList instructions = methodNode.instructions;
						
						String name = null;
						int ordinal = -1;
						
						AbstractInsnNode instruction = instructions.getFirst();
						
						final int SEARCHING = 0;
						final int FOUND_DUP = 1;
						final int FOUND_NAME = 2;
						
						int state = SEARCHING;
						
						while(instruction!=null) {
							switch(state) {
							case SEARCHING:
								name = null;
								ordinal = -1;
								if(instruction.getOpcode()==DUP) {
									state = FOUND_DUP; 
								}
								break;
							case FOUND_DUP:
								if(instruction instanceof LdcInsnNode) {
									Object o = ((LdcInsnNode)instruction).cst;
									if(o instanceof String) {
										name = (String)o;
										state = FOUND_NAME;
									}
									else {
										state = SEARCHING;
									}
								}
								else {
									state = SEARCHING;
								}
								break;
							case FOUND_NAME:
								if(instruction instanceof IntInsnNode) {
									if(instruction.getOpcode()==BIPUSH || instruction.getOpcode()==SIPUSH) {
										ordinal = ((IntInsnNode)instruction).operand;
									}
								} else if(instruction instanceof InsnNode) {
									if(instruction.getOpcode()>=ICONST_0 && instruction.getOpcode()<=ICONST_5) {
										ordinal = instruction.getOpcode()-ICONST_0;
									}
								} else if(instruction instanceof LdcInsnNode) {
									Object o = ((LdcInsnNode)instruction).cst;
									if(o instanceof Integer) {
										ordinal = (Integer)o;
									}
								}
								if(ordinal==enumFieldNames.size()) {
									enumFieldNames.add(name);
//									System.out.println("Added Enum constant name\"" + name +"\" @ " + ordinal);
									state = SEARCHING;
								}
								else {
									state = SEARCHING;
								}
							}
							instruction = instruction.getNext();
						}
						break complete;
					}
				}
				throw new RuntimeException("No static initialiser found!");
			}
		}
		
		return enumFieldNames.get(enumFieldCount++);
		
	}
	
	
	private String handleSyntheticField(FieldNode fieldNode) {
		
		//TODO roll this code into isObfuscatedField(....)  
		
		if(classNode.outerClass!=null) {
			// anonymous inner class, or local class.
			if(fieldNode.name.startsWith("this$")) {
				try {
					Integer.parseInt(fieldNode.name.substring(5));
					return fieldNode.name;
				}
				catch(NumberFormatException e) {
					
				}
			}
		}
		
		if(checkAccess(ACC_ENUM, classNode.access) && checkModifiers(ACC_PRIVATE,ACC_STATIC|ACC_FINAL|ACC_SYNTHETIC, fieldNode.access)) {
			if(fieldNode.desc.equals("[L" + classNode.name + ";")) {
				return "ENUM$VALUES";
			}
		}
		
		if(checkModifiers(ACC_PRIVATE,ACC_STATIC|ACC_SYNTHETIC, fieldNode.access)) {
			if(fieldNode.desc.equals("[I")) {
				
				//ensure that this class' methods have been deobfuscated 
				getMethodMap();
				
				if(tempSyntheticMap!=null) {
					String newName = tempSyntheticMap.get(fieldNode.name);
					if(newName!=null) {
						return newName;
					}
				}
				System.out.println("A synthetic field that looks like it's enum related, but isn't!");
			}
		}

		return null;
	}	
	
	public ClassMapping getSuper() {
		if(!superClassSet) {
			if(classNode.superName!=null) {
				superClass = context.get(classNode.superName);
			}
			superClassSet = true;
		}
		return superClass;
	}
	
	public ClassMapping(SanitizerContext context, ClassNode classNode) {
		this.context = context; 
		this.classNode = classNode;
	}

	/**
	 * traverses the inheritance heirarchy.
	 * @param obfuscatedFieldName
	 * @return
	 */
	public String getUnobfuscatedFieldName(String obfuscatedFieldName) {
		ClassMapping current = this;
		String name = null;
		while(current!=null && (name= current.getFieldMap().get(obfuscatedFieldName))==null) {
			current = current.getSuper();
		}
		
		return name;
	}

	private String generateDeobfuscatedMethodName(Method m) {
		if(checkAccess(ACC_INTERFACE, classNode.access)) {
			return modifierToString(m.node.access) + "InterfaceMethod" + context.incrementInterfaceMethodCount();
		}
		else {
			return modifierToString(m.node.access) + "Method" + deobfuscatedMethodCount++;
		}
	}
	
//	public String toString() {
//		StringBuilder sb = new StringBuilder();
//		sb.append(classNode.name).append(" ->\n");
//		sb.append(newName).append("\n");
//		Set<String> fields = fieldMap.keySet();
//		for (String field : fields) {
//			sb.append("\t").append(field).append("->").append(fieldMap.get(field)).append("\n");
//		}
//		sb.append("\n");
//		Set<Method> methods = methodMap.keySet();
//		for (Method method : methods) {
//			sb.append("\t").append(method).append("->").append(methodMap.get(method)).append("\n");
//		}
//		
//		return sb.toString();
//	}
	
	public static class Method {
		// obfuscated name
		String oldName;
		// the signature of the method
		String desc;
		
		MethodNode node;
		
		public Method(String name, String desc, MethodNode node) {
			this.oldName = name;
			this.desc = desc;
			this.node = node;
		}
		
		public int hashCode() {
			return oldName.hashCode()^desc.hashCode();
		}
		
		@Override
		public boolean equals(Object o) {
			if(o instanceof Method) {
				Method other = (Method)o;
				return other.oldName.equals(oldName) && other.desc.equals(desc);
			}
			return false;
		}
		
		public String toString() {
			return oldName + desc;
		}
	}
	
	private static Set<ClassMapping> collectInterfaces(SanitizerContext context, ClassMapping c, Set<ClassMapping> list) {
		@SuppressWarnings("unchecked")
		List<String> interfaces = c.classNode.interfaces;
	
		for (String interfaceName : interfaces) {
			ClassMapping interfaceClass = context.get(interfaceName);
			if(list.add(interfaceClass)) {
				// only collect this interface's own interfaces if it wasn't already in the collected list.
				// this is both more efficient, and prevents infinite recursion in the event of circular interface dependencies. 
				collectInterfaces(context, interfaceClass, list);
			}
		}
		return list;
	}
	
	/**
	 * Collects all non-private Methods declared in this class's parents & interfaces
	 * 
	 * @param c
	 * @param list
	 * @return

	 */
	private static Map<Method, String> collectMethods(SanitizerContext context, ClassMapping c, Map<Method, String> methodMap) {
		
		ClassMapping parent = c.getSuper();
		
		// this is set of classes & interfaces that define the method interface to which this class must adhere.
		Set<ClassMapping> interfaces = new HashSet<ClassMapping>();
		
		if(parent!=null) {
			// find the first concrete (non-abstract) parent class
			// along the way, add all the methods declared in the abstract parents' and their interfaces.
			while(checkAccess(ACC_ABSTRACT, parent.classNode.access)) {
				interfaces.add(parent);
				collectInterfaces(context, parent, interfaces);
				
				parent = parent.getSuper();
			}
	
			//add the concrete parent
			interfaces.add(parent);
		}
		
		//finally add the interfaces of this class 
		collectInterfaces(context, c, interfaces);
		
		// then collect all the methods
		for (ClassMapping classMapping : interfaces) {

			Set<Map.Entry<Method, String>> methods = classMapping.getMethodMap().entrySet();
			for (Entry<Method, String> entry : methods) {
				int access = entry.getKey().node.access;
				if(checkVisibility(ACC_PRIVATE, access) || checkAccess(ACC_STATIC, access)) {
					// ignore
				}
				else {
					// should static methods be excluded too? I think they should - but it's fine as-is, so I'm leaving it.
					methodMap.put(entry.getKey(), entry.getValue());
				}
			}
		}
		
		return methodMap;
	}

	private StringBuilder constructPackage(String [] packageElements, int upto) {
		StringBuilder sb = new StringBuilder();
		for(int i = 0 ;i < upto;i++) {
			sb.append(packageElements[i]).append('/');
		}
		return sb;
	}

	private String deobfuscatePackage(final String packagePath) {
		
		StringBuilder result = null;
		String [] packageElements = packagePath.split("/");
		
		PackageMapping current = context.getRootPackage();
		
		for(int i = 0;i < packageElements.length;i++) {

			final String packageElement = packageElements[i];				
			PackageMapping next = current.subpackages.get(packageElement);
			
			if(next==null) {
				//visiting a new package
				if(result==null) result = constructPackage(packageElements, i);
				result.append(packageElement);
				
				final String newPackageElement;
				final boolean obfuscated;
				if(isObfuscatedPackage(packageElement) || !context.registerOutputName(result.toString())) {
					newPackageElement = "package" + context.incrementPackageCount();
					obfuscated = true;
					result.setLength(result.length()-packageElement.length());
					result.append(newPackageElement);
					if(!context.registerOutputName(result.toString())) {
						throw new RuntimeException("unexpected package naming collision: " + result.toString());
					}
				}
				else {
					newPackageElement = packageElement;
					obfuscated = false;
				}
				next = new PackageMapping(newPackageElement, obfuscated);
				current.subpackages.put(packageElement, next);

				result.append('/');
			}
			else {
				if(next.isDeobfuscated()) {
					if(result==null) result = constructPackage(packageElements, i);
				}
				if(result!=null) {
					result.append(next.getNewName()).append("/");
				}
			}
			current = next;
		}
		
		if(result!=null) {
			return result.toString();
		}
		else {
			return packagePath;
		}
	}	
	

	private static Set<String> illegalIdentifiers;
	private static int minIllegalIdentifierLength;
	private static int maxIllegalIdentifierLength;
	
	static {
		minIllegalIdentifierLength = Integer.MAX_VALUE;
		maxIllegalIdentifierLength = 0;
		
		illegalIdentifiers = new HashSet<>();
		
		illegalIdentifiers.addAll(Arrays.asList(KeyWords.list));
		illegalIdentifiers.addAll(Arrays.asList(new String[] {"Object","String"}));

		for (String identifier: illegalIdentifiers) {
			maxIllegalIdentifierLength = Math.max(identifier.length(), maxIllegalIdentifierLength);
			minIllegalIdentifierLength = Math.min(identifier.length(), minIllegalIdentifierLength);
		}
		
	}

	

	public static boolean isObfuscatedClass(SanitizerContext context, String proposedNewName, String simpleName, boolean anonymous) {

//		if(context.isRegisteredOutputName(proposedNewName)) {
//			//naming collision; the obfuscated classes contained "blah.A" and "blah.a"
//			// as writing these to a case insensitive filesystem causes collisions, we need to deobfuscate one of them.
//			return true;
//		}
		
		final String name = simpleName;
		
		final char[] chars = name.toCharArray();

		if(anonymous && isAllNumbers(chars)) {
			// anonymous classes should be all numbers.
			return false;
		}
		
		// short names just cause naming conflicts that result in the code needing to fully qualify class names
		// more readable to give them a verbose name.
		if(name.length()<=1) {
			return true;
		}
		
		// quickest check is for sole illegal identifiers
		if(isKeyWord(name)) {
			return true;
		}
		
		// then check for illegal chars
		if(isAll00000(chars) || isIllegalIdentifier(chars)) {
			return true;
		}
		
		//finally the most expensive check, for concatenated illegal identifiers.
		if(isConcatenatedKeyWords(name)) {
			return true;
		}
		
		return false;
	}
	
	private static boolean isObfuscatedPackage(final String name) {
		
		// short names just cause naming conflicts that result in the code needing to fully qualify class names
		// more readable to give them a verbose name.
		if(name.length()<=1) {
			return true;
		}
		
		// quickest check is for sole illegal identifiers
		if(isKeyWord(name)) {
			return true;
		}
		
		// then check for illegal chars
		final char[] chars = name.toCharArray();
		if(isAll00000(chars) || isIllegalIdentifier(chars)) {
			return true;
		}
		
		//finally the most expensive check, for concatenated illegal identifiers.
		if(isConcatenatedKeyWords(name)) {
			return true;
		}
		
		return false;		
	}
	
	private static boolean isObfuscatedMethod(final String name) {
		// short names just cause naming conflicts that result in the code needing to fully qualify class names
		// more readable to give them a verbose name.
		if(name.length()<=1) {
			return true;
		}
		
		// quickest check is for sole illegal identifiers
		if(isKeyWord(name)) {
			return true;
		}
		
		// then check for illegal chars
		final char[] chars = name.toCharArray();
		if(isAll00000(chars) || isIllegalIdentifier(chars)) {
			return true;
		}
		
		//finally the most expensive check, for concatenated illegal identifiers.
		if(isConcatenatedKeyWords(name)) {
			return true;
		}
		
		return false;		
	}
	
	private static boolean isObfuscatedField(final String name) {
		// short names just cause naming conflicts that result in the code needing to fully qualify class names
		// more readable to give them a verbose name.
		if(name.length()<=1) {
			return true;
		}
		
		// quickest check is for sole illegal identifiers
		if(isKeyWord(name)) {
			return true;
		}
		
		// then check for illegal chars
		final char[] chars = name.toCharArray();
		if(isAll00000(chars) || isIllegalIdentifier(chars)) {
			return true;
		}
		
		//finally the most expensive check, for concatenated illegal identifiers.
		if(isConcatenatedKeyWords(name)) {
			return true;
		}
		
		return false;
	}
	
	private static boolean isKeyWord(final String name) {
		return illegalIdentifiers.contains(name);
	}
	
	private static boolean isAllNumbers(final char[] chars) {
		for (char c : chars) {
			if(c<'0' || c>'9') {
				return false;
			}
		}
		return true;
	}
	
	private static boolean isAll00000(final char[] chars) {
		for (char c : chars) {
			if(c!='0' && c!='o' && c!='O') {
				return false;
			}
		}
		return true;
	}
	
	private static boolean isIllegalIdentifier(final char[] chars) {
		char firstChar = chars[0];
		if(firstChar>='0' && firstChar<='9') {
			// identifiers can't start with a number.
			return true;
		}
		for (char c : chars) {
			if(c<48 || c>122) {
				return true;
			}			
		}
		return false;
	}
	
	private static boolean isConcatenatedKeyWords(final String name) {
		if(name.length()>=minIllegalIdentifierLength*2 && name.length()<=maxIllegalIdentifierLength*2) {
			final int maxLength = Math.min(maxIllegalIdentifierLength, name.length()-minIllegalIdentifierLength);
			for(int i = minIllegalIdentifierLength;i<maxLength;i++) {
				if(illegalIdentifiers.contains(name.substring(0, i)) && illegalIdentifiers.contains(name.substring(i))) {
					return true;
				}
			}
		}
		
		return false;
	}

	
	/**
	 * 
	 * @param accessModifiers Should not include visibility modifiers (ACC_PRIVATE, ACC_PROTECTED, ACC_PUBLIC)
	 * @param access
	 * @return
	 */
	public static boolean checkAccess(final int accessModifiers, final int access) {
		return (accessModifiers&access)==accessModifiers;
	}
	
	/**
	 * 
	 * @param visibilityModifier one of, ACC_PRIVATE, ACC_PROTECTED, ACC_PUBLIC, or 0. (ACC_DEFAULT)
	 * @param access
	 * @return
	 */
	public static boolean checkVisibility(final int visibilityModifier, final int access) {
		return (access&(ACC_PRIVATE|ACC_PROTECTED|ACC_PUBLIC))==visibilityModifier;
		
	}
	
	/**
	 * 
	 * @param visibilityModifier one of, ACC_PRIVATE, ACC_PROTECTED, ACC_PUBLIC, or 0. (ACC_DEFAULT)
	 * @param accessModifiers Should not include visibility modifiers (ACC_PRIVATE, ACC_PROTECTED, ACC_PUBLIC)
	 * @param access
	 * @return
	 */
	public static boolean checkModifiers(final int visibilityModifier, final int accessModifiers, final int access) {
		return checkVisibility(visibilityModifier, access) && checkAccess(accessModifiers, access);
	}	
	
	
	public static String modifierToString(final int modifier) {
		StringBuilder sb = new StringBuilder();
		final int visModifier = modifier & (ACC_PUBLIC|ACC_PRIVATE|ACC_PROTECTED);
		
		String visibilityModifier;
		switch(visModifier) {
		case ACC_PUBLIC:
			visibilityModifier = "public";
			break;
		case Modifier.PRIVATE:
			visibilityModifier = "private";
			break;
		case Modifier.PROTECTED:
			visibilityModifier = "protected";
			break;
		default:
			visibilityModifier = "default";
			break;
		}
		sb.append(visibilityModifier);
		
		final int accModifier = modifier & ~(ACC_PUBLIC|ACC_PRIVATE|ACC_PROTECTED);
		
		if((accModifier&ACC_SYNTHETIC)==ACC_SYNTHETIC) {
			sb.append("Synthetic");
		}
		if((accModifier&ACC_BRIDGE)==ACC_BRIDGE) {
			sb.append("Bridge");
		}
		
		return sb.toString();
		
	}

	private static final String [] debugModifier = {
	    "ACC_PUBLIC",
	    "ACC_PRIVATE",
	    "ACC_PROTECTED",
	    "ACC_STATIC",
	    "ACC_FINAL",
	    "ACC_SUPER/ACC_SYNCHRONIZED",
	    "ACC_VOLATILE/ACC_BRIDGE",
	    "ACC_VARARGS/ACC_TRANSIENT",
	    "ACC_NATIVE",
	    "ACC_INTERFACE",
	    "ACC_ABSTRACT",
	    "ACC_STRICT",
	    "ACC_SYNTHETIC",
	    "ACC_ANNOTATION",
	    "ACC_ENUM",
	    "ACC_MANDATED"	
	};
	
	public static String debugModifier(int modifier) {
		StringBuilder sb = new StringBuilder();
		
		for(int i = 0;i < debugModifier.length;i++){ 
			if((modifier&(1<<i))!=0) {
				sb.append(debugModifier[i]).append(" ");
			}
		}
		sb.setLength(sb.length()-1);
		return sb.toString();
	}
	
	public static String descToString(String desc) {
		Type t = Type.getType(desc);
		return typeToString(t);
	}

	private static String typeToString(Type t) {
		switch(t.getSort()) {
		case Type.ARRAY:
			return "ArrayOf" + typeToString(t.getElementType());
		case Type.BOOLEAN:
			return "Boolean";
		case Type.BYTE:
			return "Byte";
		case Type.CHAR:
			return "Char";
		case Type.DOUBLE:
			return "Double";
		case Type.FLOAT:
			return "Float";
		case Type.INT:
			return "Int";
		case Type.LONG:
			return "Long";
		case Type.OBJECT:
			return "Object";
		case Type.SHORT:
			return "Short";
		default:
			throw new IllegalArgumentException("Unexpected Type: " + t);
		}
	}

	public final boolean isPromotedToTopTier() {
		getNewName();
		//ensure that this class' name has been deobfuscated, so that flag is known 
		return promotedToTopTier;
	}
	
	
	
}