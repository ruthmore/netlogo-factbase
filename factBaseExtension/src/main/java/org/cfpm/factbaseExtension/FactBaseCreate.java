/*
 * FactBaseCreate.java
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
 */

package org.cfpm.factbaseExtension;

import org.nlogo.api.Argument;
import org.nlogo.api.Context;
import org.nlogo.api.DefaultReporter;
import org.nlogo.api.ExtensionException;
import org.nlogo.api.LogoException;
import org.nlogo.api.LogoList;
import org.nlogo.api.Syntax;

/** This class implements the "create" primitive for the factbase extension. It creates a new 
 * fact base of the specified structure and returns it. In NetLogo terms, creating a fact base is
 * therefore a reporter.
 * 
 * To call this primitive from NetLogo, use <code>factbase:create <i>list-of-field-names</i></code>
 *
 * @author Ruth Meyer
 *
 */
public class FactBaseCreate extends DefaultReporter {
	
	// expects a list, returns a reference to the newly created factbase
	/** The create primitive expects a list of field names as input and returns a fact base.
	 * 
	 */
	public Syntax getSyntax() {
		return Syntax.reporterSyntax(new int[]{Syntax.ListType()}, Syntax.WildcardType());
	}

	/** Performs the creation of a new fact base and reports it. The first argument {@link args[0]} has
	 * to contain the list of field names.
	 * 
	 * @param args the arguments to this call of create
	 * @param context the NetLogo context
	 * @return a reference to the newly created fact base
	 * @throw ExtensionException if the argument is invalid
	 * @see org.nlogo.api.Reporter#report(org.nlogo.api.Argument[], org.nlogo.api.Context)
	 */
	@Override
	public Object report(Argument[] args, Context context) throws ExtensionException, LogoException {
		// argument[0] should contain the list of field names
		// but also might contain a list of lists, with the first entry being the list of field names (if called from FBFromList)
		LogoList list;
		String[] fieldNames;
		try {
			list = extractFieldNameList(args[0].getList());
			if (!list.isEmpty()) {
				fieldNames = new String[list.size()];
				for (int i = 0; i < fieldNames.length; i++) {
					fieldNames[i] = list.get(i).toString();
				}
				return new FactBase(fieldNames);
			}
			else {
				return new FactBase();
			}
			
		}
		catch (LogoException e) {
			throw new ExtensionException (e.getMessage());
		}
	}
	
	private LogoList extractFieldNameList(LogoList arg) {
		// check if first entry in arg is itself a list
		Object firstEntry = arg.first();
		if (firstEntry instanceof LogoList) {
			// then we return this list
			return (LogoList)firstEntry;			
		}
		// otherwise: return the original list
		return arg;
	}

}
