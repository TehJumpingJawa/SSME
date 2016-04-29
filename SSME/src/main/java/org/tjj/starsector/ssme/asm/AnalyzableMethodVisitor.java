package org.tjj.starsector.ssme.asm;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.AnalyzerAdapter;

public abstract class AnalyzableMethodVisitor extends MethodVisitor {

	protected LiteralAnalyzingAdapter analyzer;
	
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

}
