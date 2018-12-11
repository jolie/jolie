package tests;

import jolie.runtime.Value;
import org.junit.jupiter.api.Test;
import src.MatchExpression;
import src.MatchQuery;

import static src.Utils.Constants.*;

public class MatchQueryTest {
    private Value data;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        Value data = Value.create();
        Value awards = data.getNewChild("awards");
        awards.getNewChild("award").setValue("Rosing Prize");
        awards.getNewChild("year").setValue("1999");

        this.data = data;
    }

    @Test
    void testExists() {
        Value query = Value.create();
        query.getNewChild(exists).setValue("awards.award");

        MatchExpression matchExpression = MatchQuery.createMatchExpression(query);
        System.out.println(data.toPrettyString());
        assert  (matchExpression.interpret(data).isPresent());
    }

    @Test
    void testNotExists1() {
        Value query = Value.create();
        query.getNewChild(exists).setValue("awards.award.oops");

        MatchExpression matchExpression = MatchQuery.createMatchExpression(query);
        System.out.println(data.toPrettyString());
        assert  (!matchExpression.interpret(data).isPresent());
    }

    @Test
    void testNotExists2() {
        Value query = Value.create();
        query.getNewChild(exists).setValue("awards.oops");

        MatchExpression matchExpression = MatchQuery.createMatchExpression(query);
        assert  (!matchExpression.interpret(data).isPresent());
    }

    @Test
    void testExistsIncompletePath() {
        Value query = Value.create();
        query.getNewChild(exists).setValue("awards");

        MatchExpression matchExpression = MatchQuery.createMatchExpression(query);
    }

    @Test
    void testEqual() {
        Value query = Value.create();
        Value eql = query.getNewChild(equal);
        eql.getNewChild(path).setValue("awards.award");
        eql.getNewChild(val).setValue("Rosing Prize");

        MatchExpression matchExpression = MatchQuery.createMatchExpression(query);
        assert  (matchExpression.interpret(data).isPresent());
    }

    @Test
    void testNotEqual() {
        Value query = Value.create();
        Value eql = query.getNewChild(equal);
        eql.getNewChild(path).setValue("awards.award");
        eql.getNewChild(val).setValue("Turing Award");

        MatchExpression matchExpression = MatchQuery.createMatchExpression(query);
        assert  (!matchExpression.interpret(data).isPresent());
    }

    @Test
    void testOr() {
        Value query = Value.create();
        Value ore = query.getNewChild(or);
        Value eql = ore.getNewChild(left).getNewChild(equal);
        eql.getNewChild(path).setValue("awards.award");
        eql.getNewChild(val).setValue("Rosing Prize");
        ore.getNewChild(right).getNewChild(exists).setValue("awards.yeah");

        MatchExpression matchExpression = MatchQuery.createMatchExpression(query);
        assert  (matchExpression.interpret(data).isPresent());
    }

    @Test
    void testOrNegative() {
        Value query = Value.create();
        Value ore = query.getNewChild(or);
        Value eql = ore.getNewChild(left).getNewChild(equal);
        eql.getNewChild(path).setValue("awards.award");
        eql.getNewChild(val).setValue("Turing award");
        ore.getNewChild(right).getNewChild(exists).setValue("awards.yeah");

        MatchExpression matchExpression = MatchQuery.createMatchExpression(query);
        assert  (!matchExpression.interpret(data).isPresent());
    }

    @Test
    void testAnd() {
        Value query = Value.create();
        Value ore = query.getNewChild(and);
        Value eql = ore.getNewChild(left).getNewChild(equal);
        eql.getNewChild(path).setValue("awards.award");
        eql.getNewChild(val).setValue("Rosing Prize");
        ore.getNewChild(right).getNewChild(exists).setValue("awards.year");

        MatchExpression matchExpression = MatchQuery.createMatchExpression(query);
        assert  (matchExpression.interpret(data).isPresent());
    }

    @Test
    void testAndNegative() {
        Value query = Value.create();
        Value ore = query.getNewChild(and);
        Value eql = ore.getNewChild(left).getNewChild(equal);
        eql.getNewChild(path).setValue("awards.award");
        eql.getNewChild(val).setValue("Turing award");
        ore.getNewChild(right).getNewChild(exists).setValue("awards.year");

        MatchExpression matchExpression = MatchQuery.createMatchExpression(query);
        assert  (!matchExpression.interpret(data).isPresent());
    }

    @Test
    void testNot() {
        Value query = Value.create();
        Value note = query.getNewChild(not);
        Value ore = note.getNewChild(and);
        Value eql = ore.getNewChild(left).getNewChild(equal);
        eql.getNewChild(path).setValue("awards.award");
        eql.getNewChild(val).setValue("Turing award");
        ore.getNewChild(right).getNewChild(exists).setValue("awards.year");

        MatchExpression matchExpression = MatchQuery.createMatchExpression(query);
        assert  (matchExpression.interpret(data).isPresent());
    }

    @Test
    void testGreaterThenInt() {
        Value query = Value.create();
        Value great = query.getNewChild(greaterThen);
        great.getNewChild(path).setValue("awards.year");
        great.getNewChild(val).setValue("1950");

        MatchExpression matchExpression = MatchQuery.createMatchExpression(query);
        assert  (matchExpression.interpret(data).isPresent());
    }

    @Test
    void testGreaterThenIntNegative() {
        Value query = Value.create();
        Value great = query.getNewChild(greaterThen);
        great.getNewChild(path).setValue("awards.year");
        great.getNewChild(val).setValue("2050");

        MatchExpression matchExpression = MatchQuery.createMatchExpression(query);
        assert  (!matchExpression.interpret(data).isPresent());
    }

    @Test
    void testGreaterThenString() {
        Value query = Value.create();
        Value great = query.getNewChild(greaterThen);
        great.getNewChild(path).setValue("awards.award");
        great.getNewChild(val).setValue("superprize");

        MatchExpression matchExpression = MatchQuery.createMatchExpression(query);
        assert  (matchExpression.interpret(data).isPresent());
    }

    @Test
    void testGreaterThenStringNegative() {
        Value query = Value.create();
        Value great = query.getNewChild(greaterThen);
        great.getNewChild(path).setValue("awards.award");
        great.getNewChild(val).setValue("IEEE John von Neumann Medal");

        MatchExpression matchExpression = MatchQuery.createMatchExpression(query);
        assert  (!matchExpression.interpret(data).isPresent());
    }

    @Test
    void testLowerThenInt() {
        Value query = Value.create();
        Value great = query.getNewChild(lowerThen);
        great.getNewChild(path).setValue("awards.year");
        great.getNewChild(val).setValue("1950");

        MatchExpression matchExpression = MatchQuery.createMatchExpression(query);
        assert  (!matchExpression.interpret(data).isPresent());
    }

    @Test
    void testLowerThenIntNegative() {
        Value query = Value.create();
        Value great = query.getNewChild(lowerThen);
        great.getNewChild(path).setValue("awards.year");
        great.getNewChild(val).setValue("2050");

        MatchExpression matchExpression = MatchQuery.createMatchExpression(query);
        assert  (matchExpression.interpret(data).isPresent());
    }

    @Test
    void testLowerThenString() {
        Value query = Value.create();
        Value great = query.getNewChild(lowerThen);
        great.getNewChild(path).setValue("awards.award");
        great.getNewChild(val).setValue("superprize");

        MatchExpression matchExpression = MatchQuery.createMatchExpression(query);
        assert  (!matchExpression.interpret(data).isPresent());
    }

    @Test
    void testLowerThenStringNegative() {
        Value query = Value.create();
        Value great = query.getNewChild(lowerThen);
        great.getNewChild(path).setValue("awards.award");
        great.getNewChild(val).setValue("IEEE John von Neumann Medal");

        MatchExpression matchExpression = MatchQuery.createMatchExpression(query);
        assert  (matchExpression.interpret(data).isPresent());
    }
}


