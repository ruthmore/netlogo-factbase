/*
 * Retrieval.java
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

import org.nlogo.nvm.AnonymousReporter;
import org.nlogo.api.Argument;
import org.nlogo.api.Context;
import org.nlogo.api.Dump;
import org.nlogo.api.ExtensionException;
import org.nlogo.api.LogoException;
import org.nlogo.api.LogoListBuilder;
import org.nlogo.core.LogoList;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** This class provides the necessary functionality for retrieval from a fact base. It is used by several of the primitives
 * (retrieve, retrieve-to, exists?, retract-all, one-of, n-of).
 * 
 * @author Ruth Meyer
 *
 */
public class Retrieval {

	/** The fact base to be used. */
	FactBase fb;
	/** The task specifying the retrieval condition. */
	AnonymousReporter task;
	/** The list of fields corresponding to the formal parameters of the task. */
	LogoList fields;
	/** The indices of the fields, stored for easy access later */
	int[] fIndices;
	/** The NetLogo context, needed to execute the task */
	Context context;
	/** Indices of the output fields (if specified) */
	int[] outFIndices;
	
	/** Constructor of the retrieval class. It is passed the arguments and the context directly from the primitive calling it,
	 * then checks and stores these arguments plus the context for later use in the actual retrieval methods.
	 * 
	 * @param args the arguments to the primitive calling this constructor
	 * @param context the context of the primitive calling this constructor
	 * @throws ExtensionException an exception is generated if one of the arguments is incorrect
	 * @throws LogoException
	 */
	public Retrieval(Argument[] args, Context context) throws ExtensionException, LogoException {
		// first argument needs to be a factbase
		Object arg0 = args[0].get();
		if (! (arg0 instanceof FactBase)) {
	        throw new ExtensionException ("not a factbase: " + Dump.logoObject(arg0));
		}
		fb = (FactBase)arg0;
		// second argument should be a reporter task
		Object arg1 = args[1].get();
		if (! (arg1 instanceof AnonymousReporter)) {
			throw new ExtensionException ("not a reporter task: " + Dump.logoObject(arg1));
		}
		task = (AnonymousReporter)arg1;
		// third argument is a list of which fields to associate with which formal input to the task
		Object arg2 = args[2].get();
		if (! (arg2 instanceof LogoList)) {
			throw new ExtensionException ("not a list: " + Dump.logoObject(arg2));
		}
		fields = (LogoList)arg2;
		// check that there's a field for every formal argument to the task
		if (task.formals().length != fields.size()) {
			throw new ExtensionException("the condition task has " + task.formals().length + " arguments but there are " + fields.size() + " fields specified to match them");
		}
		// determine indices of fields
		fIndices = getFieldIndices(fields);
		FactBaseExtension.writeToNetLogo("fieldIndices = " + printArray(fIndices), false, context);
		// store the context
		this.context = context;
		// if there is a fourth argument, that's either the number for n-of or the list of field names specifying the output format
		if (args.length >= 4) {
			Object arg3 = args[3].get();
			if (arg3 instanceof Number) {
				// it's the number for n-of --> do nothing here
			}
			else {
				if (! (arg3 instanceof LogoList)) {
					throw new ExtensionException ("not a list: " + Dump.logoObject(arg3));
				}
				LogoList outFields = (LogoList)arg3;
				outFIndices = getFieldIndices(outFields);
				FactBaseExtension.writeToNetLogo("outFieldIndices = " + printArray(outFIndices), false, context);
			}
		}
	}
	
	/** Finds all facts that satisfy the condition as specified in {@link #task} and {@link #fields}.
	 * 
	 * @return a list of all facts satisfying the condition
	 * @throws ExtensionException
	 * @throws LogoException
	 */
	public LogoList retrieveAll() throws ExtensionException, LogoException {
		// now for every fact (specifically: every value of the defined fields) we have to run the reporter task
		// if it evaluates to TRUE, the fact has to be stored in the results list
		LogoListBuilder results = new LogoListBuilder();
		for (int i = 0; i < fb.size(); i++) {
			// need to skip deleted entries
			if (!fb.isRetracted(i)) {
				LogoList fact = fb.retrieveFact(i);
				FactBaseExtension.writeToNetLogo("checking fact: " + fb.printFact(fact), false, context);
				Object[] values = getValuesOf(fact, fIndices);
				FactBaseExtension.writeToNetLogo("field values are: " + printArray(values), false, context);
				// run the reporter task
				Object isValidFact = task.report(context, values);
				FactBaseExtension.writeToNetLogo("task result is: " + isValidFact, false, context);
				if (isValidFact != null && (Boolean)isValidFact) {
					results.add(filter(fact));
				}
			}
		}
		return results.toLogoList();			
	}
	
	
	/** Retains only the specified output fields (stored as indices {@link #outFIndices}) from the given fact.
	 * 
	 * @param fact the fact to be filtered
	 * @return the filtered fact, that means only those values of the given fact that correspond to the specified output fields
	 * @throws ExtensionException an exception might occur during writing to NetLogo (in debugging phase)
	 */
	private LogoList filter(LogoList fact) throws ExtensionException {
		// check if output "filter" applies, i.e. if outFIndices is set
		if (outFIndices == null) {
			// no filter -> do nothing
			return fact;
		}
		// apply the filter
		ArrayList<Object> filteredFact = new ArrayList<Object>();
		for (int i : outFIndices) {
			filteredFact.add(fact.get(i));
		}
		FactBaseExtension.writeToNetLogo("filtered fact is: " + printArrayList(filteredFact), false, context);
		return LogoList.fromJava(filteredFact);
	}

