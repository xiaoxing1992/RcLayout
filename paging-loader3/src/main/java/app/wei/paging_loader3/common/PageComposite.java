package app.wei.paging_loader3.common;

public class PageComposite<T> {

   public T data;
   public String cursor;
   public boolean hasMore;

   private PageComposite() {
   }

   public PageComposite(T data, String cursor, boolean hasMore) {
      this.data = data;
      this.cursor = cursor;
      this.hasMore = hasMore;
   }
}
