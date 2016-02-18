# netlogo-factbase
A NetLogo extension providing a fact base as a new data type.

Factbase is an extension for NetLogo (version 5.2) that introduces a new data type: a structured set of data called a “fact base”. A fact base can be thought of as a table of named columns (“fields”), where each row comprises an entry (“fact”). 

At creation, the user has to define the structure of the fact base, that means define the field names. Note that in keeping with the NetLogo philosophy of a type-free language, data types for fields are not specified. After creating a fact base, facts can be asserted, queried and retracted. Facts are represented as lists of values, with one value for each field and all values in the same order as defined by the list of field names. Duplicate facts are not allowed. Therefore, trying to assert a fact with all values identical to an already existing fact is ignored.

To be able to use indexing (and thus, faster retrieval), each fact is internally assigned an ID, starting with 0. A new fact will be assigned the highest number so far in use + 1. Retracting a fact will result in its ID being unassigned, thus trying to retrieve a retracted fact will generate an error.
