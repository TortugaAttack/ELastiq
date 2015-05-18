# This file contains the basic structure of an input specification
# for relaxed query answering. The main inputs get their own block,
# e.g. [query] and additional parameters for changing the behaviour
# of the computation can be given generically in [parameters]

# relative or absolute path to the ontology file
# relative path will be seen relative to the java runtime execution path
# ontology block has to be specified before [query] [weights] and [measure] if PRIMITIVE is selected and explicit similarities are set
[ontology]
bikes.owl

# the query
# currently only manchester owl class expression format supported
[query]
Racebike and hasColor some Black and allowsUse some Offroad and allowsUse some Street

# the successor discounting factor
# gets his own block so it will not be forgotten
[discount]
0.8

# weights of entities, key-value pairs, overriding base weights of all entities (DEFAULT: 1, can be specified in [parameters])
# this block is optional
[weights]
# Black color, very important!
Black:10
# reduce importance for the fact of just being any color
Color:0.1
# Offroad more important than Street usage
Offroad:5
Street:0.3

# describe the primitive similarity measure
# first line: DEFAULT / PRIMITIVE
# if PRIMITIVE in first line, lines after that contain fixed similarity triples overriding the DEFAULT similarity between concept names
[measure]
PRIMITIVE
# both very similar
Black:DarkBlue:0.9

# simply the threshold value for accepting relaxed instances
[threshold]
0.7

# a generic and optional list of key-value pairs altering the behaviour of the application
[parameters]
# attempts to match class domain elements to domain elements of one of their instances (DEFAULT: true)
smallModel:true
# 4 normalizing modes "possible" currently only no. 2 implemented (DEFAULT: 2)
normalizing:2
# the weight all entities without explicit weight get (DEFAULT: 1)
baseWeight:1
# either give iterations or precision, if both found, first one is respected, if none found, default behaviour
# fixed number of iterations of main procedure (NOT DEFAULT METHOD)
# iterations:500
# OR give an error threshold, if all values change less than the given percentage, stop the iteration (DEFAULT: 0.001)
precision:0.01
# specify the accuracy of decimal places throughout the entire computation (DEFAULT: 10) (be aware of weird behaviour for accuracy>15)
accuracy:5
# specify the log level: SEVERE, WARNING, INFO, FINE (DEFAULT: WARNING)
log:INFO
# give the path to an existing directory for output storage, if invalid or non-existing, default is used (DEFAULT: ./)
output:./out_adjusted/

# specify what outputs you require
[output]
# ASCII readable table of interesting similarity values and their development
ASCII
# CSV table of interesting similarity values and their development
CSV
# a list of the actual individuals that are relaxed instances of the query including their similarity
INSTANCES
# a simple table of computational statistics with min, max and mean values
STATISTICS
# have the time tracking results be stored in a seperate file in addition to the LOG
TIMES
