package trie;

import util.IntegerArrayList;
import util.IntegerList;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.*;

public class DoubleArrayTrie implements Serializable {

    /**
     * The default alphabet size correspond to the english alphabet
     */
    public static final int ENGLISH_ALPHABET_SIZE = 26;

    public static final int FIRST_ALPHABET_CHARACTER = 'a';

    /**
     * To avoid confusion between words like <tt>THE</tt>
     * and <tt>THEN</tt>, a special endmarker symbol <tt>#</tt>
     * is added to the ends of all keys, so no prefix of a key
     * can be a key itself
     */
    public static final char ENDMARKER = '#';

    /**
     * The symbol used as wildcard in the following method
     * @see #query(String expression)
     */
    public static final char WILDCARD = '?';

    /**
     * The root's index in the DoubleArrayTrie
     */
    private static final int ROOT = 1;

    /**
     * Be DA_SIZE the size of the double array.
     * DA_SIZE_INDEX is the position of this value in the check array
     */
    private static final int DA_SIZE_INDEX = 1;

    private static final int EMPTY_VALUE = 0;

    private static final long serialVersionUID = -8846901845572131665L;

    private int alphabetSize;

    /**
     * The endmarker is treated as a letter after the last
     * letter of the alphabet. So the value of endmarkerOffset is set
     * equal to alphabetSize + 1.
     */
    private int endmarkerOffset;

    private IntegerList base;

    private IntegerList check;

    private ArrayList<String> tail; // if i < tail.size() and tail[i] == null, then it's an accepting word

    /**
     * Auxiliary storage for speeding up the process of searching for free positions
     */
    private transient TreeSet<Integer> freePositions;

    public DoubleArrayTrie() {
        this(ENGLISH_ALPHABET_SIZE);
    }

    public DoubleArrayTrie(int alphabetSize) {
        base = new IntegerArrayList();
        check = new IntegerArrayList();
        tail = new ArrayList<>();
        tail.add(null); //The index 0 will not be used because it's equal to EMPTY_VALUE
        freePositions = new TreeSet<>();
        setAlphabetSize(alphabetSize);
        endmarkerOffset = alphabetSize + 1;


        //index 0 will not be used
        base.add(0, EMPTY_VALUE);
        check.add(0, EMPTY_VALUE);
        //insert the root
        base.add(ROOT, 1);
        //size of the double array (called also DA-SIZE)
        check.add(DA_SIZE_INDEX, 1);
    }

    /**
     * @return the current alphabet size for this Trie
     */
    public int getAlphabetSize() {
        return alphabetSize;
    }

    /**
     * Sets the alphabet size for this Trie
     * @param alphabetSize the length of the alphabet corresponding to nodes' children length
     */
    private void setAlphabetSize(int alphabetSize) {
        if(alphabetSize < 1)
            throw new IllegalArgumentException("Alphabet size must be positive");
        this.alphabetSize = alphabetSize;
    }

    private int getDASize() {
        return getCheck(DA_SIZE_INDEX);
    }

    //Still to be tested correctly
    public void trimToSize() {
        int newSize = getCheck(DA_SIZE_INDEX);
        base.trimToSize(newSize);
        check.trimToSize(newSize);
        setCheck(DA_SIZE_INDEX, newSize);

        Integer freePos;
        while((freePos = freePositions.higher(newSize))!= null)
            freePositions.remove(freePos);
    }

    /**
     * Returns true if the word is in this Trie
     * @param word the word to search in this Trie
     * @return true if the word is in this Trie
     */
    public boolean contains(String word) {
        if(word == null)
            throw new IllegalArgumentException("String word argument is null");
        //Append the endmarker to the word
        String mWord = appendEndmarker(word);

        int currentNode = ROOT;
        int i = 0;
        while(i < mWord.length() && getBase(currentNode) > EMPTY_VALUE) {
            int offset = getOffset(mWord.charAt(i));
            int nextNode = getBase(currentNode) + offset;
            if(nextNode > getDASize() || getCheck(nextNode) != currentNode)
                return false;
            else
                currentNode = nextNode;
            i++;
        }
        if(i == mWord.length())
            return true;
        else {
            int tailPos = -getBase(currentNode);
            if(tailPos >= tail.size())
                return false;
            String temp = getTail(tailPos); //it's never null because we store always at least the endmarker
            String remainingInput = mWord.substring(i);
            return remainingInput.equals(temp);
        }
    }

