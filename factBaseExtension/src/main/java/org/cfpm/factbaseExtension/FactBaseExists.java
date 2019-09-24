/*
 * FactBaseExists.java
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
 * 
 * ruth@cfpm.org
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

/** This class implements the "exists?" primitive for the factbase extension. It checks if a fact satisfying
 * the given condition exists in the specified fact base and returns true, if this is the case. Otherwise, it returns false.
 * 
 * To call this primitive from NetLogo, use <code>factbase:exists? <i>fact-base</i> <i>condition-task</i> <i>condition-field-list</i></code>
 *
 * @author Ruth Meyer
 *
 */
public class FactBaseExists implements Reporter {

	// expecting a factbase and a a condition (as ReporterTask and List of field names) as input; returns true if there is at least one fact
	// satisfying the condition in the factbase, otherwise false
	/** The exists? primitive expects a fact base and a condition (specified as a reporter task and a list of corresponding fields) as 
	 * inputs and returns a Boolean value.
	 */
	public Syntax getSyntax() {
		return SyntaxJ.reporterSyntax(new int[]{Syntax.WildcardType(),  Syntax.ReporterType(), Syntax.ListType()}, Syntax.BooleanType());
	}
	
	
	/** Checks if a fact satisfying the given condition exists in the specified fact base. The first argument {@code args[0]} has
	 * to be a fact base, the second argument {@code args[1]} has to be a reporter task and the third argument has to be a list of
	 * field names corresponding to the formal arguments used in the task.
	 * 
	 * @param args the arguments to this call of exists?
	 * @param context the NetLogo context
	 * @return true, if a fact is found; otherwise, false
	 * @throw ExtensionException if any of the arguments are invalid
	 * @see org.nlogo.api.Reporter#report(org.nlogo.api.Argument[], org.nlogo.api.Context)
	 */
	@Override
	public Object report(Argument[] args, Context context) throws ExtensionException, LogoException 
	{
		Retrieval retrieval = new Retrieval(args, context);
		LogoList firstFact = retrieval.retrieveFirst();
		return (firstFact != null);

	}
	
}
