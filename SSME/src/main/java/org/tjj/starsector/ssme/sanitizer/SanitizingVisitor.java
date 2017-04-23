package org.tjj.starsector.ssme.sanitizer;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureWriter;

/**
 * This Visitor remaps the names of obfuscated fields & methods 
 * 
 * @author TehJumpingJawa
 *
 */
public class SanitizingVisitor extends ClassVisitor implements Opcodes {

	private Sanitizer demap;
	
	public SanitizingVisitor(Sanitizer demap) {
		super(ASM5);
		this.demap = demap;
	}

	public SanitizingVisitor(Sanitizer demap, ClassVisitor cv) {
		super(ASM5, cv);
		this.demap = demap;
	}

	private ClassMapping cm;

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {

		cm = demap.getFromWorkingSet(name);
		
//		System.out.println("visiting " + name + " -> " + cm.newName);
		name = cm.getNewName();
		superName = cm.getSuper().getNewName();
		for(int i = 0;i < interfaces.length;i++) {
			ClassMapping interfaceMapping = demap.getFromWorkingSet(interfaces[i]);
			// if the mapping is null, it's a class that was never touched by the deobfuscation
			// thus it can't be an obfuscated class, so the default value should be used.
			if(interfaceMapping!=null) {
				interfaces[i] = interfaceMapping.getNewName();
			}
			else {
//				System.out.println("Not deobfuscating missing interface name: " + name);
			}
		}

		if(signature!=null) {
			signature = deobfuscateSignature(signature);
		}
		
		super.visit(version, access, name, signature, superName, interfaces);
	}

	@Override
	public void visitOuterClass(String owner, String name, String desc) {
		ClassMapping ownerCm = demap.getFromWorkingSet(owner);
		
		owner = ownerCm.getNewName();
		if(name!=null) {
			name = ownerCm.getMethodMap().get(new ClassMapping.Method(name, desc, null));
		}
		
		super.visitOuterClass(owner, name, desc);
	}

	@Override
	public void visitInnerClass(String name, String outerName, String innerName, int access) {
		
		ClassMapping innerClass = demap.getFromWorkingSet(name);
		if(innerClass!=null) {
			if(innerClass.isPromotedToTopTier()) {
				// this class is no-longer an inner class, so do nothing.
//				System.out.println(innerClass.getNewName() + " is promoted; discarding from " + cm.getNewName());
				return;
			}
			name = innerClass.getNewName();
		}
		else {
//			System.out.println("Not deobfuscating missing inner class name: " + name);			
		}
		
		if(outerName!=null) {
			// this class is contained within another.
			ClassMapping outerClass = demap.getFromWorkingSet(outerName);
			if(outerClass!=null) { 
				outerName = outerClass.getNewName();
			}
			else {
//				System.out.println("Not deobfuscating missing inner class's container class name: " + outerName);				
			}
		}
		
		if(innerName!=null) {
			innerName = name.substring(name.lastIndexOf('$')+1);
		}
		
		super.visitInnerClass(name, outerName, innerName, access);
	}

	@Override
	public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
		
		
		String newFieldName = cm.getFieldMap().get(name);
		
		if((access&Opcodes.ACC_SYNTHETIC)==Opcodes.ACC_SYNTHETIC) {
			if((cm.classNode.access&Opcodes.ACC_ENUM)==Opcodes.ACC_ENUM) {
				// do nothing to synthetic fields inside enums
			}
			else if(newFieldName.startsWith("$SWITCH_TABLE$")) {
				// do nothing to enum switch_table fields
			}
			else {
				//TODO, eventually want to get rid of this.
				access&=~Opcodes.ACC_SYNTHETIC;
			}
		}		
		
		
		name = newFieldName; 
		
		desc = deobfuscateType(desc);
		
		if(signature!=null) {
			signature = deobfuscateSignature(signature);
		}
		
