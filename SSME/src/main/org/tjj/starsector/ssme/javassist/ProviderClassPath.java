package org.tjj.starsector.ssme.javassist;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import javassist.ClassPath;
import javassist.NotFoundException;

import org.tjj.starsector.ssme.ClassProvider;

public class ProviderClassPath implements ClassPath {

	/**
	 * return value for find(String) if the requested class exists.
	 * (either in-memory, or in the system classloader's classpath)
	 */
	private URL bolox;
	
	ClassProvider cp;
	
	public ProviderClassPath(ClassProvider cp)  {
		this.cp = cp;
		try {
			bolox = new URL("file://bolox");
		} catch (MalformedURLException e) {
			throw new RuntimeException("This shouldn't ever fail!", e);
		}
	}

	@Override
	public InputStream openClassfile(String classname) throws NotFoundException {
		try {
			return cp.getClass(classname);
		} catch (ClassNotFoundException e) {
			throw new NotFoundException("Unable to find " + classname, e);
		}
	}

	@Override
	public URL find(String classname) {
		
		if(cp.exists(classname)) {
			// Javassist doesn't actually use the returned URL for anything (it uses 'openClassfile' for that)
			// so we can safely return bolox.
			return bolox;
		}
		return null;
	}

	@Override
	public void close() {
		// TODO Auto-generated method stub
		
	}

}
