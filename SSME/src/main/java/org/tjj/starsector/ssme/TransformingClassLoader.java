package org.tjj.starsector.ssme;

import org.tjj.starsector.ssme.Utils.BinaryClassName;


/**
 * 
 * ClassLoader fed by the TransformationManager.
 * 
 * @author TehJumpingJawa
 *
 */
final class TransformingClassLoader extends ClassLoader {

	private TransformationManager cp;
	
	TransformingClassLoader(TransformationManager cp, ClassLoader parent) {
		super(parent);
		this.cp = cp;
	}

	@Override
	protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {

		Class<?> result = super.findLoadedClass(name);
		if(result!=null) {
			return result;
		}

		byte [] bytes;
		
		// if the class has already been loaded, or there are no transformed bytes for the given class
		// then defer it to the super implementation (which in turn defers to the parent)
		if(TransformerProxy.getInstance(null).isLoaded(Utils.BinaryClassName.toInternalName(name)) || (bytes = cp.getTransformedClass(name))==null) {
//			System.out.println("defering class " + name);
			result = super.loadClass(name, resolve);
		}
		else {
//			System.out.println("defining class " + name);
			result = defineClass(name, bytes, 0, bytes.length);
		}
		if(resolve) {
			resolveClass(result);
		}
		return result;
	}
}
