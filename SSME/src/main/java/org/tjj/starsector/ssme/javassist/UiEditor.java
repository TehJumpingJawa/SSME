package org.tjj.starsector.ssme.javassist;

import java.io.IOException;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import org.tjj.starsector.ssme.ClassProvider;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

public class UiEditor implements Opcodes {

	public final String uiComponentType;
	public final String alignmentType = "com.fs.starfarer.api.ui.Alignment";
	public final MethodNode uiFactoryMethod;

	
	
	public UiEditor(ClassProvider cc) throws ClassNotFoundException, IOException {
		
		ClassReader cr = new ClassReader(cc.getClass("com.fs.starfarer.launcher.opengl.GLLauncher"));
		ClassNode cn = new ClassNode(ASM5);
		cr.accept(cn, 0);
		
		MethodNode createLaunchUI = Iterables.find(cn.methods, new Predicate<MethodNode>() {
			public boolean apply(MethodNode input) {
				return input.name.equals("createLaunchUI");
			};
		});
		
		FieldNode modsField = Iterables.find(cn.fields, new Predicate<FieldNode>() {
			@Override
			public boolean apply(FieldNode input) {
				return input.name.equals("mods");
			}
		});
		
		uiComponentType = Type.getType(modsField.desc).getClassName();
		
		uiFactoryMethod = null;
		
		createLaunchUI.accept(new MethodVisitor(ASM5) {

			@Override
			public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
				// TODO Auto-generated method stub
				super.visitMethodInsn(opcode, owner, name, desc, itf);
			}
			
			
		});
		
		
//		uiFactoryMethod = ;
		
		
//		CtClass glLauncher = cp.get("com.fs.starfarer.launcher.opengl.GLLauncher");
//		
//		CtMethod createLaunchUI = glLauncher.getDeclaredMethod("createLaunchUI");
//		
//		CtField modsField = glLauncher.getField("mods");
//		uiComponentType = modsField.getType();
//
//		CtClass stringType = cp.get("java.lang.String");
//		alignmentType = cp.get("com.fs.starfarer.api.ui.Alignment");
//		
//		
//		MethodFinder uiComponentFactoryMethodFinder = new MethodFinder(
//				new MethodPrototype(
//						EnumSet.of(AccessModifier.PUBLIC), EnumSet.of(NonAccessModifier.STATIC), null,
//						null, new CtClass[]{stringType, stringType, alignmentType, null, null}, uiComponentType, new CtClass[0]));
//		
//		uiFactoryMethod = uiComponentFactoryMethodFinder.getMatch();
	}
}
