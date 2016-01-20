/*
 * FactBaseRetrieveTo.java
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

import org.nlogo.api.Argument;
import org.nlogo.api.Context;
import org.nlogo.api.DefaultReporter;
import org.nlogo.api.Dump;
import org.nlogo.api.ExtensionException;
import org.nlogo.api.LogoException;
import org.nlogo.api.LogoList;
import org.nlogo.api.Syntax;

/** This class implements the "retrieve-to" primitive for the factbase extension. Retrieve-to works the same as retrieve in that it uses a 
 * ReporterTask and a List of field names as the condition that facts have to satisfy to be included in the result. The only difference is
 * that you can also specify which fields should be included in the result. So the result does not contain whole facts but only specific parts of 
 * the selected facts.
 * 
 * To call this primitive from NetLogo, use <code>factbase:retrieve <i>fact-base</i> <i>condition-task</i> <i>condition-field-list</i> <i>output-field-list</i></code>
 *
 * @author Ruth Meyer
 *
 */
public class FactBaseRetrieveTo extends DefaultReporter {

	// expects a reference to the factbase, a condition (as ReporterTask and List of field names) and the form of the output (as a list of field names),
	// returns a list of facts satisfying that condition filtered by the output form (or an empty list if not found)
	/** The retrieve-to primitive expects a fact base, a condition (specified as a reporter task and a list of corresponding fields) and a list of
	 * field names for the output as inputs and returns a list of all facts satisfying the given condition filtered by this output format.
	 */
	public Syntax getSyntax() {
		return Syntax.reporterSyntax(new int[]{Syntax.WildcardType(), Syntax.ReporterTaskType(), Syntax.ListType(), Syntax.ListType()}, 
									 Syntax.ListType());
	}
	

	/** Returns the specified parts of all facts satisfying the given condition from the specified fact base. The first argument {@link args[0]} has
	 * to be a fact base, the second argument {@link args[1]} has to be a reporter task, the third argument has to be a list of
	 * field names corresponding to the formal arguments used in the task, and the fourth argument has to be a list of field names to be included in the output.
	 * Returns an empty list if no such facts exist. Generates an error if any of the arguments are invalid.
	 * 
	 * @param args the arguments to this call of retrieve-to
	 * @param context the NetLogo context
	 * @return all (output-format filtered) facts satisfying the given condition
	 * @throw ExtensionException if any of the arguments are invalid
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
