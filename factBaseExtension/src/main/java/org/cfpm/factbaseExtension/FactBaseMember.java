/*
 * FactBaseMember.java
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
import org.nlogo.api.Dump;
import org.nlogo.api.ExtensionException;
import org.nlogo.api.LogoException;
import org.nlogo.api.Reporter;
import org.nlogo.core.LogoList;
import org.nlogo.core.Syntax;
import org.nlogo.core.SyntaxJ;

/** This class implements the "member?" primitive for the factbase extension. Member?
 * takes a fact base and a fact as inputs and returns true, if the fact base contains
 * the given fact. Otherwise, it returns false.
 * 
 * To call this primitive from NetLogo, use <code>factbase:member? <i>fact-base</i> <i>fact</i></code>
 *
 * @author Ruth Meyer
 *
 */
public class FactBaseMember implements Reporter {

	// expecting a factbase and a list (= fact) as input; returns true if the fact is contained in the factbase, otherwise false
	/** Member? expects a fact base and a fact as inputs, returns a Boolean value.
	 * 
	 */
	public Syntax getSyntax() {
		return SyntaxJ.reporterSyntax(new int[]{Syntax.WildcardType(), Syntax.ListType()}, Syntax.BooleanType());
	}
	
	
	/** Reports true if the given fact exists within the specified fact base. Otherwise, reports false. 
	 * The first argument {@code args[0]} has to be a fact base, the second argument {@code args[1]} has to be a
	 * fact (list). Generates an error if any of the arguments is invalid.
	 * 
	 * @param args the arguments to this call of member?
	 * @param context the NetLogo context
	 * @return true if the fact is found, otherwise false
	 * @throws ExtensionException if any of the arguments are invalid
	 * @see org.nlogo.api.Reporter#report(org.nlogo.api.Argument[], org.nlogo.api.Context)
	 */
	@Override
	public Object report(Argument[] args, Context context)
			throws ExtensionException, LogoException {
		Object arg0 = args[0].get();
		if (! (arg0 instanceof FactBase)) {
	        throw new ExtensionException ("not a factbase: " + Dump.logoObject(arg0));			
		}
		FactBase fb = (FactBase)arg0;
		LogoList arg1;
		try {
			arg1 = args[1].getList();
			// check if the factbase contains it
			int result = fb.containsFact(arg1);
			return (result >= 0);
		}
		catch (LogoException e) {
			throw new ExtensionException ("not a list: " + Dump.logoObject(args[1]));
		}
	}

}
