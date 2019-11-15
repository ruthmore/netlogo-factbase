/*
 * FactBase.java
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

import org.nlogo.api.ExtensionException;
import org.nlogo.api.LogoException;
import org.nlogo.api.LogoListBuilder;
import org.nlogo.core.ExtensionObject;
import org.nlogo.core.LogoList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Random;


/** This class implements the data type "fact base" that the factbase extension provides. A fact base can be thought of as a table with named
 * columns ("fields"), where each row represents an entry ("fact").
 * 
 * At creation, the user has to define the structure of the fact base, that means define the field names. Note that in keeping with the NetLogo
 * philosophy of a type-free language, data types for fields are not specified. After creating a fact base, facts can be asserted, queried
 * and retracted. Facts are represented as lists of values, with one value for each field and all values in the same order as defined by the
 * list of field names. Duplicate facts are not allowed. Therefore, trying to assert a fact with all values identical to an already existing 
 * fact is ignored.
 * To be able to use indexing (and thus, faster retrieval), each fact is internally assigned an ID, starting with 0. A new fact 
 * will be assigned the highest number so far in use + 1. Retracting a fact will result in its ID being unassigned, thus trying to retrieve a
 * retracted fact will generate an error.
 * 
 * @author Ruth Meyer
 *
 */
public class FactBase implements ExtensionObject {
	
	/** The ID of this fact base */
	private int id; 
	/** The list of field names */
	private final String[] fieldNames;
	/** An indexed data structure to store the facts: One hashmap per field, with field values as keys and lists of corresponding fact ids as values */
	private List<LinkedHashMap<Object, List<Integer>>> facts;	
	/** The next available ID for a new fact */
	private int nextFactID = 0;
	/** Additional copy of facts as an ordered list for easy access via fact ID */
	private List<LogoList> orderedFacts = new ArrayList<LogoList>();
		// PROBLEM: need to clone facts before storing them so they can't be changed from the outside
		// so for the ordered list of facts we either need an internal Fact class or (deep-)clone the list
	/** List of IDs of retracted facts */
	private List<Integer> deleted = new ArrayList<Integer>();
	
	/** Flag to toggle output to the console. Default is false; set to true only for debugging purposes. */
	public static boolean showDump = false;
	  
	/** Default "empty" constructor. Since no field names are given, just one field is created called "unnamed".
	 * 
	 */
	public FactBase() {
		init();
		this.fieldNames = new String[]{"unnamed"};
		createFactBase(fieldNames.length);
	}
	
	/** Constructor which defines the structure of the new fact base.
	 * 
	 * @param fields the list of field names
	 */
	public FactBase(String[] fields) {
		init();
		this.fieldNames = fields;
		createFactBase(fields.length);
	}	
	
	/** Initialises the fact base, that means assigns it an ID and stores it in the overall list of fact bases. 
	 * 
	 */
	private void init(){
		FactBaseExtension.bases.put(FactBaseExtension.next, this);
		id = FactBaseExtension.next;
		FactBaseExtension.next++;		
	}
	
	/** Creates the internal data structure for storing facts.
	 * 
	 * @param len The length = number of fields of a fact
	 */
	private void createFactBase(int len){
		facts = new ArrayList<LinkedHashMap<Object, List<Integer>>>(len);
		for (int i = 0; i < len; i++) {
			facts.add(new LinkedHashMap<Object, List<Integer>>());
		}
	}
	
	/** Returns the size of this fact base, that means the number of facts it contains.
	 * 
	 * @return number of facts in this fact base
	 */
	public int size() {
		return orderedFacts.size();
	}
	
	/** Returns the list of field names defining the structure of this fact base
	 * 
	 * @return list of field names
	 */
	public String[] getFieldNames() {
		return fieldNames;
	}
	
	/** Returns the position (index) of the given field name within the list of field names. Positions are between 0 and n-1.
	 * If there is no such field name defined for this fact base, returns -1.
	 * 
	 * @param fieldName a field name
	 * @return index of the given field name or -1, if the field name does not exist
	 */
	public int getFieldIndex(String fieldName) {
		return isAField(fieldName);
	}
	
	/** Returns the whole field ("column of the table") with the given index as a linked hash map. The field values are used as keys,
	 * with lists of corresponding fact IDs as values in this hash map.
	 * 
	 * @param index index specifying which field to access
	 * @return the field as a linked hash map
	 */
	protected LinkedHashMap<Object, List<Integer>> getField(int index) {
		return facts.get(index);
	}
		
