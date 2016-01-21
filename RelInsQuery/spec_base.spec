# This file contains the basic structure of an input specification
# for relaxed query answering. The main inputs get their own block,
# e.g. [query] and additional parameters for changing the behaviour
# of the computation can be given generically in [parameters]

# relative or absolute path to the ontology file
# relative path will be seen relative to the java runtime execution path
# ontology block has to be specified before [query] [weights] and [measure] if PRIMITIVE is selected and explicit similarities are set
[ontology]
#examples/MOWL/mowlcorp/Film.owl.xml
examples/MOWL/mowlcorp/notations/nnotations01.owl.xml

# the query
# currently only manchester owl class expression format supported
[query]
#Arachnid and Broadcast and Cricketer and Town and (linksToWebsite some (CanadianFootballTeam))
GO_0000466 and GO_0005980 and PomBase_SPAC6G9.09c and (during some GO_0090375) and (transcriptionally_regulates some (GO_0006801 and PomBase_SPAC824.03c and (protease_inhibitor_of some (GO_0046474 and PomBase_SPBC18H10.19 and PomBase_SPBP23A10.13 and (transcriptionally_regulates some (GO_0006467 and PomBase_SPBP35G2.07))))))

# the successor discounting factor
# gets his own block so it will not be forgotten
[discount]
0.8

# weights of entities, key-value pairs, overriding base weights of all entities (DEFAULT: 1, can be specified in [parameters])
# this block is optional
[weights]
#A:10
#C:12
#r:0.1

# describe the primitive similarity measure
# first line: DEFAULT / PRIMITIVE
# if PRIMITIVE in first line, lines after that contain fixed similarity triples overriding the DEFAULT similarity between concept names
[measure]
DEFAULT
# PRIMITIVE
# A:C:0.5
# r:s:0.8

# either the similarity threshold for accepting relaxed instances (double)
# or a topk specification so that the best k results are returned regardless of their similarity (top[int])
[threshold]
0.01
#top10

# a generic and optional list of key-value pairs altering the behaviour of the application
[parameters]
# attempts to match class domain elements to domain elements of one of their instances (DEFAULT: true)
smallModel:true
# 4 normalizing modes "possible" currently only no. 2 implemented (DEFAULT: 2)
normalizing:2
# the weight all entities without explicit weight get (DEFAULT: 1)
baseWeight:1
# either give iterations or precision or topk, if more than one found, first one is respected, if none found, default behaviour is precision
# fixed number of iterations of main procedure
#iterations:500
# OR give an error threshold, if all values change less than the given percentage, stop the iteration (DEFAULT: 0.001)
precision:0.01
# specify the accuracy of decimal places throughout the entire computation (DEFAULT: 10) (be aware of weird behaviour for accuracy>15)
accuracy:5
# specify the log level: SEVERE, WARNING, INFO, FINE (DEFAULT: WARNING)
log:INFO
# give the path to an existing directory for output storage, if invalid or non-existing, default is used (DEFAULT: ./)
output:./out_example/

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
