package org.tjj.starsector.ssme.asm;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

public abstract class AnalyzableMethodVisitor extends MethodVisitor {

	private LiteralAnalyzingAdapter analyzer;
	
	public AnalyzableMethodVisitor(int api) {
		super(api);
		// TODO Auto-generated constructor stub
	}

	public AnalyzableMethodVisitor(int api, MethodVisitor mv) {
		super(api, mv);
		// TODO Auto-generated constructor stub
	}
	
	public void setAnalyzer(LiteralAnalyzingAdapter analyzer) {
		this.analyzer = analyzer;
	}
	
	/**
	 * Uses the provided method descriptor to retrieve the argument literals from the current stack state.
	 * 
	 * Should only be called from within subclass implementations of {@link #visitMethodInsn(int, String, String, String, boolean)} 
	 * 
	 * @param argTypes method descriptor argument types.
	 * @return
	 */
	protected Object[] getMethodArgumentLiterals(Type [] argTypes) {
		Object [] parameterLiterals = new Object[argTypes.length];
		
		int stackOffset = analyzer.stack.size();
		
		for(int i = argTypes.length-1;i>=0;i--) {
			int argSize = argTypes[i].getSize();
			stackOffset-=argSize;
			Variable v = analyzer.stack.get(stackOffset);
			parameterLiterals[i] = v.getLiteral();
		}
		
		return parameterLiterals;
	}
}
