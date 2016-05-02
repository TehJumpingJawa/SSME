package org.tjj.starsector.ssme.asm;

import java.io.IOException;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import org.tjj.starsector.ssme.ClassProvider;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

public class UiEditor implements Opcodes {

	public final Type uiComponentType;
	public final Type alignmentType = Type.getObjectType("com/fs/starfarer/api/ui/Alignment");
	public final MethodNode uiFactoryMethod;

	public final Type glLauncherType = Type.getObjectType("com/fs/starfarer/launcher/opengl/GLLauncher");
	public final Type stringType = Type.getObjectType("java/lang/String");

	public UiEditor(ClassProvider cc) throws ClassNotFoundException, IOException {

		ClassReader cr = new ClassReader(cc.getClass("com.fs.starfarer.launcher.opengl.GLLauncher"));

		ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);

		cr.accept(cw, ClassReader.SKIP_FRAMES);

		cr = new ClassReader(cw.toByteArray());

		ClassNode cn = new ClassNode(ASM5);
		cr.accept(cn, ClassReader.EXPAND_FRAMES);

		@SuppressWarnings("unchecked")
		MethodNode createLaunchUI = Iterables.find(cn.methods, new Predicate<MethodNode>() {
			public boolean apply(MethodNode input) {
				return input.name.equals("createLaunchUI");
			};
		});

		@SuppressWarnings("unchecked")
		FieldNode modsField = Iterables.find(cn.fields, new Predicate<FieldNode>() {
			@Override
			public boolean apply(FieldNode input) {
				return input.name.equals("mods");
			}
		});

		uiComponentType = Type.getType(modsField.desc);

		uiFactoryMethod = null;

		AnalyzableMethodVisitor m = new AnalyzableMethodVisitor(ASM5) {
			@Override
			public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
				Type methodDescriptor = Type.getMethodType(desc);

				if (methodDescriptor.getReturnType().equals(uiComponentType)) {
					Type[] methodParameters = methodDescriptor.getArgumentTypes();

					if (Utils.typesMatch(methodParameters,
							new Type[] { stringType, stringType, alignmentType, null, null })) {
						Object[] parameterLiterals = getMethodArgumentLiterals(methodParameters);

						if (parameterLiterals[0].equals("Mods...")) {
							System.out.println("It's a miracle!");
						}

					}

				}

				super.visitMethodInsn(opcode, owner, name, desc, itf);
			}
		};

		final LiteralAnalyzingAdapter analyzer = new LiteralAnalyzingAdapter(
				"com.fs.starfarer.launcher.opengl.GLLauncher", createLaunchUI.access, createLaunchUI.name,
				createLaunchUI.desc, m);

		m.setAnalyzer(analyzer);

		createLaunchUI.accept(analyzer);

		// uiFactoryMethod = ;

		// CtClass glLauncher =
		// cp.get("com.fs.starfarer.launcher.opengl.GLLauncher");
		//
		// CtMethod createLaunchUI =
		// glLauncher.getDeclaredMethod("createLaunchUI");
		//
		// CtField modsField = glLauncher.getField("mods");
		// uiComponentType = modsField.getType();
		//
		// CtClass stringType = cp.get("java.lang.String");
		// alignmentType = cp.get("com.fs.starfarer.api.ui.Alignment");
		//
		//
		// MethodFinder uiComponentFactoryMethodFinder = new MethodFinder(
		// new MethodPrototype(
		// EnumSet.of(AccessModifier.PUBLIC),
		// EnumSet.of(NonAccessModifier.STATIC), null,
		// null, new CtClass[]{stringType, stringType, alignmentType, null,
		// null}, uiComponentType, new CtClass[0]));
		//
		// uiFactoryMethod = uiComponentFactoryMethodFinder.getMatch();
	}
}
