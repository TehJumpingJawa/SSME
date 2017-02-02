package org.tjj.starsector.ssme;

import java.io.IOException;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.tjj.starsector.ssme.asm.discoverers.FieldTypeDiscoverer;
import org.tjj.starsector.ssme.asm.discoverers.InterfaceTypeDiscoverer;

/**
 * 
 * Class for keeping track of known Starsector class names; both unobfuscated names (that are assumed to be invariant),
 * and obfuscated class names that will almost certainly change between releases.
 * 
 * the obfuscated class names are determined by inferences performed during this class's construction. 
 * 
 * Note if discovery fails, 
 * 
 * @author TehJumpingJawa
 *
 */
public class StarsectorTypes {
	
	public final Type alignment = Type.getObjectType("com/fs/starfarer/api/ui/Alignment");

	public final Type glLauncher = Type.getObjectType("com/fs/starfarer/launcher/opengl/GLLauncher");
	public final String glLauncherClassName = "com.fs.starfarer.launcher.opengl.GLLauncher";

	public final Type string = Type.getObjectType("java/lang/String");
	
	/**
	 * type of the panel class.
	 */
	public final Type uiPanel;
	/**
	 * type of the component class.
	 */
	public final Type uiComponent;
	/**
	 * type of the action listener interface.
	 */
	public final Type uiActionListener;	
	
	StarsectorTypes(ClassProvider cc) {

		ClassReader cr;
		try {
			cr = new ClassReader(cc.getClass(glLauncherClassName));
		} catch (ClassNotFoundException | IOException e) {
			throw new RuntimeException("Starsector type discovery failed", e);
		}
		
		FieldTypeDiscoverer uiPanelTypeDiscoverer = new FieldTypeDiscoverer("panel");
		FieldTypeDiscoverer uiComponentTypeDiscoverer = new FieldTypeDiscoverer("launchButton", uiPanelTypeDiscoverer);
		InterfaceTypeDiscoverer uiActionListenerTypeDiscoverer = new InterfaceTypeDiscoverer(1, uiComponentTypeDiscoverer);
		
		cr.accept(uiActionListenerTypeDiscoverer, ClassReader.SKIP_FRAMES);
		
		uiPanel = uiPanelTypeDiscoverer.getFieldType();
		uiComponent = uiComponentTypeDiscoverer.getFieldType();
		uiActionListener = uiActionListenerTypeDiscoverer.getInterfaceType();
	}
}
