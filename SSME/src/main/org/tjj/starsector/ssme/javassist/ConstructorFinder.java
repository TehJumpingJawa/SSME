package org.tjj.starsector.ssme.javassist;

import java.util.ArrayList;

import javassist.CannotCompileException;
import javassist.CtConstructor;
import javassist.NotFoundException;
import javassist.expr.NewExpr;

import org.tjj.starsector.ssme.Utils;

public class ConstructorFinder extends BehaviorFinder<CtConstructor> {

	public ConstructorFinder(MethodPrototype target) {
		super(target);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void edit(NewExpr expr) throws CannotCompileException {
		try {
			CtConstructor constructor = expr.getConstructor();
		
			if(Utils.compare(constructor, target)) {
				if(matches==null) {
					matches = new ArrayList<>();
				}
				matches.add(constructor);
			}
		} catch (NotFoundException e) {
		}		
	}

	
}
