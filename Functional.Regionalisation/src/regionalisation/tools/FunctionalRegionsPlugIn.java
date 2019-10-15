
// Functional Regionalisation plugin (version 1.0.0) for OpenJUMP
//
// Copyright (C) Burak Beyhan
//
// This plugin is designed to form functional and plan regions by using FRGIS algorithm developed 
// by Burak Beyhan for commuting or similar flow data.
//
// If you use this plugin in your analysis please refer to the following paper;
//
// BEYHAN, B. (2019): The delimitation of planning regions on the basis of functional regions: An algorithm and its implementation in Turkey.
// Moravian Geographical Reports, 27(1): 15-30. Doi: 10.2478/mgr-2019-0002. Available on line at: http://www.geonika.cz/EN/research/ENMGRClanky/2019_1_BEYHAN.pdf
//
// This program is free software; you can redistribute it and/or modify it under the terms of 
// the GNU General Public License as published by the Free Software Foundation; either version 2 
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; 
// without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
// See the GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, see http://www.gnu.org/licenses


package regionalisation.tools;
import java.util.ArrayList;
import java.util.List;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JComboBox;
import javax.swing.JTextField;

import java.awt.*;
import java.io.*;

import javax.swing.*;

import nl.knaw.dans.common.dbflib.CorruptedTableException;
import nl.knaw.dans.common.dbflib.IfNonExistent;
import nl.knaw.dans.common.dbflib.Table;

import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.MultiInputDialog;
import com.vividsolutions.jump.workbench.ui.plugin.FeatureInstaller;


public class FunctionalRegionsPlugIn extends AbstractPlugIn{

 private FunctionalRegionsEngine engine = new FunctionalRegionsEngine();
 
 public FunctionalRegionsPlugIn() {
   // empty constructor
 }


 public String getName() {
     return "Functional Regionalisation"; // set the name / label of plugin for tooltips ...
 } 
 
 
 
 private Layer layer;
 File farpath;
 String forpath;
 String attribute;
 String fileyer;
 String sepo;
 
 boolean use_attribute;
 
 
 public void initialize(PlugInContext context) throws Exception {
     FeatureInstaller featureInstaller = new FeatureInstaller(context.getWorkbenchContext());
	        featureInstaller.addMainMenuPlugin(this, 
	        		new String[] {"FRGIS"}, //menu path
	        		"Functional Regionalisation", //name
	        		false, //checkbox
	        		new ImageIcon(this.getClass().getResource("/images/frgis.png")), //icon 
	        		new MultiEnableCheck().add(context.getCheckFactory().createTaskWindowMustBeActiveCheck()).add(context.getCheckFactory().createAtLeastNLayersMustExistCheck(1)));
	 context.getWorkbenchFrame()
            .getToolBar()
            .addPlugIn(
                    new ImageIcon(getClass().getResource("/images/frgist.png")), //icon 
                    this, new MultiEnableCheck().add(context.getCheckFactory().createTaskWindowMustBeActiveCheck()).add(context.getCheckFactory().createAtLeastNLayersMustExistCheck(1)),
                    context.getWorkbenchContext());
 }

 
 public static MultiEnableCheck createEnableCheck(WorkbenchContext workbenchContext) {
     EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);

