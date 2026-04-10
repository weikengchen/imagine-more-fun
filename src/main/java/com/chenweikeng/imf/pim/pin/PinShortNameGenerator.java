package com.chenweikeng.imf.pim.pin;

import com.chenweikeng.imf.pim.screen.PinDetailHandler;
import com.chenweikeng.imf.pim.screen.PinRarityHandler;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class PinShortNameGenerator {
  private static PinShortNameGenerator INSTANCE = null;
  private final Map<String, Map<String, String>> actualToShort = new TreeMap<>();
  private final Map<String, Map<String, String>> shortToActual = new TreeMap<>();
  private final Map<String, String> seriesActualToShort = new TreeMap<>();
  private final Map<String, String> seriesShortToActual = new TreeMap<>();
  private boolean generated = false;

  private static final Set<String> IGNORED_WORDS = Set.of("the", "pin", "a", "of");

  public static PinShortNameGenerator getInstance() {
    if (INSTANCE == null) {
      INSTANCE = new PinShortNameGenerator();
    }
    return INSTANCE;
  }

  public void generateShortNames() {
    if (generated) {
      return;
    }

    actualToShort.clear();
    shortToActual.clear();
    seriesActualToShort.clear();
    seriesShortToActual.clear();

    generateSeriesShortNames();

    PinRarityHandler rarityHandler = PinRarityHandler.getInstance();
    PinDetailHandler detailHandler = PinDetailHandler.getInstance();

    Set<String> allSeriesNames = rarityHandler.getAllSeriesNames();

    for (String seriesName : allSeriesNames) {
      PinRarityHandler.PinSeriesEntry seriesEntry = rarityHandler.getSeriesEntry(seriesName);

      if (seriesEntry == null
          || seriesEntry.availability != PinRarityHandler.Availability.REQUIRED) {
        continue;
      }

      Map<String, PinDetailHandler.PinDetailEntry> detailMap =
          detailHandler.getSeriesDetails(seriesName);

      if (detailMap == null || detailMap.isEmpty()) {
        continue;
      }

      Map<String, String> seriesActualToShort = new TreeMap<>();
      Map<String, String> seriesShortToActual = new TreeMap<>();

      Set<String> pinNames = detailMap.keySet();
      Set<String> assignedShortNames = new HashSet<>();
      Set<String> remainingPins = new HashSet<>(pinNames);

      int maxWordCount = getMaxPinWordCount(remainingPins);

      for (int wordCount = 1; wordCount <= maxWordCount; wordCount++) {
        Map<String, List<String>> potentialShortNamesFLC = new HashMap<>();

        for (String pinName : remainingPins) {
          String shortName = getShortNameWithFLC(pinName, wordCount);
          if (shortName != null) {
            potentialShortNamesFLC.computeIfAbsent(shortName, k -> new ArrayList<>()).add(pinName);
          }
        }

        Set<String> justAssigned = new HashSet<>();
        for (Map.Entry<String, List<String>> entry : potentialShortNamesFLC.entrySet()) {
          if (entry.getValue().size() == 1) {
            String shortName = entry.getKey();
            String pinName = entry.getValue().get(0);
            if (!assignedShortNames.contains(shortName)) {
              seriesActualToShort.put(pinName, shortName);
              seriesShortToActual.put(shortName, pinName);
              assignedShortNames.add(shortName);
              justAssigned.add(pinName);
            }
          }
        }

        remainingPins.removeAll(justAssigned);

        Map<String, List<String>> potentialShortNamesNoFLC = new HashMap<>();

        for (String pinName : remainingPins) {
          String shortName = getShortName(pinName, wordCount);
          if (shortName != null) {
            potentialShortNamesNoFLC
                .computeIfAbsent(shortName, k -> new ArrayList<>())
                .add(pinName);
          }
        }

        justAssigned = new HashSet<>();
        for (Map.Entry<String, List<String>> entry : potentialShortNamesNoFLC.entrySet()) {
          if (entry.getValue().size() == 1) {
            String shortName = entry.getKey();
            String pinName = entry.getValue().get(0);
            if (!assignedShortNames.contains(shortName)) {
              seriesActualToShort.put(pinName, shortName);
              seriesShortToActual.put(shortName, pinName);
              assignedShortNames.add(shortName);
              justAssigned.add(pinName);
            }
          }
        }

        remainingPins.removeAll(justAssigned);
      }

      for (String pinName : remainingPins) {
        seriesActualToShort.put(pinName, pinName);
        seriesShortToActual.put(pinName, pinName);
      }

      actualToShort.put(seriesName, seriesActualToShort);
      shortToActual.put(seriesName, seriesShortToActual);
    }

    generated = true;
  }

  private String getShortName(String pinName, int wordCount) {
    String[] words = pinName.split(" ");

    if (words.length == 0) {
      return null;
    }

    List<String> filteredWords = new ArrayList<>();
    for (String word : words) {
      if (!IGNORED_WORDS.contains(word.toLowerCase())) {
        filteredWords.add(word);
      }
    }

    if (wordCount > filteredWords.size()) {
      return null;
    }

    StringBuilder shortName = new StringBuilder();

    for (int i = 0; i < wordCount; i++) {
      if (i > 0) {
        shortName.append("-");
      }

      String word = filteredWords.get(i);
      word = word.replaceAll(":+$", "");
      boolean isLastWord = (i == wordCount - 1);

      if (isLastWord) {
        word = word.replaceAll("^\\(+", "");
        word = word.replaceAll("\\)+$", "");
        word = applyFLC(word);
      } else {
        word = word.replaceAll("^\\(+", "");
        word = word.replaceAll("\\)+$", "");
      }

      shortName.append(word);
    }

    return shortName.toString();
  }

  private String getShortNameWithFLC(String pinName, int wordCount) {
    String[] words = pinName.split(" ");

    List<String> filteredWords = new ArrayList<>();
    for (String word : words) {
      if (!IGNORED_WORDS.contains(word.toLowerCase())) {
        filteredWords.add(word);
      }
    }

    StringBuilder shortName = new StringBuilder();

    for (int i = 0; i < Math.min(wordCount, filteredWords.size()); i++) {
      if (i > 0) {
        shortName.append("-");
      }

      String word = filteredWords.get(i);
      word = word.replaceAll(":+$", "");
      boolean isLastWord = (i == Math.min(wordCount, filteredWords.size()) - 1);

      if (isLastWord) {
        word = word.replaceAll("^\\(+", "");
        word = word.replaceAll("\\)+$", "");
        word = applyFLC(word);
      } else {
        word = word.replaceAll("^\\(+", "");
        word = word.replaceAll("\\)+$", "");
      }

      shortName.append(word);
    }

    return shortName.toString();
  }

  private String applyFLC(String word) {
    if (word.length() <= 4) {
      return word;
    }

    String firstLetter = String.valueOf(word.charAt(0));
    String lastLetter = String.valueOf(word.charAt(word.length() - 1));
    String middle = word.substring(1, word.length() - 1);
    middle = middle.replaceAll("[aeiouAEIOU]", "");

    return firstLetter + middle + lastLetter;
  }

  public String getShortName(String seriesName, String actualName) {
    Map<String, String> seriesMap = actualToShort.get(seriesName);
    if (seriesMap == null) {
      return actualName;
    }
    String shortName = seriesMap.get(actualName);
    return shortName != null ? shortName : actualName;
  }

  public String getActualName(String seriesName, String shortName) {
    Map<String, String> seriesMap = shortToActual.get(seriesName);
    if (seriesMap == null) {
      return null;
    }
    return seriesMap.get(shortName);
  }

  public void reset() {
    actualToShort.clear();
    shortToActual.clear();
    seriesActualToShort.clear();
    seriesShortToActual.clear();
    generated = false;
  }

  private void generateSeriesShortNames() {
    PinRarityHandler rarityHandler = PinRarityHandler.getInstance();
    Set<String> allSeriesNames = rarityHandler.getAllSeriesNames();

    Set<String> requiredSeriesNames = new HashSet<>();
    for (String seriesName : allSeriesNames) {
      PinRarityHandler.PinSeriesEntry seriesEntry = rarityHandler.getSeriesEntry(seriesName);
      if (seriesEntry != null
          && seriesEntry.availability == PinRarityHandler.Availability.REQUIRED) {
        requiredSeriesNames.add(seriesName);
      }
    }

    Set<String> assignedShortNames = new HashSet<>();
    Set<String> remainingSeries = new HashSet<>(requiredSeriesNames);

    int maxWordCount = getMaxWordCount(remainingSeries);

    for (int wordCount = 1; wordCount <= maxWordCount; wordCount++) {
      Map<String, List<String>> potentialShortNames = new HashMap<>();

      for (String seriesName : remainingSeries) {
        String shortName = getSeriesShortName(seriesName, wordCount, assignedShortNames);
        if (shortName != null) {
          potentialShortNames.computeIfAbsent(shortName, k -> new ArrayList<>()).add(seriesName);
        }
      }

      Set<String> justAssigned = new HashSet<>();
      for (Map.Entry<String, List<String>> entry : potentialShortNames.entrySet()) {
        if (entry.getValue().size() == 1) {
          String shortName = entry.getKey();
          String seriesName = entry.getValue().get(0);
          if (!assignedShortNames.contains(shortName)) {
            seriesActualToShort.put(seriesName, shortName);
            seriesShortToActual.put(shortName, seriesName);
            assignedShortNames.add(shortName);
            justAssigned.add(seriesName);
          }
        }
      }

      remainingSeries.removeAll(justAssigned);

      if (wordCount == 1) {
        Map<String, List<String>> potentialFirstLastNames = new HashMap<>();

        for (String seriesName : remainingSeries) {
          String shortName = getSeriesFirstLastName(seriesName);
          if (shortName != null) {
            potentialFirstLastNames
                .computeIfAbsent(shortName, k -> new ArrayList<>())
                .add(seriesName);
          }
        }

        justAssigned = new HashSet<>();
        for (Map.Entry<String, List<String>> entry : potentialFirstLastNames.entrySet()) {
          if (entry.getValue().size() == 1) {
            String shortName = entry.getKey();
            String seriesName = entry.getValue().get(0);
            if (!assignedShortNames.contains(shortName)) {
              seriesActualToShort.put(seriesName, shortName);
              seriesShortToActual.put(shortName, seriesName);
              assignedShortNames.add(shortName);
              justAssigned.add(seriesName);
            }
          }
        }

        remainingSeries.removeAll(justAssigned);
      }
    }

    for (String seriesName : remainingSeries) {
      seriesActualToShort.put(seriesName, seriesName);
      seriesShortToActual.put(seriesName, seriesName);
    }
  }

  private int getMaxWordCount(Set<String> seriesNames) {
    int max = 0;
    for (String seriesName : seriesNames) {
      String cleanedName = seriesName.replaceAll("\\bSeries\\b", "").trim();
      String[] words = cleanedName.split("\\s+");
      max = Math.max(max, words.length);
    }
    return max;
  }

  private int getMaxPinWordCount(Set<String> pinNames) {
    int max = 0;
    for (String pinName : pinNames) {
      String[] words = pinName.split(" ");
      int filteredCount = 0;
      for (String word : words) {
        if (!IGNORED_WORDS.contains(word.toLowerCase())) {
          filteredCount++;
        }
      }
      max = Math.max(max, filteredCount);
    }
    return max;
  }

  private String getSeriesShortName(
      String seriesName, int wordCount, Set<String> assignedShortNames) {
    String cleanedName = seriesName.replaceAll("\\bSeries\\b", "").trim();
    String[] words = cleanedName.split("\\s+");

    if (words.length == 0) {
      return null;
    }

    if (wordCount == 1) {
      return cleanWord(words[0]);
    }

    if (wordCount <= words.length) {
      StringBuilder shortName = new StringBuilder();
      for (int i = 0; i < wordCount; i++) {
        if (i > 0) {
          shortName.append("-");
        }
        shortName.append(cleanWord(words[i]));
      }
      String result = shortName.toString();

      if (wordCount == 2 && assignedShortNames.contains(result)) {
        return null;
      }

      return result;
    }

    return null;
  }

  private String getSeriesFirstLastName(String seriesName) {
    String cleanedName = seriesName.replaceAll("\\bSeries\\b", "").trim();
    String[] words = cleanedName.split("\\s+");

    if (words.length < 2) {
      return null;
    }

    String firstWord = cleanWord(words[0]);
    String lastWord = cleanWord(words[words.length - 1]);
    return firstWord + "-" + lastWord;
  }

  private String cleanWord(String word) {
    word = word.replaceAll(":+$", "");
    word = word.replaceAll("^\\(+", "");
    word = word.replaceAll("\\)+$", "");
    return word;
  }

  public String getSeriesShortName(String actualName) {
    String shortName = seriesActualToShort.get(actualName);
    return shortName != null ? shortName : actualName;
  }

  public String getSeriesActualName(String shortName) {
    return seriesShortToActual.get(shortName);
  }
}
