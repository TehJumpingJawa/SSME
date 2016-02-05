package org.tjj.starsector.ssme;

import java.io.Reader;

import org.json.JSONException;
import org.json.JSONTokener;

/**
 * Fixes the json tokenizer so it respects '#' comments,
 * so you don't need to strip them out of Starsector's data files before parsing them.
 * 
 * @author TehJumpingJawa
 *
 */
public class ImprovedJSONTokener extends JSONTokener {

	public ImprovedJSONTokener(Reader arg0) {
		super(arg0);
	}

	public ImprovedJSONTokener(String arg0) {
		super(arg0);
	}
	
	public char nextClean() throws JSONException {
		boolean inComment = false;
		for (;;) {
			char c = this.next();
			if (c == 0) {
				// eof
				return c;
			} else {
				if (inComment) {
					if (c == '\n') {
						inComment = false;
					}
				} else {
					if (c == '#') {
						inComment = true;
					} else if (c > ' ') {
						return c;
					}
				}
			}
		}
	}
}
