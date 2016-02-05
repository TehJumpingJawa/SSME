package org.tjj.starsector.ssme.asm;

import java.lang.reflect.Field;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.TypePath;

/**
 * This visitor can be placed at the beginning of every ASM visitor chain.
 * If it encounters a class with the @NoTransform annotation,
 * it will skip the entire transformation chain and forward its visitor calls direct to the ClassWriter in the chain.
 * 
 * If there is no ClassWriter in the chain, it will cause the chain to do nothing. 
 * 
 * @deprecated The ClassLoader structure now makes it impossible to transform instrumentation classes.
 * @author TehJumpingJawa
 *
 */
public class NoTransformChecker extends ClassVisitor implements Opcodes{

	private boolean noTransform = false;
	
	private ClassWriter classWriter = null;
	// Make this visitor skip all other visitors if it detects the NoTransform annotation.
	
	public NoTransformChecker() {
		super(ASM5);
		// TODO Auto-generated constructor stub
	}

	public NoTransformChecker(ClassVisitor cv) {
		super(ASM5, cv);
		// TODO Auto-generated constructor stub
	}

	@Override
	public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
		
		if(desc.equals("Lorg/tjj/starsector/ssme/NoTransform;")) {
			// this class has the @NoTransform annotation,
			// so skip all further Visitors in the chain (except ClassWriter)
			noTransform = true;
			classWriter = getClassWriter();
			if(classWriter!=null) {
				return classWriter.visitAnnotation(desc, visible);
			}
			else {
				return null;
			}
		}
		else {
			return super.visitAnnotation(desc, visible);
		}
	}

	private ClassWriter getClassWriter() {
		try {
			Field f = ClassVisitor.class.getDeclaredField("cv");
			f.setAccessible(true);

			ClassVisitor delegate = cv;

			while (delegate != null && !(delegate instanceof ClassWriter)) {
				delegate = (ClassVisitor) f.get(delegate);
			}
			return (ClassWriter) delegate;
		} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}
	
	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		// TODO Auto-generated method stub
		if(noTransform) {
			if(classWriter!=null) {
				classWriter.visit(version, access, name, signature, superName, interfaces);
			}
		}
		else {
			super.visit(version, access, name, signature, superName, interfaces);
		}
	}

	@Override
	public void visitSource(String source, String debug) {
		if(noTransform) {
			if(classWriter!=null) {
				classWriter.visitSource(source, debug);
			}
		}
		else {
			super.visitSource(source, debug);
		}
	}

	@Override
	public void visitOuterClass(String owner, String name, String desc) {
		if(noTransform) {
			if(classWriter!=null) {
				classWriter.visitOuterClass(owner, name, desc);
			}
		}
		else {
			super.visitOuterClass(owner, name, desc);
		}
	}

	@Override
	public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
		if(noTransform) {
			if(classWriter!=null) {
				return classWriter.visitTypeAnnotation(typeRef, typePath, desc, visible);
			}
			else {
				return null;
			}
		}
		else {
			return super.visitTypeAnnotation(typeRef, typePath, desc, visible);
		}
	}

	@Override
	public void visitAttribute(Attribute attr) {
		if(noTransform) {
			if(classWriter!=null) {
				classWriter.visitAttribute(attr);
			}
		}
		else {
			super.visitAttribute(attr);
		}
	}

	@Override
	public void visitInnerClass(String name, String outerName, String innerName, int access) {
		if(noTransform) {
			if(classWriter!=null) {
				classWriter.visitInnerClass(name, outerName, innerName, access);
			}
		}
		else {
			super.visitInnerClass(name, outerName, innerName, access);
		}
	}

	@Override
	public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
		if(noTransform) {
			if(classWriter!=null) {
				return classWriter.visitField(access, name, desc, signature, value);
			}
			else {
				return null;
			}
		}
		else {
			return super.visitField(access, name, desc, signature, value);
		}
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		if(noTransform) {
			if(classWriter!=null) {
				return classWriter.visitMethod(access, name, desc, signature, exceptions);
			}
			else {
				return null;
			}
		}
		else {
			return super.visitMethod(access, name, desc, signature, exceptions);
		}
	}

	@Override
	public void visitEnd() {
		if(noTransform) {
			if(classWriter!=null) {
				classWriter.visitEnd();
			}
		}
		else {
			super.visitEnd();
		}
	}

}