		return super.visitField(access, name, desc, signature, value);
	}
	
	private String deobfuscateMethodType(String desc) {
		Type descriptor = Type.getMethodType(desc);
		
		Type returnType = deobfuscateType(descriptor.getReturnType());
		
		Type [] argumentTypes = descriptor.getArgumentTypes();
		for (int i = 0; i < argumentTypes.length; i++) {
			argumentTypes[i] = deobfuscateType(argumentTypes[i]);
		}
		
		descriptor = Type.getMethodType(returnType, argumentTypes);
		
		return descriptor.toString();		
	}

	private String deobfuscateType(String t) {
		return deobfuscateType(Type.getType(t)).toString();
	}

	private Type deobfuscateType(final Type paramType) {
		Type t = paramType;
		int dimensions = 0;
		if(t.getSort()==Type.ARRAY) {
			dimensions = t.getDimensions();
			t = t.getElementType();
		}
		if(t.getSort()==Type.OBJECT) {
			ClassMapping fieldType = demap.getFromWorkingSet(t.getInternalName());
			if(fieldType!=null) {
				StringBuilder sb = new StringBuilder();
				while(dimensions-->0) {
					sb.append('[');
				}
				sb.append('L').append(fieldType.getNewName()).append(';');
				return Type.getType(sb.toString());
			}
			else {
//				System.out.println("Not deobfuscating missing field type: " + t);
			}
		}
		return paramType;
	}
	
	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		
		String newMethodName = cm.getMethodMap().get(new ClassMapping.Method(name, desc, null));
		
		if((access&Opcodes.ACC_BRIDGE)==Opcodes.ACC_BRIDGE) {
			//remove synthetic & bridge flags from bridge methods 
//			access&=~(Opcodes.ACC_SYNTHETIC|Opcodes.ACC_BRIDGE);
		}
		else if((access&Opcodes.ACC_SYNTHETIC)==Opcodes.ACC_SYNTHETIC) {
			if((cm.classNode.access&Opcodes.ACC_ENUM)==Opcodes.ACC_ENUM) {
				// do nothing to the synthetic method inside enums
			}
			else if(newMethodName.startsWith("$SWITCH_TABLE$")) {
				// do nothing to enum switch_table methods
			}
			else {
				access&=~Opcodes.ACC_SYNTHETIC;
			}
		}		
		
		
		name = newMethodName;
		
		desc = deobfuscateMethodType(desc);

		for (int i = 0; i < exceptions.length; i++) {
			ClassMapping exceptionClass = demap.getFromWorkingSet(exceptions[i]);
			if(exceptionClass!=null) {
				exceptions[i] = exceptionClass.getNewName();
			}
			else {
//				System.out.println("Not deobfuscating missing exception type: " + exceptions[i]);
			}
		}
		
		if(signature!=null) {
			signature = deobfuscateSignature(signature);
		}
		
		return new DeMethodVisitor(super.visitMethod(access, name, desc, signature, exceptions));
	}

	class DeMethodVisitor extends MethodVisitor {

		public DeMethodVisitor(MethodVisitor mv) {
			super(ASM5, mv);
		}

		public DeMethodVisitor() {
			super(ASM5);
		}

		@Override
		public void visitTypeInsn(int opcode, String type) {
			
			// oddly array types are supplied as fully qualified java internal type definitions (e.g. "[Ljava/lang/Object;")
			// where-as Object types are supplied as raw internal class names. (e.g. java/lang/Object )
			if(type.charAt(0)=='[') {
				//array type
				type = deobfuscateType(type);
			}
			else {
				ClassMapping cm = demap.getFromWorkingSet(type);
				if(cm!=null) {
					type = cm.getNewName();
				}
				else {
//					System.out.println("Not deobfuscating type in visitTypeInsn: " + type);
				}
			}
	

			super.visitTypeInsn(opcode, type);
		}

		@Override
		public void visitFieldInsn(int opcode, String owner, String name, String desc) {
			ClassMapping cm = demap.getFromWorkingSet(owner);
			if(cm!=null) {
				owner = cm.getNewName();
				name = cm.getUnobfuscatedFieldName(name);
				desc = deobfuscateType(desc);
			}
			else {
//				System.out.println("Not deobfuscating owner in visitFieldInsn: " + owner);
			}
			
			
			super.visitFieldInsn(opcode, owner, name, desc);
		}

		@Override
		public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
			if(owner.startsWith("[")) {
				// do nothing.
				//array.clone();
			}
			else {
				ClassMapping cm = demap.getFromWorkingSet(owner);
				if(cm!=null) {
					owner = cm.getNewName();
					String newName;
					do {
						newName = cm.getMethodMap().get(new ClassMapping.Method(name, desc, null));
						//search super classes; this is necessary for static method invocations
						// as it appears to be legal for static method invocations to point at a subclass
						// when the static method itself is declared in a superclass.
						cm = cm.getSuper();
					}
					while(newName==null);
					name = newName;
					
					desc = deobfuscateMethodType(desc);
				}
				else {
	//				System.out.println("Not deobfuscating owner in visitFieldInsn: " + owner);
				}
			}
			super.visitMethodInsn(opcode, owner, name, desc, itf);
		}

		@Override
		public void visitInvokeDynamicInsn(String name, String desc, Handle bsm, Object... bsmArgs) {
			throw new NotImplementedException();
		}

		@Override
		public void visitLdcInsn(Object cst) {
			if(cst instanceof Type) {
				Type type = (Type)cst;
				cst = deobfuscateType(type);
			}
			super.visitLdcInsn(cst);
		}

		@Override
		public void visitMultiANewArrayInsn(String desc, int dims) {
			desc = deobfuscateType(desc);
			super.visitMultiANewArrayInsn(desc, dims);
		}

		@Override
		public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
			if(type!=null) {
				ClassMapping cm = demap.getFromWorkingSet(type);
				if(cm!=null) {
					type = cm.getNewName();
				}
			}
			super.visitTryCatchBlock(start, end, handler, type);
		}

		@Override
		public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
			throw new NotImplementedException();
		}
	}
	
	private String deobfuscateSignature(String signature) {
		SignatureReader sr = new SignatureReader(signature);
		SignatureWriter writer = new DeSignatureVisitor();
		sr.accept(writer);
		
		return writer.toString();
	}
	
	class DeSignatureVisitor extends SignatureWriter {

		public DeSignatureVisitor() {
		}

		@Override
		public void visitFormalTypeParameter(String name) {
			super.visitFormalTypeParameter(name);
		}

		@Override
		public void visitTypeVariable(String name) {
			super.visitTypeVariable(name);
		}

		@Override
		public void visitClassType(String name) {
			
			ClassMapping cm = demap.getFromWorkingSet(name);
			if(cm!=null) {
				name = cm.getNewName();
			}
			else {
//				System.out.println("Not deobfuscating generic classType: " + name);
			}
			super.visitClassType(name);
		}

		@Override
		public void visitInnerClassType(String name) {
			throw new NotImplementedException();
		}
	}
}
