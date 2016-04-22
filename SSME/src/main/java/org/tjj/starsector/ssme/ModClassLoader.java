package org.tjj.starsector.ssme;

import java.net.URL;
import java.net.URLClassLoader;
/**
 * SSME transformation mods are loaded inside this ClassLoader
 * This is done so that mods don't instrument upon their own code (or that of other mods) 
 * 
 * @author TehJumpingJawa
 *
 */
public class ModClassLoader extends URLClassLoader {

	ModClassLoader(URL [] urls) {
		super(urls);
	}

	ModClassLoader(URL [] urls, ClassLoader parent) {
		super(urls, parent);
	}

	void addTransformationJar(URL url) {
		addURL(url);
	}

}
