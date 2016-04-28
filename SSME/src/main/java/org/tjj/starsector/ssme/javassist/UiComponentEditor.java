package org.tjj.starsector.ssme.javassist;

import javassist.CannotCompileException;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.bytecode.AttributeInfo;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.MethodInfo;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

public class UiComponentEditor extends ExprEditor {

	private String componentText; 
	private final UiEditor editor;
	
	public UiComponentEditor(UiEditor editor, String componentText) {
		this.editor = editor;
		this.componentText = componentText;
	}
	
	
	@Override
	public void edit(MethodCall m) throws CannotCompileException {
		CtMethod method;
		try {
			method = m.getMethod();
		} catch (NotFoundException e) {
			throw new CannotCompileException(e);
		}
		
		
		
//		if(method==editor.uiFactoryMethod) {
//
////			m.
////			
////			MethodInfo mi = method.getMethodInfo();
////			CodeAttribute ca = mi.getCodeAttribute();
////			AttributeInfo ai = ((AttributeInfo)ca.getAttributes().get(0));
////			ai.
//		}

		
		super.edit(m);
	}

	

}
