package org.tjj.starsector.ssme.javassist;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;

import org.tjj.starsector.ssme.ClassAlreadyLoadedException;
import org.tjj.starsector.ssme.ClassProvider;

public class BetterClassPool extends ClassPool {

	private ClassProvider provider;
	
	public BetterClassPool(ClassProvider provider) {
		this.provider = provider;
		appendClassPath(new ProviderClassPath(provider));
	}
	
	public ClassProvider getClassProvider() {
		return provider;
	}
	
	/**
	 * returns a list of all the CtClasses loaded by this pool that have outstanding modifications.
	 * @return
	 */
	private List<CtClass> getModified() {
		
		@SuppressWarnings("unchecked")
		Hashtable<String, CtClass> classes = this.classes;
		
		List<CtClass> list = new ArrayList<>();
		
		for (CtClass value : classes.values()) {
			if(value.isModified()) {
				list.add(value);
			}
		}
		
		return list;
		
	}
	
	/**
	 * Writes the changes of all modified classes to the underlying ClassProvider
	 * 
	 * @throws ClassAlreadyLoadedException
	 * @throws CannotCompileException
	 * @throws IOException
	 */
	public void saveModifications() throws ClassAlreadyLoadedException, CannotCompileException, IOException {
		List<CtClass> modified = getModified();
		for (CtClass ctClass : modified) {
			System.out.println("Saving changes to: " + ctClass.getName());
			provider.saveTransformation(ctClass.getName(), ctClass.toBytecode());
		}
	}

}
