
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

	import com.vividsolutions.jump.workbench.plugin.Extension;
	import com.vividsolutions.jump.workbench.plugin.PlugInContext;

	/**
	 * 
	 *  - this class loads the PlugIn into OpenJUMP
	 *
	 *  - class has to be called "Extension" on the end of classname
	 *    to use the PlugIn in OpenJUMP 
	 */
	public class FunctionalRegionsExtension extends Extension{


		 public String getName() {
		     return "Functional Regionalisation (Burak Beyhan)"; // set the name / label of plugin for tooltips ...
		 } 
		 
		 public String getVersion() {
		     return "1.0.0 (2019-10-03)";
		 }
		 

		/**
		 * calls PlugIn using class method xplugin.initialize() 
		 */
		public void configure(PlugInContext context) throws Exception{
			new FunctionalRegionsPlugIn().initialize(context);
		}
		
	}