	/** Finds the first fact that satisfies the condition as specified in {@link #task} and {@link #fields}.
	 * 
	 * @return the first fact found satisfying the specified condition or null if there is no such fact
	 * @throws ExtensionException
	 * @throws LogoException
	 */
	public LogoList retrieveFirst() throws ExtensionException, LogoException {
		// now for every fact (specifically: every value of the defined fields) we have to run the reporter task
		// until it evaluates to TRUE, then we can abort the search and return the found fact
		// otherwise we have to keep searching until we can return null when nothing matching is found
		int i = 0;
		LogoList firstFact = null;
		while (firstFact == null && i < fb.size()) {
			// need to skip deleted entries
			if (!fb.isRetracted(i)) {
				LogoList fact = fb.retrieveFact(i);
				FactBaseExtension.writeToNetLogo("checking fact: " + fb.printFact(fact), false, context);
				Object[] values = getValuesOf(fact, fIndices);
				FactBaseExtension.writeToNetLogo("field values are: " + printArray(values), false, context);
				// run the reporter task
				Object isValidFact = task.report(context, values);
				FactBaseExtension.writeToNetLogo("task result is: " + isValidFact, false, context);
				if (isValidFact != null && (Boolean)isValidFact) {
					firstFact = fact;
				}
			}
			i++;						
		}
		return firstFact;
	}
	
	/** Helper method: Retrieves the values of the specified fields of the given fact.
	 * 
	 * @param fact the given fact
	 * @param fieldIndices the fields to be used (given as field indices)
	 * @return the values corresponding to the specified fields as an array
	 */
	private Object[] getValuesOf(LogoList fact, int[] fieldIndices) {
		// returns an object array ready to stick into a reporter task
		Object[] values = new Object[fieldIndices.length];
		for (int i = 0; i < fieldIndices.length; i++) {
			values[i] = fact.get(fieldIndices[i]);
		}
		return values;
	}

	/** Helper method: Determines the indices corresponding to the given field names.
	 * 
	 * @param fields a list of field names
	 * @return a list of corresponding field indices as an array
	 * @throws ExtensionException if a field name is invalid
	 */
	private int[] getFieldIndices(LogoList fields) throws ExtensionException {
		int[] ix = new int[fields.size()];
		int j = 0;
		for (Iterator<Object> fi = fields.javaIterator(); fi.hasNext(); ) {
			String fName = fi.next().toString();
			int i = fb.getFieldIndex(fName);
			if (i < 0) {
				throw new ExtensionException(fName + " is not defined as a field in the factbase " + fb.toString());
			}
			ix[j] = i;
			j++;
		}
		return ix;
	}
	
	
	/** Helper method: returns a string representation of the given array of integers.
	 * 
	 * @param nums
	 * @return
	 */
	private String printArray(int[] nums) {
		StringBuilder buff = new StringBuilder("[");
		for (int i : nums) {
			buff.append(" ");
			buff.append(i);
		}
		buff.append(" ]");
		return buff.toString();
	}

	/** Helper method: returns a string representation of the given array of objects.
	 * 
	 * @param objs
	 * @return
	 */
	private String printArray(Object[] objs) {
		StringBuilder buff = new StringBuilder("[");
		for (Object i : objs) {
			buff.append(" ");
			buff.append(i.toString());
		}
		buff.append(" ]");
		return buff.toString();
	}

	/** Helper method: returns a string representation of the given array list of objects.
	 * 
	 * @param objs
	 * @return
	 */
	private String printArrayList(ArrayList<Object> objs) {
		StringBuilder buff = new StringBuilder("[");
		for (Object i : objs) {
			buff.append(" ");
			buff.append(i);
		}
		buff.append(" ]");
		return buff.toString();
	}
	
	/** Returns the fact base.
	 * 
	 * @return the fact base this retrieval object works with
	 */
	public FactBase getFactBase() {
		return fb;
	}
	
}

