package edu.stanford.nlp.mt.process;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

import edu.stanford.nlp.mt.train.SymmetricalWordAlignment;
import edu.stanford.nlp.mt.util.ArraySequence;
import edu.stanford.nlp.mt.util.IString;
import edu.stanford.nlp.mt.util.IStrings;
import edu.stanford.nlp.mt.util.Sequence;
import edu.stanford.nlp.stats.ClassicCounter;
import edu.stanford.nlp.stats.Counter;


/**
 * Java version of the Moses compound splitter.
 * Train the model with mosesdecoder/scripts/generic/compound-splitter.perl.
 *  
 * @author Joern Wuebker
 *
 */
public class MosesCompoundSplitter {
  
  private static String[] FILLERS = {"", "s", "es"};
  private static int MIN_SIZE = 3; // the minimum number of characters is actually MIN_SIZE + 1
  private static int MIN_COUNT = 5;
  private static int MAX_COUNT = 5;

  
  private Counter<String> lcModel;
  private HashMap<String, String> trueCase;
  
  public MosesCompoundSplitter(String modelFileName) {
    try {
      loadModel(modelFileName);
    }
    catch (IOException e) {
      System.err.println("ERROR: could not load model from file " + modelFileName);
      e.printStackTrace();
      System.exit(-1);
    }
  }
  
  private void loadModel(String modelFileName) throws IOException {
    LineNumberReader reader = new LineNumberReader(new FileReader(modelFileName));
    
    lcModel = new ClassicCounter<String>();
    trueCase = new HashMap<>();
    
    int minCnt = Math.min(MAX_COUNT, MIN_COUNT);
    
    for (String line; (line = reader.readLine()) != null;) {
      String[] input = line.split("\t");
      if(input.length != 3) {
        reader.close();
        throw new IOException("Illegal input in model file, line " + reader.getLineNumber() + ": " + line);
      }
      int cnt = Integer.parseInt(input[2]);
      String tc = input[1];
      if(cnt < minCnt || tc.length() < MIN_SIZE + 1) continue; // these will never be used for splitting anyway
      
      String lc = tc.toLowerCase();
      // use the most frequent casing
      if(lcModel.getCount(lc) < cnt) {
        lcModel.setCount(lc, cnt);
        trueCase.put(lc, tc);
        //System.err.println("adding: " + input[1] + " ::: " + input[2]);
      }
    }
    reader.close();
  }
  
  public String process(String input) {
    Sequence<IString> tokenized = IStrings.toIStringSequence(input.split("\\s+"));
    return process(tokenized).e().toString();
  }
  
  
  public SymmetricalWordAlignment process(Sequence<IString> tokenizedInput) {
    int size = tokenizedInput.size();
    Sequence<IString> result = new ArraySequence<IString>(new IString[]{});
    int sizes[] = new int[size]; 
    
    int pos = 0;
    for(IString word : tokenizedInput) {
      Sequence<IString> split = splitWord(word);
      result = result.concat(split);
      sizes[pos] = split.size();
      ++pos;
    }

    SymmetricalWordAlignment rv = new SymmetricalWordAlignment(tokenizedInput, result);
    
    for(int i = 0, j = 0; i < size; ++i) {
      for(int k = 0; k < sizes[i]; ++k, ++j) rv.addAlign(i, j);
    }
    return rv;
  }
  
  public SymmetricalWordAlignment process(SymmetricalWordAlignment tokenizedInput) {
    SymmetricalWordAlignment decompounded = process(tokenizedInput.e());
    return projectAlignment(tokenizedInput, decompounded);
  }
  
  public SymmetricalWordAlignment projectAlignment(SymmetricalWordAlignment tokenizedInput, SymmetricalWordAlignment decompounded) {
    assert(tokenizedInput.e().equals(decompounded.f()));
    SymmetricalWordAlignment rv = new SymmetricalWordAlignment(tokenizedInput.f(), decompounded.e());
    
    for(int i = 0; i < tokenizedInput.fSize(); ++i)
      for(int j : tokenizedInput.f2e(i))
        for(int k : decompounded.f2e(j))
          rv.addAlign(i,k);
    
    return rv;
  }

  
  private class Match {
    int start;
    int cnt;
    String tc;
    
