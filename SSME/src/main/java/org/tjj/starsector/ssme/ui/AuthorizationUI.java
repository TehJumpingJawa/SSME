package org.tjj.starsector.ssme.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;

import org.tjj.starsector.ssme.AuthorizationManager;
import org.tjj.starsector.ssme.AuthorizationManager.Authorization;

@SuppressWarnings("serial")
public class AuthorizationUI extends JDialog implements ActionListener {

	JPanel list;
	
	boolean empty;
	
	public AuthorizationUI(JFrame parent) {
		super(parent, true);
		setUndecorated(true);
		setBounds(parent.getBounds());
		getContentPane().setLayout(new BorderLayout(0, 0));
		
		JPanel panel = new JPanel();
		getContentPane().add(panel, BorderLayout.SOUTH);
		panel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
		
		JButton btnNewButton = new JButton("Ok");
		btnNewButton.addActionListener(this);
		panel.add(btnNewButton);
		
		JButton btnCancel = new JButton("Cancel");
		btnCancel.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				AuthorizationUI.this.dispose();
			}
		});
		panel.add(btnCancel);
		
		list = new JPanel();
		list.setLayout(new GridLayout(0, 1));
		
		AuthorizationManager auth = AuthorizationManager.getInstance();
		
		Set<Entry<String, Authorization>> authorizations = auth.getAuthorizations();
		
		empty = authorizations.size()==0;
		
		if(empty) {
			JLabel label;
			list.add(label = new JLabel("<html>This dialog allows you to modify SSME authorizations.<br>However, as you've yet to grant or deny authorization to any mods, the list is empty.</html>"));
			label.setHorizontalAlignment(JLabel.CENTER);
		}
		else {
			for (Entry<String, Authorization> entry : authorizations) {
				list.add(makeListEntry(entry.getKey(), entry.getValue()));
			}
		}
		JScrollPane scrollArea = new JScrollPane(list);
		getContentPane().add(scrollArea, BorderLayout.CENTER);
		
		JLabel lblSsmeAuthorizationManager = new JLabel("SSME Authorization Manager");
		lblSsmeAuthorizationManager.setHorizontalAlignment(SwingConstants.CENTER);
		getContentPane().add(lblSsmeAuthorizationManager, BorderLayout.NORTH);
		
	}

	private JPanel makeListEntry(String key, Authorization value) {
		final JPanel p = new JPanel();
		JLabel label = new JLabel(key);
		p.add(label);
		AuthorizationToggleButton toggle = new AuthorizationToggleButton(value);
		p.add(toggle);
		JButton clear = new JButton("Clear");
		clear.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				Container c = p.getParent();
				c.remove(p);
				c.repaint();
			}
		});
		p.add(clear);
		return p;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if(!empty) {
			AuthorizationManager authManager = AuthorizationManager.getInstance();
			
			Component[] listElements = list.getComponents();
			
			String [] ids = new String[listElements.length];
			Authorization [] auths = new Authorization[listElements.length];
	
			for (int i = 0; i < auths.length; i++) {
				Container c = (Container)listElements[i];
				String id = ((JLabel)c.getComponent(0)).getText();
				Authorization auth = ((AuthorizationToggleButton)c.getComponent(1)).getState();
				ids[i] = id;
				auths[i] = auth;
			}
			
			authManager.replaceAuthorizations(ids, auths);
		}
		dispose();
	}

}
