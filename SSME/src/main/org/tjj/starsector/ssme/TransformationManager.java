package org.tjj.starsector.ssme;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.tjj.starsector.ssme.Utils.BinaryClassName;

/**
 * 
 * Stores the bytecode of all transformed classes.
 * Provides these bytes to the TransformerProxy when each class is loaded by the JVM. 
 * 
 * @author TehJumpingJawa
 *
 */
class TransformationManager implements ClassProvider {
	
	/**
	 * Once early transformations have been completed, the transformation pool becomes read only.
	 */
	private boolean readOnly = false;
	
	private HashMap<String, String> obfuscationMap = new HashMap<>();
	
	/**
	 * the deobfuscation map
	 */
	private Map<String, String> sanitisedMappings;
	
	/**
	 * the bytes of all transformed classes.
	 * (classes that have been modified at runtime) 
	 */
	private HashMap<String, byte[]> transformedClassData = new HashMap<>();
	
	/**
	 * The class loader that interacts with the above transformedClassData
	 */
	private TransformingClassLoader loader;
	/**
	 * The class loader into which SSME mods are loaded.
	 * This isolation prevents SSME mods from modify their own Transformer classes/libraries, or those of other SSME mods. 
	 */
	private ModClassLoader modLoader;
	
	private static TransformationManager instance;

	
	private TransformationManager() {
		
		String devCp = System.getProperty("org.tjj.starsector.ssme.dev.classpath");
		URL [] baseModCp;
		if(devCp==null) {
			baseModCp = new URL[0];
		}
		else {
			String [] cpElements = devCp.split(File.pathSeparator);
			baseModCp = new URL[cpElements.length];
			for (int i = 0; i < cpElements.length; i++) {
				try {
					baseModCp[i] = new File(cpElements[i]).toURI().toURL();
				} catch (MalformedURLException e) {
					e.printStackTrace();
					// fail silently on bad path elements.
				}
			}
		}
		
		modLoader = new ModClassLoader(baseModCp, StarsectorModExpander.class.getClassLoader());
		loader = new TransformingClassLoader(this, modLoader);
	}
	
	/**
	 * returns the ClassLoader that will utilize this transformation pool.
	 * 
	 * @return
	 */
	ClassLoader getClassLoader() {
		return loader;
	}
	
	/**
	 * returns the ClassLoader that should be used to load SSME mod code.
	 * 
	 * @return
	 */
	ModClassLoader getModClassLoader() {
		return modLoader;
	}
	
	void setSanitisedMappings(Map<String,String> mappings) {
		sanitisedMappings = mappings;
	}
	
	public static synchronized TransformationManager getInstance() {
		if(instance==null) {
			instance = new TransformationManager();
		}
		return instance;
	}
	
	void earlyTransformationsComplete() {
		readOnly = true;
	}
	
	/**
	 * Returns the bytes of a transformed class, or null if no transformations for the specified class have been recorded. 
	 * 
	 * @param classname
	 * @return
	 * @throws ClassNotFoundException
	 */
	byte[] getTransformedClass(String classname) {
		return transformedClassData.get(classname);
	}

	/**
	 * Returns the bytes for the specified classname.
	 * Will return transformed bytes if present in the cache, otherwise it will fall back on loading the class's bytes from the system classloader. 
	 * 
	 */
	@Override
	public InputStream getClass(String classname) throws ClassNotFoundException {
		final InputStream returnValue;

		byte[] b = transformedClassData.get(classname);

		if(b==null) {
			returnValue = loader.getResourceAsStream(BinaryClassName.toFilename(classname));
			if(returnValue==null) {
				throw new ClassNotFoundException(classname + " could not be found on the classpath");
			}
		}
		else {
			returnValue = new ByteArrayInputStream(b);
		}
		return returnValue;
	}

	@Override
	public boolean exists(String classname) {
		if(transformedClassData.containsKey(classname)) {
			return true;
		}
		if(loader.getResource(BinaryClassName.toFilename(classname))!=null) {
			return true;
		}
		return false;
	}	
	
	/**
	 * When late transformations are performed, we store the transformed class.
	 * This is so that mods further down the transformation order will see these changes if they interrogate a class
	 * that was modified by a lateTransformation earlier in the transformation order. 
	 * 
	 * @param classname
	 * @param bytes
	 */
	void storeTransformedClass(String classname, byte[] bytes) {
		transformedClassData.put(classname, bytes);
	}

	@Override
	public void saveTransformation(String classname, byte[] classBytes) throws ClassAlreadyLoadedException {
		if(readOnly) throw new IllegalStateException("Early transformations are complete, and no-longer available.");
		
		byte[] b = transformedClassData.get(classname);
		if(b!=null && TransformerProxy.getInstance(null).isLoaded(BinaryClassName.toInternalName(classname))) {
			throw new ClassAlreadyLoadedException("Cannot save transformations to " + classname +", it has already been loaded into the JVM.");
		}
		else {
			transformedClassData.put(classname, classBytes);
		}
	}

	@Override
	public Map<String, String> getObfuscationMap() {
		return obfuscationMap;
	}

	@Override
	public String getSanitisedName(String classname) {
		String newName = sanitisedMappings.get(classname);
		if(newName==null) {
			return classname;
		}
		return newName;
	}


}