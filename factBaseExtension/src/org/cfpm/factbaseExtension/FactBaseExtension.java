/*
 * FactBaseExtension.java
 * 
 * Copyright (c) 2016 Centre for Policy Modelling 
 * 
 * This file is part of Factbase-NetLogoExtension.
 * 
 * Factbase-NetLogoExtension is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 * 
 * Factbase-NetLogoExtension is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General
 * Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * Factbase-NetLogo. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Contact information: Ruth Meyer, Centre for Policy Modelling,
 * Manchester Metropolitan University Business School, Manchester, M15 6BH, UK.
 * ruth@cfpm.org
 * 
 */

package org.cfpm.factbaseExtension;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.WeakHashMap;

import org.nlogo.api.Context;
import org.nlogo.api.DefaultClassManager;
import org.nlogo.api.ExtensionException;
import org.nlogo.api.LogoException;
import org.nlogo.api.LogoList;
import org.nlogo.api.LogoListBuilder;
import org.nlogo.api.PrimitiveManager;
import org.nlogo.nvm.ExtensionContext;
import org.nlogo.nvm.Workspace.OutputDestination;

/** 
 * <!-- FactBaseExtension -->
 * 
 * This is the main class of the factbase extension. It defines all primitives for use in NetLogo.
 *  It also provides some useful functionality for interfacing NetLogo and Java, to be used by the
 *  classes implementing the different primitives.
 *  
 * @author Ruth Meyer
 *
 */
public class FactBaseExtension extends DefaultClassManager {
	
	// keep track of all instantiated factbases
	/** Counter for the next available ID for a new factbase */
	static protected int next = 0;
	/** A hash table of all instantiated factbases */
	static protected WeakHashMap<Integer, FactBase> bases = new WeakHashMap<Integer, FactBase>();
	
	/** Flag to toggle output to NetLogo (if set to true, output will take place, if set to false, output will be ignored). 
	 * In any deployed version of the extension the flag is set to false.
	 */ 
	static private boolean outputToNetlogo = false;
	
	/** A random number generator to be used in primitives classes needing to pick arbitrary facts like one-of or n-of.
	 * 
	 */
	static protected Random rng = new Random(); // ### to be replaced by MersenneTwister?
	

	/** Specifies the primitives available for the factbase extension.
	 * This method is called each time a model using the extension is compiled in NetLogo.
	 * 
	 * @see org.nlogo.api.DefaultClassManager#load(org.nlogo.api.PrimitiveManager)
	 */
	@Override
	public void load(PrimitiveManager primManager) throws ExtensionException {
		primManager.addPrimitive("create", new FactBaseCreate());
		primManager.addPrimitive("assert", new FactBaseAssert());
		primManager.addPrimitive("assert-all", new FactBaseAssertAll());
		primManager.addPrimitive("retrieve", new FactBaseRetrieve());
		primManager.addPrimitive("retrieve-to", new FactBaseRetrieveTo());
		primManager.addPrimitive("size", new FactBaseSize());
		primManager.addPrimitive("get", new FactBaseGet());
		primManager.addPrimitive("to-list", new FactBaseToList());
		primManager.addPrimitive("from-list", new FactBaseFromList());
		primManager.addPrimitive("retract", new FactBaseRetract());
		primManager.addPrimitive("retract-all", new FactBaseRetractAll());
		primManager.addPrimitive("member?", new FactBaseMember());
		primManager.addPrimitive("exists?", new FactBaseExists());
		primManager.addPrimitive("one-of", new FactBaseOneOf());
		primManager.addPrimitive("n-of", new FactBaseNOf());
		primManager.addPrimitive("r-assert", new FactBaseRAssert());
		//primManager.addPrimitive("min-one-of", new FactBaseMinOneOf());
	}
	
	/** Turns a fact (ArrayList of objects) into a NetLogo list (LogoList).
	 * 
	 * @param fact a list of objects (usually as an instance of ArrayList<Object>)
	 * @return a list of the same objects as a LogoList
	 * @see org.nlogo.api.LogoList
	 */
	protected static LogoList toLogoList(List<Object> fact) {
		LogoListBuilder list = new LogoListBuilder();
		for (Iterator<Object> it = fact.iterator(); it.hasNext(); ) {
			list.add(it.next());
		}
		return list.toLogoList();
	}
	
	/** Turns a LogoList (a NetLogo list object) into an ArrayList<Object>, which is the data type used to represent facts
	 * in the factbase extension.
	 * 
	 * @param llist a list of objects as a LogoList
	 * @return a list of the same objects as an ArrayList
	 */
	protected static List<Object> toFact(LogoList llist) {
		List<Object> fact = new ArrayList<Object>();
		for (Iterator<Object> it = llist.iterator(); it.hasNext(); ) {
			fact.add(it.next());
		}
		return fact;
	}

	/** Writes the given text to the NetLogo command center if the internal flag {@link #outputToNetlogo} is set to true.
	 * This method is solely intended for debugging purposes while developing the extension. 
	 * 
	 * @param mssg the text that is to be written to the command center
	 * @param toOutputArea should be set to false to achieve output to the command center; if set to true, 
	 * 					   output will go to the output area (if there is one), otherwise to the command center
	 * @param context	the NetLogo context
	 * @throws ExtensionException if writing fails for some reason
	 */
	protected static void writeToNetLogo(String mssg, Boolean toOutputArea, Context context) 
			throws ExtensionException
		{ 
				/* Instructions on writing to the command center as related by Seth Tissue: 
				* "Take your api.ExtensionContext, cast it to nvm.ExtensionContext, 
				* and then call the workspace() method to get a nvm.Workspace 
				* object, which has an outputObject() method declared as follows: 
				* void outputObject(Object object, Object owner, boolean addNewline, boolean readable, OutputDestination destination) 
				 * throws LogoException;
				 * 
				 * object: can be any valid NetLogo value; 
				* owner: just pass null; 
				* addNewline: whether to add a newline character afterwards; 
				* readable: "false" like print or "true" like write, controls whether 
				* the output is suitable for use with file-read and read-from-string 
				* (so e.g. whether strings are printed with double quotes); 
				* OutputDestination is an enum defined inside nvm.Workspace with 
				* three possible values: NORMAL, OUTPUT_AREA, FILE. NORMAL means 
				* to the command center, OUTPUT_AREA means to the output area if 
				* there is one otherwise to the command center, FILE is not 
				* relevant here. */ 

			if (outputToNetlogo) {
				ExtensionContext extcontext = (ExtensionContext) context; 
				try {
					extcontext.workspace().outputObject(mssg, null, true, true,
							(toOutputArea) ? OutputDestination.OUTPUT_AREA : OutputDestination.NORMAL); 
				} 
				catch (LogoException e) {
					throw new ExtensionException(e); 
				} 
			}
		}	
	
}


