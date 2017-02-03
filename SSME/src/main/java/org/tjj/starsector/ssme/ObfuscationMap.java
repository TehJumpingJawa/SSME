package org.tjj.starsector.ssme;

import java.io.IOException;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.tjj.starsector.ssme.asm.discoverers.FieldTypeDiscoverer;
import org.tjj.starsector.ssme.asm.discoverers.InterfaceTypeDiscoverer;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableBiMap.Builder;

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
public class ObfuscationMap {
	
	private BiMap <String, String> obfuscationMap;
	private BiMap <String, String> deobfuscationMap;
	
	ObfuscationMap(ClassProvider cc) {

		Builder<String, String> builder = ImmutableBiMap.builder();
		
		ClassReader cr;
		try {
			cr = new ClassReader(cc.getClass("com.fs.starfarer.launcher.opengl.GLLauncher"));
		} catch (ClassNotFoundException | IOException e) {
			throw new RuntimeException("Starsector type discovery failed", e);
		}
		
		FieldTypeDiscoverer uiPanelTypeDiscoverer = new FieldTypeDiscoverer("panel");
		FieldTypeDiscoverer uiComponentTypeDiscoverer = new FieldTypeDiscoverer("launchButton", uiPanelTypeDiscoverer);
		InterfaceTypeDiscoverer uiActionListenerTypeDiscoverer = new InterfaceTypeDiscoverer(1, uiComponentTypeDiscoverer);
		cr.accept(uiActionListenerTypeDiscoverer, ClassReader.SKIP_FRAMES);
		
		builder.put("com/fs/starfarer/ui/Panel", uiPanelTypeDiscoverer.getFieldType().getInternalName());
		builder.put("com/fs/starfarer/ui/Component", uiComponentTypeDiscoverer.getFieldType().getInternalName());
		builder.put("com/fs/starfarer/ui/ActionListener", uiActionListenerTypeDiscoverer.getInterfaceType().getInternalName());
		
		obfuscationMap = builder.build();
		deobfuscationMap = obfuscationMap.inverse();
	}
	
	/**
	 * Get the obfuscated internal classname for the supplied unobfuscated type.
	 * e.g. 
	 * "com/fs/starfarer/ui/Panel" will return something like:
	 * "com/fs/starfarer/ui/Oooo"
	 * 
	 * if no obfuscated name exists, the unobfuscated name will be returned.
	 * e.g.
	 * "com/fs/starfarer/launcher/opengl/GLLauncher" will return:
	 * "com/fs/starfarer/launcher/opengl/GLLauncher" (as it isn't an obfuscated type).
	 * 
	 * 
	 * @param unobfuscatedInternalType
	 * @return the obfuscated name
	 */
	public String obfuscate(String unobfuscatedInternalType) {
		String obfuscated = obfuscationMap.get(unobfuscatedInternalType);
		if(obfuscated==null) obfuscated = unobfuscatedInternalType;
		return obfuscated;
	}
	
	public Type obfuscateType(String unobfuscatedInternalType) {
		return Type.getObjectType(obfuscate(unobfuscatedInternalType));
	}
	
	/**
	 * Get the deobfuscated name for the supplied obfuscated type.
	 * 
	 * @param obfuscatedInternalType
	 * @return The deobfuscated name, or null if no deobfuscated name exists for the supplied obfuscated type.
	 */
	public String deobfuscate(String obfuscatedInternalType) {
		return deobfuscationMap.get(obfuscatedInternalType);
	}
}
