package org.tjj.starsector.ssme;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.json.JSONWriter;

public final class AuthorizationManager {

	private static AuthorizationManager instance;
	
	public synchronized static AuthorizationManager getInstance() {
		if(instance==null) instance = new AuthorizationManager();
		return instance;
	}

	private HashMap<String, Authorization> auth;
	
	private static final String AUTH_FILENAME = "ssmeAuth.json";
	
	public static enum Authorization {
		DENIED("Denied"), GRANTED("Granted");
		private String name;
		Authorization(String name) {
			this.name = name;
		}
		
		public String toString() {
			return name;
		}
	}	
	
	
	private AuthorizationManager() {
		readAuthList();
	}
	
	private File getAuthFile() {
		String modFolder = System.getProperty("com.fs.starfarer.settings.paths.mods");
		
		if(modFolder==null) throw new NullPointerException("com.fs.starfarer.settings.paths.mods is not set!");
		
		// temporary until I start using version control, so that my ssmeAuth.json isn't distributed when I zip the mod/SSME folder.
		String ssmeFolder = ""; //"SSME";
		
		//TODO assuming SSME is running in mod/SSME is less than ideal...
		File f = new File(new File(modFolder, ssmeFolder), AUTH_FILENAME);
		return f;
	}

	private void readAuthList() {
		
		
		auth = new HashMap<>();
		
		File f = getAuthFile();
		
		if(f.exists()) {
			boolean failed = false;
			try (FileReader reader = new FileReader(f)) {
				JSONObject authList = new JSONObject(new JSONTokener(reader));
				
				for (Iterator<?> iterator = authList.keys(); iterator.hasNext();) {
					String key = (String) iterator.next();
					auth.put(key, Authorization.valueOf(authList.getString(key)));
				}
				
			} catch (JSONException | IOException e) {
				//fail silently if any error occurs when reading the auth list.
				//losing the authlist isn't critical, we'll just default to no authorizations.
				e.printStackTrace();
				failed = true;
			}
			if(failed) {
				//something went wrong, so overwrite what's there with what was successfully read.
				writeAuthList();				
			}
		}
	}

	
	/**
	 * Gets the authorization status of the specified mod.
	 * 
	 * @param id Mod id.
	 * @return Authorization.GRANTED, Authorization.DENIED, or null.
	 */
	public Authorization getAuthorization(String id) {
		return auth.get(id);
		
	}
	
	/**
	 * Returns a Set containing all of the Authorization statuses.
	 * Note the returned Entry Set is unmodifiable; Modifications must be made through the setAuthorization(....) methods. 
	 * 
	 * @return
	 */
	public Set<Entry<String, Authorization>> getAuthorizations() {
		return Collections.unmodifiableMap(auth).entrySet();
	}
	
	/**
	 * Set the authorization for a mod specified by its id.
	 * Note changes to the authorization list are written to file immediately.
	 * 
	 * @param id id of Mod
	 * @param val Authorization state.
	 */
	public void setAuthorization(String id, Authorization val) {
		if(val==null) {
			if(auth.remove(id)!=null) {
				writeAuthList();
			}
		}
		else if(auth.put(id, val)!=val) {
			// immediately save changes.
			writeAuthList();			
		}
	}
	
	/**
	 * Reset all authorization to specified values.
	 * 
	 * @param ids
	 * @param val
	 */
	public void replaceAuthorizations(String [] ids, Authorization [] val) {
		auth.clear();
		int minLength = Math.min(ids.length, val.length);
		for(int i = 0;i< minLength;i++) {
			if(val[i]!=null) {
				auth.put(ids[i],val[i]);
			}
		}
		writeAuthList();
	}
	
	
	private void writeAuthList() {

		File f = getAuthFile();
		
		try (FileWriter fileWriter = new FileWriter(f)) {
			JSONWriter writer = new JSONWriter(fileWriter);
			writer.object();
			for (Map.Entry<String, Authorization> modAuthorization : auth.entrySet()) {
				writer.key(modAuthorization.getKey()).value(modAuthorization.getValue().name());	
			}
			writer.endObject();
		} catch (IOException | JSONException e) {
			// non-critical failure.
			e.printStackTrace();
		}	
	}
	
}