	/** Asserts the given fact to this fact base. If an identical fact already exists in this fact base, nothing happens.
	 * 
	 * @param fact the new fact to be inserted into the fact base
	 * @return the ID for the new fact (or the identical old fact)
	 * @throws ExtensionException is thrown if the fact does not match the structure of this fact base (too many or too few fields)
	 */
	public int assertFact(LogoList fact) throws ExtensionException {
		// check if fact matches fields
		// 1. length of fact = number of field names?
		if (fact.size() != fieldNames.length) {
			throw new ExtensionException("facts for this factbase have to consist of " + fieldNames.length + " fields");
		}
		// 2. types of fields ... 
		// we'll leave this for now
		
		// check if input is a simple fact (just a list of values in the correct order) or a structured fact (a list of pairs of <fieldName> <value>)
		// for now, we'll just do simple facts!!
		
		// check if this fact is already in the factbase (i.e. if there is already a fact with exactly the same field values)
		// only assert it if it's NOT already there
		int id = containsFact(fact);
		if (id < 0) {
			// stick it in the factbase, i.e. split it into fields
			for (int i = 0; i < fact.size(); i++) {
				Object fieldValue = fact.get(i);
				LinkedHashMap<Object, List<Integer>> field = this.facts.get(i);
				if (field.containsKey(fieldValue)) {
					// add fact id to list at entry <fieldValue>
					List<Integer> idList = field.get(fieldValue);
					idList.add(nextFactID);
				}
				else {
					// make a new list and stick fact id in
					// then add entry <fieldValue> <fact-id-list>
					ArrayList<Integer> idList = new ArrayList<Integer>();
					idList.add(nextFactID);
					field.put(fieldValue, idList);
				}
			}
			// also stick in the ordered list
			dump(" ** adding fact " + nextFactID + ": " + printFact(fact));
			orderedFacts.add(clone(fact));
			id = nextFactID;
			// update next fact ID
			nextFactID++;
		}
		return id;
	}
	
	/** produces a shallow copy of the given fact to be put into the ordered list of facts (this is to avoid manipulation of values from outside the fact base)
	 * 
	 * @param fact the fact to be cloned
	 * @return a shallow copy of the given fact
	 */
	private LogoList clone(LogoList fact) {
		List<Object> fcopy = new ArrayList<Object>();
		for (Iterator<Object> i = fact.javaIterator(); i.hasNext(); ) {
			Object field = i.next();
			fcopy.add(field); // only make a shallow copy
		}
		return LogoList.fromJava(fcopy);
	}
	

	// removing a fact results in re-indexing! (= re-numbering all facts with a higher fact ID)
	// NO, not anymore. IDs are immutable. Removing a fact leaves a "hole" in the list of ordered facts.
	// Trying to access a deleted fact will result in an error.
	/** Removes the given fact from the fact base. This includes removing its field value/ID associations from all the internal fields and
	 *  removing the fact from the list of ordered facts. If the fact is not contained in the fact base, nothing happens.
	 * 
	 * @param fact the fact to be removed
	 * @throws ExtensionException if the given fact's structure does not match the structure of the fact base
	 */
	public void removeFact(LogoList fact) throws ExtensionException {
		// check if fact matches fields
		// 1. length of fact = number of field names?
		if (fact.size() != fieldNames.length) {
			throw new ExtensionException("facts for this factbase have to consist of " + fieldNames.length + " fields");
		}
		// 2. types of fields ... 
		// we'll leave this for now
		
		// check if input is a simple fact (just a list of values in the correct order) or a structured fact (a list of pairs of <fieldName> <value>)
		// for now, we'll just do simple facts!!
		
		// remove it from the factbase (if it's actually there)
		int id = containsFact(fact);
		if (id >= 0) {
			for (int i = 0; i < fact.size(); i++){
				Object fieldValue = fact.get(i);
				LinkedHashMap<Object, List<Integer>> field = this.facts.get(i);
				List<Integer> idList = field.get(fieldValue);
				idList.remove(new Integer(id));
				// check if idList is now empty
				if (idList.isEmpty()) {
					// if so, we want to remove the whole entry from this field
					field.remove(fieldValue);
				}
			}
			// also remove from ordered list of facts
			orderedFacts.remove(id);
			orderedFacts.add(id, null); // replace entry with NULL
			deleted.add(id); // add its ID to the list of deleted facts
			// id is a hint whereabouts it can be found
			//removeFromOrderedList(id, fact);
			// re-index everything
//			reIndex(id);
			// adjust next available factID
//			nextFactID--;
		}
	}
	
