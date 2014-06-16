package FileSieve.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

class changeScreenAction implements ActionListener {
	
	private ScreenSwitcher ss;
	public changeScreenAction(ScreenSwitcher getSs){
		ss = getSs;
	}
	
	public void actionPerformed(ActionEvent e){
		ss.itemStateChanged(e.getActionCommand());
    }
}