package noppes.mpm.util;

public class MpmException extends RuntimeException {
   public MpmException(String message, Object... obs) {
      super(String.format(message, obs));
   }

   public MpmException(Exception ex, String message, Object... obs) {
      super(String.format(message, obs), ex);
   }
}
