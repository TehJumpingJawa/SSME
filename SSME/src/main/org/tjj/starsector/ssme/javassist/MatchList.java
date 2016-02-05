package org.tjj.starsector.ssme.javassist;

import java.util.ArrayList;
import java.util.Collection;

import javassist.NotFoundException;

@SuppressWarnings("serial")
public class MatchList<T> extends ArrayList<T> {

	public MatchList() {
		super();
		// TODO Auto-generated constructor stub
	}

	public MatchList(Collection<? extends T> c) {
		super(c);
		// TODO Auto-generated constructor stub
	}

	public MatchList(int initialCapacity) {
		super(initialCapacity);
		// TODO Auto-generated constructor stub
	}

	public T getMatch() throws NotFoundException {
		if(this.size()!=1) throw new NotFoundException("Expected 1 result; found " + this.size());
		return get(0);
	}

}
