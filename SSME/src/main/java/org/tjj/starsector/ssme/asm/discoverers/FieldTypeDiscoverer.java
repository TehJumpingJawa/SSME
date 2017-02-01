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
public class FieldTypeDiscoverer extends ClassVisitor implements Opcodes {

	private String fieldName;
	private String fieldDesc;
	
	public FieldTypeDiscoverer(String fieldName) {
		this(fieldName, null);
	}
	
	public FieldTypeDiscoverer(String fieldName, ClassVisitor cv) {
		super(ASM5, cv);
		this.fieldName = fieldName;
	}
	
	public String getFieldDescriptor() {
		return fieldDesc;
	}
	
	public Type getFieldType() {
		return Type.getType(fieldDesc);
	}
	
	@Override
	public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
		
		if(fieldName.equals(name)) {
			fieldDesc = desc;
		}
		return super.visitField(access, name, desc, signature, value);
	}

}