	private void reIndex(int deleted) {
		// fact with ID "deleted" was deleted --> all factIDs > deleted need to be shifted down by one
		// so we just iterate over all fields and all entries in fields and re-number the IDs
		for (Iterator<LinkedHashMap<Object, List<Integer>>> it = facts.iterator(); it.hasNext(); ) {
			LinkedHashMap<Object, List<Integer>> field = it.next();
			for (Iterator<List<Integer>> entries = field.values().iterator(); entries.hasNext(); ) {
				List<Integer> ids = entries.next();
				for (int i = 0; i < ids.size(); i++) {
					int currentID = ids.get(i);
					if (currentID > deleted) {
						ids.set(i, (--currentID));
					}
				}
			}
		}
	}
	
//	private void removeFromOrderedList(int id, List<Object> fact) {
//		// first check if it's still in the correct place
//		List<Object> toBeDeleted = orderedFacts.get(id);
//		if (toBeDeleted.equals(fact)) {
//			// easy: just remove it!
//			orderedFacts.remove(id);
//		}
//		else {
//			// got to search to the left until we find it
//			boolean found = false;
//			do {
//				id--;
//				found = orderedFacts.get(id).equals(fact);
//			} while (id >= 0 && !found);
//			if (id >= 0) {
//				orderedFacts.remove(id);
//			}
//		}
//	}
	

	/** Checks if the given fact is contained within this fact base. If so, returns its fact ID. If not, returns -1.
	 * 
	 * @param fact the fact to be checked
	 * @return the fact's ID (or -1 if the fact is not found in the fact base)
	 * @throws ExtensionException if more than one fact identical to the given facts are found (which should not happen!)
	 */
	public int containsFact(LogoList fact) throws ExtensionException {
		dump("checking if fact " + printFact(fact) + " is in the factbase");
		// if factbase is emtpy, fact is not in it
		if (facts.get(0).isEmpty()) {
			return -1;
		}
		// check if the given fact is already in the fact base, i.e. if there exists a fact ID, which is associated with every single field value of the given fact
		ArrayList<Integer> potentials = new ArrayList<Integer>();
		int i = 0;
		Object fieldValue = fact.get(i);
		dump("checking field " + i + ": for value " + fieldValue);
		printIndexedField(i);
		LinkedHashMap<Object, List<Integer>> field = this.facts.get(i);
		List<Integer> idList = field.get(fieldValue);
		if (idList != null) {
			potentials.addAll(idList); // put first list of fact ids in potentials
		}
		i++;
		while (!potentials.isEmpty() && i < fact.size()) {
			// check next field
			fieldValue = fact.get(i);
			dump("checking field " + i + ": for value " + fieldValue);
			printIndexedField(i);
			field = this.facts.get(i);
			idList = field.get(fieldValue);
			// remove all fact ids from potentials that are not in idList
			dump("computing intersection of " + printList(potentials) + " and " + printList(idList));
			potentials = intersect(potentials, idList);
			i++;
		}
		if (potentials.isEmpty()) {
			// fact not found --> return -1
			return -1;
		}
		else if (potentials.size() == 1) {
			// fact found --> return its ID
			return potentials.get(0);
		}
		// found more than one fact --> throw an exception
		throw new ExtensionException("found more than one fact like " + printFact(fact) + " in the factbase. Shock horror!");
	}
	
	/** Returns true if the fact with the given ID has been retracted. Otherwise, returns false.
	 * 
	 * @param id a fact ID to be checked
	 * @return true, if the fact has been deleted; false, otherwise.
	 */
	public boolean isRetracted(int id) {
		// check if given ID is in deleted list
		return deleted.contains(id);
	}
	
	
	/** Helper method to output the contents of the specified field (internal data structure).
	 * 
	 * @param which index of the field
	 */
	private void printIndexedField(int which){
		LinkedHashMap<Object, List<Integer>> field = this.facts.get(which);
		dump("Field " + which);
		for (Iterator<Object> i = field.keySet().iterator(); i.hasNext(); ) {
			Object key = i.next();
			List<Integer> values = field.get(key);
			dump(key + " | " + printList(values));
		}
	}
	