    /**
     * Inserts a word into the DoubleArrayTrie
     * @param word the word to insert
     * @throws IllegalArgumentException if the word is null
     */
    public void insert(String word) {
        if(word == null)
            throw new IllegalArgumentException("String word argument is null");
        //Append the endmarker to the word
        String mWord = appendEndmarker(word);

        //Initialize the current node number currentNode and the input position i
        //to ROOT and 0, respectively.
        int currentNode = ROOT;
        int i = 0;
        //The current input string character
        int offset;
        while(i < mWord.length() && getBase(currentNode) > EMPTY_VALUE) {
            //Set the next node number nextNode to BASE[currentNode] + offset
            offset = getOffset(mWord.charAt(i));
            int nextNode = getBase(currentNode) + offset;
            ensureReachableIndex(nextNode);

            //If nextNode exceeds DA-SIZE or CHECK[nextNode] is unequal to currentNode
            //Then no edge is defined
            if(nextNode > getDASize() || getCheck(nextNode) != currentNode) {
                A_insert(currentNode, mWord.substring(i));
                return;
            }
            else
                currentNode = nextNode;
            i++;
        }
        //If i reaches the last position then the word is already in the trie
        if(i == mWord.length())
            return;
        //Otherwise the last letters of the word are not in the trie, and we put them in the TAIL
        String temp = getTail(-getBase(currentNode));
        String remainingInput = mWord.substring(i);
        if(!remainingInput.equals(temp)) {
            String longestPrefix = longestCommonPrefix(remainingInput, temp);
            B_insert(currentNode, longestPrefix, remainingInput.substring(longestPrefix.length()), temp.substring(longestPrefix.length()));
        }
    }

    /**
     * Append an edge from currentNode + offset to a new Node in the double-array and stores
     * the remaining input in the TAIL
     */
    private void A_insert(int currentNode, String remainingInput) {
        //Set the next node number nextNode to BASE[currentNode] + offset
        int offset = getOffset(remainingInput.charAt(0));
        int nextNode = getBase(currentNode) + offset;
        ensureReachableIndex(nextNode); //Added by me

        //If the node is already defined for another string, change either BASE[currentNode]
        //or BASE[k] for k = CHECK[nextNode]
        if(getCheck(nextNode) != EMPTY_VALUE) {
            //A node k already present in that position
            int kNode = getCheck(nextNode);
            TreeSet<Integer> currentNodeList = setList(currentNode);
            TreeSet<Integer> kNodeList = setList(kNode);
            if(currentNodeList.size() + 1 < kNodeList.size()) {
                //find new position to determine BASE[currentNode] such that
                //CHECK[BASE[currentNode] + offset] = EMPTY_VALUE for all offsets in (currentNodeList and offset)
                currentNode = modify(currentNode, currentNode, offset, currentNodeList);
            } else {
                //find new position to determine BASE[k] such that
                //CHECK[BASE[currentNode] + offset] != CHECK[BASE[k] + offset] for all offsets in kNodeList,
                //and set currentNode to the returned node number by this modify()
                currentNode = modify(currentNode, kNode, EMPTY_VALUE, kNodeList);
            }
        }
        //Insert String
        insertStringInTail(currentNode, remainingInput);
    }

    /**
     * Append an edge
     */
    private void B_insert(int currentNode, String remainingInputLongestPrefix,
                          String remainingSuffix, String tempSuffix) {
        //Copy -BASE[currentNode] to oldPos
        int oldPos = getBase(currentNode); //it's a negative number

        //Define a sequence of edges for each character in remainingInputLongestPrefix
        //on the double-array:
        TreeSet<Integer> offsets = new TreeSet<>();
        for (char c : remainingInputLongestPrefix.toCharArray()) {
            int offset = getOffset(c);
            offsets.add(offset);

            //determine a new BASE[currentNode] as xCheck(offset)
            int newBase = xCheck(offsets);
            setBase(currentNode, newBase);
            //set CHECK[BASE[currentNode] + c] to currentNode
            setCheck(getBase(currentNode) + offset, currentNode);
            //set the next node number currentNode to BASE[currentNode] + c
            currentNode = getBase(currentNode) + offset;

            offsets.remove(offset);
        }
        assert offsets.isEmpty();

        //Determine a new BASE[currentNode]
        if(!remainingSuffix.isEmpty())
            offsets.add(getOffset(remainingSuffix.charAt(0)));
        if(!tempSuffix.isEmpty())
            offsets.add(getOffset(tempSuffix.charAt(0)));
        assert !offsets.isEmpty();
        setBase(currentNode, xCheck(offsets));

        //tempSuffix is overwritten in the original position oldPos of TAIL
        insertStringInTail(currentNode, tempSuffix, oldPos);

        //remainingSuffix is written in a new position of TAIL
        insertStringInTail(currentNode, remainingSuffix);
    }

