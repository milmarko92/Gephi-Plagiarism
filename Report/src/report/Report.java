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
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
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

    private String path;
    
    private JLabel selectedDegree = new JLabel();
    @Override
    public void actionPerformed(ActionEvent e) {
        FileWriter output=null;
            
        try {
            GraphController graphController = Lookup.getDefault().lookup(GraphController.class);
            AbstractEngine engine = VizController.getInstance().getEngine();
            GraphModel model = graphController.getModel();
            if (model == null){
                return;
            }
            Graph graph = model.getGraphVisible();
            int num = graph.getNodeCount();
            if(num==0) return;
            
            JButton browse = new JButton("Choose a Destination Folder");
            JLabel matchSliderTxt = new JLabel("Percentage of highest matches to show:");
            JSlider matchSlider = new JSlider();
            JLabel degreeSlideTxt = new JLabel("number of highest degree vertices to show:");
            JLabel msg = new JLabel("Enter the name of the output report file: ");
            
            JSlider degreeSlider = new JSlider(0, num);
            degreeSlider.addChangeListener(new ChangeListener() {
                @Override
                public void stateChanged(ChangeEvent e) {
                    JSlider slide = (JSlider) e.getSource();
                    selectedDegree.setText(""+(int)slide.getValue());
                }
            });
            
            
            path = System.getProperty("user.home")+"\\Desktop";
            browse.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    JFileChooser fileChooser = new JFileChooser(System.getProperty("user.home")+"\\Desktop");
                    fileChooser.setFileSelectionMode( JFileChooser.DIRECTORIES_ONLY);
                    int returnVal = fileChooser.showOpenDialog(null);
                    if(returnVal != JFileChooser.APPROVE_OPTION) {
                        path = System.getProperty("user.home")+"\\Desktop";
                        return;
                    }
                    if (fileChooser.getSelectedFile().getAbsolutePath()!=null){
                        path = fileChooser.getSelectedFile().getAbsolutePath();
                    }
                    else {
                        path = System.getProperty("user.home")+"\\Desktop";
                    }
                }
            });
            Object[] params = { browse, matchSliderTxt, matchSlider, degreeSlideTxt, degreeSlider, selectedDegree, msg};
            
            String fileName = (String)JOptionPane.showInputDialog(
                    new JFrame(),
                    params,
                    JOptionPane.OK_CANCEL_OPTION);
            
            if(fileName == null) return;
            int matchPercentage = (int)matchSlider.getValue();
            int degreeNumber = (int) degreeSlider.getValue();
            
            if(fileName.length()<5 || !fileName.substring(fileName.length()-4).equals(".txt")){
                fileName+=".txt";
            }
            output = new FileWriter(path+"\\"+fileName);
            

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
            
            output.write("*** Modularity Classes ***\n");
            output.write("Total number of modularity classes: "+numMod+"\n");
            int count = 0;
            for(ArrayList<Node> classNodes: modularityClasses){
                output.write(" Modularity Class #"+count+":\n");
                for(Node n: classNodes){
                    output.write("\t"+n.getNodeData().getLabel()+"\n");
                }
                count++;
            }
            
            Edge[] edges = graph.getEdges().toArray();
            int len;
            double[] max = new double[len = (graph.getEdgeCount()*matchPercentage)/100];
            Edge[] maxEdges = new Edge[len];
            for(Edge edge:edges){
                double weight = edge.getWeight();
                int i = 0;
                while((i<max.length) && (max[i]>weight)){
                    i++;
                }
                if(i<max.length) {
                    int j;
                    for(j = max.length-1; j>=i; j--){
                       max[j+1] = max[j];
                       maxEdges[j+1] = maxEdges[j];
                    }
                    max[i] = weight;
                    maxEdges[i] = edge;
                }
                degrees[edge.getSource().getId()-1]++;
                degrees[edge.getTarget().getId()-1]++;

            }
            output.write("\n*** Largest Similarity***\n");
            for(Edge edge:maxEdges){
                output.write(edge.getSource().getNodeData().getLabel() + " - "+edge.getTarget().getNodeData().getLabel() + ": "+edge.getWeight()+"\n");
            }
            output.write("\n*** Matched Pairs ***\n");
            for(ArrayList<Node> modClass: modularityClasses){
                if((modClass.size()==2) && (degrees[modClass.get(0).getId()-1] == 1) && (degrees[modClass.get(1).getId()-1] == 1)){
                    output.append(modClass.get(0).getNodeData().getLabel());
                    output.write('-');
                    output.write(modClass.get(1).getNodeData().getLabel()+"\n");

                }
            }
            
            output.write("\n");
            LinkedList<Node>[] maxDegreeNodes = new LinkedList[degreeNumber];
            for(int i = 0; i<degrees.length; i++){
                if(degrees[i] == 0) continue;
                int j = 0;
                while(j < degreeNumber && maxDegreeNodes[j] != null && degrees[i] < degrees[maxDegreeNodes[j].get(0).getId()-1]){
                    j++;
                }
                if(j >= degreeNumber) continue;
                if(maxDegreeNodes[j] == null){
                    maxDegreeNodes[j] = new LinkedList<Node>();
                    maxDegreeNodes[j].add(graph.getNode(i+1));
                }
                else if(degrees[i] == degrees[maxDegreeNodes[j].get(0).getId()-1]){
                    maxDegreeNodes[j].add(graph.getNode(i+1));
                }
                else {
                    for(int k = degreeNumber-1; k>j; k--){
                        maxDegreeNodes[k] = maxDegreeNodes[k-1];
                    }
                    maxDegreeNodes[j] = new LinkedList<Node>();
                    maxDegreeNodes[j].add(graph.getNode(i+1));
                }
            }
            int cnt = 0;
            output.write("*** Nodes with the highest Degree ***:\n");
            
            while(cnt<degreeNumber){
                for (LinkedList<Node> degNodes:maxDegreeNodes){
                    for(Node node: degNodes){
                        output.write(node.getNodeData().getLabel()+ " "+ degrees[node.getId()-1]+"\n");
                        cnt++;
                        if(cnt == degreeNumber){
                            return;
                        }
                    }
                }
            }

        } catch (FileNotFoundException ex) {
            Exceptions.printStackTrace(ex);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
        finally{
            try {
                if(output!=null)
                output.close();
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
    }
}