	/** Retrieves the fact with the specified fact ID from this fact base.
	 * 
	 * @param factID the fact's ID
	 * @return the fact associated with the given ID
	 * @throws ExtensionException if the fact ID is invalid or the fact with this ID was retracted or if (part of) the fact cannot be found
	 */
	public LogoList retrieveFact(int factID)
			throws ExtensionException 
	{
		// if factID is invalid, throw an exception
		if (factID < 0 || factID >= nextFactID) {
			throw new ExtensionException ("not a valid fact id: " + factID);
		}
		// if factID belongs to a deleted fact, throw an exception
		if (deleted.contains(factID)) {
			throw new ExtensionException ("the fact with id " + factID + " was retracted");
		}
		// run through all field hash maps and collect field values associated with factID
		// ### this might be too costly (in computing time) so might need to be replaced with copy of ordered facts (how costly in memory?)
		List<Object> fact = new ArrayList<Object>();
		for (java.util.Iterator<LinkedHashMap<Object, List<Integer>>> i = facts.iterator(); i.hasNext(); ) {
			LinkedHashMap<Object, List<Integer>> field = i.next();
			boolean found = false;
			java.util.Iterator<List<Integer>> j = field.values().iterator();
			java.util.Iterator<Object> values = field.keySet().iterator();
			while (!found && j.hasNext()) {
				List<Integer> idList = j.next();
				Object fieldValue = values.next();
				if (idList.contains(factID)) {
					found = true;
					fact.add(fieldValue);
				}
			}
			if (!found) {
				throw new ExtensionException ("could not find (part of) fact with id " + factID);
			}
		}
		return LogoList.fromJava(fact);
	}
	
//	public List<List<Object>> retrieveFacts (String condition, Context context) throws ExtensionException {
//		List<List<Object>> collectedFacts = new ArrayList<List<Object>>();
//		
//		// step 1: parse condition
//		if (this.parser == null) this.parser = new Parser(fieldNames);
//		ExpressionNode cExpr = parser.parse(condition);
//		dumpToNetLogo("parsed condition is " + cExpr.toString(), context);
//		
//		// step 2: find all variables in the expression and check if they correspond to fields
//		List<String> variables = cExpr.getVariables(null);
//		if (variables == null || variables.size() == 0) {
//			// check if the expression is just "true" or "false"
//			if (cExpr instanceof BooleanConstantExpressionNode) {
//				boolean value = ((Boolean)cExpr.getValue()).booleanValue();
//				if (value) {
//					// true: return all facts
//					for (Iterator<List<Object>> fi = orderedFacts.iterator(); fi.hasNext(); ) {
//						collectedFacts.add(fi.next());
//					}
//				}
//				return collectedFacts;	// this list is empty if value is false			
//			}
//			// it's something else so there's a mistake somewhere
//			throw new ExtensionException("no fields specified in the given condition: " + condition);
//		}
//		List<Integer> fieldIndices = new ArrayList<Integer>();
//		for (String v : variables) {
//			int index = isAField(v);
//			if (index < 0) {
//				throw new ExtensionException(v + " is not defined as a field in this factbase");
//			}
//			else {
//				fieldIndices.add(index);
//			}
//		}
//		dumpToNetLogo("fields identified: " + fieldIndices.size(), context);
//		
//		// step 3: check which facts satisfy the condition
//		// ### how best to do this?
//		// go through ordered list of facts and assign values of selected fields to the variables in the expression
//		// -- collect all facts that result in the expression being true
//		for (Iterator<List<Object>> f = orderedFacts.iterator(); f.hasNext(); ) {
//			List<Object> fact = f.next();
//			dump("-- checking fact " + printFact(fact));
//			dumpToNetLogo("-- checking fact " + printFact(fact), context);
//			dumpToNetLogo("----- types are:" + printFactTypes(fact), context);
//			// set variables in expression to fact field values
//			for (int i = 0; i < fieldIndices.size(); i++) {
//				cExpr.accept(new SetVariable(variables.get(i), fact.get(fieldIndices.get(i))));
//				dump("-- expression now: " + cExpr.toString());
//				dumpToNetLogo("-- expression now: " + cExpr.toString(), context);
//			}
//			// evaluate expression
//			Object result = cExpr.getValue();
//			dump("-- value of expression: " + result);
//			dumpToNetLogo("-- value of expression: " + result, context);
//			if ((Boolean)cExpr.getValue() == true) {
//				// this fact fits the condition
//				collectedFacts.add(fact);
//			}			
//		}
//		dump("-- " + collectedFacts.size() + " facts collected");		
//		dumpToNetLogo("-- " + collectedFacts.size() + " facts collected", context);
//		return collectedFacts;
//	}
	
	// checks if f is a field name and returns the corresponding index for that field (or -1 if not a field)
	/** Checks if the given name is a valid field name. If so, returns the index for the field of that name.
	 * Otherwise, returns -1.
	 * 
	 * @param f a field name to be checked
	 * @return the corresponding index for the field of the given name (or -1 if not a valid field name)
	 */
	private int isAField(String f) {
		// compare f to the field names
		boolean valid = false;
		int i = 0;
		while (i < fieldNames.length && !valid) {
			if (fieldNames[i].equals(f)) {
				// found it
				valid = true;
			}
			else {
				i++;
			}
		}
		if (valid) return i;
		return -1;
	}
	
