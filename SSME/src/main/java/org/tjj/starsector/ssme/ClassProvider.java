package org.tjj.starsector.ssme;

import java.io.InputStream;
import java.util.Map;

public interface ClassProvider {

	/**
	 * 
	 * Obtains the bytes for the specified class.
	 * 
	 * @param classname  Fully qualified binary name of the class. ('.' separator, e.g. "java.lang.String")
	 * @return The bytes of the class
	 * @throws ClassNotFoundException if the requested class could not be found
	 */
	public InputStream getClass(String classname) throws ClassNotFoundException;
	
	/**
	 * Returns the obfuscation map shared by all mods.
	 * The obfuscation map holds mappings between mod-designated keys, and obfuscated class names.
	 * 
	 * Mods are free to add key -> obfuscated class name mappings to allow other mods to operate more efficiently.
	 * 
	 * @return
	 */
	public Map<String, String> getObfuscationMap();
	
	/**
	 * returns whether or not the specified classname exists within the scope of this ClassProvider.
	 * 
	 * @param classname
	 * @return
	 */
	public boolean exists(String classname);
	
	/**
	 * Most SSME mods shouldn't need this.
	 * 
	 * SSME sanitises Starsector's obfuscated class, method & field names so that Javassist's source compiler doesn't break when trying to compile code fragments with illegal identifier names.
	 * 
	 * This method allows you to discover this mapping.
	 * Note parameter & return value are binary class names ('.' separator)
	 * 
	 * @param classname The original obfuscated class name
	 * @return			The new sanitised class name. (or the parameter, if no mapping exists)
	 */
	public String getSanitisedName(String classname);
	
	/**
	 * Stores the provided bytes for the specified class.
	 * Note, may only be called during the earlyTransformations phase.
	 * 
	 * @param classname Fully qualified 'binary name' of the class.
	 * @param bytes		Bytes to store. (a copy will be taken to avoid future modifications)
	 * @throws ClassAlreadyLoadedException if the requested class has already been loaded by the class loader, it cannot be transformed.
	 * @throws ClassNotFoundException if the requested class could not be found
	 */
	public void saveTransformation(String classname, byte[] classBytes) throws ClassAlreadyLoadedException;
}
