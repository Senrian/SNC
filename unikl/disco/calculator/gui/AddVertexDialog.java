 /*
 *  (c) 2013 Michael A. Beck, disco | Distributed Computer Systems Lab
 *                                  University of Kaiserslautern, Germany
 *         All Rights Reserved.
 *
 *  This software is work in progress and is released in the hope that it will
 *  be useful to the scientific community. It is provided "as is" without
 *  express or implied warranty, including but not limited to the correctness
 *  of the code or its suitability for any particular purpose.
 *
 *  You are free to use this software for any non-commercial educational or
 *  research purpose, provided that this copyright notice is not removed or
 *  modified. For commercial uses please contact the respective author(s).
 *
 *  If you find our software useful, we would appreciate if you mentioned it
 *  in any publication arising from the use of this software or acknowledge
 *  our work otherwise. We would also like to hear of any fixes or useful
 *  extensions to this software.
 *AnalyzeDialog21
 */
package unikl.disco.calculator.gui;

import java.awt.GridLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import unikl.disco.calculator.SNC;
import unikl.disco.calculator.commands.AddVertexCommand;
import unikl.disco.calculator.commands.Command;
import unikl.disco.calculator.symbolic_math.ServiceType;

/**
 *
 * @author Sebastian Henningsen
 */
public class AddVertexDialog {

    private JPanel panel;
    private JLabel alias;
    private JLabel service;
    private JLabel rate;
    private JTextField aliasField;
    private JTextField rateField;
    private JComboBox<ServiceType> serviceTypes;
    private GridLayout layout;

    public AddVertexDialog() {
        panel = new JPanel();
        
        alias = new JLabel("Alias of the vertex: ");
        service = new JLabel("Service Type: ");
        rate = new JLabel("Rate: ");
        aliasField = new JTextField();
        rateField = new JTextField(10);
       
        serviceTypes = new JComboBox<>(ServiceType.values());
        
        panel.add(alias);
        panel.add(aliasField);
        panel.add(service);
        panel.add(serviceTypes);
        panel.add(rate);
        panel.add(rateField);
        
        layout = new GridLayout(0, 1);
        panel.setLayout(layout);
    }

    public void display() {
        int result = JOptionPane.showConfirmDialog(null, panel, "Add Vertex",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            SNC snc = SNC.getInstance();
            double rate = Double.parseDouble(rateField.getText());
            Command cmd = new AddVertexCommand(aliasField.getText(), -1*rate, -1, snc);
            snc.invokeCommand(cmd);
            // Just for debugging
            //System.out.println(aliasField.getText());
        }
    }
}