	/** Returns a string representation of this fact base.
	 * 
	 */
	public String toString() {
		StringBuilder buff = new StringBuilder("FactBase ");
		buff.append(this.id);
		buff.append(": (");
		for (String fName : fieldNames) {
			buff.append(" <");
			buff.append(fName);
			buff.append("> ");
		}
		buff.append(")\n---------------------------------------------------------");
		// iterate over ordered facts
		int factID = 0;
		for (Iterator<LogoList> ifacts = orderedFacts.iterator(); ifacts.hasNext(); ) {
			LogoList fact = ifacts.next();
			// need to skip deleted entries
			if (fact != null) {
				buff.append("\n" + factID + ": ");
				buff.append(printFact(fact));
			}
			factID++;
		}
//		// iterate over ordered facts and calculate fact ids from a counter and the deleted list
//		int factID = 0; // current fact ID
//		int i = 0; // index into deleted
//		for (Iterator<List<Object>> ifacts = orderedFacts.iterator(); ifacts.hasNext(); ) {
//			List<Object> fact = ifacts.next();
//			while (i < deleted.size() && factID == deleted.get(i)) {
//				factID++;
//				i++;
//			}
//			buff.append("\n" + factID + ": ");
//			buff.append(printFact(fact));
//			factID++;
//		}
//		for (int i = 0; i < nextFactID; i++) {
//			List<Object> fact;
//			try {
//				fact = retrieveFact(i);
//				buff.append("\n" + i + ": ");
//				buff.append(printFact(fact));
//			}
//			catch (ExtensionException e) {
//				System.err.println("OOPS! Can't retrieve fact no. " + i);
//			}
//		}
		return buff.toString();
	}

	/** Returns a string representation of the given fact
	 * 
	 * @param fact the fact to be printed
	 * @return the fact as String
	 */
	protected String printFact(LogoList fact) {
		StringBuilder buff = new StringBuilder("( ");
		for (Object o : fact.javaIterable()) {
			if (o instanceof Collection) {
				// make a list representation of it
				buff.append("[");
				for (Iterator<Object> it = ((Collection)o).iterator(); it.hasNext(); ) {
					buff.append(" ");
					buff.append(it.next().toString());
				}
				buff.append(" ]");
			}
			else if (o instanceof Object[]) {
				// make a list representation of it
				buff.append("[");
				for (int i = 0; i < ((Object[])o).length; i++) {
					buff.append(" ");
					buff.append(((Object[])o)[i].toString());
				}
				buff.append(" ]");
			}
			else buff.append(o.toString());
			buff.append(" ");
		}
		buff.append(")");
		return buff.toString();
	}
	
//	private String printFactTypes(List<Object> fact) {
//		StringBuilder buff = new StringBuilder("( ");
//		for (Iterator<Object> elems = fact.iterator(); elems.hasNext(); ) {
//			Object e = elems.next();
//			buff.append(e.getClass().toString());
//			buff.append(" ");
//		}
//		buff.append(")");
//		return buff.toString();
//	}

	/** Helper method: Returns a string representation of the given list of IDs.
	 * 
	 * @param idList the list of fact IDs
	 * @return the given list as a String
	 */
	private String printList(List<Integer> idList) {
		if (idList == null) {
			return "NULL";
		}
		StringBuilder buff = new StringBuilder("( ");
		for (Object o : idList) {
			buff.append(o.toString());
			buff.append(" ");
		}
		buff.append(")");
		return buff.toString();
	}
	
	/** Computes the intersection of two ID lists.
	 * 
	 * @param potentials list of potential fact IDs (candidates for inclusion in retrieval result)
	 * @param idList list of established fact IDs (so far included in retrieval result)
	 * @return intersection of the two lists, that is a list with all IDs that are contained in both given lists
	 */
	private ArrayList<Integer> intersect(List<Integer> potentials, List<Integer> idList) {
		ArrayList<Integer> temp = new ArrayList<Integer>();
		if (idList == null) {
			return temp;
		}
		for (java.util.Iterator<Integer> i = potentials.iterator(); i.hasNext(); ) {
			Integer id = i.next();
			if (idList.contains(id)) {
				temp.add(id);
			}
		}
		return temp;
	}
	
	
	/** 
	 * @see org.nlogo.core.ExtensionObject#dump(boolean, boolean, boolean)
	 */
	@Override
	public String dump(boolean readable, boolean exportable, boolean reference) {
		// copied from TableExtension
		if (exportable && reference) {
			return ("" + id);
		}
		else {
			return (exportable ? (id + ": ") : "") + org.nlogo.api.Dump.logoObject(this.toList(), true, exportable);
		}	
	}

