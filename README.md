# netlogo-factbase
A NetLogo extension providing a fact base as a new data type, which will be particularly useful for those who want to program more cognitive models in their social simulations.

15/11/2019: Now updated to work with NetLogo 6.1! Please note that due to the syntax change regarding anonymous procedures in NetLogo 6, the syntax of some of the factbase primitives had to be adapted accordingly. Please refer to the documentation for details.

Factbase is an extension for NetLogo that introduces a new data type: a structured set of data called a “fact base”. A fact base can be thought of as a table of named columns (“fields”), where each row comprises an entry (“fact”). 

At creation, the user has to define the structure of the fact base, that means define the field names. Note that in keeping with the NetLogo philosophy of a type-free language, data types for fields are not specified. After creating a fact base, facts can be asserted, queried and retracted. Facts are represented as lists of values, with one value for each field and all values in the same order as defined by the list of field names. Duplicate facts are not allowed. Therefore, trying to assert a fact with all values identical to an already existing fact is ignored.

To be able to use indexing (and thus, faster retrieval), each fact is internally assigned an ID, starting with 0. A new fact will be assigned the highest number so far in use + 1. Retracting a fact will result in its ID being unassigned, thus trying to retrieve a retracted fact will generate an error.

--------
Extension developed under the DiDIY Project funded from the European Union’s Horizon  2020 research and innovation programme under grant agreement No 644344. The views expressed here do not necessarily reflect the views of the EC.
