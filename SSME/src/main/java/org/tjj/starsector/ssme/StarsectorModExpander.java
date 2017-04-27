package org.tjj.starsector.ssme;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.swing.JButton;
import javax.swing.JFrame;

import org.tjj.starsector.ssme.asm.UiEditor;
import org.tjj.starsector.ssme.installer.Installer;
import org.tjj.starsector.ssme.sanitizer.Sanitizer;
import org.tjj.starsector.ssme.ui.AuthorizationUI;

import javassist.CannotCompileException;
import javassist.NotFoundException;

public class StarsectorModExpander {

	public static final String VERSION = "SSME 1.0";
	
	/**
	 * Intercepts the call to StarfarerLauncher.actionPerformed, so that mods utilizing SSME can perform their earlyTransformations.  
	 * 
	 * @param cp
	 * @throws NotFoundException
	 * @throws CannotCompileException
	 */
//	private static void doActionPerformedTransformation(ClassPool cp) throws NotFoundException, CannotCompileException {
//		CtClass modManager = cp.get("com.fs.starfarer.launcher.ModManager");
//		
//		List<CtMethod> matches = JavassistUtils.findDeclaredMethods(modManager, new MethodPrototype(EnumSet.of(AccessModifier.PUBLIC),EnumSet.of(NonAccessModifier.SYNCHRONIZED, NonAccessModifier.STATIC),"getInstance", new CtClass[0], cp.get("com.fs.starfarer.launcher.ModManager")));
//		
//		if(matches.size()!=1) throw new NotFoundException("Expected 1 'getInstance' method, found:" + matches);
//		CtMethod getModManager = matches.get(0);
//		
//		matches = JavassistUtils.findDeclaredMethods(modManager, new MethodPrototype(EnumSet.of(AccessModifier.PUBLIC), EnumSet.of(NonAccessModifier.SYNCHRONIZED),"getEnabledMods", new CtClass[0], cp.get("java.util.List")));
//		if(matches.size()!=1) throw new NotFoundException("Expected 1 'getEnabledMods' returning List, found: " + matches);
//		
//		CtMethod getActiveMods = matches.get(0);
//		
//		CtClass launcher = cp.get("com.fs.starfarer.StarfarerLauncher");
//		CtMethod actionPerformed = launcher.getDeclaredMethod("actionPerformed", new CtClass[]{cp.get("java.awt.event.ActionEvent")});
//		
//		actionPerformed.setName("SSME_actionPerformed");
//		
//		CtMethod newActionPerformed = CtNewMethod.make("public void actionPerformed(java.awt.event.ActionEvent e) {" + StarsectorModExpander.class.getName() + ".receiveModList($0,$1," + "com.fs.starfarer.launcher.ModManager" +"." + getModManager.getName() +"()." + getActiveMods.getName() +"());}",launcher);
//		launcher.addMethod(newActionPerformed);		
//	}

	/**
	 * hook for the code injected into StarfarerLauncher.actionPerformed.
	 * static for convenience.
	 * 
	 * @param modList
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * @throws NoSuchFieldException
	 * @throws SecurityException
	 */
	public static void receiveModList(Object launcher, ActionEvent parameter, List<?> modList) {
		Component source = (Component)parameter.getSource();
		while(source.getParent()!=null) {
			source = source.getParent();
		}
		//find the root component, and hide it.
		Window w = (Window)source;
		w.setVisible(false);
		w.dispose();
		
		new Thread(new ModValidator(launcher, parameter, modList)).start();
	}	