	/** Exports this fact base as a LogoList. In fact, it will be a list of lists (= facts), with the first entry being 
	 * the list of field names.
	 * 
	 * @return this fact base as a LogoList object
	 */
	public LogoList toList() {
		// turn factbase into a list of lists (facts)
		LogoListBuilder base = new LogoListBuilder();
		// stick field names in as first entry
		base.add(convertToLogoList(fieldNames));
		// stick facts in 
//		try {
			// iterate over ordered facts
			for (Iterator<LogoList> i = orderedFacts.iterator(); i.hasNext(); ) {
				LogoList fact = i.next();
				// need to skip deleted entries
				if (fact != null) {
					base.add(fact);
				}
			}
//			for (int i = 0; i < nextFactID; i++) {
//				List<Object> fact = retrieveFact(i);
//				base.add(convertToLogoList(fact));
//			}
//		}
//		catch (ExtensionException e) {
//			System.err.println("ERROR: could not successfully turn factbase into list because " + e.getMessage());
//		}
		return base.toLogoList();
	}

	/** Helper method: Converts the given array of objects into a LogoList. Used in {@link #toList()} to convert the
	 * field names into a list.
	 * 
	 * @param objArray the object array to be converted
	 * @return a LogoList representation of the given object array
	 */
	static LogoList convertToLogoList(Object[] objArray) {
		LogoListBuilder list = new LogoListBuilder();
		for (Object o : objArray) {
			list.add(o);
		}
		return list.toLogoList();
	}

	/** Helper method: Converts the given list of objects into a LogoList. Used in {@link #toList()} to convert the
	 * facts into a list.
	 * 
	 * @param objList the object list to be converted
	 * @return a LogoList representation of the given object list
	 */
	static LogoList convertToLogoList(List<Object> objList) {
		LogoListBuilder list = new LogoListBuilder();
		for (Object o : objList) {
			list.add(o);
		}
		return list.toLogoList();
	}

	/** Returns this extension's name.
	 * 
	 * @see org.nlogo.core.ExtensionObject#getExtensionName()
	 */
	@Override
	public String getExtensionName() {
		return "factbase";
	}

	/** 
	 * @see org.nlogo.core.ExtensionObject#getNLTypeName()
	 */
	@Override
	public String getNLTypeName() {
		// copied from TableExtension:
		// since this extension only defines one type, we don't
	    // need to give it a name; "factbase:" is enough,
	    // "factbase:factbase" would be redundant
	    return "";	
	}

	/** 
	 * @see org.nlogo.core.ExtensionObject#recursivelyEqual(java.lang.Object)
	 */
	@Override
	public boolean recursivelyEqual(Object o) {
		// copied from TableExtension and adapted
		if (! (o instanceof FactBase)) {
			// not a factbase
			return false;
		}
		// o is a factbase
		FactBase other = (FactBase)o;
		if (size() != other.size()) {
			// not the same length
			return false;
		}
		if (! sameStructure(other.getFieldNames())) {
			// not same fields
			return false;
		}
		// check every fact
		try {
			// iterate over ordered facts
			for (Iterator<LogoList> i = orderedFacts.iterator(); i.hasNext(); ) {
				LogoList fact = i.next();
				if (fact != null && other.containsFact(fact) < 0) {
					// fact is not in other factbase
					return false;
				}
			}
//			for (int i = 0; i < nextFactID; i++) {	
//				List<Object> fact = retrieveFact(i);
//				if (other.containsFact(fact) < 0) {
//					// fact is not in other factbase
//					return false;
//				}
//			}
		}
		catch (ExtensionException e) {
			// something went wrong in fact retrieval
			return false;
		}
		return true;		
	}
	
	/** Helper method: checks if another fact base has the same structure as this fact base by comparing
	 * the field names of the other fact base to the field names of this fact base.
	 * 
	 * @param fieldNames2 the field names of the other fact base
	 * @return true, if field names are identical; otherwise, false
	 */
	private boolean sameStructure(String[] fieldNames2) {
		// compare our field names to the given field names
		if (this.fieldNames.length != fieldNames2.length) {
			// not the same length
			return false;
		}
		//@TODO Question: do the fields have to be in the same order to qualify?
		// At the moment: YES (because it's easier and faster to check)
		boolean same = true;
		for (int i = 0; i < fieldNames.length; i++) {
			same &= fieldNames[i].equalsIgnoreCase(fieldNames2[i]);
		}
		return same;
	}
	

//	private void dumpToNetLogo(String msg, Context context) throws ExtensionException
//	{
//		if (context != null) {
//			FactBaseExtension.writeToNetLogo(msg, false, context);
//		}
//	}
	
	/** Prints the given string to console if the flag {@link #showDump} is set to true.
	 * 
	 * @param string the text to be printed
	 */
	public static void dump(String string) {
		if (showDump) {
			System.out.println(string);
		}
	}
	

