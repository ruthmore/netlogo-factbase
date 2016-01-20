/*
 * FactBaseGet.java
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

import java.util.Iterator;
import java.util.List;

import org.nlogo.api.Argument;
import org.nlogo.api.Context;
import org.nlogo.api.DefaultReporter;
import org.nlogo.api.Dump;
import org.nlogo.api.ExtensionException;
import org.nlogo.api.LogoException;
import org.nlogo.api.LogoListBuilder;
import org.nlogo.api.Syntax;

/** This class implements the "get" primitive for the factbase extension. Get takes a fact base
 * and an integer (fact ID) as inputs and returns the fact with the specified ID. Generates an
 * error if no such fact exists.
 *  * 
 * To call this primitive from NetLogo, use <code>factbase:get <i>fact-base</i> <i>id</i></code>
 *
 * @author Ruth Meyer
 *
 */
public class FactBaseGet extends DefaultReporter {

	// expects a reference to the factbase and an index, returns the fact with the given index (or generates an error)
	/** Get expects a fact base and an integer number (fact ID) as inputs, returns a list (the fact with the 
	 * given ID).
	 * 
	 */
	public Syntax getSyntax() {
		return Syntax.reporterSyntax(new int[]{Syntax.WildcardType(), Syntax.NumberType()}, Syntax.ListType());
	}
	
	/** Reports the fact with the given ID from the specified fact base. The first argument {@link args[0]} has
	 * to be a fact base, the second argument {@link args[1]} has to be a valid fact ID. Generates an error if 
	 * the given ID is invalid (no fact with such an ID exists).
	 * 
	 * @param args the arguments to this call of get
	 * @param context the NetLogo context
	 * @return the fact with the given ID
	 * @throw ExtensionException if any of the arguments are invalid
	 * @see org.nlogo.api.Reporter#report(org.nlogo.api.Argument[], org.nlogo.api.Context)
	 */
	@Override
	public Object report(Argument[] args, Context context) throws ExtensionException, LogoException 
	{
		Object arg0 = args[0].get();
		if (! (arg0 instanceof FactBase)) {
	        throw new ExtensionException ("not a factbase: " + Dump.logoObject(arg0));
		}
		Object arg1 = args[1].get();
		if (! ((arg1 instanceof Double) || (arg1 instanceof Integer))) {
			throw new ExtensionException ("not a number: " + Dump.logoObject(arg1));
		}
		FactBase fb = (FactBase)arg0;
		int i = ((Double)arg1).intValue();
		List<Object> fact =  fb.retrieveFact(i);  // checking of ID happens in retrieveFact()
		// turn into LogoList before returning
		return FactBaseExtension.toLogoList(fact);
	}

}
