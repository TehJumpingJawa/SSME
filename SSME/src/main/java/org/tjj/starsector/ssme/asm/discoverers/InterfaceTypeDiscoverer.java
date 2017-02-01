package org.tjj.starsector.ssme.asm.discoverers;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * 
 * Will return the type of the field matching the supplied name.
 * 
 * @author TehJumpingJawa
 *
 */
public class InterfaceTypeDiscoverer extends ClassVisitor implements Opcodes {

	private int interfaceIndex;
	private String fieldDesc;
	
	public InterfaceTypeDiscoverer(int interfaceIndex) {
		this(interfaceIndex, null);
	}
	
	public InterfaceTypeDiscoverer(int interfaceIndex, ClassVisitor cv) {
		super(ASM5, cv);
		this.interfaceIndex = interfaceIndex;
	}
	
	public String getInterfaceDescriptor() {
		return fieldDesc;
	}
	
	public Type getInterfaceType() {
		return Type.getType(fieldDesc);
	}

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		fieldDesc = interfaces[interfaceIndex];
		super.visit(version, access, name, signature, superName, interfaces);
	}
}
