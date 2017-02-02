package org.tjj.starsector.ssme.asm;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.tjj.starsector.ssme.StarsectorTypes;

public class UiElementIdentifier extends AnalyzableMethodVisitor implements Opcodes {

	// the name of the field in which the ui element is stored.
	private String fieldName;

	public String getFieldName() {
		return fieldName;
	}

	private final StarsectorTypes types;

	public UiElementIdentifier(int api, MethodVisitor mv, StarsectorTypes types) {
		super(api, mv);
		this.types = types;
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
			if (methodDescriptor.getReturnType().equals(types.uiComponent)) {
				Type[] params = methodDescriptor.getArgumentTypes();

				if (Utils.typesMatch(params,
						new Type[] { types.string, types.string, types.alignment, null, null })) {
					
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
