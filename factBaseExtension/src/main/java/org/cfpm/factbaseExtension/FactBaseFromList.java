/*
 * FactBaseFromList.java
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

import java.util.Iterator;

/** This class implements the "from-list" primitive for the factbase extension. From-list
 * takes a list of facts, including a list of field names as the first entry, as input and
 * creates a new fact base from it.
 * 
 * To call this primitive from NetLogo, use <code>factbase:from-list <i>list-of-field-names-and-facts</i></code>
 *
 * @author Ruth Meyer
 */
public class FactBaseFromList implements Reporter {

	// expects a list (of lists), returns a reference to the newly created factbase
	/** The primitive from-list expects a list (of lists, with first entry the list of field names) as input
	 * and returns a fact base.
	 * 
	 */
	public Syntax getSyntax() {
		return SyntaxJ.reporterSyntax(new int[]{Syntax.ListType()}, Syntax.WildcardType());
	}

	/** Creates a new fact base from the given list and reports it. The first argument {@code args[0]} has
	 * to contain the list of field names as its first entry.
	 * 
	 * @param args the arguments to this call of from-list
	 * @param context the NetLogo context
	 * @return a reference to the newly created fact base
	 * @throws ExtensionException if the argument is invalid
	 * @see org.nlogo.api.Reporter#report(org.nlogo.api.Argument[], org.nlogo.api.Context)
	 */
	@Override
	public Object report(Argument[] args, Context context) throws ExtensionException, LogoException {
		// first argument is a list of lists
		LogoList arg0;
		try {
			arg0 = args[0].getList();
			// first entry in this list is the list of field names
			// create factbase with this
			FactBaseCreate fbCreator = new FactBaseCreate();
			FactBase fb = (FactBase)fbCreator.report(args, context); // stripping of field names from args is now handled in FactBaseCreate
			// rest of list are the facts to be added
			// for each element of arg0.butFirst(), check if it's a list.
			for (Iterator<Object> facts = arg0.butFirst().javaIterator(); facts.hasNext(); ) {
				LogoList fact = (LogoList)facts.next();
				// then try and assert it. All checks of the fact are done in assertFact()
				// this will throw an ExtensionException if things go wrong
				fb.assertFact(fact); 
			}
			return fb;
		}
		catch (LogoException e) {
			throw new ExtensionException ("not a list: " + Dump.logoObject(args[0]));
		}

	}

}
