/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package report;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import org.gephi.graph.api.Edge;

import org.gephi.graph.api.Graph;
import org.gephi.graph.api.GraphController;
import org.gephi.graph.api.GraphModel;
import org.gephi.graph.api.Node;
import org.gephi.graph.api.NodeIterable;
import org.gephi.graph.api.NodeIterator;
import org.gephi.visualization.VizController;
import org.gephi.visualization.opengl.AbstractEngine;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.*;
import org.openide.util.NbBundle.Messages;

@ActionID(
        category = "Edit",
        id = "report.Report"
)
@ActionRegistration(
        displayName = "#CTL_Report"
)
@ActionReference(path = "Menu/Plugins", position = 3333)
@Messages("CTL_Report=Generate Report")
public final class Report implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            FileOutputStream output = new FileOutputStream("report.txt");


            GraphController graphController = Lookup.getDefault().lookup(GraphController.class);
            AbstractEngine engine = VizController.getInstance().getEngine();
            GraphModel model = graphController.getModel();
            if (model == null){
                return;
            }
            Graph graph = model.getGraphVisible();
            Node[] nodes = graph.getNodes().toArray();
            ArrayList<ArrayList<Node>> modularityClasses = new ArrayList<ArrayList<Node>>();
            int degrees[] = new int[nodes.length];
            int numMod = 0;
            for(Node n: nodes){
                int x = (Integer) n.getAttributes().getValue("Modularity Class");
                if(numMod < x)
                    numMod = x;

                if(x>= modularityClasses.size() || modularityClasses.get(x)==null){
                    int temp = modularityClasses.size();
                    while(temp<=x){
                        modularityClasses.add(temp, null);
                        temp++;
                    }
                    modularityClasses.set(x, new ArrayList<Node>());
                }
                modularityClasses.get(x).add(n);
            }
            numMod++;
            Edge[] edges = graph.getEdges().toArray();
            double[] max = new double[10];
            for(Edge edge:edges){
                double weight = edge.getWeight();
                int i = 0;
                while((i<10) && (max[i]>weight)){
                    i++;
                }
                if(i<10) {
                    int j = 9;
                    for(j = 8; j>=i; j--){
                       max[j+1] = max[j]; 
                    }
                    max[i] = weight;
                }
                degrees[edge.getSource().getId()-1]++;
                degrees[edge.getTarget().getId()-1]++;

            }
            for(ArrayList<Node> modClass: modularityClasses){
                if((modClass.size()==2) && (degrees[modClass.get(0).getId()-1] == 1) && (degrees[modClass.get(1).getId()-1] == 1)){
                    output.write(modClass.get(0).getNodeData().getLabel().getBytes());
                    output.write('-');
                    output.write((modClass.get(1).getNodeData().getLabel()+"\n ").getBytes());

                }
            }
            int maxDegree = 0;
            LinkedList<Node> maxDegreeNodes = new LinkedList<Node>();
            for(int i = 0; i<degrees.length; i++){
                if(degrees[i] == maxDegree){
                    maxDegreeNodes.add(graph.getNode(i+1));
                }
                if(degrees[i]>maxDegree){
                    maxDegreeNodes = new LinkedList<Node>();
                    maxDegree = degrees[i];
                    maxDegreeNodes.add(graph.getNode(i+1));
                }
            }


        } catch (FileNotFoundException ex) {
            Exceptions.printStackTrace(ex);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
    }
}