    private int modify(int currentNode, int hNode, int add, TreeSet<Integer> org) {
        //Copy BASE[hNode] to oldBase
        int oldBase = getBase(hNode);

        TreeSet<Integer> temp = (TreeSet<Integer>) org.clone();
        if(add != EMPTY_VALUE)
            temp.add(add);
        //Determine a new BASE[hNode]
        int newBase = xCheck(temp);
        setBase(hNode, newBase);

        if(org.isEmpty())
            return currentNode;

        for(Integer c : org) {
            //Set an old node number oldNode to oldBase + c
            int oldNode = oldBase + c; //t
            //Set a new node number newNode to BASE[hNode] + c
            int newNode = getBase(hNode) + c; //t'
            //Copy the original BASE[oldNode] into BASE[newNode] and
            //set CHECK[newNode] to hNode
            setBase(newNode, getBase(oldNode));
            setCheck(newNode, hNode);


            //Replace an old node number oldNode in CHECK by a new newNode
            int tempBase = getBase(oldNode);
            if(tempBase > EMPTY_VALUE) {
                ensureReachableIndex(tempBase + alphabetSize + 1); //include endmarker

                //Set CHECK[q] to newNode for each q such that
                //CHECK[BASE[oldNode] + offset] = oldNode and q = BASE[oldNode] + offset

                //<= alphabetSize + 1 because it includes the endmarker
                //offset = 1 because we set 'a' = 1
                for (int offset = 1; offset <= alphabetSize + 1; offset++) {
                    if(getCheck(tempBase + offset) == oldNode)
                        setCheck(tempBase + offset, newNode);
                }
            }
            if(currentNode == oldNode)
                currentNode = newNode;

            //Initialize BASE[oldNode] and CHECK[oldNode] to EMPTY_VALUE
            setBase(oldNode, EMPTY_VALUE);
            setCheck(oldNode, EMPTY_VALUE);
        }
        return currentNode;
    }

    private void insertStringInTail(int fromNode, String string) {
        //We use EMPTY_VALUE for adding a new string, instead of replacing one
        insertStringInTail(fromNode, string, EMPTY_VALUE);
    }

    private void insertStringInTail(int fromNode, String string, int toTailPosition) {
        toTailPosition = Math.abs(toTailPosition);
        int nextNode = getBase(fromNode) + getOffset(string.charAt(0));
        String toAdd = (string.length() > 1) ? string.substring(1) : null;
        if(toTailPosition == EMPTY_VALUE) {
            toTailPosition = tail.size();
            addTail(toAdd);
        } else
            setTail(toTailPosition, toAdd);

        setBase(nextNode, -toTailPosition);
        setCheck(nextNode, fromNode);
    }

    /**
     * Return the minimum q such that q > 0 and
     * CHECK[q + c] = 0 for all <<tt>c</tt> in list
     */
    private int xCheck(TreeSet<Integer> list) {
        int minOffset = list.first();
        int maxOffset = list.last();
        int shift = 0;
        boolean gapsFound;
        Iterator<Integer> it = freePositions.iterator();
        while(it.hasNext()) {
            int freePos = it.next();
            int offset = freePos - minOffset;
            if(offset + maxOffset >= base.size()) { //base.size()
                ensureReachableIndex(offset + maxOffset);
                it = freePositions.iterator();
                shift++;
                for(int s = 0; s < shift; s++)
                    it.next();
            }

            gapsFound = offset > EMPTY_VALUE;
            Iterator<Integer> it2 = list.iterator();
            while(gapsFound && it2.hasNext()) {
                if(!freePositions.contains(offset + it2.next()))
                    gapsFound = false;
            }
            if(gapsFound)
                return offset;
            shift++;
        }

        //If there are no gaps
        int neededPositions = maxOffset - minOffset + 1;
        ensureReachableIndex(base.size() + neededPositions - 1);
        int q = base.size() - neededPositions - minOffset;
        assert q > 0;
        return q;
    }

