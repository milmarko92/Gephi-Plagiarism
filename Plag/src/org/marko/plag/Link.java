/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.marko.plag;
import org.openide.util.Lookup;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import org.openide.NotifyDescriptor;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;
import org.gephi.graph.api.*;
import org.gephi.project.api.ProjectController;
import org.gephi.project.api.Workspace;
import org.gephi.project.api.WorkspaceInformation;
import org.gephi.visualization.VizController;
import org.gephi.visualization.apiimpl.ModelImpl;
import org.openide.DialogDisplayer;
import org.openide.util.Exceptions;
@ActionID(
    category="Edit",
    id="org.marko.plag.Link"
)
@ActionRegistration(
    displayName="#CTL_Link"
)
  @ActionReference(path="Menu/Plugins", position=3333)
@Messages("CTL_Link=See similarities...")
public final class Link implements ActionListener {
    @Override public void actionPerformed(ActionEvent e) {
        
        GraphController graphController = Lookup.getDefault().lookup(GraphController.class);
        org.gephi.visualization.opengl.AbstractEngine engine = VizController.getInstance().getEngine();
        GraphModel model = graphController.getModel();
            if (model == null){
                return;
            }
            Graph graph = model.getGraphVisible();
        ArrayList<Node> selectedNodes = new ArrayList<Node>();
        ModelImpl[] selectedNodeModels =
        engine.getSelectedObjects(org.gephi.visualization.opengl.AbstractEngine.CLASS_NODE);
        for (int i = 0; i < selectedNodeModels.length; i++) {
            Node node = ((NodeData) selectedNodeModels[i].getObj()).getNode(graph.getView().getViewId());
            if (node != null) {
                selectedNodes.add(node);
            }
        }
        String link = "";
        if (selectedNodes.size() == 2){
            Edge edge = graph.getEdge(selectedNodes.get(0), selectedNodes.get(1));
            try{
                EdgeData data = edge.getEdgeData();
                link = (String)data.getAttributes().getValue("link");
                link = link.replaceAll(" ", "%20");
                java.awt.Desktop.getDesktop().browse(new URI(link.replace('\\', '/')));
            } catch (IOException ex) {
                if(link.contains("file:///") && ex.getMessage().contains("cannot find the file")){
                    NotifyDescriptor d =new NotifyDescriptor.Message("The file has not been downloaded yet or has been deleted.", NotifyDescriptor.INFORMATION_MESSAGE);
                    DialogDisplayer.getDefault().notify(d);
                }
            } catch (URISyntaxException ex) {
                Exceptions.printStackTrace(ex);
            }catch (NullPointerException ex){
                NotifyDescriptor d =new NotifyDescriptor.Message("You need to select 2 nodes.", NotifyDescriptor.INFORMATION_MESSAGE);
                DialogDisplayer.getDefault().notify(d);
            }
        }
        
    }
}
