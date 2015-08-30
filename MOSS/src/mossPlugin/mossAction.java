/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mossPlugin;


import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import org.gephi.data.attributes.api.AttributeColumn;
import org.gephi.data.attributes.api.AttributeController;
import org.gephi.data.attributes.api.AttributeModel;
import org.gephi.data.attributes.api.AttributeType;

import org.gephi.graph.api.GraphController;
import org.gephi.graph.api.GraphModel;
import org.gephi.io.importer.api.EdgeDefault;
import org.gephi.io.importer.api.EdgeDraft;
import org.gephi.io.importer.api.ImportController;
import org.gephi.io.importer.api.Report;
import org.gephi.io.processor.plugin.DefaultProcessor;
import org.gephi.layout.plugin.forceAtlas2.ForceAtlas2;
import org.gephi.layout.plugin.forceAtlas2.ForceAtlas2Builder;
import org.gephi.partition.api.Partition;
import org.gephi.partition.api.PartitionController;
import org.gephi.partition.plugin.NodeColorTransformer;
import org.gephi.project.api.ProjectController;
import org.gephi.project.api.Workspace;

import org.gephi.ranking.api.Ranking;
import org.gephi.ranking.api.RankingController;
import org.gephi.ranking.api.Transformer;
import org.gephi.ranking.plugin.transformer.AbstractSizeTransformer;

import org.gephi.statistics.plugin.Modularity;
import org.gephi.statistics.plugin.builder.ModularityBuilder;
import org.gephi.visualization.VizController;
import org.openide.*;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.*;
import org.openide.util.NbBundle.Messages;

@ActionID(
        category = "Edit",
        id = "mossPlugin.mossAction"
)
@ActionRegistration(
        displayName = "#CTL_mossAction"
)
@ActionReference(path = "Menu/Plugins", position = 3133)
@Messages("CTL_mossAction=Load MOSS results")
public final class mossAction implements ActionListener {

    private static class DownloaderThread extends Thread{
        String indexPage;
        String localPath;
        public DownloaderThread(String pageContents, String path) {
            indexPage = pageContents;
            localPath = path;
        }
        public void run(){
            FileOutputStream outputStream = null;
            try {
                String line;
                if(localPath.contains("file:///")){
                    localPath = localPath.substring(8);
                }
                outputStream = new FileOutputStream(localPath+"index.html");
                outputStream.write(indexPage.getBytes());
                outputStream.close();
                    
                Matcher match = Pattern.compile("<TD><A HREF=\"(.*)\">.*</A>\\s*<TD><A HREF=\".*\">.*</A>").matcher(indexPage);
                while(match.find()){
                    //matchX.html
                    String strURL = match.group(1);
                    URL url = new URL(strURL);
                    InputStream is = url.openStream();  // throws an IOException
                    BufferedReader br = new BufferedReader(new InputStreamReader(is));
                    outputStream = new FileOutputStream(localPath+strURL.substring(strURL.lastIndexOf('/')+1));
                    while((line = br.readLine())!=null){
                        outputStream.write((line+"\n").getBytes());
                    }
                    outputStream.close();
                    
                    String[] others = {"-top.html", "-0.html", "-1.html"};
                    //matchX-top.html, matchX-0.html, matchX-1.html
                    for(String post:others){
                        StringBuilder build = new StringBuilder();
                        build.append(strURL.substring(0,strURL.lastIndexOf(".html")));
                        build.append(post);
                        String newURL = build.toString();
                        url = new URL(newURL);
                        is = url.openStream();  // throws an IOException
                        br = new BufferedReader(new InputStreamReader(is));
                        outputStream = new FileOutputStream(localPath+newURL.substring(newURL.lastIndexOf('/')+1));
                        while((line = br.readLine())!=null){
                            outputStream.write((line+"\n").getBytes());
                        }
                        outputStream.close();

                    }
                    
                }
                
                NotifyDescriptor d =new NotifyDescriptor.Message("Finished downloading results to "+localPath, NotifyDescriptor.INFORMATION_MESSAGE);
                DialogDisplayer.getDefault().notify(d);
                
            } catch (FileNotFoundException ex) {
                Exceptions.printStackTrace(ex);
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            } finally {
                try {
                    outputStream.close();
                    
                } catch (IOException ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
        }
    }
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
    public String path;
    public static void deleteFolder(File folder) {
        File[] files = folder.listFiles();
        if(files!=null) { //some JVMs return null for empty dirs
            for(File f: files) {
                if(f.isDirectory()) {
                    deleteFolder(f);
                } else {
                    f.delete();
                }
            }
        }
        //folder.delete();
    }
    @Override
    public void actionPerformed(ActionEvent e) {
        
        JCheckBox checkbox = new JCheckBox("Do You want to save web results locally?");
        String msg = "Enter MOSS results URL";
        JTextField textField = new JTextField();
        JButton browse = new JButton("Browse");
        browse.addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser("C:\\Users\\Marko\\Desktop");
                fileChooser.setFileSelectionMode( JFileChooser.DIRECTORIES_ONLY);
                int returnVal = fileChooser.showOpenDialog(null);
                if(returnVal != JFileChooser.APPROVE_OPTION) {
                    return;
                }
                if (fileChooser.getSelectedFile().getAbsolutePath()!=null){
                    path = fileChooser.getSelectedFile().getAbsolutePath();
                }
                else {//Moze da se doda novi folder pod brojem submissiona
                    path = "C:\\MOSS_Output";
                    
                }
            }
        });
        Object[] params = { checkbox, browse,msg};
        String strURL = (String)JOptionPane.showInputDialog(
                    new JFrame(),
                    params,
                    JOptionPane.YES_NO_OPTION);
        
