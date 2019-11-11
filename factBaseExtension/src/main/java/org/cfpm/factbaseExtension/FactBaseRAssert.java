/*
 * FactBaseRAssert.java
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

/** This class implements the "r-assert" primitive for the factbase extension, which is a variant
 * of assert (see {@link FactBaseAssert}). While assert is realised as a command, that means, it does
 * not return a result, r-assert is implemented as a reporter and returns the ID of the newly 
 * asserted fact.  
 * 
 * To call this primitive from NetLogo, use <code>factbase:r-assert <i>fact-base</i> <i>fact</i></code>
 * 
 * @author Ruth Meyer
 *
 */
public class FactBaseRAssert implements Reporter {

	// expecting a factbase and a list (= fact) as input, returning a number (= fact ID)
	/** The r-assert primitive expects a fact base and a list (= fact) as inputs and returns a number (= fact ID).
	 * 
	 */
	public Syntax getSyntax() {
		return SyntaxJ.reporterSyntax(new int[]{Syntax.WildcardType(), Syntax.ListType()}, Syntax.NumberType());
	}
	
	/** Performs the assertion. First argument {@code args[0]} has to be a fact base, second argument
	 * {@code args[1]} has to be a list (the fact to be asserted). Returns the ID of the given fact.
	 * 
	 *  @param args the arguments to this call of assert
	 *  @param context the NetLogo context
	 *  @return the fact ID of the given fact
	 *  @throws ExtensionException if any of the arguments are of the wrong type
	 * @see org.nlogo.api.Reporter#report(org.nlogo.api.Argument[], org.nlogo.api.Context)
	 */
	@Override
	public Object report(Argument[] args, Context context) throws ExtensionException, LogoException {
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
			int id = fb.assertFact(arg1); 
			return (double) id;
		}
		catch (LogoException e) {
			throw new ExtensionException ("not a list: " + Dump.logoObject(args[1]));
		}
	}	

}