    private String longestCommonPrefix(String a, String b) {
        int minLength = Math.min(a.length(), b.length());
        for(int i = 0; i < minLength; i++)
            if(a.charAt(i) != b.charAt(i))
                return a.substring(0, i);
        return a.substring(0, minLength);
    }

    /**
     * Return a set of symbols <<tt>a</tt> such that
     * CHECK[BASE[node] + a] = node
     */
    private TreeSet<Integer> setList(int node) {
        TreeSet<Integer> charsOffset = new TreeSet<>();
        int offset = 1; // = 1 because 'a' = 1
        //<= alphabetSize + 1 for the endmarker
        while(offset <= alphabetSize + 1) {
            int tempNext = getBase(node) + offset;
            if(tempNext < getDASize() && getCheck(tempNext) == node)
                charsOffset.add(offset);
            offset++;
        }
        return charsOffset;
    }

    private void ensureReachableIndex(int limit) {
        //getDASize return the current nodes used, not the actual size
        while(base.size() <= limit) {
            base.add(EMPTY_VALUE);
            check.add(EMPTY_VALUE);

            //All new positions are free by default
            freePositions.add(base.size() - 1);
        }
    }

    private int getBase(int index) {
        return base.get(index);
    }

    private int getCheck(int index) {
        return check.get(index);
    }

    private String getTail(int index) {
        return tail.get(index);
    }

    private void setBase(int index, int value) {
        base.set(index, value);
        if(value == EMPTY_VALUE)
            freePositions.add(index);
        else {
            setCheck(DA_SIZE_INDEX, Math.max(getCheck(DA_SIZE_INDEX), index + 1));
            freePositions.remove(index);
        }
    }

    private void setCheck(int index, int value) {
        check.set(index, value);
        if(index != DA_SIZE_INDEX) {
            if (value == EMPTY_VALUE)
                freePositions.add(index);
            else
                freePositions.remove(index);
        }
    }

    private void addTail(String s) {
        tail.add(s);
    }

    private void setTail(int index, String s) {
        tail.set(index, s);
    }

    private int getOffset(int letter) {
        //+ 1 because 'a' = 1
        return (letter == ENDMARKER) ? endmarkerOffset : letter - FIRST_ALPHABET_CHARACTER + 1;
    }

    private char getCharFromOffset(int offset) {
        return (char)((offset == endmarkerOffset) ? ENDMARKER : offset + FIRST_ALPHABET_CHARACTER - 1);
    }

    private String appendEndmarker(String word) {
        return word + ENDMARKER;
    }

    /**
     * Find all words which starts with the given prefix
     * @param prefix the characters with which start the words
     * @return ArrayList with all the words that starts with the given prefix
     */
    public ArrayList<String> startsWith(String prefix){
        //Append the endmarker to the word or remove it
        int currentNode = getTrieNode(prefix);
        if(currentNode == EMPTY_VALUE)
            return new ArrayList<>();
        ArrayList<String> words = new ArrayList<>();
        ArrayDeque<Integer> nodesQueue = new ArrayDeque<>();
        ArrayDeque<String> prefixQueue = new ArrayDeque<>();
        //int tailPos = getBase(currentNode);
        nodesQueue.offer(currentNode);
        prefixQueue.offer(prefix);
        while(!nodesQueue.isEmpty()) {
            Integer current = nodesQueue.poll();
            String prefixTemp = prefixQueue.poll();
            if(getBase(current) < EMPTY_VALUE)
                words.add(composeWord(prefixTemp, getBase(current)));
            else {
                for(int j = 1; j <= alphabetSize + 1; j++) {
                    int tempNode = getBase(current) + j;
                    if (tempNode < getDASize() && getCheck(tempNode) == current) {
                        nodesQueue.offer(tempNode);
                        prefixQueue.offer(composeWord(prefixTemp, getCharFromOffset(j)));
                    }
                }
            }
        }
        return words;
    }