        if(strURL == null || !strURL.contains("http://moss.stanford.edu/results/"))return;
        
        //default output
        if(checkbox.isSelected()){
            if (path == null) path = "C:\\MOSS_Output";
            if(path.equals("C:\\MOSS_Output")){
                path+="\\"+strURL.substring(strURL.lastIndexOf("results/")+8);
                if(path.charAt(path.length()-1) == '/'){
                    path = path.substring(0,path.length()-1);
                }
                File f;
                if(!(f= new File(path)).exists()){
                    f.mkdirs();
                }
                else{
                    deleteFolder(f);
                }
            }
            path = "file:///"+path+"\\";
        }
        URL url;
        InputStream is = null;

        BufferedReader br;
        String line;
        String content = "";
        try {
            url = new URL(strURL);
            
            is = url.openStream();  // throws an IOException if URL is wrong
            br = new BufferedReader(new InputStreamReader(is));

            while ((line = br.readLine()) != null) {
                content+=line+"\n";
            }
        } catch (MalformedURLException mue) {
             mue.printStackTrace();
             NotifyDescriptor d =new NotifyDescriptor.Message("The MOSS Results Page Is Invalid", NotifyDescriptor.INFORMATION_MESSAGE);
            DialogDisplayer.getDefault().notify(d);
            return;
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
            return;
        } finally {
            try {
                if (is != null) is.close();
            } catch (IOException ioe) {
                // nothing to see here
          }
        }
        boolean local = false;
        if(checkbox.isSelected()){
            DownloaderThread dlThread = new DownloaderThread(content, path);
            dlThread.start();
            local = true;
        }
        try {

           Matcher m = Pattern.compile("<TR><TD><A HREF=\"(.*)\">(.*)\\(([0-9]*)%\\)</A>\\s*<TD><A HREF=\".*\">(.*)\\(([0-9]*)%\\)</A>").matcher(content);

            String srcNodeName = "";
            String rest = "";
            ArrayList<String> nodeNames = new ArrayList<String>();
            ArrayList<LinkStruct> links = new ArrayList<LinkStruct>();
            org.gephi.io.importer.impl.ImportContainerImpl cont = new org.gephi.io.importer.impl.ImportContainerImpl();
            //AttributeColumn col = cont.getAttributeModel().getNodeTable().addColumn("url", AttributeType.STRING);
            cont.setReport(new Report());
            cont.setEdgeDefault(EdgeDefault.UNDIRECTED);
            cont.getAttributeModel().getEdgeTable().addColumn("link", AttributeType.STRING);
            cont.getAttributeModel().getEdgeTable().addColumn("NodeName1", AttributeType.STRING);
            cont.getAttributeModel().getEdgeTable().addColumn("NodeName2", AttributeType.STRING);
            
            org.gephi.io.importer.impl.NodeDraftImpl srcnodedraft, destnodedraft;
            int id = 0;
            while(m.find()){
                 srcNodeName = m.group(2);
                if(nodeNames.contains(srcNodeName)){
                         srcnodedraft = cont.getNode(srcNodeName);
                     }else{
                         srcnodedraft = new org.gephi.io.importer.impl.NodeDraftImpl(cont, srcNodeName);
                         nodeNames.add(srcNodeName);
                         srcnodedraft.setLabelVisible(true);
                        cont.addNode(srcnodedraft);
                     }
                      String destNodeName = m.group(4);
                    
                 
                     if(nodeNames.contains(destNodeName)){
                         destnodedraft = cont.getNode(destNodeName);
                     }else{
                         destnodedraft = new org.gephi.io.importer.impl.NodeDraftImpl(cont, destNodeName);
                         nodeNames.add(destNodeName);
                         destnodedraft.setLabelVisible(true);
                        cont.addNode(destnodedraft);
                     }
                     String link = m.group(1);
                     if(local){
                         link = link.replace(link.substring(0,link.lastIndexOf('/')+1), path);
                     }
                     double perc = (Double.parseDouble(m.group(3))+Double.parseDouble(m.group(3)))/2;
                     org.gephi.io.importer.impl.EdgeDraftImpl edgedraft = new org.gephi.io.importer.impl.EdgeDraftImpl(cont, "" + id++);
                    edgedraft.setWeight((float) perc);
                    AttributeColumn linkColumn = cont.getAttributeModel().getEdgeTable().getColumn("link");
                    edgedraft.addAttributeValue(linkColumn, link);
                    AttributeColumn name1Column = cont.getAttributeModel().getEdgeTable().getColumn("NodeName1");
                    edgedraft.addAttributeValue(name1Column, srcNodeName);
                    AttributeColumn name2Column = cont.getAttributeModel().getEdgeTable().getColumn("NodeName2");
                    edgedraft.addAttributeValue(name2Column, destNodeName);
                    
                    edgedraft.setLabel(m.group(3));
                    edgedraft.setSource(srcnodedraft);
                    edgedraft.setTarget(destnodedraft);
                    edgedraft.setLabelVisible(true);
                    edgedraft.setType(EdgeDraft.EdgeType.UNDIRECTED);
                    cont.addEdge(edgedraft);
                    // links.add(new LinkStruct(srcNodeName, destNodeName, perc, link));
                 
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
            VizController.getInstance().getTextManager().getModel().setShowNodeLabels(true);
            VizController.getInstance().getTextManager().getModel().setSelectedOnly(true);
            VizController.getInstance().getTextManager().getModel().setShowEdgeLabels(true);
            VizController.getInstance().getVizModel().getTextModel().setShowEdgeLabels(true);
            VizController.getInstance().getVizModel().getTextModel().setShowNodeLabels(true);
            VizController.getInstance().getVizModel().getTextModel().setSelectedOnly(true);
            //ForceAtlas2
            WorkerThread thread = new WorkerThread();
            thread.start();
               Thread.sleep(1500);
               thread.work = false;
               thread = null;
              
            path = null;
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
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
