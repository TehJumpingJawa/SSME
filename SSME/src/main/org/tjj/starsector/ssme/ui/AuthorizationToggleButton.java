package org.tjj.starsector.ssme.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JToggleButton;

import org.tjj.starsector.ssme.AuthorizationManager.Authorization;

@SuppressWarnings("serial")
public class AuthorizationToggleButton extends JToggleButton {

	private Authorization state;
	
	public AuthorizationToggleButton(Authorization initialState) {
		this.state = initialState;
		
		setText(state.toString());
		setSelected(initialState==Authorization.GRANTED);
		
		
		addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				if(state==Authorization.GRANTED) {
					state = Authorization.DENIED;
				}
				else {
					state = Authorization.GRANTED;
				}
				setText(state.toString());
			}
		});
	}
	
	public Authorization getState() {
		return state;
	}
}
