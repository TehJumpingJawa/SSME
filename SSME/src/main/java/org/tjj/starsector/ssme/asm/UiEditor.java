package org.tjj.starsector.ssme.asm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import org.tjj.starsector.ssme.ClassAlreadyLoadedException;
import org.tjj.starsector.ssme.ClassProvider;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

public class UiEditor implements Opcodes {

	public final Type uiComponentType;
	public final Type alignmentType = Type.getObjectType("com/fs/starfarer/api/ui/Alignment");
	public final MethodNode uiFactoryMethod;

	public final Type glLauncherType = Type.getObjectType("com/fs/starfarer/launcher/opengl/GLLauncher");
	public final Type stringType = Type.getObjectType("java/lang/String");

	public UiEditor(ClassProvider cc) throws ClassNotFoundException, IOException, ClassAlreadyLoadedException {

		String launcherClassname = "com.fs.starfarer.launcher.opengl.GLLauncher";
		
		ClassReader cr = new ClassReader(cc.getClass(launcherClassname));

		ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);

		cr.accept(cw, ClassReader.SKIP_FRAMES);

		cr = new ClassReader(cw.toByteArray());

		ClassNode cn = new ClassNode(ASM5);
		cr.accept(cn, ClassReader.EXPAND_FRAMES);

		final List<MethodNode> removedMethods = new ArrayList<>();
		
		@SuppressWarnings("unchecked")
		boolean found = Iterables.removeIf(cn.methods, new Predicate<MethodNode>() {
			public boolean apply(MethodNode input) {
				if(input.name.equals("createLaunchUI")) {
					removedMethods.add(input);
					return true;
				}
				return false;
			};
		});
		
		if(removedMethods.size()!=1) {
			throw new RuntimeException("Multiple createLaunchUI found");
		}
		MethodNode createLaunchUI = removedMethods.get(0);

		@SuppressWarnings("unchecked")
		FieldNode modsField = Iterables.find(cn.fields, new Predicate<FieldNode>() {
			@Override
			public boolean apply(FieldNode input) {
				return input.name.equals("mods");
			}
		});

		uiComponentType = Type.getType(modsField.desc);

		uiFactoryMethod = null;
		
		
		String[] exceptions = new String[createLaunchUI.exceptions.size()];
		createLaunchUI.exceptions.toArray(exceptions);
		
		MethodNode newMethod = new MethodNode(createLaunchUI.access, createLaunchUI.name, createLaunchUI.desc, createLaunchUI.signature, exceptions); 

		AnalyzableMethodVisitor m = new AnalyzableMethodVisitor(ASM5, newMethod) {
			
			boolean foundMods = false;
			
			@Override
			public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
				Type methodDescriptor = Type.getMethodType(desc);

				if (methodDescriptor.getReturnType().equals(uiComponentType)) {
					Type[] methodParameters = methodDescriptor.getArgumentTypes();

					if (Utils.typesMatch(methodParameters,
							new Type[] { stringType, stringType, alignmentType, null, null })) {
						Object[] parameterLiterals = getMethodArgumentLiterals(methodParameters);

						if (parameterLiterals[0].equals("Mods...")) {
							foundMods = true;
						}

					}

				}
				else if(foundMods) {
					if(name.equals("inBMid")) {
						Type inBMiddesc = Type.getMethodType(desc);
						
						Type[] params = inBMiddesc.getArgumentTypes();
						
						if(Utils.typesMatch(params, new Type[]{Type.FLOAT_TYPE})) {
							if(getMethodArgumentLiterals(params)[0].equals(25.0F)) {
								visitInsn(POP);
								visitLdcInsn(0F);
							}
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
		
		cn.methods.add(newMethod);
		
		cw = new ClassWriter(0);
		
		cn.accept(cw);
		
		cc.saveTransformation(launcherClassname, cw.toByteArray());
		


	}
}
