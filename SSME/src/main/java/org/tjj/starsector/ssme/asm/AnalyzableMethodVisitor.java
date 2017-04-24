package org.tjj.starsector.ssme.asm;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

/**
 * Method Visitor for interacting with a LiteralAnalyzingAdapter 
 * 
 * @author TehJumpingJawa
 *
 */
public abstract class AnalyzableMethodVisitor extends MethodVisitor {

	private LiteralAnalyzingAdapter analyzer;

	public AnalyzableMethodVisitor(int api, MethodVisitor mv) {
		super(api, mv);
		// TODO Auto-generated constructor stub
	}
	
	public void setAnalyzer(LiteralAnalyzingAdapter analyzer) {
		this.analyzer = analyzer;
	}

	
	/**
	 * Same function as {@link #getMethodArgumentInfo(Type[], int)}, but retrieves just a single argument value (as indicated by argIndex)
	 * 
	 * 
	 * @param argTypes The argument types of the Method occupying the top of the stack
	 * @param argIndex index of the argument value to return
	 * @return
	 */
	protected StackElement getMethodArgumentInfo(Type [] argTypes, int argIndex) {
		assert argIndex>=0 && argIndex<argTypes.length;
		
		int stackOffset = analyzer.stack.size();
		
		for(int i = argTypes.length-1;i>=argIndex;i--) {
			int argSize = argTypes[i].getSize();
			stackOffset-=argSize;
		}
		
		return analyzer.stack.get(stackOffset);
	}
	
	/**
	 * Uses the provided method argument types to retrieve the value of the argument (if known) from the current stack state.
	 * 
	 * Should only be called from within subclass implementations of {@link #visitMethodInsn(int, String, String, String, boolean)} 
	 * 
	 * @param argTypes The argument types of the Method occupying the top of the stack.
	 * @return The values of the method arguments, retrieved from the current stack state.
	 */
	protected StackElement[] getMethodArgumentInfos(Type [] argTypes) {
		StackElement [] parameterLiterals = new StackElement[argTypes.length];
		
		int stackOffset = analyzer.stack.size();
		
		for(int i = argTypes.length-1;i>=0;i--) {
			int argSize = argTypes[i].getSize();
			stackOffset-=argSize;
			StackElement v = analyzer.stack.get(stackOffset);
			parameterLiterals[i] = v;
		}
		
		return parameterLiterals;
	}
}
