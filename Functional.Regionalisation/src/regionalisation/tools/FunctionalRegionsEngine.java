
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

import java.awt.Color;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.JFrame;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.operation.union.UnaryUnionOp;
import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.feature.FeatureDataset;
import com.vividsolutions.jump.feature.BasicFeature;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.LayerManager;
import com.vividsolutions.jump.workbench.model.LayerStyleUtil;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.MultiInputDialog;
import com.vividsolutions.jump.workbench.ui.renderer.style.BasicStyle;
import com.vividsolutions.jump.workbench.ui.renderer.style.ColorScheme;
import com.vividsolutions.jump.workbench.ui.renderer.style.ColorThemingStyle;
import com.vividsolutions.jump.workbench.ui.task.TaskMonitorDialog;

import org.openjump.core.geomutils.algorithm.GeometryConverter;

import nl.knaw.dans.common.dbflib.*;


public class FunctionalRegionsEngine {

 private Layer katmanim;
 private boolean maxse;
 private boolean kobje;
 private boolean kosje;

 private String aloc;
 private String floc;
 private String origin;
 private String destin;
 private String ratiom;
 private String commut;
 private String ase;
 private String delim;
 private String sepo;

 private Integer GT;
 private Double CT;
 private Integer iterdia;
 private Integer AT;
 private Integer keye;
 private Integer keyo;

    
 public FunctionalRegionsEngine() {
 }

