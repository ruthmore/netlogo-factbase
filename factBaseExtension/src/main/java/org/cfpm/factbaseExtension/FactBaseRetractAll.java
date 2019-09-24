/*
 * FactBaseRetractAll.java
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
import org.nlogo.api.Command;
import org.nlogo.api.Context;
import org.nlogo.api.ExtensionException;
import org.nlogo.api.LogoException;
import org.nlogo.core.LogoList;
import org.nlogo.core.Syntax;
import org.nlogo.core.SyntaxJ;

import java.util.Iterator;

/** This class implements the "retract-all" primitive for the factbase extension. Retract-all finds all facts
 * satisfying the given condition and then retracts them from the fact base.  If there are no such facts,
 * nothing happens.
 * 
 * To call this primitive from NetLogo, use <code>factbase:retract-all <i>fact-base</i> <i>condition-task</i> <i>condition-field-list</i></code>
 * 
 * @author Ruth Meyer
 *
 */
public class FactBaseRetractAll implements Command {

	// expects a reference to the factbase and a condition (as ReporterTask and List of field names) as input
	/** The retract-all primitive expects a fact base and a condition (specified as a reporter task and a list of 
	 * corresponding fields) as inputs.
	 * 
	 */
	public Syntax getSyntax() {
		return SyntaxJ.commandSyntax(new int[]{Syntax.WildcardType(), Syntax.ReporterType(), Syntax.ListType()});
	}

	/** Retracts all facts satisfying the given condition from the specified fact base. The first argument {@code args[0]} has
	 * to be a fact base, the second argument {@code args[1]} has to be a reporter task and the third argument has to be a list of
	 * field names corresponding to the formal arguments used in the task.
	 * 
	 * @param args the arguments to this call of retract-all
	 * @param context the NetLogo context
	 * @throws ExtensionException if any of the arguments are invalid
	 * @see org.nlogo.api.Command#perform(org.nlogo.api.Argument[], org.nlogo.api.Context)
	 */
	@Override
	public void perform(Argument[] args, Context context)
			throws ExtensionException, LogoException {
		// use retrieval to do the actual collecting of facts to be deleted; this also checks validity of arguments
		Retrieval retrieval = new Retrieval(args, context);
		LogoList selectedFacts = retrieval.retrieveAll();
		FactBaseExtension.writeToNetLogo("Result has " + selectedFacts.size() + " facts: " + selectedFacts.toString(), false, context);
		// retract the selected facts
		FactBase fb = retrieval.getFactBase();
		for (Iterator<Object> fi = selectedFacts.javaIterator(); fi.hasNext(); ) {
			LogoList fact = (LogoList)fi.next();  // retrieve made facts be LogoLists
			fb.removeFact(fact);
		}
		// and we're finished
		FactBaseExtension.writeToNetLogo("selected facts have been retracted", false, context);
	}

}
