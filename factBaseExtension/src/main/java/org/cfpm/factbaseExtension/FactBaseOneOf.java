/*
 * FactBaseOneOf.java
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

import org.nlogo.api.Argument;
import org.nlogo.api.Context;
import org.nlogo.api.ExtensionException;
import org.nlogo.api.LogoException;
import org.nlogo.api.Reporter;
import org.nlogo.core.LogoList;
import org.nlogo.core.Syntax;
import org.nlogo.core.SyntaxJ;

/** This class implements the "one-of" primitive for the factbase extension. One-of retrieves a
 * random fact satisfying the specified condition from the given fact base. It generates an error
 * if no such fact exists.
 * 
 * To call this primitive from NetLogo, use <code>factbase:one-of <i>fact-base</i> <i>condition-task</i> <i>condition-field-list</i></code>
 *
 * @author Ruth Meyer
 *
 */
public class FactBaseOneOf implements Reporter {

	// expects a reference to the factbase and a condition (as ReporterTask and List of field names), returns a random fact satisfying that condition 
	// generates an error if there is no such fact in the factbase
	/** The one-of primitive expects a fact base and a condition (specified as a reporter task and a list of corresponding fields) 
	 * as inputs and returns an arbitrary fact satisfying the given condition. 
	 */
	public Syntax getSyntax() {
		return SyntaxJ.reporterSyntax(new int[]{Syntax.WildcardType(), Syntax.ReporterType(), Syntax.ListType()}, Syntax.ListType());
	}
	
	/** Returns a random fact satisfying the given condition from the specified fact base. The first argument {@code args[0]} has
	 * to be a fact base, the second argument {@code args[1]} has to be a reporter task and the third argument has to be a list of
	 * field names corresponding to the formal arguments used in the task.
	 * Generates an error if no such fact exists or any of the arguments are invalid.
	 * 
	 * @param args the arguments to this call of one-of
	 * @param context the NetLogo context
	 * @return one random fact satisfying the given condition
	 * @throws ExtensionException if any of the arguments are invalid
	 * @see org.nlogo.api.Reporter#report(org.nlogo.api.Argument[], org.nlogo.api.Context)
	 */
	@Override
	public Object report(Argument[] args, Context context) throws ExtensionException, LogoException 
	{
		// all the work is done in the retrieval class
		// easy way to determine one-of: retrieve ALL facts that satisfy the condition, then pick a random one
		// ### this might prove too slow, then we need to first pick a random number N and then try to retrieve the Nth fact that satisfies the condition
		Retrieval r = new Retrieval(args, context);
		LogoList result = r.retrieveAll();
		if (! result.isEmpty()) {
			int n = FactBaseExtension.rng.nextInt(result.size());
			return result.get(n);
		}
		// result is empty --> throw an exception
		throw new ExtensionException("there are no facts satisfying the given condition");
	}

}
