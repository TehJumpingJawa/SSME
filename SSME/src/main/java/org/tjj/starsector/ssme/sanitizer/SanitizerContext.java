package org.tjj.starsector.ssme.sanitizer;

import java.io.IOException;

import org.objectweb.asm.tree.ClassNode;
import org.tjj.starsector.ssme.ObfuscationMap;

public interface SanitizerContext {

	/**
	 * Load the specified ClassNode
	 * @param name
	 * @return
	 */
	public ClassNode load(String classname) throws ClassNotFoundException, IOException;
	
	/**
	 * Retrieve (or construct) a ClassMapping for the specified class. 
	 * @param name
	 * @return
	 */
	public ClassMapping get(String classname);
	
	/**
	 * return then increment the interfaceMethodCount.
	 * 
	 * @return
	 */
	public int incrementInterfaceMethodCount();
	/**
	 * 
	 * @return
	 */
	public int incrementInterfaceCount();
	
	/**
	 * 
	 * @return
	 */
	public int incrementEnumCount();
	
	/**
	 * 
	 * @return
	 */
	public int incrementClassCount();

	/**
	 * 
	 * @param classname
	 * @return
	 */
	public boolean inWorkingSet(String classname);
	
	/**
	 * 
	 * @param lowerCase
	 */
	public boolean registerOutputName(String lowerCase);

	/**
	 * 
	 * @param proposedNewName
	 * @return
	 */
	public ObfuscationMap getObfuscationMap();

	/**
	 * 
	 * @param packagePath
	 * @param deobfuscate
	 * @return
	 */
	public String recordPackageElements(String packagePath, String deobfuscatedPath);
	/**
	 * 
	 * @return
	 */
	public int incrementPackageCount();
}
