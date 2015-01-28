[query]
A AND s SOME (B AND C)

[ontology]
examples/ex03-paper01.ofn

[discount]
0.5

[weights]
A:0.3
C:0.5
r:0.2

[measure]
DEFAULT

[threshold]
0.6

[parameters]
smallModel:true
normalizing:2 # modes of 1-4
iterations:10 # maximal iterations of main procedure
