package org.tjj.starsector.ssme.installer;

import java.io.File;
import java.util.List;

import net.bytebuddy.agent.ByteBuddyAgent;

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
		// better way of doing it:
		// replace starfarer_obj.jar with my own implementation that simply
		// relaunches the VM.
		
		ByteBuddyAgent.attach(new File("C:/Users/TehJumpingJawa/git/SSME/SSME/target/Agent.jar"), ProcessID.getProcessId());
		//it works!
		
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