	/** The main() method was used to do unit tests.
	 * 
	 * @param args any arguments are ignored
	 * @throws ExtensionException
	 * @throws LogoException
	 */
	public static void main(String[] args) throws ExtensionException, LogoException {
		// unit test create
		FactBase fb = new FactBase(new String[]{"name", "is-male", "type"});
		FactBase fb2 = new FactBase();
		System.out.println(fb2.toString());
		FactBase fb3 = new FactBase(new String[]{"NAME", "IS-MALE", "TYPE"});
		
		// unit test assert, containsFact, retrieveFact, toString, recursivelyEqual
		List<Object> fact = new ArrayList<Object>();
		System.out.println("\nAsserting first fact...");
		fact.add("Boris"); fact.add(true); fact.add("cat");
		fb.assertFact(convertToLogoList(fact));
		fb3.assertFact(convertToLogoList(fact));
		System.out.println(fb.toString());
		System.out.println("\nAsserting second fact...");
		fact = new ArrayList<Object>();
		fact.add("Felix"); fact.add(true); fact.add("cat");
		fb.assertFact(convertToLogoList(fact));
		fb3.assertFact(convertToLogoList(fact));
		System.out.println(fb.toString());
		System.out.println("\nAsserting third fact...");
		fact = new ArrayList<Object>();
		fact.add("Kitty"); fact.add(false); fact.add("guinea pig");
		fb.assertFact(convertToLogoList(fact));
		fb3.assertFact(convertToLogoList(fact));
		System.out.println(fb.toString());
		// testing assert of existing fact
		System.out.println("\nTrying to assert third fact again...");
		fb.assertFact(convertToLogoList(fact));
		System.out.println(fb.toString());
		System.out.println("\nAsserting fourth fact...");
		fact = new ArrayList<Object>();
		fact.add("Mieze"); fact.add(false); fact.add("cat");
		fb.assertFact(convertToLogoList(fact));
		fb3.assertFact(convertToLogoList(fact));
		System.out.println(fb.toString());
		
		System.out.println("\nAre fb and fb2 recursivley equal? " + fb.recursivelyEqual(fb2));
		System.out.println("\nAre fb and fb3 recursivley equal? " + fb.recursivelyEqual(fb3));
		
		fact = new ArrayList<Object>();
		fact.add("Boris"); fact.add(false); fact.add("dog");
		fb.assertFact(convertToLogoList(fact));
		System.out.println(fb.toString());
		System.out.println(fb.orderedFactsToString());
		fact.clear(); // = new ArrayList<Object>();
		fact.add("Boris"); fact.add(true); fact.add("dog");
		fb.assertFact(convertToLogoList(fact));
		System.out.println(fb.toString());
		
		FactBase test = new FactBase(new String[]{"x", "y", "cost", "alist"});
		fact = new ArrayList<Object>();
		fact.add(0); fact.add(-1); fact.add(5); fact.add(makeAList(new Object[]{2,3,4}));
		test.assertFact(convertToLogoList(fact));
		System.out.println(test);
		fact = new ArrayList<Object>();
		fact.add(0); fact.add(-2); fact.add(3); fact.add(makeAList(new Object[]{"alpha", "beta", "gamma", "delta"}));
		test.assertFact(convertToLogoList(fact));
		System.out.println(test);
		
		// unit test retrieve with condition
		System.out.println("\nTrying factbase RETRIEVAL from");
		System.out.println(fb.toString());
		System.out.println(fb.orderedFactsToString());
		System.out.println("Condition: type == \"dog\"");
//		List<List<Object>> retrieved = fb.retrieveFacts("type == \"dog\"", null);
//		System.out.println("RETRIEVED: " + retrieved.size() + " facts");
//		for (Iterator<List<Object>> i = retrieved.iterator(); i.hasNext(); ) {
//			System.out.println("FACT: " + fb.printFact(i.next()));
//		}

		System.out.println("\nTrying factbase RETRIEVAL from");
		System.out.println(test.toString());
		System.out.println("Condition: y > 0");		
//		retrieved = test.retrieveFacts("y > 0", null);
//		System.out.println("RETRIEVED: " + retrieved.size() + " facts");
//		for (Iterator<List<Object>> i = retrieved.iterator(); i.hasNext(); ) {
//			System.out.println("FACT: " + fb.printFact(i.next()));
//		}
//		System.out.println("Condition: cost >= 5");
//		retrieved = test.retrieveFacts("cost >= 5", null);
//		System.out.println("RETRIEVED: " + retrieved.size() + " facts");
//		for (Iterator<List<Object>> i = retrieved.iterator(); i.hasNext(); ) {
//			System.out.println("FACT: " + fb.printFact(i.next()));
//		}
//		System.out.println("Condition: size(alist) == 3");
//		retrieved = test.retrieveFacts("size(alist) == 3", null);
//		System.out.println("RETRIEVED: " + retrieved.size() + " facts");
//		for (Iterator<List<Object>> i = retrieved.iterator(); i.hasNext(); ) {
//			System.out.println("FACT: " + fb.printFact(i.next()));
//		}
		
		// unit test retraction
		System.out.println("\nTrying RETRACTION from");
		System.out.println(fb.toString());
		fact = new ArrayList<Object>();
		fact.add("Felix"); fact.add(true); fact.add("cat");
		System.out.println("\nretracting " + fb.printFact(convertToLogoList(fact)));
		System.out.println("\nSTATE OF INDEXED FIELDS BEFORE RETRACTION");
		for (int j = 0 ; j < fb.facts.size(); j++){
			fb.printIndexedField(j);
		}
		fb.removeFact(convertToLogoList(fact));
		System.out.println("\nResult is:");
		System.out.println(fb.toString());
		
		System.out.println("\nSTATE OF INDEXED FIELDS AFTER RETRACTION");
		for (int j = 0 ; j < fb.facts.size(); j++){
			fb.printIndexedField(j);
		}

		fact = new ArrayList<Object>();
		fact.add("Boris"); fact.add(false); fact.add("dog");
		System.out.println("\nretracting " + fb.printFact(convertToLogoList(fact)));
		fb.removeFact(convertToLogoList(fact));
		System.out.println("Result is:");
		System.out.println(fb.toString());
		
		System.out.println("\nSTATE OF INDEXED FIELDS AFTER RETRACTION");
		for (int j = 0 ; j < fb.facts.size(); j++){
			fb.printIndexedField(j);
		}

		System.out.println("size of fb: " + fb.size());
		System.out.println("\nTrying to retract a fact that's not there");
		fact = new ArrayList<Object>();
		fact.add("Boris"); fact.add(false); fact.add("dog");
		System.out.println("\nretracting " + fb.printFact(convertToLogoList(fact)));
		fb.removeFact(convertToLogoList(fact));
		System.out.println("Result is:");
		System.out.println(fb.toString());
		
		System.out.println("\nSTATE OF INDEXED FIELDS AFTER FAILED RETRACTION");
		for (int j = 0 ; j < fb.facts.size(); j++){
			fb.printIndexedField(j);
		}

		System.out.println("size of fb: " + fb.size());

		// does the retrieve still work?
		System.out.println("Condition: type == \"dog\"");
//		retrieved = fb.retrieveFacts("type == \"dog\"", null);
//		System.out.println("RETRIEVED: " + retrieved.size() + " facts");
//		for (Iterator<List<Object>> i = retrieved.iterator(); i.hasNext(); ) {
//			System.out.println("FACT: " + fb.printFact(i.next()));
//		}
//		System.out.println("Condition: length(name) >= 5");
//		retrieved = fb.retrieveFacts("length(name) >= 5", null);
//		System.out.println("RETRIEVED: " + retrieved.size() + " facts");
//		for (Iterator<List<Object>> i = retrieved.iterator(); i.hasNext(); ) {
//			System.out.println("FACT: " + fb.printFact(i.next()));
//		}
		
		// remove all left-over facts
		System.out.println("REMOVING all facts that are left");
		int n = fb.size();
		Random sample = new Random();
		while (n > 0) {
			// pick a random fact
			int i = sample.nextInt(n);
			LogoList randomFact = fb.retrieveFact(i);
			System.out.println("-- picking fact " + i + ": " + fb.printFact(randomFact));
			fb.removeFact(randomFact);
			System.out.println(fb.toString());			
			n = fb.size();
		}
		
		
		
	}
	
	/** Helper method: turns the given object array into an arraylist.
	 * 
	 * @param objects the objects to be made into a list
	 * @return an ArrayList<Object> of the given objects
	 */
	private static Object makeAList(Object[] objects) {
		List<Object> theList = new ArrayList<Object>();
		for (int i = 0; i < objects.length; i++){
			theList.add(objects[i]);
		}
		return theList;
	}

	/** Helper method: returns a string representation of {@link #orderedFacts}
	 * 
	 * @return the list of ordered facts as a String
	 */
	private String orderedFactsToString() {
		StringBuilder buf = new StringBuilder();
		for (LogoList f : orderedFacts) {
			// need to skip deleted entries
			if (f != null) {
				buf.append(printFact(f));
				buf.append("\n");
			}
		}
		return buf.toString();
	}

}
