package six42.fitnesse.jdbcslim;

public class HeaderCell {
  private String sortType;
  private int sortIndex;
  private int sortDirection;

  public HeaderCell(String sortType, int sortIndex, int sortDirection) {
    super();
    this.sortType = sortType;
    this.sortIndex = sortIndex;
    this.sortDirection = sortDirection;
  }

  public String getSortType() {
    return sortType;
  }

  public int getSortIndex() {
    return sortIndex;
  }

  public int getSortDirection() {
    return sortDirection;
  }


}
