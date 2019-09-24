/*
 * FactBaseAssert.java
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
import org.nlogo.api.Command;
import org.nlogo.api.Dump;
import org.nlogo.api.ExtensionException;
import org.nlogo.api.LogoException;
import org.nlogo.core.LogoList;
import org.nlogo.core.Syntax;
import org.nlogo.core.SyntaxJ;

/** This class implements the "assert" primitive for the factbase extension. In Netlogo terms,
 * asserting a fact is a command, that means using the "assert" primitive does not return any
 * result. If the fact to be asserted is already contained in the fact base, nothing happens.
 * 
 * To call this primitive from NetLogo, use <code>factbase:assert <i>fact-base</i> <i>fact</i></code>
 * 
 * @author Ruth Meyer
 *
 */
public class FactBaseAssert implements Command {
	
	// expecting a factbase and a list (= fact) as input
	/** The assert primitive expects a fact base and a list (= fact) as inputs.
	 * 
	 */
	public Syntax getSyntax() {
		return SyntaxJ.commandSyntax(new int[]{Syntax.WildcardType(), Syntax.ListType()});
	}
	
	
	/** Performs the assertion. First argument {@code args[0]} has to be a fact base, second argument
	 * {@code args[1]} has to be a list (the fact to be asserted).
	 * 
	 *  @param args the arguments to this call of assert
	 *  @param context the NetLogo context
	 *  @throws ExtensionException if any of the arguments are of the wrong type
	 * @see org.nlogo.api.Command#perform(org.nlogo.api.Argument[], org.nlogo.api.Context)
	 */
	@Override
	public void perform(Argument[] args, Context context) throws ExtensionException, LogoException {
		Object arg0 = args[0].get();
		if (! (arg0 instanceof FactBase)) {
	        throw new ExtensionException ("not a factbase: " + Dump.logoObject(arg0));			
		}
		FactBase fb = (FactBase)arg0;
		LogoList arg1;
		try {
			arg1 = args[1].getList();
			// try and assert it. All checks of the fact are done in assertFact()
			// this will throw an ExtensionException if things go wrong
			fb.assertFact(arg1); 
		}
		catch (LogoException e) {
			throw new ExtensionException ("not a list: " + Dump.logoObject(args[1]));
		}
		

	}

}
