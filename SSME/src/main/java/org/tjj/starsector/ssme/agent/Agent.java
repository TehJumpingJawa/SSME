package org.tjj.starsector.ssme.agent;

import java.lang.instrument.Instrumentation;

import org.tjj.starsector.ssme.TransformerProxy;
/**
 * The java agent to intercept all class loading.
 * @author TehJumpingJawa
 *
 */
public class Agent {

	private Agent() {
	}
	
	public static void agentmain(String agentArgs, Instrumentation inst) {
		inst.addTransformer(TransformerProxy.getInstance(inst), false);
	}
	
	public static void premain(String agentArgs, Instrumentation inst) {
		inst.addTransformer(TransformerProxy.getInstance(inst), false);
	}

}
