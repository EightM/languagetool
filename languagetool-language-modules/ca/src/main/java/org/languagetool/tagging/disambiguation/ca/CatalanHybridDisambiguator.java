/* LanguageTool, a natural language style checker 
 * Copyright (C) 2007 Daniel Naber (http://www.danielnaber.de)
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301
 * USA
 */

package org.languagetool.tagging.disambiguation.ca;

import java.io.IOException;
import java.util.Arrays;

import org.languagetool.AnalyzedSentence;
import org.languagetool.AnalyzedToken;
import org.languagetool.AnalyzedTokenReadings;
import org.languagetool.language.Catalan;
import org.languagetool.tagging.disambiguation.AbstractDisambiguator;
import org.languagetool.tagging.disambiguation.Disambiguator;
import org.languagetool.tagging.disambiguation.MultiWordChunker;
import org.languagetool.tagging.disambiguation.rules.XmlRuleDisambiguator;

/**
 * Hybrid chunker-disambiguator for Catalan
 * 
 * @author Marcin Miłkowski
 */
public class CatalanHybridDisambiguator extends AbstractDisambiguator {

  private final Disambiguator chunker = new MultiWordChunker("/ca/multiwords.txt", true);
  private final Disambiguator disambiguator = new XmlRuleDisambiguator(new Catalan());

  /**
   * Calls two disambiguator classes: (1) a chunker; (2) a rule-based
   * disambiguator.
   */
  @Override
  public final AnalyzedSentence disambiguate(AnalyzedSentence input)
      throws IOException {
    AnalyzedSentence analyzedSentence = chunker.disambiguate(input);
    
    /* Put the results of the MultiWordChunker in a more appropriate and useful way
      <NP..></NP..> becomes NP.. NP..
      <NCMS000></NCMS000> becomes NCMS000 AQ0MS0
      The individual original tags are removed */
        
    AnalyzedTokenReadings[] aTokens = analyzedSentence.getTokens();
    int i=0;
    String POSTag = "";
    String lemma = "";
    String nextPOSTag = "";
    AnalyzedToken analyzedToken = null;
    while (i < aTokens.length) {
      if (!aTokens[i].isWhitespace()) {  
        if (!nextPOSTag.isEmpty()) {
          AnalyzedToken newAnalyzedToken = new AnalyzedToken(aTokens[i].getToken(), nextPOSTag, lemma);
          if (aTokens[i].hasPosTag("</" + POSTag + ">")) {
            nextPOSTag = "";
            lemma = "";
          }
          aTokens[i] = new AnalyzedTokenReadings(aTokens[i], Arrays.asList(newAnalyzedToken),
              "CatalanHybridDisambiguator");
        } else if ((analyzedToken = getMultiWordAnalyzedToken(aTokens[i])) != null) {
          POSTag = analyzedToken.getPOSTag().substring(1, analyzedToken.getPOSTag().length() - 1);
          lemma = analyzedToken.getLemma();
          AnalyzedToken newAnalyzedToken = new AnalyzedToken(analyzedToken.getToken(), POSTag, lemma);
          aTokens[i] = new AnalyzedTokenReadings(aTokens[i], Arrays.asList(newAnalyzedToken), "CATHybridDisamb");
          if (POSTag.startsWith("NC")) {
            nextPOSTag = "AQ0" + POSTag.substring(2, 4) + "0";
          } else {
            nextPOSTag = POSTag;
          }
        }
      }
      i++;
    }

    return disambiguator.disambiguate(new AnalyzedSentence(aTokens));
  }
  
  private AnalyzedToken getMultiWordAnalyzedToken(AnalyzedTokenReadings anTokReadings) {
    for (AnalyzedToken reading : anTokReadings) {
      String POSTag = reading.getPOSTag();
      if (POSTag != null) {
        if (POSTag.startsWith("<") && POSTag.endsWith(">") && !POSTag.startsWith("</")) {
          return reading;
        }
      }
    }
    return null;
    
  }

}