     return new MultiEnableCheck()
     .add(checkFactory.createWindowWithLayerNamePanelMustBeActiveCheck());
 }

 
 public boolean execute(PlugInContext context) throws Exception {

 	// script for GUI
 	
 	final MultiInputDialog dialog =
     new MultiInputDialog(context.getWorkbenchFrame(),
         		"Formation of Functional Regions", true);
	
	 dialog.addLabel("<HTML><<EM>"+"Selection of Spatial and Flow Data"+"<HTML><EM>");
	
	 dialog.setSideBarDescription("This plugin is designed to form functional and plan regions by using FRGIS algorithm developed " +
		 		"by Burak Beyhan for commuting or similar flow data. " + '\n' + '\n' + "In order to run the plugin, you should have two sets of databases: " +
		 		"(1) a spatial database showing the basic spatial units (BSU) and (2) a flow database showing the magnitude and direction " +
		 		"of linkages between BSUs." + '\n' + '\n' + "BSUs in these two databases will be interlinked to each other via an identity field. Thus, in " +
		 		"the attribute table of the spatial database there should be a field showing a unique identity number for each BSU. In " +
		 		"the flow database, the linkages between BSUs should also be organized by using the same set of identity numbers. " +
		 		"Further explanations can be found in the following paper and guide:" + '\n' + '\n' + 
		 		"BEYHAN, B. (2019): The delimitation of planning regions on the basis of functional regions: An algorithm and its implementation in Turkey. " +
		 		"Moravian Geographical Reports, 27(1): 15-30."  + '\n' + '\n' + 
		 		"BEYHAN, B. (2019): A simple user’s guide for Functional Regionalisation Plugin. Available on line at https://github.com/burakbeyhan/delimitation-of-functional-and-planning-regions");

     JComboBox BSU_layer = dialog.addLayerComboBox("Spatial data:", context.getCandidateLayer(0), "please select a polygon layer", context.getLayerManager());
     BSU_layer.setPreferredSize(new Dimension(96, 20));

     List fieldim = getFieldsFromLayerWithoutGeometry(context.getCandidateLayer(0));       

     Object val = fieldim.size() > 0 ? fieldim.iterator().next() : null;
     
     final JComboBox BSU_attribute = dialog.addComboBox("Identity field for BSUs:", val, fieldim, "please select a field for the identification of Basic Spatial Units (BSU)");

     JButton dosya = dialog.addButton("Flow data (FD):", "file", "please select the file containing flow data");
     
     final List CAT = new ArrayList();
	 CAT.add("available fields");

	 dialog.addLabel("<HTML><EM>"+"Specify the fields for:"+"<HTML><EM>");

	 final JComboBox OBSU = dialog.addComboBox("Origin BSU (OB):", 0, CAT, "orgin BSU (OB)");
	 final JComboBox DBSU = dialog.addComboBox("Destination BSU (DB):", 0, CAT, "destination BSU (DB)");
	 final JComboBox COMM = dialog.addComboBox("Amount of Commuting (AC):", 0, CAT, "amount of commuting (AC)");
	 final JComboBox COMR = dialog.addComboBox("Commuting Level (CL):", 0, CAT, "commuting level (CL)");
  
     BSU_attribute.setPreferredSize(new Dimension(96, 20));
     
	 dialog.addLabel(" ");
	 dialog.addLabel("<HTML><EM>"+"Functional Region (FR) thresholds for:"+"<HTML><EM>");

	 dialog.addIntegerField("Maximum GGD (GT):", 3, 11, "Graph Theoretical Geodesic Distance (GGD) threshold (GT)");  
	 dialog.addDoubleField("Minimum CL (CT):", 3.0, 11, "commuting threshold (CT)");
	 dialog.addIntegerField("Number of Iterations (IT):", 3, 11, "iteration threshold (IT)");  
	 dialog.addIntegerField("Maximum Area of a FR (AT):", 40000,11, "area threshold (AT) in km²").setEnabled(false);;  

	 dialog.addLabel(" ");

	 dialog.addLabel("<HTML><EM>"+"Functional Region Options"+"<HTML><EM>");
     JCheckBox BB = dialog.addCheckBox("Use AT in FR formation", false, "restrict formation of FRs according to given area threshold");
     JCheckBox CHUB = dialog.addCheckBox("Finish when all FR ids are filled", true, "finish formation of FRs when all FR ids are filled");
     JCheckBox MEC = dialog.addCheckBox("Create FD file for FRs", false, "flow data file for FRs");
     
   	 final JTextField dosyer = dialog.addTextField("Location of FR FD file", "",11, null, "choose a location for and enter file name of flow data for FRs");
     dosyer.setEnabled(false);
     
     if (context.getCandidateLayer(0).hasReadableDataSource()) {
    	 forpath = context.getCandidateLayer(0).getDataSourceQuery().getDataSource().getProperties().get("File").toString(); 
    	 farpath = new File(forpath).getParentFile();
     }
     
	 BSU_layer.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
         	layer = dialog.getLayer("Spatial data:");
         	if (layer.hasReadableDataSource()) {
         		forpath = layer.getDataSourceQuery().getDataSource().getProperties().get("File").toString(); 
            	farpath = new File(forpath).getParentFile();
         	}
             List list = getFieldsFromLayerWithoutGeometry(layer);
             if (list.size() == 0) {
             	BSU_attribute.setModel(new DefaultComboBoxModel(new String[0]));
             	BSU_attribute.setEnabled(true);
             }
             BSU_attribute.setModel(new DefaultComboBoxModel(list.toArray(new String[0])));
         }            
     });

        
	 ActionListener flowdata = new ActionListener() {
		      public void actionPerformed(ActionEvent e){
	            JFileChooser openmyFile = new JFileChooser();
	            openmyFile.addChoosableFileFilter(null);
	            openmyFile.setCurrentDirectory(farpath);
	            openmyFile.showOpenDialog(null);
	            File file = openmyFile.getSelectedFile();
	            String flowpath = file.getPath();
                fileyer = flowpath;
                
	            OBSU.removeAllItems();
	            DBSU.removeAllItems();
	            COMM.removeAllItems();
	            COMR.removeAllItems();

	            OBSU.addItem("available fields");
	            DBSU.addItem("available fields");
	            COMM.addItem("available fields");
	            COMR.addItem("available fields");

	            int len = flowpath.length();
	            String ext = flowpath.substring(len-3, len).toLowerCase();
	            
	            
	            // processing column headings for the files delimited with a separator
	            
	            if (ext.equals("csv") || ext.equals("txt")) {
		            
	                FileReader FieldList = null;
	                
				    try {
					    FieldList = new FileReader(flowpath);
				    } catch (FileNotFoundException e1) {
					// TODO Auto-generated catch block
					    e1.printStackTrace();
				    }
				
	                BufferedReader reader = new BufferedReader(FieldList);
	            
	                String alans = null;
				    try {
					    alans = reader.readLine();
				    } catch (IOException e2) {
					// TODO Auto-generated catch block
					   e2.printStackTrace();
				    }

				    String[] fdfm = {";", ",", "tab", "space", "-"};
				    JComboBox jcb = new JComboBox(fdfm);
				    jcb.setEditable(true);
				    JOptionPane.showMessageDialog( null, jcb, "Select separator for flow data file", JOptionPane.PLAIN_MESSAGE);
				    
				    sepo = (String) jcb.getSelectedItem();
				    
				    if (sepo == "tab" || sepo == "space") sepo = "\\s+";
				    
	                String[] ss = alans.trim().split(sepo);

	                for(int i=0; i<ss.length; i++) {
	            	    OBSU.addItem(ss[i]);
	              	    DBSU.addItem(ss[i]);
             	        COMM.addItem(ss[i]);
	            	    COMR.addItem(ss[i]);
	                }
	            
	                try {
					    FieldList.close();
				    } catch (IOException e1) {
					// TODO Auto-generated catch block
					    e1.printStackTrace();
				    }
	            }
	            
	            
	            if (ext.equals("dbf")) {
	                Table table = new Table(new File(flowpath));
	                try {
						table.open(IfNonExistent.ERROR);
					} catch (CorruptedTableException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
	                List fieldim = table.getFields();
	                int fieldnum = fieldim.size();
	                for(int i = 0; i < fieldnum; i++) {
	                	OBSU.addItem(table.getFields().get(i).getName());
	                	DBSU.addItem(table.getFields().get(i).getName());
	                	COMM.addItem(table.getFields().get(i).getName());
	                	COMR.addItem(table.getFields().get(i).getName());
	                }
	                try {
						table.close();
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
	            }
	            
	            
		      }
	 };
	 dosya.addActionListener(flowdata);

	 
	 ActionListener csvdata = new ActionListener() {
		      public void actionPerformed(ActionEvent e){
		    	  if (dialog.getBoolean("Create FD file for FRs")) {
		            JFileChooser savemyFile = new JFileChooser();
		            savemyFile.addChoosableFileFilter(null);
		            savemyFile.setCurrentDirectory(farpath);
		            savemyFile.showSaveDialog(null);
		            File filed = savemyFile.getSelectedFile();
		            String akimpath = filed.getPath();
		            dosyer.setText(akimpath);
		    	  }
		      }
	 };
	 MEC.addActionListener(csvdata);

	 
	 ActionListener aream = new ActionListener() {
		      public void actionPerformed(ActionEvent e){
         		 dialog.setFieldEnabled("Maximum Area of a FR (AT):", dialog.getBoolean("Use AT in FR formation"));
		      }
	 };	
	 BB.addActionListener(aream);
		
		
     GUIUtil.centreOnWindow(dialog);
     dialog.setVisible(true);

     if (!dialog.wasOKPressed()) {
         return false;
     }

     getDialogValues(dialog);
     engine.execute(context);
     
     final int origin = OBSU.getSelectedIndex();
    
     return true;
 }


 private List getFieldsFromLayerWithoutGeometry(Layer lyr) {
   List fields = new ArrayList();
   FeatureSchema schema = lyr.getFeatureCollectionWrapper().getFeatureSchema();
   for (int i = 0; i < schema.getAttributeCount(); i++) {
     if (schema.getAttributeType(i) != AttributeType.GEOMETRY) {
       fields.add(schema.getAttributeName(i));
     }
   }
   return fields;
 }

 private void getDialogValues(MultiInputDialog dialog) {
     engine.aktarim0(dialog.getLayer("Spatial data:"));
     engine.aktarim1(dialog.getText("Identity field for BSUs:"));
     engine.aktarim2(dialog.getText("Location of FR FD file"));
     engine.aktarim3(fileyer);
     engine.aktarim4(dialog.getText("Origin BSU (OB):"));
     engine.aktarim5(dialog.getText("Destination BSU (DB):"));
     engine.aktarim6(dialog.getText("Commuting Level (CL):"));
     engine.aktarim7(dialog.getText("Amount of Commuting (AC):"));
     engine.aktarim8(dialog.getInteger("Maximum GGD (GT):"));
     engine.aktarim9(dialog.getDouble("Minimum CL (CT):"));
     engine.aktarim10(dialog.getInteger("Number of Iterations (IT):"));
     engine.aktarim11(dialog.getInteger("Maximum Area of a FR (AT):"));
     engine.aktarim12(dialog.getBoolean("Use AT in FR formation"));
     engine.aktarim13(dialog.getBoolean("Finish when all FR ids are filled"));
     engine.aktarim14(dialog.getBoolean("Create FD file for FRs"));
     engine.aktarim15(sepo);
 }

 
}
