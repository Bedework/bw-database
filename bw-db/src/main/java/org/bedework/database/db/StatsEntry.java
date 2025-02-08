package org.bedework.database.db;

import java.util.Collection;

/** Class to hold a statistics. We build a collection of these.
 * We use Strings for values as these are going to be dumped as xml.
 */
public class StatsEntry {
  // ENUM
  /** */
  public final static int statKindHeader = 0;
  /** */
  public final static int statKindStat = 1;

  private final int statKind;

  private final String statLabel;

  // ENUM
  /** */
  public final static int statTypeString = 0;
  /** */
  public final static int statTypeInt = 1;
  /** */
  public final static int statTypeLong = 2;
  /** */
  public final static int statTypeDouble = 3;
  private int statType;

  private String statVal;

  /** Constructor for an int val
   *
   * @param label of val
   * @param val value
   */
  public StatsEntry(final String label,
                    final int val) {
    statKind = statKindStat;
    statLabel = label;
    statType = statTypeInt;
    statVal = String.valueOf(val);
  }

  /** Constructor for a long val
   *
   * @param label of val
   * @param val value
   */
  public StatsEntry(final String label,
                    final long val) {
    statKind = statKindStat;
    statLabel = label;
    statType = statTypeLong;
    statVal = String.valueOf(val);
  }

  /** Constructor for a double val
   *
   * @param label of val
   * @param val value
   */
  public StatsEntry(final String label,
                    final double val) {
    statKind = statKindStat;
    statLabel = label;
    statType = statTypeDouble;
    statVal = String.valueOf(val);
  }

  /** Constructor for a String val
   *
   * @param label of val
   * @param val value
   */
  public StatsEntry(final String label,
                    final String val) {
    statKind = statKindStat;
    statLabel = label;
    statType = statTypeString;
    statVal = val;
  }

  /** Constructor for a header
   *
   * @param header for output
   */
  public StatsEntry(final String header) {
    statKind = statKindHeader;
    statLabel = header;
  }

  /**
   * @return int kind of stat
   */
  public int getStatKind() {
    return statKind;
  }

  /**
   * @return String label
   */
  public String getStatLabel() {
    return statLabel;
  }

  /**
   * @return int type
   */
  public int getStatType() {
    return statType;
  }

  /**
   * @return String value
   */
  public String getStatVal() {
    return statVal;
  }

  /** Turn the Collection of StatsEntry into a String for dumps.
   *
   * @param c  Collection of StatsEntry
   * @return String formatted result.
   */
  public static String toString(final Collection<StatsEntry> c) {
    final StringBuilder sb = new StringBuilder();

    for (final StatsEntry se: c) {
      final int k = se.getStatKind();

      if (k == StatsEntry.statKindHeader) {
        header(sb, se.getStatLabel());
      } else {
        format(sb, se.getStatLabel(), se.getStatVal());
      }
    }

    return sb.toString();
  }

  private static void header(final StringBuilder sb,
                             final String h) {
    sb.append("\n");
    final int len = padlen - h.length();
    if (len > 0) {
      sb.append(" ".repeat(len));
    }

    sb.append(h)
      .append("\n");
  }

  private static final int maxvalpad = 10;
  private static final int padlen = 40;

  private static void format(final StringBuilder sb,
                             final String name,
                             final String val) {
    final int len = padlen - name.length();
    if (len > 0) {
      sb.append(" ".repeat(len));
    }
    sb.append(name).append(": ");

    final int vlen = maxvalpad - val.length();

    if (vlen > 0) {
      sb.append(" ".repeat(vlen));
    }

    sb.append("\n");
  }
}
