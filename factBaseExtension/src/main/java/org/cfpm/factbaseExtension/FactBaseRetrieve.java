/*
 * FactBaseRetrieve.java
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
import org.nlogo.core.Syntax;
import org.nlogo.core.SyntaxJ;

/** This class implements the "retrieve" primitive for the factbase extension. Retrieve uses a ReporterTask and a List of field names as the condition
 * that facts have to satisfy to be included in the result. If no such facts exist in the specified fact base, an empty list is returned.
 * 
 * To call this primitive from NetLogo, use <code>factbase:retrieve <i>fact-base</i> <i>condition-task</i> <i>condition-field-list</i></code>
 *
 * @author Ruth Meyer
 *
 */
public class FactBaseRetrieve implements Reporter {
	
	// expects a reference to the factbase and a condition (as ReporterTask and List of field names), returns a list of facts satisfying that condition (or an empty list if not found)
	/** The retrieve primitive expects a fact base and a condition (specified as a reporter task and a list of corresponding fields) 
	 * as inputs and returns a list of all facts satisfying the given condition. 
	 */
	public Syntax getSyntax() {
		return SyntaxJ.reporterSyntax(new int[]{Syntax.WildcardType(), Syntax.ReporterType(), Syntax.ListType()}, Syntax.ListType());
	}
	

	/** Returns all facts satisfying the given condition from the specified fact base. The first argument {@code args[0]} has
	 * to be a fact base, the second argument {@code args[1]} has to be a reporter task and the third argument has to be a list of
	 * field names corresponding to the formal arguments used in the task.
	 * Returns an empty list if no such facts exist. Generates an error if any of the arguments are invalid.
	 * 
	 * @param args the arguments to this call of retrieve
	 * @param context the NetLogo context
	 * @return all facts satisfying the given condition
	 * @throws ExtensionException if any of the arguments are invalid
	 * @see org.nlogo.api.Reporter#report(org.nlogo.api.Argument[], org.nlogo.api.Context)
	 */
	@Override
	public Object report(Argument[] args, Context context) throws ExtensionException, LogoException 
	{
		// all the work is done in the retrieval class
		Retrieval r = new Retrieval(args, context);
		return r.retrieveAll();
	}
	
	


}