	/**
	 * Inserts some UI elements into the launcher to allow user configuration of the stored SSME authorizations.
	 * (basically a front-end to ssmeAuth.json) 
	 * @param cp
	 * @throws IOException 
	 * @throws ClassNotFoundException 
	 * @throws ClassAlreadyLoadedException 
	 * @throws NotFoundException 
	 * @throws CannotCompileException 
	 */
	private static void doLauncherPreferencesTransformation(ClassProvider cc) throws ClassNotFoundException, IOException, ClassAlreadyLoadedException {
		
		new UiEditor(cc, "", "");
		
//		if(false) {
//			
//			//old code (will be used to base the legacy launcher hooks upon)
//			CtClass launcherUiType = cp.get("com.fs.starfarer.launcher.StarfarerLauncherUI");
//			
//			CtMethod method = Utils.findDeclaredMethods(launcherUiType, new MethodPrototype(null, new CtClass[] { cp.get("java.awt.event.ActionListener"), CtClass.booleanType, cp.get("java.lang.String"), CtClass.floatType}, cp.get("javax.swing.JFrame"))).getMatch();
//	
//			method.insertAfter("{" + StarsectorModExpander.class.getName()+ ".receiveLauncherJFrame($_);}");
//		}
//		else {
//			CtClass glLauncher = cp.get("com.fs.starfarer.launcher.opengl.GLLauncher");
//			
//			CtMethod createLaunchUI = glLauncher.getDeclaredMethod("createLaunchUI");
//			
//			CtField modsField = glLauncher.getField("mods");
//			CtClass uiComponentType = modsField.getType();
//
//			CtClass stringType = cp.get("java.lang.String");
//			CtClass alignmentType = cp.get("com.fs.starfarer.api.ui.Alignment");
//			
//			
//			MethodFinder uiComponentFactoryMethodFinder = new MethodFinder(
//					new MethodPrototype(
//							EnumSet.of(AccessModifier.PUBLIC), EnumSet.of(NonAccessModifier.STATIC), null,
//							null, new CtClass[]{stringType, stringType, alignmentType, null, null}, uiComponentType, new CtClass[0]));
//			
//			createLaunchUI.instrument(uiComponentFactoryMethodFinder);
//			
//			CtMethod factoryMethod = uiComponentFactoryMethodFinder.getMatch();
//			
//			glLauncher.addField(CtField.make("private " + uiComponentType.getName() + " ssme;", glLauncher));
//			
//			String ssmeConfigCode = "{this.ssme = " + factoryMethod.getDeclaringClass().getName() + "." + factoryMethod.getName() + "(" +
//					"\"SSME...\", \"graphics/fonts/orbitron24aabold.fnt\",com.fs.starfarer.api.ui.Alignment.MID,null,this);\n"
//					+ "this.panel.add(this.ssme).setSize(100F,20F).belowLeft(this.mods, 5F);\n"
//					+ "System.out.println(\"insertion complete\");}";
//			
//			
//					
//			createLaunchUI.insertAfter(ssmeConfigCode);
//			
//			
//		}
	}
	
	/**
	 * hook for the code injected into the method that returns the Launcher's assembled JFrame.
	 * We add the necessary components to the JFrame for configuring SSME.
	 * 
	 * @param frame
	 */
	public static void receiveLauncherJFrame(final JFrame frame) {


		JButton mods = findModButton(frame);
		mods.setPreferredSize(new Dimension(200,30));
		
		final JButton ssmeConfig = new JButton("SSME...");
		ssmeConfig.setFont(mods.getFont());
		ssmeConfig.setIcon(mods.getIcon());
		ssmeConfig.setBackground(mods.getForeground());
		ssmeConfig.setForeground(mods.getForeground());
		ssmeConfig.setContentAreaFilled(mods.isContentAreaFilled());
		ssmeConfig.setPreferredSize(new Dimension(200,30));
		ssmeConfig.setBorder(mods.getBorder());
		ssmeConfig.setBorderPainted(mods.isBorderPainted());
		final Color selected = new Color(216, 240, 255, 255);
		final Color deselected = new Color(147, 203, 251, 255);
		ssmeConfig.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseEntered(MouseEvent e) {
				ssmeConfig.setForeground(selected);
			}

			@Override
			public void mouseExited(MouseEvent e) {
				ssmeConfig.setForeground(deselected);
			}
			
		});
		
		ssmeConfig.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				//TODO
				// pass in the L&F attributes (see above) obtained from the 'mods' button
				// so that the AuthorizationUI doesn't look so out of place.
				new AuthorizationUI(frame).setVisible(true);
			}
		});
		
		
		Container modsParent = mods.getParent();

		modsParent.add(ssmeConfig, new GridBagConstraints(0, 1, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 10, 0), 0, 0));
		
	}

	private static JButton findModButton(Container c) {

		Component [] components = c.getComponents();
		for (Component component : components) {

			if(component instanceof JButton) {
				JButton button = (JButton)component;
				if(button.getText().equals("Mods...")) {
					return button;
				}
			}
			else {			
				if(component instanceof Container) {
					JButton value = findModButton((Container)component);
					if(value!=null) return value;
				}
			}
		}
		return null;
	}
	
	/**
	 * Application entry point for users that want to attach a debugger.
	 * @throws InterruptedException 
	 * @throws ExecutionException 
	 * @throws SigarException 
	 */
	public static void main(String[] args) throws ClassNotFoundException, NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, ClassAlreadyLoadedException, IOException, InterruptedException, ExecutionException {

		Installer.install();
		
		boolean writeClasses = false;
		if(args.length>0 && args[0].equals("writeClasses")) {
			writeClasses = true;
		}
		
		TransformationManager cc = TransformationManager.getInstance();

		ClassLoader cl = StarsectorModExpander.class.getClassLoader();
		
		if(writeClasses) {
			Sanitizer s = new Sanitizer(cc, writeClasses, "starfarer_obf.jar", "fs.common_obf.jar", "fs.sound_obf.jar").apply();
			cl = new SanitizedClassLoader(cc, cl);
		}
		
		doLauncherPreferencesTransformation(cc);

		Class<?> c = cl.loadClass("com.fs.starfarer.StarfarerLauncher");
			
		
		Method main = c.getMethod("main", String[].class);
		main.invoke(null, new Object[]{new String[0]});
	}

}