    Match(int s, int c, String w) {
      start = s;
      cnt = c;
      tc = w;
    }
  }
    
  private Sequence<IString> splitWord(IString word) {
    List<List<Match>> matches = new ArrayList<List<Match>>();
    for(int i = 0; i < word.length(); ++i) matches.add(null);
    
    String lc = word.toString().toLowerCase();
    if(lcModel.getCount(lc) >= MAX_COUNT || !containsLetter(lc)) return new ArraySequence<IString>(new IString[]{ word });
    // TODO: do something with the word
    int length = lc.length();
    for(int end = MIN_SIZE; end < length; ++end) {
      for(int start = 0; start <= end - MIN_SIZE; ++start) {
        if(start != 0 && matches.get(start - 1) == null) continue;
        // we do not want fillers at the beginning of a word
        // FILLERS[0] == "";
        int maxI = start == 0 ? 1 : FILLERS.length;
        for(int i = 0; i < maxI; ++i) {
          if(end - start - FILLERS[i].length() < MIN_SIZE) continue;
          if(!lc.substring(start, start + FILLERS[i].length()).equals(FILLERS[i])) continue;
          String subword = lc.substring(start + FILLERS[i].length(), end + 1);
          if(lcModel.getCount(subword) < MIN_COUNT) continue;
          if(matches.get(end) == null) matches.set(end, new ArrayList<>());
          matches.get(end).add(new Match(start, (int) lcModel.getCount(subword), trueCase.get(subword)));
          //System.err.println("add match at pos " + end + ": " + trueCase.get(subword) + " " + lcModel.getCount(subword));
        }
      }
    }
    
    if(matches.get(matches.size() - 1) == null) return new ArraySequence<IString>(new IString[]{ word });//no possible split

    int[] iterator = new int[word.length()];
    for(int i = 0; i < iterator.length; ++i) iterator[i] = 0;
    double bestScore = 0.0;
    List<String> bestSplit = new ArrayList<>();
    List<String> reverseSplit = new ArrayList<>();
    
    boolean finished = false;
    while(!finished) {
      int pos = word.length() - 1;
      reverseSplit.clear();
      double score = 1;
      double num = 0;
      Stack<Integer> splitPositions = new Stack<>();

      while(pos >= 0) {
        List<Match> posMatches = matches.get(pos);
        if(posMatches == null || posMatches.size() <= iterator[pos]) break;
        Match match = posMatches.get(iterator[pos]);
        
        reverseSplit.add(match.tc);
        score *= match.cnt;
        ++num;
        splitPositions.push(pos);
        pos = match.start - 1;
      }
      
      score = Math.pow(score, 1/num);
      
      if(score > bestScore) {
        bestScore = score;
        bestSplit = new ArrayList<>(reverseSplit);
      }
      
      int splitPos = -1;
      while(splitPos < word.length() - 1) {
        splitPos = splitPositions.pop();
        if(iterator[splitPos] < matches.get(splitPos).size() - 1) {
          iterator[splitPos]++;
          break;
        }
        else if(splitPos < word.length() - 1) {
          iterator[splitPos] = 0;
        }
        else {
          finished = true;
          break;
        }
      }
    }
   
    Collections.reverse(bestSplit);
   
    return IStrings.toIStringSequence(bestSplit);
  }
  
  private boolean containsLetter(String s) {
    for(int i = 0; i < s.length(); ++i) {
      char ch = s.charAt(i);
      if((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z') ) return true;
    }
    return false;
  }
  
  private static void usage() {
    System.err.println("Usage:");
    System.err.println("java " + MosesCompoundSplitter.class.getName() + " [modelFile] < [inputFile] > [outputFile] ");
    System.exit(0);
  }
  
  public static void main(String[] args) {
    if(args.length < 1) usage();
    
    MosesCompoundSplitter splitter = new MosesCompoundSplitter(args[0]);
    
    try {
      BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
      String input;
      while((input = reader.readLine() ) != null) {
        System.out.println(splitter.process(input));
      }
    } catch (IOException e) {
      e.printStackTrace();
    } 
  }
  
}
