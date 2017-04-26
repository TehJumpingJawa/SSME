package org.tjj.starsector.ssme.installer;

import java.util.List;

import org.jutils.jprocesses.JProcesses;
import org.jutils.jprocesses.model.ProcessInfo;

public class Installer {
	private Installer() {
		
	}
	
	/**
	 * determine if SSME is enabled in the current VM (does it have its agent active, and intercepting class construction)
	 * @return
	 */
	public static boolean isInstalled() {
		return false;
	}
	
	/**
	 * Performs the necessary changes to the current Starsector installation to install SSME
	 * 
	 * @return Whether installation was successful
	 */
	public static boolean install() {

		String thisPid = ProcessID.getProcessId();

		
		List<ProcessInfo> processInfoList = JProcesses.get().fastMode().listProcesses();
		
		for (ProcessInfo processInfo : processInfoList) {
			if(processInfo.getPid().equals(thisPid)) {
				System.out.println("Commandline: " + processInfo.getCommand());
				break;
			}
		}			
		
		return false;
	}
	
	/**
	 * Removes SSME from the current Starsector installation.
	 * @return
	 */
	public static boolean uninstall() {
		return true;
	}
	
	
}
