package tests;

import jolie.runtime.Value;
import jolie.runtime.ValueVector;
import org.junit.jupiter.api.Test;
import src.MatchExpression;
import src.MatchQuery;
import src.UnwindQuery;

import static src.Utils.Constants.*;

class UnwindQueryTest {
    @Test
    void test() {
        Value unwindrequest = Value.create();

        unwindrequest.getNewChild("query").setValue("awards.award");;
        Value data = unwindrequest.getNewChild("data");
        Value name = data.getNewChild("name");
        name.getNewChild("first").setValue("Kristen");
        name.getNewChild("last").setValue("Nyygard");

        Value awards = data.getNewChild("awards");
        awards.getNewChild("award").setValue("Rosing Prize");
        awards.getNewChild("award").setValue("Turing Award");
        awards.getNewChild("award").setValue("IEEE John von Neumann Medal");


        ValueVector result = UnwindQuery.unwind(unwindrequest);
        result.forEach(it -> System.out.println(it.toPrettyString()));
    }
}