    /**
     * Finds all words occurring in a given pattern from left to right.
     * e.g: given the pattern "wverticall" the words found will be
     * "vertical", "call" and "all".
     * @param pattern the letters in which to search words
     * @return ArrayList containing all the words found
     */
    public ArrayList<String> match(String pattern) {
        ArrayList<String> words = new ArrayList<>();
        for(int i = 0; i < pattern.length(); i++) {
            char beginLetter = pattern.charAt(i);
            int currentNode = getBase(ROOT) + getOffset(beginLetter);
            String prefix = "" + beginLetter;
            words.add(prefix);
            for(int j = i+1; j < pattern.length(); j++) {
                char nextLetter = pattern.charAt(j);
                prefix += nextLetter;
                if(getBase(currentNode) >= EMPTY_VALUE) {
                    int nextNode = getBase(currentNode) + getOffset(nextLetter);
                    if(nextNode < getDASize()) {
                        if (getCheck(nextNode) == currentNode && getBase(nextNode) < EMPTY_VALUE) {
                            String word = composeWord(prefix, getBase(nextNode));
                            String suffix = pattern.substring(i);
                            //Check if the letters in the tail are the prefix for the remaining pattern
                            if(suffix.startsWith(word))
                                words.add(word);
                        }
                        if (getBase(nextNode) >= EMPTY_VALUE) {
                            int tailNode = getBase(nextNode) + getOffset(ENDMARKER);
                            if (getCheck(tailNode) == nextNode && getBase(tailNode) < EMPTY_VALUE)
                                words.add(composeWord(prefix, ENDMARKER, getBase(tailNode)));
                        }
                        currentNode = nextNode;
                    }
                }
            }
        }
        return words;
    }

    /**
     * Finds all the valid words from the permutation of the given letters.
     * e.g: given the letters "a e r d" the words found will be
     * "dare","dear","are","rad","red","read","ear" and "era"
     * @param letters the characters to permute to find words
     * @return ArrayList with all the words found
     */
    public ArrayList<String> permute(char[] letters) {
        ArrayList<String> words = new ArrayList<>();
        Queue<Character> queue = new ArrayDeque<>(letters.length);
        for(char c : letters)
            queue.offer(c);
        //permute(queue, "", words);
        permute(queue, "", ROOT, words);
        return words;
    }

    private void permute(Queue<Character> letters, String current, int node, List<String> words) {
        if(!current.equals("")){
            if(getBase(node) < EMPTY_VALUE) {
                String word = composeWord(current, getBase(node));
                String str = word.substring(current.length());
//                String certain = Arrays.stream(array).map(Object::toString).collect(Collectors.joining());
                Queue<Character> certain = new ArrayDeque<>(letters);
                if(str.equals("") || containsOnly(str, certain))
                    words.add(word);
                return;
            } else {
                int nextNode = getBase(node) + getOffset(ENDMARKER);
                if (nextNode < getDASize() && getCheck(nextNode) == node && getBase(nextNode) < EMPTY_VALUE)
                    words.add(composeWord(current, ENDMARKER, getBase(nextNode)));
            }
        }
        if(!letters.isEmpty()){
            Character[] array = new Character[letters.size()];
            array = letters.toArray(array);
            for(Character ch : array){
                Queue<Character> temp = new ArrayDeque<>(array.length);
                temp.addAll(letters);
                temp.remove(ch);
                int offset = getOffset(ch);
                int nextNode = getBase(node) + offset;
                if(nextNode < getDASize() && getCheck(nextNode) == node)
                    permute(temp, current + ch, nextNode, words);
            }
        }
    }

    /**
     * Finds all the words that fit in the given string expression according to the wildcards' position.
     * e.g: given the expression "s??ce" the words found will be "slice", "space", "since", ecc ...
     * @see #WILDCARD for the character to use as wildcard
     * @param expression the string containing the letters and wildcards
     * @return ArrayList containing all the words found
     */
    public ArrayList<String> query(String expression) {
        ArrayList<String> words = new ArrayList<>();
        query(expression.toCharArray(),0, ROOT, "", words);
        return words;
    }

