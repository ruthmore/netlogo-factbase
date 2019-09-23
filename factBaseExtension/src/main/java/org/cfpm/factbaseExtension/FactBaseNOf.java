/*
 * FactBaseNOf.java
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

import org.nlogo.api.Argument;
import org.nlogo.api.Context;
import org.nlogo.api.DefaultReporter;
import org.nlogo.api.Dump;
import org.nlogo.api.ExtensionException;
import org.nlogo.api.LogoException;
import org.nlogo.api.LogoList;
import org.nlogo.api.LogoListBuilder;
import org.nlogo.api.Syntax;

/** This class implements the "n-of" primitive for the factbase extension. N-of retrieves n
 * random facts satisfying the specified condition from the given fact base. It is an error
 * if n is greater than the number of such facts.
 * 
 * To call this primitive from NetLogo, use <code>factbase:n-of <i>fact-base</i> <i>condition-task</i> <i>condition-field-list</i> <i>number</i></code>
 *
 * @author Ruth Meyer
 *
 */
public class FactBaseNOf extends DefaultReporter {

	// expects a reference to the factbase, a condition (as ReporterTask and List of field names) and a number n, 
	// returns a list containing n random facts satisfying that condition 
	// generates an error if n > number of possible facts
	/** The n-of primitive expects a fact base, a condition (specified as a reporter task and a list of corresponding fields) 
	 * and an integer number (number of facts wanted) as inputs and returns a list of n facts satisfying the given condition. 
	 */
	public Syntax getSyntax() {
		return Syntax.reporterSyntax(new int[]{Syntax.WildcardType(), Syntax.ReporterTaskType(), Syntax.ListType(), Syntax.NumberType()}, Syntax.ListType());
	}
	
	/** Returns n random facts satisfying the given condition from the specified fact base. The first argument {@link args[0]} has
	 * to be a fact base, the second argument {@link args[1]} has to be a reporter task, the third argument has to be a list of
	 * field names corresponding to the formal arguments used in the task, and the fourth argument has to be an integer number.
	 * Generates an error if n > number of possible facts.
	 * 
	 * @param args the arguments to this call of n-of
	 * @param context the NetLogo context
	 * @return list of n random facts satisfying the given condition
	 * @throw ExtensionException if any of the arguments are invalid
	 * @see org.nlogo.api.Reporter#report(org.nlogo.api.Argument[], org.nlogo.api.Context)
	 */
	@Override
	public Object report(Argument[] args, Context context) throws ExtensionException, LogoException 
	{
		// determine parameter n from args (args[0] = factbase, args[1] = reporter task. args[2] = list of field names, args[3] = n
		Object arg3 = args[3].get();
		if (! ((arg3 instanceof Double) || (arg3 instanceof Integer))) {
			throw new ExtensionException ("not a number: " + Dump.logoObject(arg3));
		}
		int n = ((Double)arg3).intValue();
		// all the work is done in the retrieval class
		// easy way to determine n-of: retrieve ALL facts that satisfy the condition, then pick n random ones
		Retrieval r = new Retrieval(args, context);
		LogoList result = r.retrieveAll();
		// check if there are at least n facts in the result
		if (result.isEmpty() || result.size() < n) {
			throw new ExtensionException("cannot pick " + n + " facts from " + result.size() + " facts satisfying the condition; the given number n = " + n + " is too large");
		}
		// now pick n random facts from result without repeats
		// -- done as picking (and removing) from list of indices since removing from a LogoList gives an UnsupportedOperationException
		ArrayList<Integer> possible = new ArrayList<Integer>();
		ArrayList<Integer> chosen = new ArrayList<Integer>();
		for (int i = 0; i < result.size(); i++) {
			possible.add(i);
		}
		FactBaseExtension.writeToNetLogo("possible = " + printArrayList(possible), false, context);
		for (int i = 0; i < n; i++) {
			int index = FactBaseExtension.rng.nextInt(possible.size());
			chosen.add(possible.get(index));
			possible.remove(index);
			FactBaseExtension.writeToNetLogo("element " + i + ": " + index, false, context);
			FactBaseExtension.writeToNetLogo("chosen = " + printArrayList(chosen), false, context);
			FactBaseExtension.writeToNetLogo("possible = " + printArrayList(possible), false, context);
		}
		LogoListBuilder picked = new LogoListBuilder();
		for (Iterator<Integer> it = chosen.iterator(); it.hasNext(); ) {
			picked.add(result.get(it.next()));
		}
		return picked.toLogoList();
	}

	/** Helper method: returns a string representation of the given array list of integers.
	 * 
	 * @param nums an array list of integers
	 * @return the array list as a string
	 */
	private String printArrayList(ArrayList<Integer> nums) {
		StringBuilder buff = new StringBuilder("[");
		for (Object i : nums) {
			buff.append(" ");
			buff.append(i);
		}
		buff.append(" ]");
		return buff.toString();
	}
	
	
}
