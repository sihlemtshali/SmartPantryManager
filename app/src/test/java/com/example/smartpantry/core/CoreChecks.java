package com.example.smartpantry.core;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/** Framework-free checks: run directly with a JDK or through MatcherTest in Gradle. */
public final class CoreChecks {
  private static int checks;
  private static final LocalDate TODAY = LocalDate.of(2027, 1, 15);

  private static Ingredient i(String n, String q, String u) {
    return new Ingredient(0, n, new BigDecimal(q), u, null);
  }

  private static Ingredient dated(String q, LocalDate expiry) {
    return new Ingredient(0, "milk", new BigDecimal(q), "ml", expiry);
  }

  private static Recipe r(Ingredient... ingredients) {
    return new Recipe(1, "Test", "", 1, 1, "Test method", Arrays.asList(ingredients));
  }

  private static boolean match(Recipe r, Ingredient... pantry) {
    return RecipeMatcher.canMake(r, Arrays.asList(pantry), TODAY);
  }

  private static void check(boolean ok, String label) {
    if (!ok) throw new AssertionError(label);
    checks++;
    System.out.println("PASS " + label);
  }

  private static void reject(Runnable action, String label) {
    boolean failed = false;
    try {
      action.run();
    } catch (IllegalArgumentException expected) {
      failed = true;
    }
    check(failed, label);
  }