    private void query(char[] expression, int index, int root, String current, List<String> words){
        String word;
        if(getBase(root) < EMPTY_VALUE) {
            word = composeWord(current, getBase(root));
            if(word.length() == expression.length && areEquals(word.substring(index),new String(expression).substring(index)))
                words.add(word);
            return;
        }
        else {
            int nextNode = getBase(root) + getOffset(ENDMARKER);
            if (nextNode < getDASize() && getCheck(nextNode) == root && getBase(nextNode) < EMPTY_VALUE
                    && (word = composeWord(current, ENDMARKER, getBase(nextNode))).length() == expression.length)
                words.add(word);
        }
        if(index < expression.length) {
            char next = expression[index];
            if(next == WILDCARD) {
                int nextIndex = index + 1;
                for(int i = 1; i <= alphabetSize; i++) { //exclude the endmarker
                    int nextNode = getBase(root) + i;
                    if (nextNode < getDASize() && getCheck(nextNode) == root)
                        query(expression, nextIndex, nextNode, current + getCharFromOffset(i), words);
                }
            } else {
                int nextNode = getBase(root) + getOffset(next);
                if(nextNode < getDASize() && getCheck(nextNode) == root)
                    query(expression, index + 1, nextNode, current + next, words);
            }
        }
    }

    /**
     * Gets the TrieNode whose children have all the given prefix
     * @param prefix the prefix with which the words start
     * @return int node
     */
    private int getTrieNode(String prefix) {
        int currentNode = ROOT;
        int i = 0;
        while(i < prefix.length() && getBase(currentNode) > EMPTY_VALUE) {
            int offset = getOffset(prefix.charAt(i));
            int nextNode = getBase(currentNode) + offset;
            if(nextNode > getDASize() || getCheck(nextNode) != currentNode)
                return EMPTY_VALUE;
            else
                currentNode = nextNode;
            i++;
        }
        return (i == prefix.length()) ? currentNode : EMPTY_VALUE;
    }

    private String composeWord(String prefix, char add) {
        return (add == ENDMARKER) ? prefix : prefix + add;
    }

    private String composeWord(String prefix, char add, int tailPosition) {
        //if add = endmarker then tail is null
        return (add == ENDMARKER) ? prefix : composeWord(prefix + add, tailPosition);
    }

    private String composeWord(String prefix, int tailPosition) {
        String tail = getTail(-tailPosition);
        if(tail == null)
            return prefix;
        if(tail.length() == 1) //it contains only the endmarker
            return prefix;
        return prefix + tail.substring(0, tail.length() - 1); //remove the endmarker
    }

    /**
     * Check if two strings are equals, but considering the
     * wildcard '?' as equal to every character
     */
    private static boolean areEquals(String s1, String s2) {
        if(s1.length() != s2.length())
            return false;
        int i = 0;
        char[] c1 = s1.toCharArray();
        char[] c2 = s2.toCharArray();
        while(i < c1.length) {
            if(c1[i] != c2[i] && c1[i] != WILDCARD && c2[i] != WILDCARD)
                return false;
            i++;
        }
        return true;
    }

    /**
     * Determine if a string is made up of only certain characters
     */
    private static boolean containsOnly(String str, String certain) {
        //This will match an empty string always. To change this, replace the * to +
        return !certain.equals("") && str.matches("[" + certain + "]*");
    }

    private static boolean containsOnly(String str, Queue<Character> certain) {
        if(certain.size() == 0)
            return false;
        boolean contained = true;
        char[] array = str.toCharArray();
        int i = 0;
        while(contained && i < array.length) {
            char c = array[i];
            if(certain.contains(c))
                certain.remove(c);
            else
                contained = false;
            i++;
        }
        return contained;
    }

    private void writeObject(ObjectOutputStream s) throws IOException {
        s.defaultWriteObject();
    }

    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
        s.defaultReadObject();
        freePositions = new TreeSet<>();

        //scan for empty positions? slows down a little the read operation
        //scanEmptyPositions();
    }

    private void scanEmptyPositions() {
        int sz = getCheck(DA_SIZE_INDEX);
        for(int i = 0; i < sz; i++)
            if(getCheck(i) == EMPTY_VALUE)
                freePositions.add(i);
    }
}
