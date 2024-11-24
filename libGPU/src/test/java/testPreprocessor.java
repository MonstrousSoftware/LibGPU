import com.monstrous.graphics.Preprocessor;
import static org.junit.Assert.*;
import org.junit.Test;

public class testPreprocessor {


    // Note that #if ... #endif may be nested and nested conditionals need to be ignored
    // in the branch that is not taken (except to keep track of the nesting level).
    /*
  if(true)
      y
      if(false)
          n
      else
          y
      endif
   else
      n
      if(true)
          n
      else
          n
      endif
   endif
*/
    @Test
    public void test(){
        Preprocessor preprocessor = new Preprocessor();

        String out = preprocessor.process(
                "abcd\n#define A\n#define B 456\n" +
                        "#ifdef A\nyes1\n" +
                        "#ifdef B\nyes2\n#else\nno\n#endif\nyes3\n" +
                        "#else\nno, inside main else\n" +
                        "#ifdef B\nno\n#else\nno\n#endif\nno\n" +
                        "#endif\n" +
                        "yes4\n");

        assertTrue(out.contentEquals("abcd\n" +
                "yes1\n" +
                "yes2\n" +
                "yes3\n" +
                "yes4\n"));

    }
}
