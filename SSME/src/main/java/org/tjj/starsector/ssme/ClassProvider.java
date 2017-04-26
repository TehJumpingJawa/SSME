package org.tjj.starsector.ssme;

import java.io.InputStream;

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
	 * return the discovered names of obfuscated types. 
	 * @return
	 */
	public ObfuscationMap getObfuscationMap();
	
	/**
	 * returns whether or not the specified classname exists within the scope of this ClassProvider.
	 * 
	 * @param classname
	 * @return
	 */
	public boolean exists(String classname);
	
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
