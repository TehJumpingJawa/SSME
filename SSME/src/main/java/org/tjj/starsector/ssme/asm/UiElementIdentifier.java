package org.tjj.starsector.ssme.asm;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.tjj.starsector.ssme.ObfuscationMap;
import org.tjj.starsector.ssme.Unobfuscated;

public class UiElementIdentifier extends AnalyzableMethodVisitor implements Opcodes {

	// the name of the field in which the ui element is stored.
	private String fieldName;

	public String getFieldName() {
		return fieldName;
	}

	private final ObfuscationMap obfuscationMap;

	private Type componentType;
	
	public UiElementIdentifier(int api, MethodVisitor mv, ObfuscationMap types) {
		super(api, mv);
		this.obfuscationMap = types;
		componentType = obfuscationMap.obfuscateType("com/fs/starfarer/ui/Component");
	}

	enum State {
		COMPONENT_CONSTRUCTION, COMPONENT_ASSIGNMENT, COMPONENT_ADDITION, COMPONENT_POSITIONING, DONE;
	};

	private State state = State.COMPONENT_CONSTRUCTION;

	@Override
	public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
		Type methodDescriptor = Type.getMethodType(desc);

		switch (state) {
		case COMPONENT_CONSTRUCTION: {
			if (methodDescriptor.getReturnType().equals(componentType)) {
				Type[] params = methodDescriptor.getArgumentTypes();

				if (Utils.typesMatch(params,
						new Type[] { Unobfuscated.Types.string, Unobfuscated.Types.string, Unobfuscated.Types.alignment, null, null })) {
					
					if ("Mods...".equals(getMethodArgumentInfo(params, 0).literalValue)) {
						state = State.COMPONENT_ASSIGNMENT;
					}

				}

			}
		}
			break;
		case COMPONENT_ADDITION: {
			if (name.equals("add")) {
				Type[] params = methodDescriptor.getArgumentTypes();

				if (fieldName.equals(getMethodArgumentInfo(params, 0).sourceField)) {
					// found the add(mods) expression.
					state = State.COMPONENT_POSITIONING;
				}
			}
		}
			break;
		case COMPONENT_POSITIONING: {
			if (name.equals("inBMid")) {
				Type[] params = methodDescriptor.getArgumentTypes();

				if (Utils.typesMatch(params, new Type[] { Type.FLOAT_TYPE })) {
					if(((Float)25F).equals(getMethodArgumentInfo(params, 0).literalValue)) {
						name = "inTMid";
//						visitInsn(POP);
//						visitLdcInsn(0F);
						state = State.DONE;
					}
				}
			}
		}
			break;
		}

		super.visitMethodInsn(opcode, owner, name, desc, itf);
	}

	@Override
	public void visitFieldInsn(int opcode, String owner, String name, String desc) {
		switch (state) {
		case COMPONENT_ASSIGNMENT: {
			if (opcode == PUTFIELD) {
				fieldName = name;
				state = State.COMPONENT_ADDITION;
			}
		}
		}
		super.visitFieldInsn(opcode, owner, name, desc);
	}

}
