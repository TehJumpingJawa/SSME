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
import org.tjj.starsector.ssme.asm.discoverers.FieldTypeDiscoverer;
import org.tjj.starsector.ssme.asm.discoverers.InterfaceTypeDiscoverer;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

public class UiEditor implements Opcodes {

	public static final Type alignmentType = Type.getObjectType("com/fs/starfarer/api/ui/Alignment");

	public static final Type glLauncherType = Type.getObjectType("com/fs/starfarer/launcher/opengl/GLLauncher");
	public static final Type stringType = Type.getObjectType("java/lang/String");
	
	public static final String glLauncherClassName = "com.fs.starfarer.launcher.opengl.GLLauncher";
	private static final int glLauncherActionListenerInterfaceIndex = 1;
	
	/**
	 * type of the panel class.
	 */
	public final Type uiPanelType;
	/**
	 * type of the component class.
	 */
	public final Type uiComponentType;
	/**
	 * type of the action listener interface.
	 */
	public final Type uiActionListenerType; 
	
	public UiEditor(ClassProvider cc) throws ClassNotFoundException, IOException, ClassAlreadyLoadedException {

		ClassReader cr = new ClassReader(cc.getClass(glLauncherClassName));
		
		FieldTypeDiscoverer uiPanelTypeDiscoverer = new FieldTypeDiscoverer("panel");
		FieldTypeDiscoverer uiComponentTypeDiscoverer = new FieldTypeDiscoverer("launchButton", uiPanelTypeDiscoverer);
		InterfaceTypeDiscoverer uiActionListenerTypeDiscoverer = new InterfaceTypeDiscoverer(glLauncherActionListenerInterfaceIndex, uiComponentTypeDiscoverer);
		
		cr.accept(uiActionListenerTypeDiscoverer, ClassReader.SKIP_FRAMES);
		
		uiPanelType = uiPanelTypeDiscoverer.getFieldType();
		uiComponentType = uiComponentTypeDiscoverer.getFieldType();
		uiActionListenerType = uiActionListenerTypeDiscoverer.getInterfaceType();
		
		
		
		
		
		
		
		
		testLauncherManipulation(cc);
	}
	
	public void testLauncherManipulation(ClassProvider cc) throws ClassNotFoundException, IOException, ClassAlreadyLoadedException {
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

		MethodNode uiFactoryMethod = null;
		
		
		String[] exceptions = new String[createLaunchUI.exceptions.size()];
		createLaunchUI.exceptions.toArray(exceptions);
		
		MethodNode newMethod = new MethodNode(createLaunchUI.access, createLaunchUI.name, createLaunchUI.desc, createLaunchUI.signature, exceptions); 

		UiElementIdentifier m = new UiElementIdentifier(ASM5, newMethod, this);


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
