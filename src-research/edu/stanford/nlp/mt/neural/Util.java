/**
 * 
 */
package edu.stanford.nlp.mt.neural;

import edu.stanford.nlp.math.ArrayMath;
import edu.stanford.nlp.mt.base.PhraseAlignment;

import java.util.ArrayList;
import java.util.List;

import edu.stanford.nlp.mt.base.IString;
import edu.stanford.nlp.mt.base.Sequence;
import edu.stanford.nlp.mt.base.SimpleSequence;

/**
 * @author Minh-Thang Luong <lmthang@stanford.edu>, created on Mar 6, 2014
 *
 */
public class Util {
  public static int[] reverseArray(int[] values){
    int numElements = values.length;
    int[] reverseValues = new int[numElements];
    for(int i=0; i<numElements; i++){
      reverseValues[i] = values[numElements-i-1];
    }
    return reverseValues;
  }

  public static String intArrayToString(int[] values){
    StringBuffer sb = new StringBuffer();
    for (int value : values) {
      sb.append(value + " ");
    }
    sb.deleteCharAt(sb.length()-1);
    return sb.toString();
  }
  
  /**
   * Find average of source positions that correspond to the current tgtPos w.r.t to the give phrase alignment.
   * 
   * @param tgtPos
   * @param alignment
   * @return
   */
  public static int findSrcAvgPos(int tgtPos, PhraseAlignment alignment){
    int tgtLength = alignment.size();
    int srcAvgPos = -1;
    int distance = 0;
    
//    System.err.println("findSrcAvgPos tgtPos=" + tgtPos + ", alignment=" + alignment);
    int[] alignments;
    while(true){
      // look right
      int rightPos = tgtPos + distance;
      boolean isStop = true;
      if(rightPos<tgtLength){
        alignments = alignment.t2s(rightPos);
        if (alignments != null) {
//          System.err.print("right " + rightPos + ": " + intArrayToString(alignments));
          srcAvgPos = ArrayMath.mean(alignments);
          break;
        }
        
        isStop = false;
      }
      
      // look left
      int leftPos = tgtPos - distance;
      if(leftPos>=0 && leftPos!=rightPos){
        alignments = alignment.t2s(leftPos);
        if (alignments != null) {
//          System.err.print("left " + leftPos + ": " + intArrayToString(alignments));
          srcAvgPos = ArrayMath.mean(alignments);
          break;
        }
        
        isStop = false;
      }
      
      distance++;
      if (isStop) break;
    }
    
    return srcAvgPos;
  }

  public static void error(boolean cond, String message){
  	if(cond){
  		System.err.println(message);
  		System.exit(1);
  	}
  }
  
  // print double array
  public static String sprint(int[] values){
  	StringBuffer sb = new StringBuffer("[");

  	if(values.length > 0){
  		for(int value : values){
  			sb.append(value + ", ");
  		}
  		sb.delete(sb.length()-2, sb.length());
  	}
  	sb.append("]");
  	return sb.toString();
  }

  //print double array
  public static String sprint(double[] values){
  	StringBuffer sb = new StringBuffer("[");

  	if(values.length > 0){
  		for(double value : values){
  			sb.append(value + ", ");
  		}
  		sb.delete(sb.length()-2, sb.length());
  	}
  	sb.append("]");
  	return sb.toString();
  }

  
  public static Sequence<IString> getIStringSequence(String[] tokens){
  	List<IString> istringList = new ArrayList<IString>();
  	for (String token : tokens) {
			istringList.add(new IString(token));
		}
  	return new SimpleSequence<IString>(istringList);
  }

}
