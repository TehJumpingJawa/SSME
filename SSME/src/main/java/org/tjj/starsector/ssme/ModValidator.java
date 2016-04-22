package org.tjj.starsector.ssme;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.swing.JOptionPane;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.tjj.starsector.ssme.AuthorizationManager.Authorization;

public class ModValidator implements Runnable {

	private final Object launcher;
	private final ActionEvent parameter;
	private final List<?> modList;

	private class Profile implements Comparable<Profile> {
		
		String name;
		String path;
		String id;
		
		public final int priority;
		/**
		 * The jars to be added to the classpath for this mod.
		 */
		public final File [] jars;
		/**
		 * The main class to be invoked.
		 * (Note, it MUST implement the interface org.tjj.starsector.ssme.ClassTransformer & have a public no-args constructor.)
		 */
		public final String mainClass;
		
		public Profile(File [] jars, String mainClass, int priority) {
			this.jars = jars;
			this.mainClass = mainClass;
			this.priority = priority;
		}

		@Override
		public int compareTo(Profile o) {
			return o.priority-this.priority;
		}
	}
	
	public ModValidator(Object launcher, ActionEvent parameter, List<?> modList) {
		this.launcher = launcher;
		this.parameter = parameter;
		this.modList = modList;
	}


	
	@Override
	public void run() {
		try {
			checkMods(modList);
			
		} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException | IOException | JSONException | ClassNotFoundException | InstantiationException e) {
			e.printStackTrace();
			throw new RuntimeException("SSME failed " + e);
		}
		
		// Early transformations are no-longer permitted. 
		
		TransformationManager.getInstance().earlyTransformationsComplete();
		
		// now all mods have been processed, jump back to the starsector launcher.
		// note, this is executed on the event dispatch thread to ensure consistent behaviour with a non-SSME launch.
		EventQueue.invokeLater(
		new Runnable() {
			public void run() {
				try {
					Method actionPerformed = launcher.getClass().getDeclaredMethod("SSME_actionPerformed", java.awt.event.ActionEvent.class);
					actionPerformed.invoke(launcher, parameter);
				} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
					throw new RuntimeException("SSME failed" + e);
				}
			}
		});
	}

	/**
	 * utility method for reflectively retrieving the value of a field, ignoring its accessibility flags. 
	 * 
	 * @param object
	 * @param fieldName
	 * @return
	 * @throws NoSuchFieldException
	 * @throws SecurityException
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 */
	private static Object getFieldForcibly(Object object, String fieldName) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
		Field field = object.getClass().getDeclaredField(fieldName);
		field.setAccessible(true);
		return field.get(object);
	}
	
	private void checkMods(List<?> modList) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException, IOException, JSONException, ClassNotFoundException, InstantiationException {
		
		List<Profile> mods = new ArrayList<>();
		for (Object object : modList) {
			
			String path = (String)getFieldForcibly(object, "path");
			String name = (String)getFieldForcibly(object, "name");
			String id = (String)getFieldForcibly(object, "id");
			
			Profile p = readMod(name, id, path);
			if(p!=null) {
				mods.add(p);
			}
		}
		
		Collections.sort(mods);
		
		for (Profile profile : mods) {
			initMod(profile);
		}

	}

	
	private void initMod(Profile profile) throws MalformedURLException, ClassNotFoundException, InstantiationException, IllegalAccessException {
		TransformationManager cache = TransformationManager.getInstance();

		for (File jarFile : profile.jars) {
			cache.getModClassLoader().addTransformationJar(jarFile.toURI().toURL());
		}
		
		Class<?> transformerClass = cache.getModClassLoader().loadClass(profile.mainClass);
		
		ClassTransformer transformer = (ClassTransformer)transformerClass.newInstance();
		transformer.init(profile.path);
		
		TransformerProxy tp = TransformerProxy.getInstance(null);
		//register the transformer for late transformations
		tp.addTransformer(transformer);
		
		//and do the early transformations now.
		final long start = System.nanoTime();
		transformer.doEarlyTransformations(cache);
		final long end = System.nanoTime();
		System.out.println("SSME Mod \"" + profile.name + "\" early transformations complete in " + (end-start)/1000000 + "ms");
		
	}
	
	/**
	 * 
	 * Checks if the specified mod requires SSME injection.
	 * IF so, pops up the UI
	 * @param path
	 * @throws JSONException 
	 * @throws IOException 
	 * @throws ClassNotFoundException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 * @throws TransformationFailed 
	 */
	private Profile readMod(String name, String id, String path) throws IOException, JSONException, ClassNotFoundException, InstantiationException, IllegalAccessException {
		
		File iniFile = new File(path, "ssme.json");
		if(!iniFile.exists()) {
			return null;
		}
		else {
			
			AuthorizationManager auth = AuthorizationManager.getInstance();
			
			Authorization val = auth.getAuthorization(id);
			
			if(val==null) {
				final int YES_ALWAYS = JOptionPane.YES_OPTION;
				final int YES_ONCE = JOptionPane.NO_OPTION;
				final int NO = JOptionPane.CANCEL_OPTION;
				final int CANCEL = JOptionPane.CLOSED_OPTION;

				final int response = JOptionPane.showOptionDialog(null, "The mod \"" + name + "\" requires SSME code injection.\nAllowing this will give the mod the same access privileges as Starsector itself.\nONLY accept If you trust the mod author!", "SSME code injection request" , JOptionPane.YES_NO_CANCEL_OPTION,  JOptionPane.WARNING_MESSAGE, null, new String[] {"Yes, Always", "Yes, Once", "No, Never"}, "No");
				
				if(response==NO || response==CANCEL) {
					val = Authorization.DENIED;
				}
				else if (response==YES_ALWAYS) {
					val = Authorization.GRANTED;
				}
				auth.setAuthorization(id, val);
			}

			if(val==Authorization.DENIED) {
				return null;
			}
			Profile p = readProfile(iniFile);
			
			p.name = name;
			p.id = id;
			p.path = path;
			
			return p;
		}
	}
	
	private Profile readProfile(File iniFile) throws IOException, JSONException {
		
		try(FileReader reader = new FileReader(iniFile)) {
		
			JSONObject jsonObject = new JSONObject(new JSONTokener(reader));
	
			File [] jars = null;
			String mainClass = null;
			int priority = 0;
	
			for (Iterator<?> iterator = jsonObject.keys(); iterator.hasNext();) {
				String key = (String) iterator.next();
				switch (key) {
				case "jars":
					JSONArray array = jsonObject.getJSONArray(key);
					jars = new File[array.length()];
					for(int i = 0;i < jars.length;i++) {
						jars[i] = new File(iniFile.getParentFile(), array.getString(i));
					}
					break;
				case "classTransformer":
					mainClass = jsonObject.getString(key);
					break;
				case "priority":
					priority = jsonObject.getInt(key);
					break;
				default:
					throw new JSONException("Unrecognised key: " + key);
				}
			}
	
			if(jars==null) {
				throw new JSONException(iniFile +" missing \"jar\" attribute");
			}
			
			if(mainClass==null) {
				throw new JSONException(iniFile +" missing \"classTransformer\" attribute");
			}
			
			return new Profile(jars , mainClass, priority);
		}
	}
	
	
}
