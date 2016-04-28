package org.tjj.starsector.ssme.javassist;

import java.util.HashSet;

import javassist.CannotCompileException;
import javassist.CtConstructor;
import javassist.NotFoundException;
import javassist.expr.NewExpr;

public class ConstructorFinder extends BehaviorFinder<CtConstructor> {

	public ConstructorFinder(MethodPrototype target) {
		super(target);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void edit(NewExpr expr) throws CannotCompileException {
		try {
			CtConstructor constructor = expr.getConstructor();
		
			if(JavassistUtils.compare(constructor, target)) {
				if(matches==null) {
					matches = new HashSet<>();
				}
				matches.add(constructor);
			}
		} catch (NotFoundException e) {
		}		
	}

	
}
