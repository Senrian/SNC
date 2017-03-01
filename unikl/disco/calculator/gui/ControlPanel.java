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
 *
 */
package unikl.disco.calculator.gui;

import java.awt.FlowLayout;
import java.awt.GridBagLayout;

import javax.swing.JButton;
import javax.swing.JPanel;

/**
 * The main control panel for the GUI. Note that this class is only for
 * displaying, the corresponding button actions are defined in
 * {@link ControlPanelActions}.
 *
 * @author Sebastian Henningsen
 */
public class ControlPanel {

    private final JPanel controllerPanel;
    private final JButton addVertexButton;
    private final JButton removeVertexButton;
    private final JButton addFlowButton;
    private final JButton removeFlowButton;
    private final JButton analyzeButton;
    private final JButton optimizeButton;
    private final JButton subtractButton;
    private final JButton convoluteButton;

    /**
     * Constructs a new panel.
     */
    public ControlPanel() {
        controllerPanel = new JPanel();
        
        FlowLayout layout = new FlowLayout();
		controllerPanel.setLayout(layout);

        addVertexButton = new JButton();
        addVertexButton.setAction(new ControlPanelActions.AddNodeAction("Add Node"));
        controllerPanel.add(addVertexButton);

        addFlowButton = new JButton();
        addFlowButton.setAction(new ControlPanelActions.AddFlowAction("Add Flow"));
        controllerPanel.add(addFlowButton);

        removeVertexButton = new JButton();
        removeVertexButton.setAction(new ControlPanelActions.RemoveNodeAction("Remove Vertex"));
        controllerPanel.add(removeVertexButton);

        removeFlowButton = new JButton();
        removeFlowButton.setAction(new ControlPanelActions.RemoveFlowAction("Remove Flow"));
        controllerPanel.add(removeFlowButton);

        analyzeButton = new JButton();
        analyzeButton.setAction(new ControlPanelActions.AnalyzeNetworkAction("Analyze Network"));
        controllerPanel.add(analyzeButton);

        optimizeButton = new JButton();
        optimizeButton.setAction(new ControlPanelActions.OptimizationAction("Optimize Bound"));
        controllerPanel.add(optimizeButton);

        subtractButton = new JButton();
        subtractButton.setAction(new ControlPanelActions.SubtractAction("Subtract Flow"));
        controllerPanel.add(subtractButton);
        
        convoluteButton = new JButton();
        convoluteButton.setAction(new ControlPanelActions.ConvoluteAction("Convolute Vertices"));
        controllerPanel.add(convoluteButton);
        
        
        
    }

    /**
     * Returns the {@link JPanel} on which everything is displayed.
     *
     * @return
     */
    public JPanel getPanel() {
        return controllerPanel;
    }
}
