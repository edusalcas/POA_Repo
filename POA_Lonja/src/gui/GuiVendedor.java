package gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import agents.seller.SellerAgent;

@SuppressWarnings("serial")
public class GuiVendedor extends JFrame {
	
	private SellerAgent myAgent;

	private JTextField titleField, cantidadField;

	public GuiVendedor(SellerAgent sellerAgent) {
		super(sellerAgent.getLocalName());
		
		myAgent = sellerAgent;
		
		JPanel p = new JPanel();
		p.setLayout(new GridLayout(2, 2));
		p.add(new JLabel("Nombre de mercancia:"));
		titleField = new JTextField(15);
		p.add(titleField);
		p.add(new JLabel("Cantidad:"));
		cantidadField = new JTextField(15);
		p.add(cantidadField);
		getContentPane().add(p, BorderLayout.CENTER);
		
		JButton addButton = new JButton("Añadir");
		addButton.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent ev) {
				try {
					String title = titleField.getText().trim();
					String cantidad = cantidadField.getText().trim();
					myAgent.nuevaMercancia(title, Float.parseFloat(cantidad));
					titleField.setText("");
					cantidadField.setText("");
				}
				catch (Exception e) {
					JOptionPane.showMessageDialog(GuiVendedor.this, "Invalid values. "+e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE); 
				}
			}
		} );
		p = new JPanel();
		p.add(addButton);
		getContentPane().add(p, BorderLayout.SOUTH);
		
		// Make the agent terminate when the user closes 
		// the GUI using the button on the upper right corner	
		addWindowListener(new	WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				myAgent.doDelete();
			}
		} );
		
		setResizable(false);
	}

	public void showGui() {
		pack();
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int centerX = (int) screenSize.getWidth() / 2;
		int centerY = (int) screenSize.getHeight() / 2;
		setLocation(centerX - getWidth() / 2, centerY - getHeight() / 2);
		super.setVisible(true);
	}
}
