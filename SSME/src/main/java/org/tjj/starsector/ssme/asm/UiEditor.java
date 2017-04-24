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
import org.tjj.starsector.ssme.ObfuscationMap;
import org.tjj.starsector.ssme.asm.discoverers.FieldTypeDiscoverer;
import org.tjj.starsector.ssme.asm.discoverers.InterfaceTypeDiscoverer;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

public class UiEditor implements Opcodes {

	public UiEditor(ClassProvider cc, String type, String method) throws ClassNotFoundException, IOException, ClassAlreadyLoadedException {

		testLauncherManipulation(cc);
	}
	
	public void testLauncherManipulation(ClassProvider cc) throws ClassNotFoundException, IOException, ClassAlreadyLoadedException {
		
		ObfuscationMap types = cc.getObfuscationMap();
		
		ClassReader cr = new ClassReader(cc.getClass(Unobfuscated.Types.glLauncher.getClassName()));

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

		MethodNode uiFactoryMethod = null;
		
		
		String[] exceptions = new String[createLaunchUI.exceptions.size()];
		createLaunchUI.exceptions.toArray(exceptions);
		
		MethodNode newMethod = new MethodNode(createLaunchUI.access, createLaunchUI.name, createLaunchUI.desc, createLaunchUI.signature, exceptions); 

		UiElementIdentifier m = new UiElementIdentifier(ASM5, newMethod, types);


		final LiteralAnalyzingAdapter analyzer = new LiteralAnalyzingAdapter(
				Unobfuscated.Types.glLauncher.getInternalName(), createLaunchUI.access, createLaunchUI.name,
				createLaunchUI.desc, m);

		m.setAnalyzer(analyzer);
		createLaunchUI.accept(analyzer);
		
		cn.methods.add(newMethod);
		
		cw = new ClassWriter(0);
		
		cn.accept(cw);
		
		cc.saveTransformation(Unobfuscated.Types.glLauncher.getClassName(), cw.toByteArray());
				
	}
}