 public void aktarim0(Layer katman){katmanim = katman;}
 public void aktarim1(String st0){ase = st0;}
 public void aktarim2(String st1){aloc = st1;}
 public void aktarim3(String st2){floc = st2;}
 public void aktarim4(String st3){origin = st3;}
 public void aktarim5(String st4){destin = st4;}
 public void aktarim6(String st5){ratiom = st5;} 
 public void aktarim7(String st6){commut = st6;}
 public void aktarim8(Integer in1){GT = in1;} 
 public void aktarim9(Double do0){CT = do0;}
 public void aktarim10(Integer in2){iterdia = in2;}
 public void aktarim11(Integer in3){AT = in3;}
 public void aktarim12(boolean bo1){maxse = bo1;}
 public void aktarim13(boolean bo2){kobje = bo2;}
 public void aktarim14(boolean bo3){kosje = bo3;}
 public void aktarim15(String st7){delim = st7;}


 
 public void execute(PlugInContext context) throws Exception {

	final LayerManager layerManager = context.getLayerManager();
	final MultiInputDialog dialogi = new MultiInputDialog(context.getWorkbenchFrame(), "Functional Regionalisation Statistics", true);
	
	sepo = delim;
    if (delim == "\\s+") sepo = "\t";
	
    final JFrame desktop = (JFrame) context.getWorkbenchFrame();
    final TaskMonitorDialog progressDialog = new TaskMonitorDialog(desktop, null);
    progressDialog.setTitle("Background Operations");
    progressDialog.addComponentListener(new ComponentAdapter() {
        public void componentShown(ComponentEvent e) {
            new Thread(new Runnable() {
                public void run() {
                    try {
	
 File files = new File(floc);
 String parent = files.getParent();

 
 // initializing log file that keeps statistics for the specified parameters and results of the analysis regarding the different runs of the plug-in
 
 File stat = new File(parent+"\\stats.csv");                	
 if (!stat.exists()) {
	 FileWriter stats = new FileWriter(stat);
	 stats.write("GT;CT;NI;AT;FRs;MCL;Lline;MCL_out;Lline_out;MFRs;Parts;max_area;min_area;med_area;mean_area;max_LSC;min_LSC;med_LSC;mean_LSC;max_res;min_res;med_res;mean_res;max_work;min_work;med_work;mean_work");
	 stats.write('\n');
	 stats.close();
 }
 FileWriter stats = new FileWriter(stat, true);
 stats.write(GT+";");
 stats.write(CT+";");
 stats.write(1+";");
 String  atlim = AT.toString();
 
 if (maxse == false) { // if a limitation is not set for the maximum area, internally it is set to a very high value in order to let BSUs unite with each other.
 	AT = 100000000;
 	atlim = "-";
 }
 
 stats.write(atlim+";");

 
 // construction of adjacency matrix and calculation of graph theoretic geodesic distance
 
 Layer actualLayer = katmanim;
 LayerStyleUtil.setLinearStyle(actualLayer, Color.black, 1, 0);
 actualLayer.fireAppearanceChanged();
 
 FeatureCollection fc = actualLayer.getFeatureCollectionWrapper().getWrappee();

 progressDialog.report("Construction of adjacency matrix for BSUs");
 progressDialog.setSize(progressDialog.getWidth()+30, progressDialog.getHeight());
 
 int xx;
 int yy;
 int cntr;
 int enyuk;
 double commur = 0;
 String  commus;
 int obj = fc.size();
 double[][] rat = new double[obj][obj];
 int[][] dst = new int[obj][obj];
 int[] atr = new int[obj];
 int[][] ortak = new int[obj][3];
 int[][] ortakm = new int[obj][2];
 int[][] ortakc = new int[obj][2];

 xx = obj;

 List fortas = new ArrayList();
 List iliskim = new ArrayList();
 
for (Iterator iter = fc.iterator(); iter.hasNext();) {
	Feature element = (Feature) iter.next();
   	Geometry cc = element.getGeometry();
    cntr = 0;
    
    yy = obj;

     List iliski = new ArrayList();
     
	 int alanim = (int) (cc.getArea()/1000000);
	 ortak[obj-xx][2] = alanim;
  
     for (Iterator oter = fc.iterator(); oter.hasNext();) {
    	  Feature olement = (Feature) oter.next();
    	  Geometry dd = olement.getGeometry();
          boolean ser = cc.intersects(dd);
          if (ser) {
              cntr++;
              dst[obj-xx][obj-yy] = 1;
              iliski.add(obj-yy);
          }
          if (xx == yy) {
              dst[obj-xx][obj-yy] = 0;
          }
          yy--;
     }

     iliskim.add(""+element.getAttribute(ase));
       
     fortas.add(iliski);

     cntr--;
     atr[obj-xx] = cntr;
     xx--;
   }



progressDialog.report("Calculating graph theoretical geodesic distance between BSUs");
progressDialog.setSize(progressDialog.getWidth()+30, progressDialog.getHeight());

for (int mas = 0; mas < obj; mas++) {
    for (int kas = 1; kas < GT; kas++) {
        int fas = 1;
        for (int sas = 0; sas < obj; sas++) {
          if (dst[mas][sas] == kas && sas != mas) {
                List iliski = new ArrayList();
                iliski = (List) fortas.get(sas);
                int sizeOfList = iliski.size();
                for (int sowIndex = 0; sowIndex < sizeOfList; sowIndex++){
                   int tas = (Integer) iliski.get(sowIndex);
                   if (tas != mas && (dst[mas][tas] > kas || dst[mas][tas] == 0)) {
                      dst[mas][tas] = kas + 1;
                      fas = fas + 1;
                   }
                }
          }
        }
        if (fas == 1) break;
    }
}


// reading flow data file and construction of flow matrix and list 

progressDialog.setTitle("Construction of Flow Matrix and List");
progressDialog.setSize(progressDialog.getWidth()+60, progressDialog.getHeight());

    int len = floc.length();
    String ext = floc.substring(len-3, len).toLowerCase();

    int origindia = 0;
    int destindia = 0;
    int ratiomdia = 0;
    int commutdia = 0;
    int sayarko = 1;
	ArrayList<String[]> verim = new ArrayList();
	
    if (ext.equals("csv") || ext.equals("txt")) {
	    FileReader FlowData = new FileReader(floc);
	    BufferedReader reader = new BufferedReader(FlowData);
	    String alans = reader.readLine();
	    String[] ss = alans.split(delim);
	      
	    origindia = Arrays.asList(ss).lastIndexOf(origin);
	    destindia = Arrays.asList(ss).lastIndexOf(destin);
	    ratiomdia = Arrays.asList(ss).lastIndexOf(ratiom);
	    commutdia = Arrays.asList(ss).lastIndexOf(commut);

	    String line = reader.readLine();
	
	    while (line != null) {
	        String[] kayip = line.split(delim);
            String[] kayit = new String[4];
            
            progressDialog.report(sayarko+". line");

		    String origini = kayip[origindia];
            int or = iliskim.lastIndexOf(origini);
            kayit[0] = origini;
		    String destini = kayip[destindia];
		    kayit[1] = destini;
            int de = iliskim.lastIndexOf(destini);
        
	        int trip = Integer.parseInt(kayip[commutdia], 10);
            kayit[2] = trip+"";
	        Double ratio = Double.parseDouble(kayip[ratiomdia]);
	        kayit[3] = ratio+"";
	        
	        rat[or][de] = ratio;
	    
            ortakc[or][0] = ortakc[or][0] + trip;
            ortakc[de][1] = ortakc[de][1] + trip;

	        verim.add(kayit);
	        line = reader.readLine();
	        sayarko++;

	    }
	    FlowData.close();
    }

    
    if (ext.equals("dbf")) {
        Table table = new Table(new File(floc));
        table.open(IfNonExistent.ERROR);
        Iterator iterator = table.recordIterator();

        while (iterator.hasNext()) {
               Record record = (Record) iterator.next();
               String[] kayit = new String[4];
               
               progressDialog.report(sayarko+". line");
               
               String origini = record.getNumberValue(origin)+"";
               int or = iliskim.lastIndexOf(origini);
               kayit[0] = origini;
               String destini = record.getNumberValue(destin)+"";
               int de = iliskim.lastIndexOf(destini);
               kayit[1] = destini;

               int trip = (Integer) record.getNumberValue(commut);
               kayit[2] = trip+"";
               Double ratio = (Double) record.getNumberValue(ratiom);
               kayit[3] = ratio+"";

               rat[or][de] = ratio;
   	        
               ortakc[or][0] = ortakc[or][0] + trip;
               ortakc[de][1] = ortakc[de][1] + trip;

               verim.add(kayit);
               sayarko++;

        }
        table.close();
    }
    
	
	int rgs = verim.size();
	
    
	String katiket = "Functional Regions";
	if (maxse) katiket = "Functional Regions with Delimited Area";
	  
    progressDialog.setTitle("Forming "+katiket);    
   
	
    for (int dbi = 0; dbi < iterdia; dbi++) {

		 // FR ids are assigned to BSUs according to the predefined thresholds set for the parameters by reading the commuting database:
         int dbis = dbi+1;
         int dbbr = 0;
		 int sss = 0;
		 int sso = 0;

		 progressDialog.report(dbis+". iteration - 1. stage");
		 
		 // resetting FR ids for BSUs and preparation of values for area occupied by each BSU for the current iteration:
		 for (int mum = 0; mum < obj; mum++) {
		    ortak[mum][0] = 0;
		    ortak[mum][1] = ortak[mum][2];
		 }
	  
         for (int dbb = 0; dbb < rgs; dbb++) {

			    String[] kayit = verim.get(dbb);

				String origini = kayit[0];
		        int or = iliskim.lastIndexOf(origini);
				String destini = kayit[1];
		        int de = iliskim.lastIndexOf(destini);
		        
			    double CL = Double.parseDouble(kayit[3]);
			    
		    	if (CL < CT) {
		            break;
		    	}
		    	
			    int salan = ortak[or][1] + ortak[de][1];
			    int TG = dst[or][de];

			    int FIOB = ortakm[or][0];
			    int FIOD = ortakm[de][0];
			    int CIOB = ortakm[or][1];
			    int CIOD = ortakm[de][1];

		    	
			    if (TG > 0 && ((FIOB == 0 && FIOD == 0) || (FIOB == FIOD && CIOB == CIOD))) {
			      if (TG <= GT && CL >= CT && salan <= AT) {
			         commur = CL;
			         dbbr = dbb;
			    	 if (ortak[or][0] == 0 && ortak[de][0] == 0) {
			             sss++;
			             sso = sso + 2;
			             ortak[or][0] = sss;
			             ortak[de][0] = sss;
			             ortak[or][1] = salan;
			             ortak[de][1] = salan;
			         }
			         if (ortak[or][0] != 0 && ortak[de][0] == 0) {
			             sso++;
			             ortak[de][0] = ortak[or][0];
			             ortak[de][1] = salan;
			             if (maxse) { 
			               for (int zas = 0; zas < obj; zas++) {
			                 if (ortak[zas][0] == ortak[or][0]) {
			                     ortak[zas][1] = salan;
			                 }
			               }
			             }
			         }
			         if (ortak[or][0] == 0 && ortak[de][0] != 0) {
			             sso++;
			             ortak[or][0] = ortak[de][0];
			             ortak[or][1] = salan;
			             if (maxse) { 
			               for (int zas = 0; zas < obj; zas++) {
			                 if (ortak[zas][0] == ortak[de][0]) {
			                     ortak[zas][1] = salan;
			                 }
			               }
			             }
			         }
			         
			         // If there remains no BSU without a FR id, the first stage of the iteration is automatically stopped based on the option for "Finish when all FR ids are filled".
			         // If this option is not marked, larger FRs can be created according to the predefined values for minimum commuting level.
			         // Thus, this option is checked by default in order to prevent the formation of overly large FRs.
			         if (sso >= obj && kobje) {
			               break;
			         }

			         if (ortak[or][0] > 0 && ortak[de][0] > 0 && ortak[or][0] != ortak[de][0]) {
			            keye = ortak[or][0];
			            keyo = ortak[de][0];
 	                    int ks = 0;
		                int km = 0;
			            for (int zas = 0; zas < obj; zas++) {
				            if (ortak[zas][0] == keye && zas != or) {
				           	    ks++;
				           	    if(rat[de][zas] >= CT || rat[zas][de] >= CT) {
				                  km++;
				          	    }
				            }
				            if (ortak[zas][0] == keyo && zas != de) {
				           	    ks++;
				           	    if(rat[or][zas] >= CT || rat[zas][or] >= CT) {
				                  km++;
				          	    }
				            }
			            }
		                if (ks == km){
				           sss++; 
				           for (int zas = 0; zas < obj; zas++) {
			                    if (ortak[zas][0] == keye || ortak[zas][0] == keyo) {
			                        ortak[zas][0] = sss;
			                        ortak[zas][1] = salan;
			                    }
				           }  
		                }
			         }
			      }    
		        }
         }

         
        // The code given below is produced in order to make sure that all BSUs have a FR id. Based on the given CL value, there may exist some BSUs without a FR id. In the second stage,
        // in order to prevent this, the commuting database is re-read and a FR id is assigned to those BSUs having no FR id according to CL between them and the BSUs adjacent to them.
        // Since the commuting database is sorted in descending order according to CL value, FR id of the first BSU having the highest CL with the BSU having no FR id is assigned to 
        // the BSU concerned by also taking the total area occupied by the resulting FR into account. In this process, an automatic control is established by using limitsay variable.
        // In order to prevent the redundancy in re-reading the database, re-reading process is finished when the number of BSUs to which a FR id is assigned reaches to limitsay.

        double[][] dongu = new double[obj][2];
        
        commus = "--";
        int dbbs = 0;
        int dbl = 0;
        if (dbi == 0) dbl = dbbr;
        	
        for (int dbj = 0; dbj < iterdia; dbj++) {

          ArrayList purtas = new ArrayList();
          for (int mum = 0; mum < obj; mum++) {
            if (ortak[mum][0] == 0) {
              purtas.add(mum);
            }
          }
          int limitli = purtas.size();
          if (limitli == 0) {
            break;
          }

          int limitsay = 0;

 		 progressDialog.report(dbis+". iteration - 2. stage");

          for (int dbb = dbl; dbb < rgs; dbb++) {

            String[] kayit = verim.get(dbb);

            String origini = kayit[0];
            int or = iliskim.lastIndexOf(origini);
            String destini = kayit[1];
            int de = iliskim.lastIndexOf(destini);
            
            int salan = ortak[or][1] + ortak[de][1];

            int TG = dst[or][de];
            double CL = Double.parseDouble(kayit[3]);

            if (TG == 1 && salan <= AT) {

              if (ortak[or][0] != 0 && ortak[de][0] == 0) {
                  ortak[de][0] = ortak[or][0];

                  ortak[de][1] = salan;
                   if (maxse) { 
                     for (int zas = 0; zas < obj; zas++) {
                       if (ortak[zas][0] == ortak[or][0]) {
                           ortak[zas][1] = salan;
                       }
                     }
                  }

                  dongu[de][0] = dbj+1;
                  dongu[de][1] = CL;
                  commus = CL+"";
                  limitsay++;
              }
              if (ortak[or][0] == 0 && ortak[de][0] != 0) {
                  ortak[or][0] = ortak[de][0];

                  ortak[or][1] = salan;
                   if (maxse) { 
                     for (int zas = 0; zas < obj; zas++) {
                       if (ortak[zas][0] == ortak[de][0]) {
                           ortak[zas][1] = salan;
                       }
                     }
                  }

                  dongu[or][0] = dbj+1;
                  dongu[or][1] = CL;
                  commus = CL+"";
                  limitsay++;
              }
              if (limitli == limitsay) {
                  dbbs = dbb;
                  break;
              }
            }
          }
        }
         
             

       // The code below is produced in order to re-organise FR ids in such a way that they will start with 1 and increase incrementally without any leap 
       // between FR ids and without any BSUs still having no FR id. Thus, a new and unique FR id is assigned to each of those BSUs still having no FR id.

       ArrayList surtas = new ArrayList();
       for (int mum = 0; mum < obj; mum++) {
         surtas.add(ortak[mum][0]);
       }

       HashSet hs = new HashSet();
       hs.addAll(surtas);
       surtas.clear();
       surtas.addAll(hs);
       Collections.sort(surtas);
       int enkuc = (Integer) surtas.get(0);
       enyuk = surtas.size();
       int ilave = 1;

       if (enkuc == 0) {
         ilave = 0;
       }

       int sifirsayar = 0;

       for (int mum = 0; mum < obj; mum++) {
          int pri = Collections.binarySearch(surtas, ortak[mum][0]);
          if (ortak[mum][0] == 0) {
             ortak[mum][0] = enyuk + sifirsayar;
             sifirsayar++;
          }
          else {
             ortak[mum][0] = pri + ilave;
          }
       }

       

		   // In the second stage, the general statistics for all the BSUs and those BSUs having no FR id after the first stage are recorded to the  
		   // attribute table of BSUs by using the information stored in an array (dongu). Three important statistics are recorded to the attribute table; 
           // 1.) NID = new identity number showing the resulting FR that covers the BSU concerned,
		   // 2.) NIT = number of iteration of commuting database when a FR id is assigned to the BSU having no FR id,
		   // 3.) LoC = level of commuting observed between the BSU previously having no FR id and the BSU whose FR id is assigned to the one having no FR id.
	
		   FeatureSchema fs = fc.getFeatureSchema();
		   if (fs.hasAttribute("NID_"+dbis) == false) fs.addAttribute("NID_"+dbis, AttributeType.INTEGER);
		   if (fs.hasAttribute("NIT_"+dbis) == false) fs.addAttribute("NIT_"+dbis, AttributeType.INTEGER);
		   if (fs.hasAttribute("LoC_"+dbis) == false) fs.addAttribute("LoC_"+dbis, AttributeType.DOUBLE);

		   FeatureDataset newDataset = new FeatureDataset(fs);

		   xx = obj;
		   
		   for (Iterator iter = fc.iterator(); iter.hasNext();) {
			  Feature k = (Feature) iter.next();
		      BasicFeature nf = new BasicFeature(fs);
		      for (int i = 0 ; i < (fs.getAttributeCount()-3) ; i++) {
		          nf.setAttribute(i, k.getAttribute(i));
		      }
		      int ind = iliskim.lastIndexOf(k.getAttribute(ase)+"");
		      nf.setAttribute("NID_"+dbis, ortak[ind][0]);
		      nf.setAttribute("NIT_"+dbis, (int)dongu[ind][0]);
		      nf.setAttribute("LoC_"+dbis, dongu[ind][1]);
		      newDataset.add(nf);
		      xx--;
		   }

		   katmanim.setFeatureCollection(newDataset);
	    

			  // Forming FRs according to NID assigned to BSUs

		      Layer layer = katmanim;
			  fc = katmanim.getFeatureCollectionWrapper();
			  FeatureSchema schema = fc.getFeatureSchema();

			  // Create the schema for the output dataset:
			  FeatureSchema FRSchema = new FeatureSchema();
			  FRSchema.addAttribute(schema.getAttributeName(schema.getGeometryIndex()), AttributeType.GEOMETRY);
			  FRSchema.addAttribute("FRID", AttributeType.INTEGER);
			  FRSchema.addAttribute("Area", AttributeType.DOUBLE);
			  
			  // Order features by attribute value in a map:
			  Map<Object, FeatureCollection> map = new HashMap<Object, FeatureCollection>();
			  for (Iterator i = fc.iterator() ; i.hasNext() ; ) {
			    Feature p = (Feature)i.next();
			    Geometry s = p.getGeometry();
			    Object key = p.getAttribute("NID_"+dbis);
			    if (key == null || key.toString().trim().length() == 0) {
			       continue;
			    }
			    else if (!map.containsKey(key)) {
			      FeatureCollection fd = new FeatureDataset(fc.getFeatureSchema());
			      fd.add(p);
			      map.put(key, fd);
			    }
			    else {
			      ((FeatureCollection)map.get(key)).add(p);
			    }
			  }

			  FeatureCollection resFR = new FeatureDataset(FRSchema);
			  ArrayList<Geometry> geomsi = new ArrayList<Geometry>();
			  ArrayList<Double> areas = new ArrayList<Double>();//
			  double totalan = 0;//
		        
			  for (Iterator i = map.keySet().iterator(); i.hasNext();) {
			    Object key = i.next();
			    FeatureCollection fca = (FeatureCollection)map.get(key);
			    if (!key.equals(0)) {
			      if (fca.size() > 0) {
			        ArrayList polygons = new ArrayList();
			        for (Iterator it = fca.iterator(); it.hasNext();) {
			          Feature f = (Feature) it.next();
			          Geometry g = f.getGeometry();
			          polygons.add(g);
			        }
			        Geometry unionim = new UnaryUnionOp(polygons).union();
			        Feature feature = new BasicFeature(FRSchema);
			        feature.setGeometry(unionim);
			        double alani = unionim.getArea()/1000000;//
			        areas.add(alani);//
			        totalan = totalan + alani;
			        feature.setAttribute("FRID", key);
			        feature.setAttribute("Area", Math.round(alani*100000)/100000.0);
			        geomsi.add(unionim);
			        resFR.add(feature);
			      }
			    }
			  }


			  
			  // Creation of the schema for the output data set for MPRs features.

			  FeatureSchema MPRSchema = new FeatureSchema();
			  MPRSchema.addAttribute(schema.getAttributeName(schema.getGeometryIndex()), AttributeType.GEOMETRY);
			  MPRSchema.addAttribute("FRID", AttributeType.INTEGER);

			  FeatureCollection resMPR = new FeatureDataset(MPRSchema);
		      ArrayList<Integer> MPRListim = new ArrayList<Integer>();

			  int mgsayar = 0;
			  for (Iterator iter = resFR.iterator(); iter.hasNext();) {
				Feature f = (Feature) iter.next();
				Geometry geom = f.getGeometry();
			    if(geom instanceof MultiPolygon){ 
			      MPRListim.add((int)f.getAttribute(1));
			      ArrayList parts = GeometryConverter.explodeGeomsIfMultiG(geom);
			      ArrayList tempListim = new ArrayList();
			      for (Iterator ite = parts.iterator(); ite.hasNext();) {
			        Geometry geomic = (Geometry) ite.next();
			        tempListim.add(geomic);
			      }		      
			      for (Iterator it = tempListim.iterator(); it.hasNext();) {
			        Geometry geomic = (Geometry) it.next();
			        Feature feature = new BasicFeature(MPRSchema);
			        feature.setGeometry(geomic);
			        feature.setAttribute("FRID", f.getAttribute(1));
			        resMPR.add(feature);
			      }
			      mgsayar++;
			    }
			  }

  	          Collections.sort(areas);//
			  int aa = resFR.size();
			  int modus = aa % 2;
  	          double minalan = areas.get(0);//
  	          double maxalan = areas.get(areas.size()-1);//
  	          double medalan = areas.get(Math.round(areas.size()/2));//
  	          if (modus == 0) {
  	  	          medalan = (areas.get(Math.round(areas.size()/2)) + areas.get(Math.round((areas.size()/2))-1))/2;//
  	          }
  	          double avealan = totalan/(double)areas.size();
  	          
			  int obje = resMPR.size();
			  dialogi.addLabel("<HTML><STRONG>" + dbis+". iteration results:" + "<HTML><STRONG>");
			  dialogi.addLabel("  total number of FRs formed: "+aa);
			  dialogi.addLabel("  minimum CL (MCL) with threshold: "+commur);
			  dialogi.addLabel("  number of rows read to satisfy CT: "+dbbr);		  
			  dialogi.addLabel("  MCL without threshold: "+commus);
			  dialogi.addLabel("  number of rows read before finishing: "+dbbs);	
			  dialogi.addLabel("  number of multi-polygon regions (MPR): "+mgsayar);
			  dialogi.addLabel("  number of polygons in MPRs: "+obje);
			  
			  if (obje == 0) {
			     dialogi.addLabel("  area of the largest FR: "+Math.round(maxalan*100000)/100000.0);
			     dialogi.addLabel("  area of the smallest FR: "+Math.round(minalan*100000)/100000.0);
			     dialogi.addLabel("  area of the median FR: "+Math.round(medalan*100000)/100000.0);
			     dialogi.addLabel("  average area of a FR: "+Math.round(avealan*100000)/100000.0);
			  }
			  
			  stats.write(aa+";");// total number of FRs formed
			  stats.write(commur+";");// minimum CL (MCL) taken into consideration within the given limits of thresholds
			  stats.write(dbbr+";");// number of rows read to satisfy CT (last line read - Lline)
			  stats.write(commus+";");// MCL for those OB and DB that cannot be assigned to a FR within the given limits of CT (MCL without threshold – MCL_out)
			  stats.write(dbbs+";");// number of rows read in order to assign those BSUs involved in the couples of OB and DB having a CL value below CT to a FR (last line read for minimum CL without threshold – Lline_out)
			  stats.write(mgsayar+";");// number of multi-polygon regions (MPR)
			  stats.write(obje+";");// number of polygons in MPRs (total number of disconnected parts (components) in MPRs)
			  stats.write(Math.round(maxalan*100000)/100000.0+";");// area of the largest FR
			  stats.write(Math.round(minalan*100000)/100000.0+";");// area of the smallest FR
			  stats.write(Math.round(medalan*100000)/100000.0+";");// area of the median FR
			  stats.write(Math.round(avealan*100000)/100000.0+";");// average area of a FR
			  
			  if (obje != 0){
				  stats.write("-;-;-;-;-;-;-;-;-;-;-;-");//
				  stats.write('\n');//
			  }
			  
			  String patiket = "MPRs";
			  if (maxse) patiket = "MPRs with Delimited Area";
			  
			  if (resMPR.isEmpty() == false) {
				  Layer resFRlyr = layerManager.addLayer("Result", katiket+"-"+dbis, resFR);
				  resFRlyr.setVisible(false);
				  resFRlyr.fireAppearanceChanged();
				  
				  Layer resMPRlyr = layerManager.addLayer("Result", patiket+"-"+dbis, resMPR);

				  Map attributeToStyleMap = new HashMap();
			      ColorScheme colorScheme = ColorScheme.create("spectral (ColorBrewer)");		  
				  Map<Object, String> attributeToLabelMap = new HashMap<Object, String>(); 
				  for (int mprs = 0; mprs < mgsayar; mprs++) {
					attributeToStyleMap.put(MPRListim.get(mprs), new BasicStyle(colorScheme.next()));
					attributeToLabelMap.put(MPRListim.get(mprs), MPRListim.get(mprs).toString());
				  }
				  ColorThemingStyle.get(resMPRlyr).setAttributeName("FRID");
				  ColorThemingStyle.get(resMPRlyr).setAttributeValueToBasicStyleMap(attributeToStyleMap);
				  ColorThemingStyle.get(resMPRlyr).setAttributeValueToLabelMap(attributeToLabelMap);
				  ColorThemingStyle.get(resMPRlyr).setEnabled(true);

			  }

			  
			  // If obje = 0, it means that there is no multi-polygon region (MPR). 
			  // Since at this stage we have regular FRs, the required statistics can be calculated for them as it is done below:
			  
			  if (obje == 0) {
				  
				   List ilis = new ArrayList();
				   for (Iterator iter = resFR.iterator(); iter.hasNext();) {
						Feature f = (Feature) iter.next();
						ilis.add(f.getAttribute("FRID"));
				   }	
				   FeatureSchema fsc = (FeatureSchema) resFR.getFeatureSchema().clone();
				   fsc.addAttribute("Resident", AttributeType.INTEGER);
				   fsc.addAttribute("Working", AttributeType.INTEGER);
				   fsc.addAttribute("LSC", AttributeType.DOUBLE);
				   FeatureDataset fscDataset = new FeatureDataset(fsc);
				    
						  
				   progressDialog.report("Creation of Flow Database for FRs"); 
				    		
				   
				        // Construction of the database showing the flows between the resulting FRs.

					    int[][] FRS = new int[aa][2];
					    for (int mum = 0; mum < obj; mum++) {
					         int osi = ortak[mum][0];
					         FRS[osi-1][0] = FRS[osi-1][0] + ortakc[mum][0];
					         FRS[osi-1][1] = FRS[osi-1][1] + ortakc[mum][1];
					    }

					    int[][] r2n = new int[aa][aa];

					    for (int dbb = 0; dbb < rgs; dbb++) {
					      String[] kayit = verim.get(dbb);
					      String origini = kayit[0];
					      int or = iliskim.lastIndexOf(origini);
					      int oo = ortak[or][0];
					      String destini = kayit[1];
					      int de = iliskim.lastIndexOf(destini);
					      int ds = ortak[de][0];
					      r2n[oo-1][ds-1] = r2n[oo-1][ds-1] + Integer.parseInt(kayit[2], 10);
					    }
						    
					  double[][] orani = new double[aa*aa][4];
					  ArrayList<Double> tripo = new ArrayList<Double>();
					  int[] mutres = new int[aa]; // for statistics
					  int[] mutwrk = new int[aa]; // for statistics

		 		      List setim = resFR.getFeatures();
					  double totcomm = 0;
					  int totresi = 0; // for statistics
					  int totwork = 0; // for statistics
					  
					  for (int oo = 0; oo < aa; oo++) {
						int re = ilis.lastIndexOf(oo+1);
						Feature frf = (Feature) setim.get(re);
						BasicFeature nfc = new BasicFeature(fsc);
					    for (int i = 0 ; i < (fsc.getAttributeCount()-3) ; i++) {
					    	nfc.setAttribute(i, frf.getAttribute(i));
					    }
					    for (int ds = 0; ds < aa; ds++) {
					         orani[oo*aa+ds][0] = (double) Math.round(r2n[oo][ds]*10000000.0/FRS[oo][0])/100000;
					         orani[oo*aa+ds][1] = (double) (oo+1);
					         orani[oo*aa+ds][2] = (double) (ds+1);
					         orani[oo*aa+ds][3] = (double) r2n[oo][ds];
					         if(oo == ds){
					        	 nfc.setAttribute("Resident", FRS[oo][0]);
					        	 nfc.setAttribute("Working", FRS[oo][1]);
					        	 nfc.setAttribute("LSC", orani[oo*aa+ds][0]);
					        	 totcomm = totcomm + orani[oo*aa+ds][0];
					        	 tripo.add(orani[oo*aa+ds][0]);
					        	 totresi = totresi + FRS[oo][0]; // for statistics
					        	 totwork = totwork + FRS[oo][1]; // for statistics
								 mutres[oo] = FRS[oo][0]; // for statistics
								 mutwrk[oo] = FRS[oo][1]; // for statistics
					         }
					    }
					    fscDataset.add(nfc); 
					  }
					
					  
		  	          Collections.sort(tripo);
		  	          double mincomm = tripo.get(0);
		  	          double maxcomm = tripo.get(tripo.size()-1);
		  	          double medcomm = tripo.get(Math.round(tripo.size()/2));
		  	          if (modus == 0) {
			  	          medcomm = (tripo.get(tripo.size()/2) + tripo.get((tripo.size()/2)-1))/2;
		  	          }
					  dialogi.addLabel("<HTML><STRONG>" + "level of self-containment (LSC) statistics:" + "<HTML><STRONG>");
					  dialogi.addLabel("  maximum LSC observed for a FR: "+Math.round(maxcomm*100000)/100000.0);
					  dialogi.addLabel("  minimum LSC observed for a FR: "+Math.round(mincomm*100000)/100000.0);
					  dialogi.addLabel("  median LSC observed for a FR : "+Math.round(medcomm*100000)/100000.0);
					  dialogi.addLabel("  average LSC observed for a FR: "+Math.round((totcomm/aa)*100000)/100000.0);
					  
			          // for statistics regarding residents and working population
					  Arrays.sort(mutres);
					  Arrays.sort(mutwrk);
		  	          int minres = mutres[0];
		  	          int maxres = mutres[aa-1];
		  	          int medres = mutres[Math.round(aa/2)];
		  	          if (modus == 0) {
		  	        	  medres = (mutres[aa/2] + mutres[(aa/2)-1])/2;
		  	          }
		  	          int minwrk = mutwrk[0];
		  	          int maxwrk = mutwrk[aa-1];
		  	          int medwrk = mutwrk[Math.round(aa/2)];
		  	          if (modus == 0) {
		  	        	  medwrk = (mutwrk[aa/2] + mutwrk[(aa/2)-1])/2;
		  	          }	
		  	          
					  stats.write(Math.round(maxcomm*100000)/100000.0+";"); // maximum level of self-containment (LSC) observed for a FR
					  stats.write(Math.round(mincomm*100000)/100000.0+";"); // minimum LSC observed for a FR
					  stats.write(Math.round(medcomm*100000)/100000.0+";"); // median LSC observed for a FR
					  stats.write(Math.round((totcomm/aa)*100000)/100000.0+";"); // the average LSC observed for a FR 
					  stats.write(maxres+";"); // maximum number of people residing in a FR
					  stats.write(minres+";"); // minimum number of people residing in a FR
					  stats.write(medres+";"); // median number of people residing in a FR
					  stats.write(Math.round(totresi/aa)+";"); // average number of people residing in a FR
					  stats.write(maxwrk+";"); // maximum number of people working in a FR
					  stats.write(minwrk+";"); // minimum number of people working in a FR
					  stats.write(medwrk+";"); // median number of people working in a FR
					  stats.write(Math.round(totwork/aa)+""); // average number of people working in a FR
					  stats.write('\n');
					  stats.close();
					  
					  
				      Layer FRlyr = layerManager.addLayer("Result", katiket, fscDataset);
					  LayerStyleUtil.setLinearStyle(FRlyr, Color.red, 4, 0);
					  FRlyr.fireAppearanceChanged();

					  
					  if (kosje) {
						  
					  Comparator<double[]> doucomp = new Comparator<double[]>() {
						  public int compare(double[] a, double[] b) {
							  if(a[0] == b[0]) {
					             return Double.compare(b[3], a[3]);
					          }
					          return Double.compare(b[0], a[0]);
					      }
					  };

					  Arrays.sort(orani, doucomp); 

					  DecimalFormat df = new DecimalFormat("#.#####");
					  DecimalFormatSymbols custom = new DecimalFormatSymbols();
					  custom.setDecimalSeparator('.');
					  df.setDecimalFormatSymbols(custom);
					    
					  if (ext.equals("csv") || ext.equals("txt")) {
					    FileWriter EdgeList = new FileWriter(aloc+"."+ext);
					    EdgeList.append("OB"+sepo+"DB"+sepo+"AC"+sepo+"RESIDE"+sepo+"WORK"+sepo+"CL");
					    EdgeList.append('\n');

					    int kaynak = 0;

					    for (int oo = 0; oo < aa; oo++) {
					      for (int ds = 0; ds < aa; ds++) {
					        if (orani[oo*aa+ds][3] != 0) {
					           kaynak = (int) orani[oo*aa+ds][1];
					           EdgeList.append((int)orani[oo*aa+ds][1]+sepo+(int)orani[oo*aa+ds][2]);
					           EdgeList.append(sepo);
					           EdgeList.append((int)orani[oo*aa+ds][3]+"");
					           EdgeList.append(sepo);
					           EdgeList.append(FRS[kaynak-1][0]+"");
					           EdgeList.append(sepo);
					           EdgeList.append(FRS[kaynak-1][1]+"");
					           EdgeList.append(sepo);
					           EdgeList.append(df.format(orani[oo*aa+ds][0])+"");
					           EdgeList.append('\n');
					        }
					      }
					    }
					    EdgeList.close();
					  }
					  
					  if (ext.equals("dbf")) {
						    List fields = new ArrayList();
						    fields.add(new Field("OB", Type.NUMBER, 16));
						    fields.add(new Field("DB", Type.NUMBER, 16));
						    fields.add(new Field("AC", Type.NUMBER, 16));
						    fields.add(new Field("RESIDE", Type.NUMBER, 16));
						    fields.add(new Field("WORK", Type.NUMBER, 16));
						    fields.add(new Field("CL", Type.NUMBER, 16, 5));

						    Table table = new Table(new File(aloc+".dbf"), Version.DBASE_3, fields);
						    table.open(IfNonExistent.CREATE);

						    int kaynak = 0;

						    for (int oo = 0; oo < aa; oo++) {
						      for (int ds = 0; ds < aa; ds++) {
						        if (orani[oo*aa+ds][3] != 0) {
						           kaynak = (int) orani[oo*aa+ds][1];
						           Map mapi = new HashMap();
						           mapi.put("OB", new NumberValue(orani[oo*aa+ds][1]));
						           mapi.put("DB", new NumberValue(orani[oo*aa+ds][2]));
						           mapi.put("AC", new NumberValue(orani[oo*aa+ds][3]));
						           mapi.put("RESIDE", new NumberValue(FRS[kaynak-1][0]));
						           mapi.put("WORK", new NumberValue(FRS[kaynak-1][1]));
						           mapi.put("CL", new NumberValue(orani[oo*aa+ds][0]));
						           Record record = new Record(mapi);
						           table.addRecord(record);
						        }
						      }
						    }
						    table.close();
						}
		             }
				    
				    break;
			  }

			  
			  stats.write(GT+";");
			  stats.write(CT+";");
			  stats.write((dbis+1)+";");
			  stats.write(atlim+";");

			  
			  // In order to prevent the formation of multi-polygon regions (MPR), the code given below is created. The general statistics for those MPRs are
			  // also recorded to the attribute table of BSUs. Two important statistics are created for those BSUs involved in a MPR;
			  // 1.) PFID = Previous FR id assigned to those BSUs involved in a MPR in the previous iteration of FR formation.
			  // 2.) CID = Component id of the BSU involved in the MPR concerned. MRP may involve several components (disconnected parts). CID show the component encapsulating the BSU concerned.
			  
			  int[] atrimi = new int[obje];

			  FeatureSchema fl = fc.getFeatureSchema();
		      if (fl.hasAttribute("PFID_"+dbis) == false) fl.addAttribute("PFID_"+dbis, AttributeType.INTEGER);
		      if (fl.hasAttribute("CID_"+dbis) == false) fl.addAttribute("CID_"+dbis, AttributeType.INTEGER);

			  FeatureDataset fewDataset = new FeatureDataset(fl);

			  int sx = 0;
			  for (Iterator iter = fc.iterator(); iter.hasNext();) {
				Feature g = (Feature) iter.next();
			    BasicFeature ng = new BasicFeature(fl);
			    for (int i = 0 ; i < (fl.getAttributeCount()-2) ; i++) {
			      ng.setAttribute(i, g.getAttribute(i));
			    }
			    Geometry dd = g.getGeometry();
			    Point sabah = dd.getInteriorPoint();
			    ng.setAttribute("PFID_"+dbis, 0);
			    ng.setAttribute("CID_"+dbis, 0);

			    int xu = 1;
				for (Iterator oter = resMPR.iterator(); oter.hasNext();) {
				  Feature f = (Feature) oter.next();
			      Geometry cc = f.getGeometry();
			      boolean ser = cc.contains(sabah);
			      if (ser) {
			        ng.setAttribute("PFID_"+dbis, f.getAttribute(1));
			        ng.setAttribute("CID_"+dbis, xu);
			        ortakm[sx][0] = (Integer) f.getAttribute(1);
			        ortakm[sx][1] = xu;
			        atrimi[xu-1] = atrimi[xu-1]+1;
			        break;
			      }
			      xu++;
			    }
			    fewDataset.add(ng);
			    sx++;
			  }

			  katmanim.setFeatureCollection(fewDataset);
			  layer.setFeatureCollection(fewDataset);
			  fc = layer.getFeatureCollectionWrapper();
			  
			  
    }	  

    
                    } catch (Exception e) {

                    } finally {
                        progressDialog.setVisible(false);
                        progressDialog.dispose();
                    }
                }
            }).start();
        }
    });

    GUIUtil.centreOnWindow(progressDialog);
    progressDialog.setVisible(true);
    
    GUIUtil.centreOnWindow(dialogi); 
	dialogi.setVisible(true);
	

	
 return;

	}
}