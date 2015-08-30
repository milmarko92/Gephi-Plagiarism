/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package PlagFolder;

//import com.sun.org.glassfish.external.statistics.Statistic;
import com.sun.corba.se.spi.monitoring.StatisticsAccumulator;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JFileChooser;
import org.gephi.data.attributes.api.AttributeColumn;
import org.gephi.data.attributes.api.AttributeController;
import org.gephi.data.attributes.api.AttributeModel;
import org.gephi.data.attributes.api.AttributeType;

import org.gephi.filters.plugin.partition.PartitionBuilder;
import org.gephi.filters.spi.FilterBuilder;
import org.gephi.graph.api.Graph;
import org.gephi.graph.api.GraphController;
import org.gephi.graph.api.GraphModel;
import org.gephi.io.importer.api.EdgeDefault;
import org.gephi.io.importer.api.EdgeDraft;
import org.gephi.io.importer.api.ImportController;
import org.gephi.io.importer.api.NodeDraftGetter;
import org.gephi.io.importer.api.Report;
import org.gephi.io.importer.impl.EdgeDraftImpl;
import org.gephi.io.importer.impl.ImportContainerImpl;
import org.gephi.io.importer.impl.NodeDraftImpl;
import org.gephi.io.processor.plugin.DefaultProcessor;
import org.gephi.layout.plugin.forceAtlas2.ForceAtlas2;
import org.gephi.layout.plugin.forceAtlas2.ForceAtlas2Builder;
import org.gephi.partition.api.NodePartition;
import org.gephi.partition.api.Partition;
import org.gephi.partition.api.PartitionController;
import org.gephi.partition.plugin.NodeColorTransformer;
import org.gephi.preview.*;
import org.gephi.preview.api.PreviewProperty;
import org.gephi.preview.plugin.renderers.EdgeLabelRenderer;
import org.gephi.project.api.ProjectController;
import org.gephi.project.api.Workspace;

import org.gephi.ranking.api.Ranking;
import org.gephi.ranking.api.RankingController;
import org.gephi.ranking.api.Transformer;
import org.gephi.ranking.plugin.DegreeRankingBuilder;
import org.gephi.ranking.plugin.transformer.AbstractSizeTransformer;

import org.gephi.statistics.plugin.Modularity;
import org.gephi.statistics.plugin.builder.ModularityBuilder;
import org.gephi.statistics.spi.Statistics;
import org.gephi.visualization.VizController;
import org.gephi.visualization.opengl.text.TextModel;
import org.openide.*;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.*;
import org.openide.util.NbBundle.Messages;

@ActionID(
        category = "File",
        id = "PlagFolder.PlagFolder"
)
@ActionRegistration(
        displayName = "#CTL_PlagFolder"
)
@ActionReference(path = "Menu/Plugins", position = 3233)
@Messages("CTL_PlagFolder=Select JPlag Output Folder...")
public final class PlagFolder implements ActionListener {
    
    class LinkStruct{
        String src;
        String dest;
        double weight;
        String link;

