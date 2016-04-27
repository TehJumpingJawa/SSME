package org.tjj.starsector.ssme.javassist;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;

import javassist.CtBehavior;
import javassist.NotFoundException;
import javassist.expr.ExprEditor;

public abstract class BehaviorFinder<T extends CtBehavior> extends ExprEditor {

	protected Set<T> matches;
	protected MethodPrototype target;
	
	public BehaviorFinder(MethodPrototype target) {
		this.target = target;
	}
	
	public T getMatch() throws NotFoundException {
		if(matches==null) throw new NotFoundException("No matches found");

		Iterator<T> iterator = matches.iterator();
		
	    T first = iterator.next();
	    if (!iterator.hasNext()) {
	      return first;
	    }

		throw new NotFoundException("Found too many matches (" + matches.size() +")");
	}
	
	public Set<T> getMatches() throws NotFoundException {
		if(matches==null) throw new NotFoundException("No matches found");
		return matches;
	}
}
