/*
 * FactBaseSize.java
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

/** This class implements the "size" primitive for the factbase extension. Size returns the number of facts in
 * the given fact base.
 * 
 * To call this primitive from NetLogo, use <code>factbase:size <i>fact-base</i></code>
 *
 * @author Ruth Meyer
 *
 */
public class FactBaseSize extends DefaultReporter {

	/** The size primitive expects a fact base as input and returns a number (the size of the given fact base).
	 * 
	 */
	public Syntax getSyntax() {
		return Syntax.reporterSyntax(new int[]{Syntax.WildcardType()}, Syntax.NumberType());
	}
	
	/** Returns the size of the specified fact base, that is the number of facts in this fact base. The first argument {@link args[0]} has
	 * to be a fact base-
	 * Generates an error if the argument is invalid.
	 * 
	 * @param args the arguments to this call of size
	 * @param context the NetLogo context
	 * @return the size of the fact base
	 * @throw ExtensionException if any of the arguments are invalid
	 * @see org.nlogo.api.Reporter#report(org.nlogo.api.Argument[], org.nlogo.api.Context)
	 */
	@Override
	public Object report(Argument[] args, Context context) throws ExtensionException, LogoException {
	      Object arg0 = args[0].get();
	      if (!(arg0 instanceof FactBase)) {
	        throw new ExtensionException ("not a factbase: " + Dump.logoObject(arg0));
	      }
	      return Double.valueOf(((FactBase) arg0).size());
	}

}
