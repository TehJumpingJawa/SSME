package org.tjj.starsector.ssme.asm;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class UiElementIdentifier extends AnalyzableMethodVisitor implements Opcodes {

	// the name of the field in which the ui element is stored.
	private String fieldName;

	public String getFieldName() {
		return fieldName;
	}

	private final Type uiComponentType;

	public UiElementIdentifier(int api, MethodVisitor mv, Type uiComponentType) {
		super(api, mv);
		this.uiComponentType = uiComponentType;
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
			if (methodDescriptor.getReturnType().equals(uiComponentType)) {
				Type[] methodParameters = methodDescriptor.getArgumentTypes();

				if (Utils.typesMatch(methodParameters,
						new Type[] { UiEditor.stringType, UiEditor.stringType, UiEditor.alignmentType, null, null })) {
					Object[] parameterLiterals = getMethodArgumentLiterals(methodParameters);

					if (parameterLiterals[0].equals("Mods...")) {
						state = State.COMPONENT_ASSIGNMENT;
					}

				}

			}
		}
			break;
		case COMPONENT_ADDITION: {
			if (name.equals("add")) {
				Type[] params = methodDescriptor.getArgumentTypes();

				if (getMethodArgumentSources(params)[0].equals(fieldName)) {
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
					if (getMethodArgumentLiterals(params)[0].equals(25.0F)) {
						visitInsn(POP);
						visitLdcInsn(0F);
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