        private LinkStruct(String srcNodeName, String destNodeName, double perc, String link) {
            src = srcNodeName;
            dest = destNodeName;
            weight = perc;
            this.link = link;
        }
    }
    static String readFile(String path, Charset encoding) 
      throws IOException 
    {
      byte[] encoded = Files.readAllBytes(Paths.get(path));
      return new String(encoded, encoding);
    }
    @Override
    public void actionPerformed(ActionEvent e) {
        JFileChooser fileChooser = new JFileChooser("C:\\Users\\Marko\\Desktop\\jplag-master\\jplag\\target");
        fileChooser.setFileSelectionMode( JFileChooser.DIRECTORIES_ONLY);
        int returnVal = fileChooser.showOpenDialog(null);
        if(returnVal != JFileChooser.APPROVE_OPTION) {
            return;
        }
        try {
            String pathToGraph = fileChooser.getSelectedFile().getAbsolutePath();

            String strFile = readFile(pathToGraph+"\\index.html", Charset.defaultCharset());
            strFile = strFile.split("Matches sorted by maximum similarity")[0];
            Matcher m = Pattern.compile("<TR><TD BGCOLOR=#[0-9a-f]*>(.*?)</TD>(.*)</TD>").matcher(strFile);

            String srcNodeName = "";
            String rest = "";
            ArrayList<String> nodeNames = new ArrayList<String>();
            ArrayList<LinkStruct> links = new ArrayList<LinkStruct>();

            ImportContainerImpl cont = new ImportContainerImpl();
            //AttributeColumn col = cont.getAttributeModel().getNodeTable().addColumn("url", AttributeType.STRING);
            cont.setReport(new Report());
            cont.setEdgeDefault(EdgeDefault.UNDIRECTED);
            cont.getAttributeModel().getEdgeTable().addColumn("link", AttributeType.STRING);
            cont.getAttributeModel().getEdgeTable().addColumn("NodeName1", AttributeType.STRING);
            cont.getAttributeModel().getEdgeTable().addColumn("NodeName2", AttributeType.STRING);
            
            NodeDraftImpl srcnodedraft, destnodedraft;
            int id = 0;
            while(m.find()){
                 srcNodeName = m.group(1);
                if(nodeNames.contains(srcNodeName)){
                         srcnodedraft = cont.getNode(srcNodeName);
                     }else{
                         srcnodedraft = new NodeDraftImpl(cont, srcNodeName);
                         nodeNames.add(srcNodeName);
                         srcnodedraft.setLabelVisible(true);
                        cont.addNode(srcnodedraft);
                     }
                 rest = m.group(2);
                 Matcher m1 = Pattern.compile("A HREF=\"(.*?)\">(.*?)</A><BR><FONT COLOR=\"#[0-9a-f]*\">\\((.*?)%\\)</FONT>").matcher(rest);
                 while(m1.find()){
                     String destNodeName = m1.group(2);
                    
                 
                     if(nodeNames.contains(destNodeName)){
                         destnodedraft = cont.getNode(destNodeName);
                     }else{
                         destnodedraft = new NodeDraftImpl(cont, destNodeName);
                         nodeNames.add(destNodeName);
                         destnodedraft.setLabelVisible(true);
                        cont.addNode(destnodedraft);
                     }
                     String link = m1.group(1);
                     double perc = Double.parseDouble(m1.group(3));
                     EdgeDraftImpl edgedraft = new EdgeDraftImpl(cont, "" + id++);
                    edgedraft.setWeight((float) perc);
                    AttributeColumn linkColumn = cont.getAttributeModel().getEdgeTable().getColumn("link");
                    edgedraft.addAttributeValue(linkColumn, pathToGraph+"\\"+link);
                    AttributeColumn name1Column = cont.getAttributeModel().getEdgeTable().getColumn("NodeName1");
                    edgedraft.addAttributeValue(name1Column, srcNodeName);
                    AttributeColumn name2Column = cont.getAttributeModel().getEdgeTable().getColumn("NodeName2");
                    edgedraft.addAttributeValue(name2Column, destNodeName);
                    
                    edgedraft.setLabel(m1.group(3));
                    edgedraft.setSource(srcnodedraft);
                    edgedraft.setTarget(destnodedraft);
                    edgedraft.setLabelVisible(true);
                    edgedraft.setType(EdgeDraft.EdgeType.UNDIRECTED);
                    cont.addEdge(edgedraft);
                    // links.add(new LinkStruct(srcNodeName, destNodeName, perc, link));
                 }
            }
            if(!cont.verify()){
                throw new Exception();
            }
            
            ProjectController pc = Lookup.getDefault().lookup(ProjectController.class);
            pc.newProject();
            Workspace workspace = pc.getCurrentWorkspace();
            
            ImportController importController = Lookup.getDefault().lookup(ImportController.class);
            importController.process(cont, new DefaultProcessor(), workspace);
            
            
            //Ranking
            RankingController rankingController = Lookup.getDefault().lookup(RankingController.class);
            Ranking degreeRanking = rankingController.getModel().getRanking(Ranking.NODE_ELEMENT, Ranking.DEGREE_RANKING);
            AbstractSizeTransformer sizeTransformer = (AbstractSizeTransformer) rankingController.getModel().getTransformer(Ranking.NODE_ELEMENT, Transformer.RENDERABLE_SIZE);
            sizeTransformer.setMinSize(20);
            sizeTransformer.setMaxSize(80);
            rankingController.transform(degreeRanking,sizeTransformer);
            
            //Modularity
            ModularityBuilder modularityBuilder = new ModularityBuilder();
            Modularity modularity;
            modularity = (Modularity) modularityBuilder.getStatistics();
            
            GraphController graphController = Lookup.getDefault().lookup(GraphController.class);
            GraphModel model = graphController.getModel();
            
            
            PartitionController partitionController = Lookup.getDefault().lookup(PartitionController.class);
            AttributeModel attributeModel = Lookup.getDefault().lookup(AttributeController.class).getModel();
            modularity.execute(model, attributeModel);
            
            AttributeColumn modColumn = attributeModel.getNodeTable().getColumn(Modularity.MODULARITY_CLASS);
            Partition p2 = partitionController.buildPartition(modColumn, model.getUndirectedGraphVisible());
            NodeColorTransformer nodeColorTransformer2 = new NodeColorTransformer();
            nodeColorTransformer2.randomizeColors(p2);
            partitionController.transform(p2, nodeColorTransformer2);   
            
            //Labels
            Graph g = model.getDirectedGraphVisible();
            TextModel textModel = VizController.getInstance().getTextManager().getModel();
            textModel.setShowNodeLabels(true);
            textModel.setSelectedOnly(true);
            textModel.setShowEdgeLabels(true);
            VizController.getInstance().getTextManager().setModel(textModel);
            //ForceAtlas2
            WorkerThread thread = new WorkerThread();
            thread.start();
               Thread.sleep(1500);
               thread.work = false;
               thread = null;
              
            
        } catch (IOException ex) {
            NotifyDescriptor d =new NotifyDescriptor.Message("The seleted folder does not contain JPlag's output", NotifyDescriptor.INFORMATION_MESSAGE);
            DialogDisplayer.getDefault().notify(d);
        }
        catch(Exception ex){
            Exceptions.printStackTrace(ex);
        }
        
        
         
        
    }
    
    
    class WorkerThread extends Thread{
        ForceAtlas2 forceatlas;
        boolean work = true;
        public void run(){
            GraphController graphController = Lookup.getDefault().lookup(GraphController.class);
            

            //org.gephi.visualization.opengl.AbstractEngine engine = VizController.getInstance().getEngine();
            GraphModel model = graphController.getModel();
            
            
             forceatlas = (new ForceAtlas2Builder()).buildLayout();
           
            //setting the template
            forceatlas.setGraphModel(model);
            forceatlas.setEdgeWeightInfluence(0.2);
            forceatlas.setScalingRatio(150.0);
            forceatlas.setGravity(20.0);
            forceatlas.setJitterTolerance(0.05);
            forceatlas.setBarnesHutTheta(1.2);
            forceatlas.setBarnesHutOptimize(false);
            forceatlas.setAdjustSizes(Boolean.TRUE);
            forceatlas.setOutboundAttractionDistribution(Boolean.TRUE); 
            
            if(forceatlas.canAlgo()){
               forceatlas.initAlgo();
               while(work){
                   forceatlas.goAlgo();
               }
            forceatlas.endAlgo();
            }
        }
    }
}
