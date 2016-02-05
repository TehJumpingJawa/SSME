package org.tjj.starsector.ssme.sanitizer;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;

public class PackageMapping {
	HashMap<String, PackageMapping> subpackages = new HashMap<>();
	
	private final String newName;
	private final boolean isDeobfuscated;
	
	public PackageMapping(String newName, boolean isDeobfuscated) {
		this.newName = newName;
		this.isDeobfuscated = isDeobfuscated;
	}
	
	public boolean isDeobfuscated() {
		return isDeobfuscated;
	}
	
	public String getNewName() {
		return newName;
	}

	public StringBuilder toString(StringBuilder sb, int indent) {
		Set<Entry<String,PackageMapping>> entries = subpackages.entrySet();
		
		for (Entry<String, PackageMapping> entry : entries) {
			for(int i = 0;i < indent;i++) {
				sb.append('\t');
			}

			final String oldName = entry.getKey();
			final String newName = entry.getValue().newName;

			sb.append(oldName);
			if(!oldName.equals(newName)) {
				sb.append("->").append(newName);
			}
			
			sb.append('\n');
			entry.getValue().toString(sb,indent+1);
		}
		
		return sb;
	}
	
	public String toString() {
		return this.toString(new StringBuilder(), 0).toString();
	}
	
	
}