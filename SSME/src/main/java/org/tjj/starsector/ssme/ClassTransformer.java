package org.tjj.starsector.ssme;

/**
 * Interface for Classes to perform ClassTransformation.
 * 
 * Transformers have 2 opportunities to perform transformations.
 * 1) Before the game launches (after the Launcher has closed, but before any of the game's classes have been instanciated)
 * 2) Immediately before each class is loaded
 * 
 * Transformations performed in step 1) will be visible in step 2).
 * Likewise, transformations performed by other ClassTransformers will be visible.
 * 
 * The ordering in which transformations are performed is determined by the mod load order. (which is itself determined by the starsector launcher) 
 * 
 * @author TehJumpingJawa
 *
 */
public interface ClassTransformer {

	
	/**
	 * 
	 * Called immediately after class construction.
	 * 
	 * @param path The path in which this SSME mod is operating.
	 */
	public void init(String path);	
	
	/**
	 * 
	 * Called after the starsector launcher has completed, but before the game's main classes have begun loading.
	 * 
	 * @param provider Provider of the class bytes.
	 */
	public void doEarlyTransformations(ClassProvider provider);

	/**
	 * Called for each class when it is loaded by the class loader.
	 * 
	 * @param classname Name of the class to transform.
	 * @param classBytes bytes of the class to transform. *This input byte[] must not be modified*
	 * @return The transformed classBytes, or null if no transformation was performed.
	 */
	public byte[] doLateTransformation(String classname, byte[] classBytes);
		
		
}
