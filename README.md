# DoubleArrayTrie
A Double Array Trie with Tail implementation in Java, with the following features:
* finds words that start with the given prefix;
* finds all words occurring in a given pattern from left to right;

  e.g: given the pattern _"wverticall"_ the words found will be _"vertical"_, _"call"_ and _"all"_;
* finds all the valid words from the permutation of the given letters;

  e.g: given the letters _"a e r d"_ the words found will be _"dare"_,_"dear"_,_"are"_,_"rad"_,_"red"_,_"read"_,_"ear"_ and _"era"_;
* finds all the words that fit in the given string expression according to the wildcards' position;

  e.g: given the expression _"s??ce"_, where the symbol _?_ is the wildcard, the words found will be _"slice"_, _"space"_, _"since"_, ecc ...
  
Useful for finding anagrams and words.

## Description

It is an improvement of the standard [Trie](https://github.com/Automatik/Trie), based on a double array trie and a tail. 

Thanks to the double array, it has better performance and memory occupation.

Credits:

* Aoe, J. An Efficient Digital Search Algorithm by Using a Double-Array Structure. IEEE Transactions on Software Engineering. Vol. 15, 9 (Sep 1989). pp. 1066-1077.
* https://linux.thai.net/~thep/datrie/
* https://github.com/digitalstain/DoubleArrayTrie

## Usage

Create and populate the DATrie
```
DoubleArrayTrie trie = new DoubleArrayTrie();
for(String word : myDictionary) {
  trie.insert(word);
```
