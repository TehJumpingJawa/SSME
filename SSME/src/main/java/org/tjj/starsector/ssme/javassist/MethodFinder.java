package org.tjj.starsector.ssme.javassist;

import java.util.HashSet;

import org.tjj.starsector.ssme.Utils;

import javassist.CannotCompileException;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.expr.MethodCall;

public class MethodFinder extends BehaviorFinder<CtMethod> {

	public MethodFinder(MethodPrototype target) {
		super(target);
		// TODO Auto-generated constructor stub
	}
	

	@Override
	public void edit(MethodCall m) throws CannotCompileException {
		try {
			CtMethod method = m.getMethod();
		
			if(Utils.compare(method, target)) {
				if(matches==null) {
					matches = new HashSet<>();
				}
				matches.add(method);
			}
		} catch (NotFoundException e) {
		}
	}	

}