  public static int run() {
    checks = 0;
    Recipe five =
        r(
            i("egg", "2", "each"),
            i("tomato", "2", "each"),
            i("onion", "1", "each"),
            i("olive oil", "15", "ml"),
            i("salt", "1", "g"));
    Ingredient[] four = {
      i("eggs", "2", "each"),
      i("tomatoes", "2", "each"),
      i("onions", "1", "each"),
      i("olive oil", "1", "tbsp")
    };
    check(!match(five, four), "4 of 5 ingredients is NOT a suggestion");
    List<Ingredient> all = new ArrayList<>(Arrays.asList(four));
    all.add(i("salt", "1", "g"));
    check(RecipeMatcher.canMake(five, all, TODAY), "All five requirements satisfied");
    check(
        !match(r(i("milk", "200", "ml")), i("milk", "199", "ml")),
        "Insufficient quantity rejected");
    check(match(r(i("milk", "200", "ml")), i("milk", "200", "ml")), "Exact quantity accepted");
    check(match(r(i("milk", "200", "ml")), i("milk", "201", "ml")), "Surplus quantity accepted");
    check(match(r(i("oats", "500", "g")), i("oats", "0.5", "kg")), "Kilograms converted to grams");
    check(
        match(r(i("oats", "0.5", "kg")), i("oats", "500", "grams")),
        "Grams converted to kilograms");
    check(match(r(i("salt", "1", "g")), i("salt", "1000", "mg")), "Milligrams converted");
    check(match(r(i("milk", "250", "ml")), i("milk", "0.25", "litres")), "Litres converted");
    check(match(r(i("olive oil", "15", "ml")), i("olive oil", "3", "tsp")), "Teaspoons converted");
    check(match(r(i("milk", "250", "ml")), i("milk", "1", "cup")), "Metric cup is 250 ml");
    check(!match(r(i("milk", "250", "ml")), i("milk", "250", "g")), "Mass cannot satisfy volume");
    check(
        !match(r(i("tomato", "2", "each")), i("tomato", "500", "g")), "Mass cannot satisfy count");
    check(match(r(i("egg", "2", "each")), i("eggs", "2", "pieces")), "Piece and plural aliases");
    check(
        !match(r(i("bread", "2", "slice")), i("bread", "2", "each")),
        "Slices cannot equal loaves or each");
    check(match(r(i("garlic", "2", "clove")), i("garlic", "2", "cloves")), "Clove alias");
    check(
        match(r(i("tomato", "2", "each")), i("  TOMATOES  ", "2", "each")),
        "Case whitespace and tomato plural");
    check(
        NameNormalizer.normalize("OLIVE   OIL").equals("olive oil"),
        "Internal whitespace normalised");
    check(NameNormalizer.normalize("yoghurt").equals("yogurt"), "Common spelling alias");
    check(NameNormalizer.normalize("café").equals("cafe"), "Simple accent normalisation");
    check(
        !match(r(i("rice", "100", "g")), i("cooked rice", "100", "g")),
        "Cooked and raw rice stay distinct");
    check(
        !match(r(i("tomato", "2", "each")), i("tomato sauce", "2", "each")),
        "No unsafe substring match");
    check(
        match(r(i("milk", "300", "ml")), i("milk", "100", "ml"), i("milk", "0.2", "l")),
        "Duplicate pantry lots aggregated");
    check(
        !match(r(i("milk", "100", "ml"), i("milk", "100", "ml")), i("milk", "150", "ml")),
        "Duplicate recipe rows do not double-spend pantry");
    check(
        match(r(i("milk", "100", "ml"), i("milk", "100", "ml")), i("milk", "200", "ml")),
        "Aggregated recipe quantities accepted");
    check(
        match(r(i("salt", "0.3", "g")), i("salt", "0.1", "g"), i("salt", "0.2", "g")),
        "Decimal sums are exact");
    check(
        !match(r(i("milk", "100", "ml")), dated("100", TODAY.minusDays(1))),
        "Expired quantity excluded");
    check(match(r(i("milk", "100", "ml")), dated("100", TODAY)), "Expiry today remains usable");
    check(
        match(r(i("milk", "100", "ml")), dated("100", TODAY.plusDays(1))),
        "Future expiry remains usable");
    check(
        !match(
            r(i("milk", "100", "ml")),
            dated("90", TODAY.minusDays(1)),
            dated("10", TODAY.plusDays(1))),
        "Expired lots cannot supplement fresh lots");
    check(!match(r()), "Empty recipe rejected");
    check(
        !match(r(i("milk", "1", "unknown")), i("milk", "1", "ml")),
        "Unsupported recipe unit fails closed");
    check(
        !match(r(i("milk", "0", "ml")), i("milk", "1", "ml")), "Zero recipe requirement rejected");
    check(
        !match(r(i("milk", "1", "ml")), i("milk", "-1", "ml")),
        "Invalid pantry quantity fails closed");
    check(!match(r(i("salt", "1", "g"))), "Salt is not assumed present");
    check(!match(r(i("water", "1", "ml"))), "Water is not assumed present");
    check(InputValidator.name("  Red   onions ").equals("Red onions"), "Name trimmed and spaced");
    reject(() -> InputValidator.name(""), "Blank name rejected");
    reject(() -> InputValidator.name("123"), "Name needs letters");
    reject(
        () -> InputValidator.name(String.join("", Collections.nCopies(61, "a"))),
        "Long name rejected");
    check(InputValidator.quantity("0.000001").signum() > 0, "Small positive quantity accepted");
    check(
        InputValidator.quantity("1000000").intValue() == 1000000,
        "Quantity upper boundary accepted");
    reject(() -> InputValidator.quantity("1000001"), "Quantity above upper boundary rejected");
    reject(() -> InputValidator.quantity("0"), "Zero input rejected");
    reject(() -> InputValidator.quantity("-2"), "Negative input rejected");
    reject(() -> InputValidator.quantity("NaN"), "NaN rejected");
    reject(() -> InputValidator.quantity("1e3"), "Scientific notation rejected");
    reject(() -> InputValidator.quantity("1,5"), "Ambiguous comma input rejected");
    reject(() -> InputValidator.quantity("0.0000001"), "More than six decimals rejected");
    check(InputValidator.expiry("") == null, "Expiry optional");
    check(InputValidator.expiry("2028-02-29").getDayOfMonth() == 29, "Leap day validated");
    reject(() -> InputValidator.expiry("2027-02-29"), "Impossible calendar date rejected");
    reject(() -> InputValidator.expiry("15/01/2027"), "Wrong date format rejected");
    check(
        !NameNormalizer.normalize("chickpeas").equals(NameNormalizer.normalize("canned chickpeas")),
        "Raw and canned chickpeas remain distinct");
    System.out.println(checks + " core checks passed.");
    return checks;
  }

  public static void main(String[] args) {
    run();
  }
}
