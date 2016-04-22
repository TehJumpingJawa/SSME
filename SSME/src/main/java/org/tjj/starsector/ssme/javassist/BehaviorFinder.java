package org.tjj.starsector.ssme.javassist;

import java.util.ArrayList;

import javassist.CtBehavior;
import javassist.NotFoundException;
import javassist.expr.ExprEditor;

public abstract class BehaviorFinder<T extends CtBehavior> extends ExprEditor {

	protected ArrayList<T> matches;
	protected MethodPrototype target;
	
	public BehaviorFinder(MethodPrototype target) {
		this.target = target;
	}
	
	public T getMatch() throws NotFoundException {
		if(matches==null) throw new NotFoundException("No matches found");
		if(matches.size()!=1) throw new NotFoundException("Found too many matches (" + matches.size() +")");
		return matches.get(0);
	}
	
	public ArrayList<T> getMatches() throws NotFoundException {
		if(matches==null) throw new NotFoundException("No matches found");
		return matches;
	}
}
