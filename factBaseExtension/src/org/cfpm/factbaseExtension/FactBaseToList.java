/*
 * FactBaseToList.java
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
import org.nlogo.api.DefaultReporter;
import org.nlogo.api.Dump;
import org.nlogo.api.ExtensionException;
import org.nlogo.api.LogoException;
import org.nlogo.api.Syntax;

/** This class implements the "to-list" primitive for the factbase extension. To-list
 * takes a fact base as input and returns it as a list of facts, including a list of field names as the first entry.
 * 
 * To call this primitive from NetLogo, use <code>factbase:to-list <i>fact-base</i></code>
 *
 * @author Ruth Meyer
 */
public class FactBaseToList extends DefaultReporter {

	/** The primitive from-list expects a fact base as input and returns a list (of lists, 
	 * with first entry the list of field names).
	 */
	public Syntax getSyntax() {
		return Syntax.reporterSyntax(new int[]{Syntax.WildcardType()}, Syntax.ListType());
	}
	
	/** Reports the given fact base in the form of a list of lists, with the list of field names as the first entry.
	 *  The first argument {@link args[0]} has to be a fact base.
	 * 
	 * @param args the arguments to this call of to-list
	 * @param context the NetLogo context
	 * @return the fact base as a list
	 * @throw ExtensionException if the argument is invalid
	 * @see org.nlogo.api.Reporter#report(org.nlogo.api.Argument[], org.nlogo.api.Context)
	 */
	@Override
	public Object report(Argument[] args, Context context)
			throws ExtensionException, LogoException 
	{
		Object arg0 = args[0].get();
		if (! (arg0 instanceof FactBase)) {
			throw new ExtensionException ("not a factbase: " + Dump.logoObject(arg0));
		}
		FactBase fb = (FactBase)arg0;
		return fb.toList();
	}

}
