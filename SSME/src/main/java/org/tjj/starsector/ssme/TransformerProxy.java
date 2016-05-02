package org.tjj.starsector.ssme;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.tjj.starsector.ssme.Utils.InternalClassName;
/**
 * Handles communication between application code, and instrumentation api.
 * 
 * @author TehJumpingJawa
 *
 */
public class TransformerProxy implements ClassFileTransformer {

	/**
	 * retrieve the TransformerProxy instance.
	 * 1st call will use the Instrumentation parameter to initialise the instance.
	 * Subsequent calls will ignore it.
	 * 
	 * @param inst
	 * @return
	 */
	public static synchronized TransformerProxy getInstance(Instrumentation inst) {
		if(instance==null) {
			instance = new TransformerProxy(inst);
		}
		return instance;
	}
	
	private static TransformerProxy instance;
	// list of loaded classes.
	private HashSet<String> loadedClasses = new HashSet<>();
	// list of registered transformers
	private List<ClassTransformer> transformers = new ArrayList<>();
	private Instrumentation inst;
	
	private TransformerProxy(Instrumentation inst) {
		this.inst = inst;
	}

	/**
	 * 
	 * 
	 * @param className *internal* name of class ('/' separated)
	 * @return
	 */
	boolean isLoaded(String className) {
		return loadedClasses.contains(className);
	}
	
	
	/**
	 * registers a Transformer so that it receives transform(...) events when classloading occurs.
	 * @param ct
	 */
	void addTransformer(ClassTransformer ct) {
		transformers.add(ct);
	}
	
	@Override
	public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, final byte[] classfileBuffer) throws IllegalClassFormatException {

		loadedClasses.add(className);
		
		TransformationManager cc = TransformationManager.getInstance();

		if(loader==cc.getModClassLoader() || className.startsWith("org/tjj/starsector/ssme")) {
			return null;
		}
		
		String binaryName = Utils.InternalClassName.toBinaryName(className);
		byte[] newClass = cc.getTransformedClass(binaryName);
		if(newClass==null) {
			newClass = classfileBuffer;
		}
		for (ClassTransformer transformer : transformers) {
			final byte [] transformedBytes = transformer.doLateTransformation(binaryName, newClass);
			if(transformedBytes!=null) {
//				System.out.println("transformed " + className + " from "  + loader);
				newClass = transformedBytes;
				cc.storeTransformedClass(binaryName, newClass);
			}
		}
		
		if(newClass!=classfileBuffer) {
			return newClass;
		}
		else {
			return null;
		}
	}

}
